package com.dmvmotor.api.mockexam.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure unit tests for the extracted scoring/pass-fail rules (dev-audit #4).
 * No Spring context — the policy is a dependency-free calculation.
 */
class MockScoringPolicyTest {

    private final MockScoringPolicy policy = new MockScoringPolicy();

    @Test
    void maxAllowedWrong_30Question_at85_tolerates5() {
        // CA-M1: 85% pass → ceil(30 × 0.15) = ceil(4.5) = 5.
        assertEquals(5, policy.maxAllowedWrong(30, 85));
    }

    @Test
    void maxAllowedWrong_roundsUp() {
        assertEquals(2, policy.maxAllowedWrong(10, 85));  // ceil(1.5) = 2
        assertEquals(0, policy.maxAllowedWrong(0, 85));   // ceil(0)   = 0
    }

    @Test
    void maxAllowedWrong_perExamThreshold() {
        // The threshold now comes from the exam (V26), so a stricter or looser
        // standard changes the tolerance: 30 questions at 90% → ceil(3) = 3;
        // at 80% → ceil(6) = 6.
        assertEquals(3, policy.maxAllowedWrong(30, 90));
        assertEquals(6, policy.maxAllowedWrong(30, 80));
    }

    @Test
    void maxAllowedWrong_floatingPointQuirk_20Question_is4() {
        // 1 - 0.85 isn't exactly representable (≈ 0.15000000000000002), so
        // 20 × that ≈ 3.0000000000000004 → ceil = 4, not the mathematical 3.
        // This pins MockExamService's long-standing behavior (the refactor must
        // preserve it). The production exam is 30 questions → ceil(4.5) = 5,
        // where the quirk doesn't bite. Flagged to product as a minor latent
        // nuance, deliberately not "fixed" here (would change behavior).
        assertEquals(4, policy.maxAllowedWrong(20, 85));
    }

    @Test
    void scorePercent_rounds() {
        assertEquals(90, policy.scorePercent(27, 30));
        assertEquals(33, policy.scorePercent(1, 3));   // round(33.33)
        assertEquals(100, policy.scorePercent(30, 30));
    }

    @Test
    void scorePercent_emptyExam_isZero() {
        assertEquals(0, policy.scorePercent(0, 0));
    }
}
