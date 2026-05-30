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
                .andExpect(jsonPath("$.data.learning.in_progress_practice").doesNotExist())
                .andExpect(jsonPath("$.data.learning.has_in_progress_review").value(false));
    }

    @Test
    void getMe_withInProgressPractice_returnsSessionDetails() throws Exception {
        Long topicId = fixtures.insertTopic("LANES", "Lane", "车道", false, 10);
        Long q1 = fixtures.insertQuestion(topicId, "A");
        Long v1 = fixtures.insertEnVariantReturningId(q1, "stem 1", "expl 1");

        // Insert in-progress session and 2 attempts
        Long sessionId = fixtures.insertInProgressPracticeSession(userId, 0, "full", "en");
        fixtures.insertPracticeAttempt(userId, sessionId, q1, v1, "A", true);
        Long q2 = fixtures.insertQuestion(topicId, "B");
        Long v2 = fixtures.insertEnVariantReturningId(q2, "stem 2", "expl 2");
        fixtures.insertPracticeAttempt(userId, sessionId, q2, v2, "B", true);

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learning.has_in_progress_practice").value(true))
                .andExpect(jsonPath("$.data.learning.in_progress_practice.session_id")
                        .value(sessionId.toString()))
                .andExpect(jsonPath("$.data.learning.in_progress_practice.entry_type").value("full"))
                .andExpect(jsonPath("$.data.learning.in_progress_practice.language").value("en"))
                .andExpect(jsonPath("$.data.learning.in_progress_practice.answered_count").value(2))
                .andExpect(jsonPath("$.data.learning.in_progress_practice.total_count").exists())
                .andExpect(jsonPath("$.data.learning.in_progress_practice.last_activity_at").exists());
    }

    @Test
    void getMe_inProgressTopicFilteredPractice_totalReflectsFilteredPool() throws Exception {
        // Dev-quality audit #1 (correctness): the /me Resume CTA must show the
        // FILTERED pool size for a topic-scoped in-progress session, not
        // min(cap, full bank).
        Long topicId = fixtures.insertTopic("LANES", "Lane", "车道", false, 10);
        // The filtered topic holds 3 questions (each with an en variant, so each
        // is counted by countTotal which joins on the language variant).
        Long q1 = fixtures.insertQuestion(topicId, "A");
        Long v1 = fixtures.insertEnVariantReturningId(q1, "stem 1", "expl 1");
        Long q2 = fixtures.insertQuestion(topicId, "B");
        fixtures.insertEnVariantReturningId(q2, "stem 2", "expl 2");
        Long q3 = fixtures.insertQuestion(topicId, "A");
        fixtures.insertEnVariantReturningId(q3, "stem 3", "expl 3");

        // Pad the bank with 40 questions in another topic so an unfiltered
        // count would hit the cap.
        Long otherTopic = fixtures.insertTopic("BANK", "Bank", "题库", false, 11);
        for (int i = 0; i < 40; i++) {
            Long qid = fixtures.insertQuestion(otherTopic, "A");
            fixtures.insertEnVariantReturningId(qid, "bank " + i, "x");
        }

        Long sessionId = fixtures.insertInProgressPracticeSession(
                userId, 0, "full", "en", String.valueOf(topicId));
        fixtures.insertPracticeAttempt(userId, sessionId, q1, v1, "A", true);

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learning.in_progress_practice.total_count").value(3));
    }

    @Test
    void getMe_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void getMe_brandNewFirebaseUid_jitProvisionsAndReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer 9999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access.state").value("free_trial"));
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

    @Test
    void resetLearning_isSoftReset_practiceSessionStillExistsAfterReset() throws Exception {
        // Create a practice session before reset — data must not be deleted
        Long topicId = fixtures.insertTopic("RESET_TEST_TOPIC");
        Long qId     = fixtures.insertQuestion(topicId, "A");
        fixtures.insertVariant(qId, "en", "Soft reset test question?",
                "[{\"key\":\"A\",\"text\":\"Yes\"},{\"key\":\"B\",\"text\":\"No\"}]",
                null);

        // Start a practice session (creates a row in practice_sessions)
        mockMvc.perform(post("/api/v1/practice/sessions")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"en"}
                                """))
                .andExpect(status().isCreated());

        // Reset
        mockMvc.perform(post("/api/v1/me/reset-learning")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirm":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reset").value(true));

        // After reset: getMe still works — user exists, data not deleted
        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(String.valueOf(userId)))
                // No in-progress session in the NEW cycle
                .andExpect(jsonPath("$.data.learning.has_in_progress_practice").value(false));
    }
}
