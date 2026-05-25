package com.dmvmotor.api.progressreadiness.application;

import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.content.domain.SubTopic;
import com.dmvmotor.api.content.domain.Topic;
import com.dmvmotor.api.content.infrastructure.SubTopicRepository;
import com.dmvmotor.api.content.infrastructure.TopicRepository;
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
    private final UserRepository userRepository;

    public MasteryViewService(
            TopicRepository topicRepository,
            SubTopicRepository subTopicRepository,
            PracticeHistoryRepository historyRepository,
            SubTopicMasteryEvaluator evaluator,
            UserRepository userRepository) {
        this.topicRepository = topicRepository;
        this.subTopicRepository = subTopicRepository;
        this.historyRepository = historyRepository;
        this.evaluator = evaluator;
        this.userRepository = userRepository;
    }

    public TopicMasteryView build(Long userId) {
        int cycle = userRepository.findById(userId)
                .map(UserRepository.UserRow::resetCount)
                .orElse(0);

        Map<Long, Integer> bankSizes = subTopicRepository.countActiveQuestionsBySubTopic();

        List<Topic> topics = topicRepository.findAllOrderBySortOrder();
        List<SubTopic> subTopics = subTopicRepository.findAllOrderBySortOrder();

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
            topicViews.add(new TopicView(
                    topic.id(), topic.code(), topic.nameEn(), topic.nameZh(),
                    topicMastered, subTopicViews));
        }

        return new TopicMasteryView(topicViews, subTopics.size(), masteredSubTopicCount);
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
            List<SubTopicView> subTopics
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
