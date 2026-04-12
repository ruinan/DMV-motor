package com.dmvmotor.api.authaccess.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId = fixtures.insertUser("alice@example.com");
    }

    // ---------------------------------------------------------------
    // GET /api/v1/me
    // ---------------------------------------------------------------

    @Test
    void getMe_authenticated_returnsProfileWithFreeTrialAccess() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(String.valueOf(userId)))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.language").value("en"))
                .andExpect(jsonPath("$.data.access.state").value("free_trial"))
                .andExpect(jsonPath("$.data.access.has_active_pass").value(false))
                .andExpect(jsonPath("$.data.access.mock_remaining").value(0))
                .andExpect(jsonPath("$.data.learning.has_in_progress_practice").value(false))
                .andExpect(jsonPath("$.data.learning.has_in_progress_review").value(false));
    }

    @Test
    void getMe_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void getMe_unknownUserId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer 9999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMe_nullEmailUser_returnsEmptyStringForEmail() throws Exception {
        Long noEmailUserId = fixtures.insertUserWithoutEmail();
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + noEmailUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(""));
    }

    @Test
    void getMe_withActivePass_expiresAtIsPresent() throws Exception {
        fixtures.insertAccessPass(userId, "active",
                java.time.OffsetDateTime.now().minusDays(1),
                java.time.OffsetDateTime.now().plusDays(30), 3, 0);

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access.state").value("active"))
                .andExpect(jsonPath("$.data.access.has_active_pass").value(true))
                .andExpect(jsonPath("$.data.access.expires_at").isString());
    }

    // ---------------------------------------------------------------
    // PUT /api/v1/me/language
    // ---------------------------------------------------------------

    @Test
    void updateLanguage_toZh_updatesAndReturnsZh() throws Exception {
        mockMvc.perform(put("/api/v1/me/language")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"zh"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language").value("zh"));
    }

    @Test
    void updateLanguage_anonymous_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/me/language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"zh"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateLanguage_invalidCode_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/me/language")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"fr"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/me/reset-learning
    // ---------------------------------------------------------------

    @Test
    void resetLearning_withConfirmTrue_returnsTrue() throws Exception {
        mockMvc.perform(post("/api/v1/me/reset-learning")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirm":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reset").value(true));
    }

    @Test
    void resetLearning_withoutConfirm_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/me/reset-learning")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void resetLearning_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/me/reset-learning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirm":true}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
