package com.dmvmotor.api.dev;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The dev backdoor that stands in for real checkout while billing isn't built:
 * grant / revoke a per-exam access pass (V32 subscription model). Gated by
 * {@code app.dev.endpoints=true} — enabled here so the bean registers; it's
 * never set in prod (and {@code @Profile("!prod")} double-locks it).
 */
@TestPropertySource(properties = "app.dev.endpoints=true")
class DevControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;
    private Long examId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId = fixtures.insertUser("dev@example.com");
        examId = fixtures.defaultExamId();
    }

    @Test
    void grantThenRevoke_togglesAccessAndEntitlement() throws Exception {
        // Subscribe: grant a pass for the exam → access flips to active and the
        // entitlement reads subscribed.
        mockMvc.perform(post("/api/v1/dev/grant-pass")
                        .param("exam_id", String.valueOf(examId))
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam_id").value(String.valueOf(examId)))
                .andExpect(jsonPath("$.data.mock_quota").value(5));

        fixtures.setUserCurrentExam(userId, examId);
        mockMvc.perform(get("/api/v1/access").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_active_pass").value(true));
        mockMvc.perform(get("/api/v1/exams/entitlements").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.entitlements[?(@.exam_id=='%d')].subscribed"
                        .formatted(examId)).value(hasItem(true)));

        // Unsubscribe: revoke → access drops to free trial, entitlement not subscribed.
        mockMvc.perform(post("/api/v1/dev/revoke-pass")
                        .param("exam_id", String.valueOf(examId))
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam_id").value(String.valueOf(examId)))
                .andExpect(jsonPath("$.data.cancelled").value(1));

        mockMvc.perform(get("/api/v1/access").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_active_pass").value(false));
        mockMvc.perform(get("/api/v1/exams/entitlements").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.entitlements[?(@.exam_id=='%d')].subscribed"
                        .formatted(examId)).value(hasItem(false)));
    }

    @Test
    void grantPass_noExamParam_usesCurrentExam() throws Exception {
        // Omitting exam_id falls back to the user's resolved exam (default here).
        mockMvc.perform(post("/api/v1/dev/grant-pass")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam_id").value(String.valueOf(examId)));
    }

    @Test
    void revokePass_noActivePass_cancelsZero() throws Exception {
        mockMvc.perform(post("/api/v1/dev/revoke-pass")
                        .param("exam_id", String.valueOf(examId))
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cancelled").value(0));
    }

    @Test
    void grantPass_staleSession_requiresReauth() throws Exception {
        // Stale auth_time (the "~<epoch>" stub suffix) → subscription change is
        // gated on a recent password proof.
        mockMvc.perform(post("/api/v1/dev/grant-pass")
                        .header("Authorization", "Bearer " + userId + "~1000000000"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("REAUTH_REQUIRED"));
    }

    @Test
    void grantPass_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/dev/grant-pass"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void revokePass_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/dev/revoke-pass"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
