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
                .andExpect(jsonPath("$.data.userId").value(String.valueOf(userId)))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.language").value("en"))
                .andExpect(jsonPath("$.data.access.state").value("free_trial"))
                .andExpect(jsonPath("$.data.access.hasActivePass").value(false))
                .andExpect(jsonPath("$.data.access.mockRemaining").value(0))
                .andExpect(jsonPath("$.data.learning.hasInProgressPractice").value(false));
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
