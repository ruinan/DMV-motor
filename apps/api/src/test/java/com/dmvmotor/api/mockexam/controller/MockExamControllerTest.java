package com.dmvmotor.api.mockexam.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MockExamControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;
    private Long topicId;
    private Long q1;
    private Long q2;
    private Long v1;
    private Long v2;
    private Long mockExamId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId  = fixtures.insertUser("exam@example.com");
        topicId = fixtures.insertTopic("EXAM_TOPIC");
        q1 = fixtures.insertQuestion(topicId, "B");
        q2 = fixtures.insertQuestion(topicId, "A");
        v1 = fixtures.insertVariantReturningId(q1, "en", "Q1 stem?",
                "[{\"key\":\"A\",\"text\":\"Wrong\"},{\"key\":\"B\",\"text\":\"Right\"}]",
                "Explanation 1");
        v2 = fixtures.insertVariantReturningId(q2, "en", "Q2 stem?",
                "[{\"key\":\"A\",\"text\":\"Right\"},{\"key\":\"B\",\"text\":\"Wrong\"}]",
                "Explanation 2");
        mockExamId = fixtures.insertMockExam("TEST_EXAM_V1", 2);
        fixtures.insertMockExamQuestion(mockExamId, q1, 1);
        fixtures.insertMockExamQuestion(mockExamId, q2, 2);
    }

    // ---------------------------------------------------------------
    // GET /api/v1/mock-exams/access
    // ---------------------------------------------------------------

    @Test
    void getMockAccess_anonymous_returns401() throws Exception {
        // api-contract.md §16: GET /mock-exams/access 需要登录
        mockMvc.perform(get("/api/v1/mock-exams/access"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void getMockAccess_nonBearerAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/mock-exams/access")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMockAccess_nonNumericBearerToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/mock-exams/access")
                        .header("Authorization", "Bearer notanumber"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMockAccess_userNoPass_returnsNotAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/mock-exams/access")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false));
    }

    @Test
    void getMockAccess_userWithActivePassAndQuota_returnsAllowed() throws Exception {
        fixtures.insertAccessPass(userId, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);

        mockMvc.perform(get("/api/v1/mock-exams/access")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.mock_remaining").value(3));
    }

    @Test
    void getMockAccess_userWithNoRemainingQuota_returnsNotAllowed() throws Exception {
        fixtures.insertAccessPass(userId, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 3);

        mockMvc.perform(get("/api/v1/mock-exams/access")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/mock-exams/attempts
    // ---------------------------------------------------------------

    @Test
    void startMockAttempt_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startMockAttempt_userNoPass_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void startMockAttempt_withValidPass_returnsMockAttemptWithQuestions() throws Exception {
        fixtures.insertAccessPass(userId, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);

        mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mock_attempt_id").isString())
                .andExpect(jsonPath("$.data.status").value("in_progress"))
                .andExpect(jsonPath("$.data.mock_remaining_after_start").value(2))
                .andExpect(jsonPath("$.data.questions", hasSize(2)))
                .andExpect(jsonPath("$.data.questions[0].question_id").isString())
                .andExpect(jsonPath("$.data.questions[0].stem").isString());
    }

    // ---------------------------------------------------------------
    // POST /api/v1/mock-exams/attempts/{id}/answers
    // ---------------------------------------------------------------

    @Test
    void saveAnswer_newAnswer_returnsSavedTrue() throws Exception {
        String attemptId = startMockAndGetId();

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(q1, v1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(true))
                .andExpect(jsonPath("$.data.answered_count").value(1));
    }

    @Test
    void saveAnswer_retry_returnsUpdatedCount() throws Exception {
        String attemptId = startMockAndGetId();

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(q1, v1)));

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(q1, v1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(true))
                .andExpect(jsonPath("$.data.answered_count").value(1));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/mock-exams/attempts/{id}/submit
    // ---------------------------------------------------------------

    @Test
    void submitMockExam_returnsScoreAndWeakTopics() throws Exception {
        String attemptId = startMockAndGetId();
        saveAnswerForAttempt(attemptId, q1, v1, "B"); // correct
        saveAnswerForAttempt(attemptId, q2, v2, "B"); // wrong

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/submit", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mock_attempt_id").value(attemptId))
                .andExpect(jsonPath("$.data.status").value("submitted"))
                .andExpect(jsonPath("$.data.correct_count").value(1))
                .andExpect(jsonPath("$.data.wrong_count").value(1))
                .andExpect(jsonPath("$.data.score_percent").isNumber())
                .andExpect(jsonPath("$.data.weak_topics").isArray())
                .andExpect(jsonPath("$.data.next_action.type").isString());
    }

    @Test
    void submitMockExam_alreadySubmitted_returns409() throws Exception {
        String attemptId = startMockAndGetId();
        submitAttempt(attemptId);

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/submit", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MOCK_ALREADY_ENDED"));
    }

    @Test
    void submitMockExam_partialAnswers_scoreBasedOnTotalQuestions() throws Exception {
        String attemptId = startMockAndGetId();
        // Only answer q1 correctly; q2 left unanswered
        saveAnswerForAttempt(attemptId, q1, v1, "B"); // correct

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/submit", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct_count").value(1))
                .andExpect(jsonPath("$.data.wrong_count").value(0))
                // score = round(100 * 1 / 2) = 50, NOT 100 (total = 2 questions in exam)
                .andExpect(jsonPath("$.data.score_percent").value(50));
    }

    @Test
    void submitMockExam_allCorrect_nextActionIsPractice() throws Exception {
        String attemptId = startMockAndGetId();
        saveAnswerForAttempt(attemptId, q1, v1, "B"); // correct (q1 correct key = "B")
        saveAnswerForAttempt(attemptId, q2, v2, "A"); // correct (q2 correct key = "A")

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/submit", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct_count").value(2))
                .andExpect(jsonPath("$.data.wrong_count").value(0))
                .andExpect(jsonPath("$.data.next_action.type").value("practice"));
    }

    @Test
    void startMockAttempt_noActiveMockTemplate_returns422() throws Exception {
        fixtures.truncateAll();
        Long noTemplateUser = fixtures.insertUser("notemplate@example.com");
        fixtures.insertAccessPass(noTemplateUser, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);
        // No mock exam template inserted

        mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .header("Authorization", "Bearer " + noTemplateUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_MOCK_EXAM_AVAILABLE"));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/mock-exams/attempts/{id}/answers — ownership
    // ---------------------------------------------------------------

    @Test
    void saveAnswer_questionNotInExam_returns400() throws Exception {
        // C1: question that's not part of this attempt's mock_exam template
        Long stranger = fixtures.insertQuestion(topicId, "A");
        Long strangerVid = fixtures.insertVariantReturningId(stranger, "en", "Off-exam?",
                "[{\"key\":\"A\",\"text\":\"x\"},{\"key\":\"B\",\"text\":\"y\"}]", "expl");
        // Note: stranger is NOT inserted into mock_exam_questions for mockExamId.

        String attemptId = startMockAndGetId();

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(stranger, strangerVid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("QUESTION_NOT_IN_MOCK_EXAM"));
    }

    @Test
    void saveAnswer_attemptAlreadySubmitted_returns409() throws Exception {
        // C2: after submit, further saves must be rejected
        String attemptId = startMockAndGetId();
        saveAnswerForAttempt(attemptId, q1, v1, "B");
        submitAttempt(attemptId);

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(q2, v2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MOCK_ALREADY_ENDED"));
    }

    @Test
    void saveAnswer_attemptAlreadyExited_returns409() throws Exception {
        // C3: after exit, further saves must be rejected
        String attemptId = startMockAndGetId();
        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/exit", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(q1, v1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MOCK_ALREADY_ENDED"));
    }

    @Test
    void saveAnswer_forbiddenUser_returns403() throws Exception {
        Long otherUser = fixtures.insertUser("other@example.com");
        String attemptId = startMockAndGetId();

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + otherUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(q1, v1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/mock-exams/attempts/{id}/exit
    // ---------------------------------------------------------------

    @Test
    void exitMockExam_setsStatusEndedByExit() throws Exception {
        String attemptId = startMockAndGetId();

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/exit", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mock_attempt_id").value(attemptId))
                .andExpect(jsonPath("$.data.status").value("ended_by_exit"))
                .andExpect(jsonPath("$.data.quota_consumed").value(true));
    }

    @Test
    void exitMockExam_alreadySubmitted_returns409() throws Exception {
        String attemptId = startMockAndGetId();
        saveAnswerForAttempt(attemptId, q1, v1, "B");
        saveAnswerForAttempt(attemptId, q2, v2, "A");
        submitAttempt(attemptId);

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/exit", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MOCK_ALREADY_ENDED"));
    }

    @Test
    void exitMockExam_alreadyExited_returns409() throws Exception {
        String attemptId = startMockAndGetId();

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/exit", attemptId)
                        .header("Authorization", "Bearer " + userId));

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/exit", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MOCK_ALREADY_ENDED"));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String startMockAndGetId() throws Exception {
        fixtures.insertAccessPass(userId, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);

        var result = mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String key = "\"mock_attempt_id\":\"";
        int start = body.indexOf(key) + key.length();
        int end   = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    private void saveAnswerForAttempt(String attemptId, Long qId, Long vId, String choice)
            throws Exception {
        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"question_id":"%s","variant_id":"%s","selected_choice_key":"%s"}
                        """.formatted(qId, vId, choice)));
    }

    private void submitAttempt(String attemptId) throws Exception {
        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/submit", attemptId)
                .header("Authorization", "Bearer " + userId));
    }
}
