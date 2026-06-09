package com.dmvmotor.api.progressreadiness.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Single-slot progress backup (bug1, paid cloud-save). sync (write) is
 * pass-gated; latest (read) is owner-only and survives a downgrade; restore
 * mutates progress and is pass-gated + re-auth-gated + throttled. Snapshots are
 * server-computed and exam-scoped.
 */
class BackupControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId = fixtures.insertUser("backup@example.com");
        fixtures.setUserCurrentExam(userId, fixtures.defaultExamId());
    }

    private void grantPass() {
        OffsetDateTime now = OffsetDateTime.now();
        fixtures.insertAccessPassForExam(userId, fixtures.defaultExamId(), "active",
                now.minusDays(1), now.plusDays(30), 5, 0);
    }

    @Test
    void sync_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/backup/sync"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void latest_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/backup/latest"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restore_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/backup/restore"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sync_freeUser_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/backup/sync")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void sync_paidUser_persistsAndLatestReturnsIt() throws Exception {
        grantPass();

        mockMvc.perform(post("/api/v1/backup/sync")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.changed").value(true))
                .andExpect(jsonPath("$.data.backup.id").exists())
                .andExpect(jsonPath("$.data.backup.updated_at").exists());

        mockMvc.perform(get("/api/v1/backup/latest")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.has_backup").value(true))
                .andExpect(jsonPath("$.data.backup.readiness_score").exists());
    }

    @Test
    void sync_unchanged_secondCallIsNoOp() throws Exception {
        grantPass();
        mockMvc.perform(post("/api/v1/backup/sync")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.changed").value(true));
        // No progress changed between calls → identical hash → no write.
        mockMvc.perform(post("/api/v1/backup/sync")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.changed").value(false));
    }

    @Test
    void latest_noBackup_returnsAbsent() throws Exception {
        mockMvc.perform(get("/api/v1/backup/latest")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.has_backup").value(false));
    }

    @Test
    void latest_isExamScoped() throws Exception {
        grantPass();
        Long examB = fixtures.insertExam("WA", "C", "Washington Class C", "华盛顿 C 类", 83);

        mockMvc.perform(post("/api/v1/backup/sync")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());

        // Switch to exam B → no backup there (the CA-M1 one doesn't leak).
        fixtures.setUserCurrentExam(userId, examB);
        mockMvc.perform(get("/api/v1/backup/latest")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_backup").value(false));

        fixtures.setUserCurrentExam(userId, fixtures.defaultExamId());
        mockMvc.perform(get("/api/v1/backup/latest")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_backup").value(true));
    }

    @Test
    void latest_survivesDowngrade_butSyncBlocks() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        Long passId = fixtures.insertAccessPassForExam(userId, fixtures.defaultExamId(), "active",
                now.minusDays(1), now.plusDays(30), 5, 0);
        mockMvc.perform(post("/api/v1/backup/sync")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());

        // Lapse the pass: the backup is still readable…
        fixtures.expireAccessPass(passId);
        mockMvc.perform(get("/api/v1/backup/latest")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_backup").value(true));

        // …but a new sync is blocked until they re-subscribe.
        mockMvc.perform(post("/api/v1/backup/sync")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void restore_freeUser_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/backup/restore")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void restore_staleSession_requiresReauth() throws Exception {
        grantPass();
        // Stale auth_time (the "~<epoch>" stub suffix) → restore needs a recent
        // password proof, even with a pass.
        mockMvc.perform(post("/api/v1/backup/restore")
                        .header("Authorization", "Bearer " + userId + "~1000000000"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("REAUTH_REQUIRED"));
    }

    @Test
    void restore_noBackup_returns404() throws Exception {
        grantPass();
        mockMvc.perform(post("/api/v1/backup/restore")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NO_BACKUP"));
    }

    @Test
    void restore_reappliesMistakesIntoCurrentCycle() throws Exception {
        grantPass();
        Long topic = fixtures.insertTopic("SIGNS_BK", "Signs", "标志", true, 1);
        Long q = fixtures.insertQuestion(topic, "A");
        fixtures.insertMistakeRecord(userId, q, topic, 2, "practice");

        // Snapshot while the mistake is active (cycle 0).
        mockMvc.perform(post("/api/v1/backup/sync")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());

        // Reset the learning cycle → the cycle-0 mistake no longer counts.
        fixtures.incrementUserResetCount(userId);
        mockMvc.perform(get("/api/v1/practice/sessions/stats")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.active_mistakes_count").value(0));

        // Restore re-applies the backed-up mistake into the current cycle.
        mockMvc.perform(post("/api/v1/backup/restore")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restored_mistakes").value(1));

        mockMvc.perform(get("/api/v1/practice/sessions/stats")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.active_mistakes_count").value(1));
    }

    @Test
    void restore_secondWithinCooldown_returns429() throws Exception {
        grantPass();
        Long topic = fixtures.insertTopic("SIGNS_TH", "Signs", "标志", true, 1);
        Long q = fixtures.insertQuestion(topic, "A");
        fixtures.insertMistakeRecord(userId, q, topic, 1, "practice");
        mockMvc.perform(post("/api/v1/backup/sync")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/backup/restore")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());
        // Immediately again → throttled.
        mockMvc.perform(post("/api/v1/backup/restore")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RESTORE_THROTTLED"));
    }
}
