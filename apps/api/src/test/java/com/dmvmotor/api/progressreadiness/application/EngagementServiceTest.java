package com.dmvmotor.api.progressreadiness.application;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pure streak logic — no DB. Covers the day-grace + gap edge cases. */
class EngagementServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 7);

    @Test
    void noActivity_zeroStreak() {
        assertEquals(0, EngagementService.computeStreak(Set.of(), TODAY));
    }

    @Test
    void todayOnly_streakOne() {
        assertEquals(1, EngagementService.computeStreak(Set.of(TODAY), TODAY));
    }

    @Test
    void todayAndYesterday_streakTwo() {
        assertEquals(2, EngagementService.computeStreak(
                Set.of(TODAY, TODAY.minusDays(1)), TODAY));
    }

    @Test
    void yesterdayButNotToday_graceKeepsStreak() {
        // Morning before today's practice: yesterday + day-before → 2, not reset.
        assertEquals(2, EngagementService.computeStreak(
                Set.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY));
    }

    @Test
    void lastActivityTwoDaysAgo_broken_zero() {
        assertEquals(0, EngagementService.computeStreak(
                Set.of(TODAY.minusDays(2), TODAY.minusDays(3)), TODAY));
    }

    @Test
    void gapBreaksRun() {
        // today, yesterday, GAP at -2, then -3 → streak counts only the run of 2.
        assertEquals(2, EngagementService.computeStreak(
                Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(3)), TODAY));
    }
}
