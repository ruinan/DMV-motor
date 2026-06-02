package com.dmvmotor.api.mockexam.application;

import org.springframework.stereotype.Component;

/**
 * Mock-exam scoring + pass/fail rules.
 *
 * <p>Extracted from {@link MockExamService} (dev-audit #4) so the rules are a
 * named, directly unit-tested unit instead of being tangled with attempt
 * orchestration. The pass threshold is no longer a constant here — it lives on
 * the exam ({@code exams.pass_threshold_percent}, V26) so each
 * state × license type can set its own standard, and is passed in per attempt.
 */
@Component
public class MockScoringPolicy {

    /**
     * Maximum wrong answers tolerated before the attempt auto-fails, given the
     * exam's pass threshold (as a percent, e.g. 85). A 30-question exam at 85%
     * tolerates {@code ceil(30 × 0.15) = 5} wrong; on the 6th wrong it fails.
     */
    public int maxAllowedWrong(int totalQuestions, int passThresholdPercent) {
        return (int) Math.ceil(totalQuestions * (1 - passThresholdPercent / 100.0));
    }

    /** Score as a 0–100 percentage; an empty exam scores 0 (no divide-by-zero). */
    public int scorePercent(int correctCount, int totalQuestions) {
        if (totalQuestions == 0) return 0;
        return (int) Math.round(100.0 * correctCount / totalQuestions);
    }
}
