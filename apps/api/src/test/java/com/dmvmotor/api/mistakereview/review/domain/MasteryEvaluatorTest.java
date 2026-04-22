package com.dmvmotor.api.mistakereview.review.domain;

import com.dmvmotor.api.mistakereview.config.MasteryProperties;
import com.dmvmotor.api.mistakereview.review.domain.MasteryEvaluator.TopicStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MasteryEvaluatorTest {

    private final MasteryProperties props = new MasteryProperties(80, 8, 6);
    private final MasteryEvaluator evaluator = new MasteryEvaluator(props);

    @Test
    void masteryReached_whenBothGatesPass() {
        // 10 attempts, 9 correct → 90% ≥ 80%; last 8 all true → 8 ≥ 6
        TopicStats stats = new TopicStats(10, 9);
        List<Boolean> recent = List.of(true, true, true, true, true, true, true, true);
        assertThat(evaluator.isMastered(stats, recent)).isTrue();
    }

    @Test
    void mastered_atExactThresholds() {
        // Spec allows ≥80% and ≥6; boundary must be inclusive
        TopicStats stats = new TopicStats(10, 8);
        List<Boolean> recent = List.of(true, true, true, true, true, true, false, false);
        assertThat(evaluator.isMastered(stats, recent)).isTrue();
    }

    @Test
    void notMastered_whenTopicCorrectnessBelowThreshold() {
        // 10 attempts, 7 correct → 70% < 80%; recent window alone is strong but overall weak
        TopicStats stats = new TopicStats(10, 7);
        List<Boolean> recent = List.of(true, true, true, true, true, true, true, true);
        assertThat(evaluator.isMastered(stats, recent)).isFalse();
    }

    @Test
    void notMastered_whenRecentWindowBelowThreshold() {
        // Overall 90% but recent 8 only 5 correct → 5 < 6
        TopicStats stats = new TopicStats(20, 18);
        List<Boolean> recent = List.of(false, false, false, true, true, true, true, true);
        assertThat(evaluator.isMastered(stats, recent)).isFalse();
    }

    @Test
    void notMastered_whenInsufficientAttempts() {
        // Recent window must contain N entries — 5 < 8 means not enough evidence
        TopicStats stats = new TopicStats(5, 5);
        List<Boolean> recent = List.of(true, true, true, true, true);
        assertThat(evaluator.isMastered(stats, recent)).isFalse();
    }

    @Test
    void notMastered_whenNoAttempts() {
        TopicStats stats = new TopicStats(0, 0);
        List<Boolean> recent = List.of();
        assertThat(evaluator.isMastered(stats, recent)).isFalse();
    }
}
