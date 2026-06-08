package com.dmvmotor.api.progressreadiness.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Engagement endpoint (streak + daily goal). Local-day bucketing is exercised
 * with offset 0 (UTC) using attempts on consecutive UTC days; the pure streak
 * logic + grace edge cases live in {@link com.dmvmotor.api.progressreadiness.application.EngagementServiceTest}.
 */
class EngagementControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;
    private Long examId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId = fixtures.insertUser("eng@example.com");
        examId = fixtures.defaultExamId();
    }

    @Test
    void anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/engagement"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void noActivity_zerosWithDailyGoal() throws Exception {
        mockMvc.perform(get("/api/v1/engagement")
                        .param("tz_offset_minutes", "0")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current_streak_days").value(0))
                .andExpect(jsonPath("$.data.answered_today").value(0))
                .andExpect(jsonPath("$.data.daily_goal").value(10));
    }

    @Test
    void todayActivity_countsAnsweredAndStreakOne() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        fixtures.insertPracticeAttemptAt(userId, examId, now);
        fixtures.insertPracticeAttemptAt(userId, examId, now);

        mockMvc.perform(get("/api/v1/engagement")
                        .param("tz_offset_minutes", "0")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answered_today").value(2))
                .andExpect(jsonPath("$.data.current_streak_days").value(1));
    }

    @Test
    void consecutiveDays_buildStreak_gapBreaksIt() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        fixtures.insertPracticeAttemptAt(userId, examId, now);             // today
        fixtures.insertPracticeAttemptAt(userId, examId, now.minusDays(1)); // -1
        fixtures.insertPracticeAttemptAt(userId, examId, now.minusDays(2)); // -2
        // gap at -3, activity at -4 must NOT extend the streak
        fixtures.insertPracticeAttemptAt(userId, examId, now.minusDays(4));

        mockMvc.perform(get("/api/v1/engagement")
                        .param("tz_offset_minutes", "0")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current_streak_days").value(3))
                .andExpect(jsonPath("$.data.answered_today").value(1));
    }

    @Test
    void otherUsersActivity_isExcluded() throws Exception {
        Long other = fixtures.insertUser("other@example.com");
        fixtures.insertPracticeAttemptAt(other, examId, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/engagement")
                        .param("tz_offset_minutes", "0")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.current_streak_days").value(0))
                .andExpect(jsonPath("$.data.answered_today").value(0));
    }
}
