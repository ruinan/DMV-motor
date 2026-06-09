package com.dmvmotor.api.progressreadiness.application;

import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.content.application.ExamContext;
import com.dmvmotor.api.content.domain.SubTopic;
import com.dmvmotor.api.content.domain.Topic;
import com.dmvmotor.api.content.infrastructure.SubTopicRepository;
import com.dmvmotor.api.content.infrastructure.TopicRepository;
import com.dmvmotor.api.mistakereview.config.MasteryProperties;
import com.dmvmotor.api.mistakereview.review.domain.MasteryEvaluator;
import com.dmvmotor.api.mistakereview.review.domain.MasteryEvaluator.TopicStats;
import com.dmvmotor.api.mistakereview.review.domain.SubTopicMasteryEvaluator;
import com.dmvmotor.api.mistakereview.review.domain.SubTopicMasteryEvaluator.SubTopicStats;
import com.dmvmotor.api.mistakereview.review.infrastructure.PracticeHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the Study Hub mastery donut payload — for each topic, every
 * sub-topic's mastery state plus a parent "is_mastered" derived from
 * "all children mastered". Designed for {@code GET /api/v1/topics/mastery}.
 */
@Service
public class MasteryViewService {

    private final TopicRepository topicRepository;
    private final SubTopicRepository subTopicRepository;
    private final PracticeHistoryRepository historyRepository;
    private final SubTopicMasteryEvaluator evaluator;
    private final MasteryEvaluator topicEvaluator;
    private final MasteryProperties masteryProps;
    private final UserRepository userRepository;
    private final ExamContext examContext;

    public MasteryViewService(
            TopicRepository topicRepository,
            SubTopicRepository subTopicRepository,
            PracticeHistoryRepository historyRepository,
            SubTopicMasteryEvaluator evaluator,
            MasteryEvaluator topicEvaluator,
            MasteryProperties masteryProps,
            UserRepository userRepository,
            ExamContext examContext) {
        this.topicRepository = topicRepository;
        this.subTopicRepository = subTopicRepository;
        this.historyRepository = historyRepository;
        this.evaluator = evaluator;
        this.topicEvaluator = topicEvaluator;
        this.masteryProps = masteryProps;
        this.userRepository = userRepository;
        this.examContext = examContext;
    }

    public TopicMasteryView build(Long userId) {
        int cycle = userRepository.findById(userId)
                .map(UserRepository.UserRow::resetCount)
                .orElse(0);

        // Donut reflects the user's current exam's taxonomy only.
        Long examId = examContext.resolveExamId(userId);

        Map<Long, Integer> bankSizes = subTopicRepository.countActiveQuestionsBySubTopic();

        List<Topic> topics = topicRepository.findByExam(examId);
        List<SubTopic> subTopics = subTopicRepository.findByExam(examId);

        Map<Long, List<SubTopic>> subTopicsByParent = new LinkedHashMap<>();
        for (SubTopic st : subTopics) {
            subTopicsByParent.computeIfAbsent(st.parentTopicId(), k -> new ArrayList<>()).add(st);
        }

        List<TopicView> topicViews = new ArrayList<>();
        int masteredSubTopicCount = 0;

        for (Topic topic : topics) {
            List<SubTopic> children = subTopicsByParent.getOrDefault(topic.id(), List.of());
            List<SubTopicView> subTopicViews = new ArrayList<>();
            boolean topicMastered = !children.isEmpty();
            for (SubTopic st : children) {
                SubTopicStats stats = historyRepository.subTopicStats(userId, st.id(), cycle);
                List<Boolean> recent = historyRepository.lastNAttemptsForSubTopic(
                        userId, st.id(), cycle, evaluator.recentWindow());
                boolean mastered = evaluator.isMastered(stats, recent);
                int bank = bankSizes.getOrDefault(st.id(), 0);
                subTopicViews.add(new SubTopicView(
                        st.id(), st.code(), st.nameEn(), st.nameZh(),
                        mastered, stats.total(), stats.correct(), bank));
                if (mastered) masteredSubTopicCount++;
                else topicMastered = false;
            }
            // Topic-level mastery PROGRESS — the same gate that clears this
            // topic's mistakes in the live practice flow (PracticeService).
            // Surfaced so the Study Hub can show "how far to clearing this
            // topic" instead of a silent stuck state.
            TopicStats tStats = historyRepository.topicStats(userId, topic.id(), cycle);
            List<Boolean> tRecent = historyRepository.lastNAttemptsForTopic(
                    userId, topic.id(), cycle, topicEvaluator.recentWindow());
            TopicMasteryProgress progress = topicProgress(tStats, tRecent);
            topicViews.add(new TopicView(
                    topic.id(), topic.code(), topic.nameEn(), topic.nameZh(),
                    topicMastered, subTopicViews, progress));
        }

        return new TopicMasteryView(topicViews, subTopics.size(), masteredSubTopicCount);
    }

    /**
     * Progress toward the topic-level mastery gate (rate ≥ threshold AND ≥K of
     * last N correct). progressPercent is the binding constraint as a percent:
     * the min of (accuracy / threshold), (recent-correct / K), and (recent
     * attempts / N) — so a user short on accuracy, recent hits, OR sheer
     * practice volume sees exactly how far they are. 100 once mastered.
     */
    private TopicMasteryProgress topicProgress(TopicStats stats, List<Boolean> recent) {
        int window          = masteryProps.recentWindow();
        int accThreshold    = masteryProps.topicCorrectRateThreshold();
        int recentThreshold = masteryProps.recentCorrectThreshold();

        int accuracyPct   = stats.total() > 0 ? Math.round(stats.correct() * 100f / stats.total()) : 0;
        int recentCorrect = (int) recent.stream().filter(Boolean.TRUE::equals).count();

        int progress;
        if (topicEvaluator.isMastered(stats, recent)) {
            progress = 100;
        } else {
            // Thresholds come from config with sensible non-zero defaults
            // (80 / 6 / 8); double division keeps this safe even if misconfigured.
            double g1 = Math.min(1.0, (double) accuracyPct   / accThreshold);
            double g2 = Math.min(1.0, (double) recentCorrect / recentThreshold);
            double g3 = Math.min(1.0, (double) recent.size() / window);
            progress = (int) Math.round(Math.min(g1, Math.min(g2, g3)) * 100);
        }
        return new TopicMasteryProgress(stats.total(), accuracyPct, recentCorrect,
                window, accThreshold, recentThreshold, progress);
    }

    public record TopicMasteryView(
            List<TopicView> topics,
            int totalSubTopics,
            int masteredSubTopics
    ) {}

    public record TopicView(
            Long topicId,
            String code,
            String nameEn,
            String nameZh,
            boolean isMastered,
            List<SubTopicView> subTopics,
            TopicMasteryProgress masteryProgress
    ) {}

    /** How close a topic is to the mastery gate that clears its mistakes. */
    public record TopicMasteryProgress(
            int attempted,
            int accuracyPercent,
            int recentCorrect,
            int recentWindow,
            int accuracyThreshold,
            int recentCorrectThreshold,
            int progressPercent
    ) {}

    public record SubTopicView(
            Long subTopicId,
            String code,
            String nameEn,
            String nameZh,
            boolean isMastered,
            int attemptedCount,
            int correctCount,
            int bankSize
    ) {}
}
