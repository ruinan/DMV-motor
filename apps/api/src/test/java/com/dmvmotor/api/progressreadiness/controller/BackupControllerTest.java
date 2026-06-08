package com.dmvmotor.api.progressreadiness.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Progress backup (paid snapshot). Create is pass-gated; listing is open so a
 * downgraded user keeps the history they recorded. Snapshots are exam-scoped.
 */
class BackupControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId = fixtures.insertUser("backup@example.com");
    }

    private void grantPass() {
        OffsetDateTime now = OffsetDateTime.now();
        fixtures.insertAccessPass(userId, "active", now.minusDays(1), now.plusDays(30), 5, 0);
    }

    @Test
    void create_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/backup/snapshots"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void list_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/backup/snapshots"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_freeUser_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/backup/snapshots")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void create_paidUser_thenList_returnsSnapshot() throws Exception {
        grantPass();

        mockMvc.perform(post("/api/v1/backup/snapshots")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.readiness_score").exists())
                .andExpect(jsonPath("$.data.completion_score").exists())
                .andExpect(jsonPath("$.data.created_at").exists());

        mockMvc.perform(get("/api/v1/backup/snapshots")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshots", hasSize(1)));
    }

    @Test
    void list_isExamScoped() throws Exception {
        grantPass();
        Long examB = fixtures.insertExam("WA", "C", "Washington Class C", "华盛顿 C 类", 83);

        // Snapshot taken on the default exam (CA-M1).
        mockMvc.perform(post("/api/v1/backup/snapshots")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());

        // Switch to exam B → its list is empty (the CA-M1 snapshot doesn't leak).
        fixtures.setUserCurrentExam(userId, examB);
        mockMvc.perform(get("/api/v1/backup/snapshots")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshots", hasSize(0)));

        // Back to CA-M1 → the snapshot is there.
        fixtures.setUserCurrentExam(userId, fixtures.defaultExamId());
        mockMvc.perform(get("/api/v1/backup/snapshots")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshots", hasSize(1)));
    }

    @Test
    void list_survivesDowngrade_butCreateBlocks() throws Exception {
        // Record a snapshot while paid…
        OffsetDateTime now = OffsetDateTime.now();
        Long passId = fixtures.insertAccessPass(userId, "active",
                now.minusDays(1), now.plusDays(30), 5, 0);
        mockMvc.perform(post("/api/v1/backup/snapshots")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());

        // …then lapse the pass (downgrade). History is KEPT…
        fixtures.expireAccessPass(passId);
        mockMvc.perform(get("/api/v1/backup/snapshots")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.snapshots", hasSize(1)));

        // …but a new backup is blocked until they re-subscribe.
        mockMvc.perform(post("/api/v1/backup/snapshots")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }
}
