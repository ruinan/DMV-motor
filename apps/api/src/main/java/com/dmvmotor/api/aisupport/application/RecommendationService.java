package com.dmvmotor.api.aisupport.application;

import com.dmvmotor.api.aisupport.infrastructure.RecommendationRepository;
import com.dmvmotor.api.aisupport.infrastructure.RecommendationRepository.TopicMistakeCount;
import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.content.application.ExamContext;
import com.dmvmotor.api.content.domain.Topic;
import com.dmvmotor.api.content.infrastructure.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Deterministic "what to reinforce next" recommendation (mvp.md §5 #10).
 *
 * <p>Proactive and cross-session — complements {@code /ai/explain} (per-mistake
 * reason) and {@code /ai/review-plan} (post-mock plan), and surfaces, at the
 * topic level, the same weakness-first signal the practice selector applies per
 * question. Ranking, highest priority first:
 * <ol>
 *   <li>topics with the most active mistakes (clear your weak spots);</li>
 *   <li>key topics not yet practiced this cycle (cover the must-knows).</li>
 * </ol>
 *
 * <p>Reasons are returned as a {@code reasonCode} + count so the client
 * localises them; an optional LLM-phrased reason can layer on later (§34-B)
 * without changing this ranking.
 */
@Service
public class RecommendationService {

    private static final int MAX_LIMIT = 8;

    private final RecommendationRepository recRepo;
    private final TopicRepository          topicRepo;
    private final UserRepository           userRepo;
    private final ExamContext              examContext;
    private final AccessService            accessService;

    public RecommendationService(RecommendationRepository recRepo,
                                 TopicRepository topicRepo,
                                 UserRepository userRepo,
                                 ExamContext examContext,
                                 AccessService accessService) {
        this.recRepo   = recRepo;
        this.topicRepo = topicRepo;
        this.userRepo  = userRepo;
        this.examContext = examContext;
        this.accessService = accessService;
    }

    public List<Recommendation> recommend(Long userId, String language, int requestedLimit) {
        Long examId = examContext.resolveExamId(userId);
        // The next-step recommendation is a paid perk (bug4: free users don't get
        // it — they just practice random). Backend-enforced, so it's not merely
        // hidden in the UI. Empty for users without an active pass.
        if (!accessService.getAccess(userId, examId).hasActivePass()) {
            return List.of();
        }
        int limit = Math.min(Math.max(requestedLimit, 1), MAX_LIMIT);
        int cycle = userRepo.findById(userId)
                .map(UserRepository.UserRow::resetCount).orElse(0);

        // Recommend only within the user's current exam taxonomy. Mistake-topic
        // ids from other exams (shouldn't occur for a single-exam user) fall out
        // naturally via the byId lookup below.
        List<Topic> topics = topicRepo.findByExam(examId);
        Map<Long, Topic> byId = topics.stream()
                .collect(Collectors.toMap(Topic::id, Function.identity()));

        List<TopicMistakeCount> mistakeCounts =
                recRepo.activeMistakeCountsByTopic(userId, cycle);
        Set<Long> covered = recRepo.coveredTopicIds(userId, cycle);

        List<Recommendation> recs = new ArrayList<>();
        Set<Long> used = new HashSet<>();

        // 1 — weak topics, most active mistakes first.
        for (TopicMistakeCount tc : mistakeCounts) {
            if (recs.size() >= limit) break;
            Topic t = byId.get(tc.topicId());
            if (t == null) continue;  // orphaned topic id, skip defensively
            recs.add(new Recommendation(tc.topicId(), label(t, language),
                    "active_mistakes", tc.count()));
            used.add(tc.topicId());
        }

        // 2 — key topics not yet covered this cycle, in sort order, fill the rest.
        for (Topic t : topics) {
            if (recs.size() >= limit) break;
            if (t.isKeyTopic() && !covered.contains(t.id()) && !used.contains(t.id())) {
                recs.add(new Recommendation(t.id(), label(t, language),
                        "uncovered_key_topic", 0));
                used.add(t.id());
            }
        }

        return recs;
    }

    private static String label(Topic t, String language) {
        return "zh".equalsIgnoreCase(language) ? t.nameZh() : t.nameEn();
    }

    public record Recommendation(Long topicId, String label, String reasonCode, int mistakeCount) {}
}
