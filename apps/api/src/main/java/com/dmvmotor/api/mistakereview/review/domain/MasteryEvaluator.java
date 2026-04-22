package com.dmvmotor.api.mistakereview.review.domain;

import com.dmvmotor.api.mistakereview.config.MasteryProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Evaluates whether a user has achieved mastery of a topic, per
 * {@code docs/parameters.md} mastery_threshold (MVP subset).
 *
 * <p>Two gates (both must pass):
 * <ol>
 *   <li>Topic correctness rate ≥ {@link MasteryProperties#topicCorrectRateThreshold()} percent
 *   <li>Last N attempts (N = {@link MasteryProperties#recentWindow()}) have
 *       ≥ {@link MasteryProperties#recentCorrectThreshold()} correct
 * </ol>
 *
 * <p>Insufficient history (fewer than N attempts) is not enough evidence — the user
 * has not yet demonstrated mastery, so the answer is {@code false}.
 */
@Component
public class MasteryEvaluator {

    private final MasteryProperties props;

    public MasteryEvaluator(MasteryProperties props) {
        this.props = props;
    }

    /** Window size callers should pass to {@code #isMastered} — so history reads match. */
    public int recentWindow() {
        return props.recentWindow();
    }

    public boolean isMastered(TopicStats stats, List<Boolean> recentAttempts) {
        if (stats.total() == 0) return false;
        if (recentAttempts.size() < props.recentWindow()) return false;

        int correctRate = stats.correct() * 100 / stats.total();
        if (correctRate < props.topicCorrectRateThreshold()) return false;

        long recentCorrect = recentAttempts.stream().filter(Boolean.TRUE::equals).count();
        return recentCorrect >= props.recentCorrectThreshold();
    }

    public record TopicStats(int total, int correct) {}
}
