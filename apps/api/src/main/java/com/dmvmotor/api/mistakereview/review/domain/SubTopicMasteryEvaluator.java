package com.dmvmotor.api.mistakereview.review.domain;

import com.dmvmotor.api.mistakereview.config.MasteryProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sub-topic mastery evaluation, parallel to {@link MasteryEvaluator} but with
 * a smaller recent-window (4 vs 8) because sub-topics are sparser. Same
 * two-gate shape — overall rate ≥80% AND recent attempts ≥3 of last 4 correct.
 *
 * <p>Insufficient history (fewer than {@code recentWindow} attempts) defaults
 * to NOT mastered.
 */
@Component
public class SubTopicMasteryEvaluator {

    private final MasteryProperties.Subtopic props;

    public SubTopicMasteryEvaluator(MasteryProperties props) {
        this.props = props.subtopic();
    }

    public int recentWindow() {
        return props.recentWindow();
    }

    public boolean isMastered(SubTopicStats stats, List<Boolean> recentAttempts) {
        if (stats.total() == 0) return false;
        if (recentAttempts.size() < props.recentWindow()) return false;

        int correctRate = stats.correct() * 100 / stats.total();
        if (correctRate < props.correctRateThreshold()) return false;

        long recentCorrect = recentAttempts.stream().filter(Boolean.TRUE::equals).count();
        return recentCorrect >= props.recentCorrectThreshold();
    }

    public record SubTopicStats(int total, int correct) {}
}
