package com.dmvmotor.api.mistakereview.review.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Boundary tests for {@link ReviewController#priorityTier(int)} — the static
 * helper that maps the raw wrong-count sum to a display tier. Lives outside
 * the {@link com.dmvmotor.api.mistakereview.review.ReviewControllerTest IT}
 * because the {@code low} tier is unreachable through the integration path
 * (active mistakes always carry {@code wrong_count >= 1}) and JaCoCo would
 * otherwise flag that branch as uncovered.
 */
class ReviewControllerPriorityTierTest {

    @Test
    void priorityTier_threeOrMore_returnsHigh() {
        assertEquals("high", ReviewController.priorityTier(3));
        assertEquals("high", ReviewController.priorityTier(99));
    }

    @Test
    void priorityTier_oneOrTwo_returnsMedium() {
        assertEquals("medium", ReviewController.priorityTier(1));
        assertEquals("medium", ReviewController.priorityTier(2));
    }

    @Test
    void priorityTier_zeroOrNegative_returnsLow() {
        // Defensive: active mistakes always have wrong_count >= 1, so 0 is
        // only theoretically reachable. Pinning the contract anyway.
        assertEquals("low", ReviewController.priorityTier(0));
        assertEquals("low", ReviewController.priorityTier(-1));
    }
}
