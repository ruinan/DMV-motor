package com.dmvmotor.api.mistakereview.review.domain;

import com.dmvmotor.api.mistakereview.config.MasteryProperties;
import com.dmvmotor.api.mistakereview.review.domain.SubTopicMasteryEvaluator.SubTopicStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubTopicMasteryEvaluatorTest {

    private final MasteryProperties props = new MasteryProperties(80, 8, 6,
            new MasteryProperties.Subtopic(80, 4, 3));
    private final SubTopicMasteryEvaluator evaluator = new SubTopicMasteryEvaluator(props);

    @Test
    void mastered_whenBothGatesPass() {
        // 5 attempts, 4 correct → 80% ≥ 80%; last 4 = 4 correct → 4 ≥ 3
        SubTopicStats stats = new SubTopicStats(5, 4);
        List<Boolean> recent = List.of(true, true, true, true);
        assertThat(evaluator.isMastered(stats, recent)).isTrue();
    }

    @Test
    void mastered_atExactThresholds() {
        // 5 attempts 4 correct = 80%; recent 4 with exactly 3 correct
        SubTopicStats stats = new SubTopicStats(5, 4);
        List<Boolean> recent = List.of(true, true, true, false);
        assertThat(evaluator.isMastered(stats, recent)).isTrue();
    }

    @Test
    void notMastered_whenOverallBelowRate() {
        // 5 attempts, 3 correct = 60% < 80%
        SubTopicStats stats = new SubTopicStats(5, 3);
        List<Boolean> recent = List.of(true, true, true, true);
        assertThat(evaluator.isMastered(stats, recent)).isFalse();
    }

    @Test
    void notMastered_whenRecentBelowThreshold() {
        // overall 90% but recent 4 only 2 correct → fails recent gate
        SubTopicStats stats = new SubTopicStats(10, 9);
        List<Boolean> recent = List.of(true, true, false, false);
        assertThat(evaluator.isMastered(stats, recent)).isFalse();
    }

    @Test
    void notMastered_whenInsufficientHistory() {
        // Only 3 recent attempts < window 4
        SubTopicStats stats = new SubTopicStats(3, 3);
        List<Boolean> recent = List.of(true, true, true);
        assertThat(evaluator.isMastered(stats, recent)).isFalse();
    }

    @Test
    void notMastered_whenNoAttempts() {
        SubTopicStats stats = new SubTopicStats(0, 0);
        assertThat(evaluator.isMastered(stats, List.of())).isFalse();
    }

    @Test
    void recentWindow_returnsConfiguredValue() {
        assertThat(evaluator.recentWindow()).isEqualTo(4);
    }
}
