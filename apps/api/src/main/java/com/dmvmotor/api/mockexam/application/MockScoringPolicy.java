package com.dmvmotor.api.mockexam.application;

import org.springframework.stereotype.Component;

/**
 * Mock-exam scoring + pass/fail rules (docs/parameters.md: 85% pass threshold).
 *
 * <p>Extracted from {@link MockExamService} (dev-audit #4) so the rules are a
 * named, directly unit-tested unit instead of being tangled with attempt
 * orchestration. The threshold stays a constant here (not an env property) so
 * unit-test overrides land without a config round-trip.
 */
@Component
public class MockScoringPolicy {

    /**
     * Pass threshold per docs/parameters.md. A question is part of a fixed-size
     * exam; anything strictly above the resulting wrong cap auto-terminates the
     * attempt. A 30-question exam tolerates {@code ceil(30 × 0.15) = 5} wrong;
     * on the 6th wrong it fails.
     */
    private static final double PASS_THRESHOLD = 0.85;

    /** Maximum wrong answers tolerated before the attempt auto-fails. */
    public int maxAllowedWrong(int totalQuestions) {
        return (int) Math.ceil(totalQuestions * (1 - PASS_THRESHOLD));
    }

    /** Score as a 0–100 percentage; an empty exam scores 0 (no divide-by-zero). */
    public int scorePercent(int correctCount, int totalQuestions) {
        if (totalQuestions == 0) return 0;
        return (int) Math.round(100.0 * correctCount / totalQuestions);
    }
}
