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

    @Test
    void startMockAttempt_userWithMultiplePasses_decrementsOnlyCurrentlyActive()
            throws Exception {
        // Sec audit #3b: the buggy consumeMockQuota updated EVERY pass with
        // status='active' for the user, so a user with one current pass + one
        // old expired-but-still-status-active pass would burn quota on both.
        // Fixed query targets the specific currently-in-window pass id.
        OffsetDateTime now = OffsetDateTime.now();
        Long oldExpiredPassId = fixtures.insertAccessPass(userId, "active",
                now.minusDays(60), now.minusDays(30), 5, 0);
        Long currentPassId = fixtures.insertAccessPass(userId, "active",
                now.minusDays(1), now.plusDays(30), 5, 0);

        mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andExpect(status().isCreated());

        // Only the currently-active pass should have its quota incremented.
        org.junit.jupiter.api.Assertions.assertEquals(
                1, fixtures.getAccessPassMockUsed(currentPassId),
                "current pass should have one quota consumed");
        org.junit.jupiter.api.Assertions.assertEquals(
                0, fixtures.getAccessPassMockUsed(oldExpiredPassId),
                "expired pass must not have its quota touched");
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
                .andExpect(jsonPath("$.data.answered_count").value(1))
                .andExpect(jsonPath("$.data.is_correct").value(true))
                .andExpect(jsonPath("$.data.correct_choice_key").value("B"))
                .andExpect(jsonPath("$.data.wrong_count").value(0))
                .andExpect(jsonPath("$.data.should_terminate").value(false));
    }

    @Test
    void saveAnswer_wrongAnswer_marksTerminateWhenCapExceeded() throws Exception {
        // The setUp seeds a 2-question mock exam. With pass_threshold = 0.85,
        // max_allowed_wrong = ceil(2 * 0.15) = 1. So the SECOND wrong answer
        // should flip should_terminate=true.
        String attemptId = startMockAndGetId();

        // First wrong — within cap, no terminate
        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(q1, v1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_correct").value(false))
                .andExpect(jsonPath("$.data.wrong_count").value(1))
                .andExpect(jsonPath("$.data.should_terminate").value(false));

        // Second wrong — exceeds cap (1), attempt should terminate
        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(q2, v2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wrong_count").value(2))
                .andExpect(jsonPath("$.data.should_terminate").value(true));

        // Subsequent answers on the terminated attempt → 409
        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(q1, v1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MOCK_ALREADY_ENDED"));
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

    // ===== GET /api/v1/mock-exams/attempts/{id} =====

    @Test
    void getAttempt_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/mock-exams/attempts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAttempt_resumesWithQuestionsAndSavedAnswers() throws Exception {
        fixtures.insertAccessPass(userId, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);

        // Start an attempt first to get a real attemptId + question IDs to answer
        String startBody = mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andReturn().getResponse().getContentAsString();

        String attemptId = extractJsonString(startBody, "mock_attempt_id");
        // Pluck the first question_id + variant_id to save one answer against
        String firstQuestionId = extractJsonStringFromQuestions(startBody, "question_id");
        String firstVariantId = extractJsonStringFromQuestions(startBody, "variant_id");
        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                .header("Authorization", "Bearer " + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                        """.formatted(firstQuestionId, firstVariantId)));

        // Now resume from a clean slate via GET — should return all questions
        // + the one saved answer.
        mockMvc.perform(get("/api/v1/mock-exams/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mock_attempt_id").value(attemptId))
                .andExpect(jsonPath("$.data.status").value("in_progress"))
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.questions[0].question_id").isString())
                .andExpect(jsonPath("$.data.questions[0].stem").isString())
                .andExpect(jsonPath("$.data.saved_answers", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.saved_answers[0].selected_choice_key").value("B"))
                // In-progress attempts have no score yet — sentinel -1, counts 0.
                .andExpect(jsonPath("$.data.score_percent").value(-1))
                .andExpect(jsonPath("$.data.correct_count").value(0))
                .andExpect(jsonPath("$.data.wrong_count").value(0));
    }

    @Test
    void getAttempt_afterSubmit_includesScoreSummary() throws Exception {
        // A finished attempt, re-opened cold (refresh / mock history click),
        // must carry its score so the client renders the result view instead
        // of the answering UI.
        String attemptId = startMockAndGetId();
        saveAnswerForAttempt(attemptId, q1, v1, "B"); // correct (q1 key = "B")
        saveAnswerForAttempt(attemptId, q2, v2, "B"); // wrong (q2 key = "A")
        submitAttempt(attemptId);

        mockMvc.perform(get("/api/v1/mock-exams/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("submitted"))
                .andExpect(jsonPath("$.data.score_percent").value(50))
                .andExpect(jsonPath("$.data.correct_count").value(1))
                .andExpect(jsonPath("$.data.wrong_count").value(1));
    }

    @Test
    void getAttempt_afterSubmit_savedAnswersCarryReviewDetail() throws Exception {
        String attemptId = startMockAndGetId();
        saveAnswerForAttempt(attemptId, q1, v1, "B"); // correct (q1 key = B)
        saveAnswerForAttempt(attemptId, q2, v2, "B"); // wrong (q2 key = A)
        submitAttempt(attemptId);

        mockMvc.perform(get("/api/v1/mock-exams/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved_answers", hasSize(2)))
                .andExpect(jsonPath("$.data.saved_answers[0].question_id").value(q1.toString()))
                .andExpect(jsonPath("$.data.saved_answers[0].correct_choice_key").value("B"))
                .andExpect(jsonPath("$.data.saved_answers[0].is_correct").value(true))
                .andExpect(jsonPath("$.data.saved_answers[1].question_id").value(q2.toString()))
                .andExpect(jsonPath("$.data.saved_answers[1].is_correct").value(false))
                .andExpect(jsonPath("$.data.saved_answers[1].explanation")
                        .value(containsString("Explanation 2")));
    }

    @Test
    void getAttempt_inProgress_savedAnswersOmitExplanation() throws Exception {
        // During the live exam the "why" stays hidden — only right/wrong is
        // shown. The review's explanation must not leak until the exam ends.
        String attemptId = startMockAndGetId();
        saveAnswerForAttempt(attemptId, q1, v1, "B");

        mockMvc.perform(get("/api/v1/mock-exams/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.status").value("in_progress"))
                .andExpect(jsonPath("$.data.saved_answers[0].explanation").value(""));
    }

    // ===== Mock timer =====

    @Test
    void getAttempt_includesTimerFields() throws Exception {
        Long attemptId = fixtures.insertInProgressMockAttempt(userId, mockExamId);

        mockMvc.perform(get("/api/v1/mock-exams/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.time_limit_seconds").isNumber())
                .andExpect(jsonPath("$.data.started_at").isString())
                .andExpect(jsonPath("$.data.time_used_seconds").value(-1));
    }

    @Test
    void submitMockExam_afterTimeLimit_endsByTimeout() throws Exception {
        // Default limit is 1800s; this attempt started 2000s ago → expired.
        Long attemptId = fixtures.insertInProgressMockAttemptStartedSecondsAgo(
                userId, mockExamId, 2000);

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/submit", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ended_by_timeout"))
                .andExpect(jsonPath("$.data.score_percent").isNumber());
    }

    @Test
    void saveAnswer_afterTimeLimit_expiresWith409() throws Exception {
        Long attemptId = fixtures.insertInProgressMockAttemptStartedSecondsAgo(
                userId, mockExamId, 2000);

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/answers", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(q1, v1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MOCK_EXPIRED"));

        // The late answer auto-finalized the attempt as a timeout.
        mockMvc.perform(get("/api/v1/mock-exams/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.status").value("ended_by_timeout"));
    }

    @Test
    void timeoutAttempt_isScoredAndCountsInStats() throws Exception {
        Long attemptId = fixtures.insertInProgressMockAttemptStartedSecondsAgo(
                userId, mockExamId, 2000);
        fixtures.insertMockAttemptResult(attemptId, q1, v1, "B", true); // 1 of 2 correct

        mockMvc.perform(post("/api/v1/mock-exams/attempts/{id}/submit", attemptId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.status").value("ended_by_timeout"))
                .andExpect(jsonPath("$.data.score_percent").value(50));

        // A timeout is a real score — it must feed best/latest stats.
        mockMvc.perform(get("/api/v1/mock-exams/attempts/stats")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.best_score_percent").value(50))
                .andExpect(jsonPath("$.data.latest_score_percent").value(50));
    }

    @Test
    void getAttempt_withLanguageOverride_returnsRequestedVariant() throws Exception {
        fixtures.insertAccessPass(userId, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);
        // Need ZH variants for the override path to find a row.
        fixtures.insertZhVariant(q1, "Q1 中文", "解释 1");
        fixtures.insertZhVariant(q2, "Q2 中文", "解释 2");

        String startBody = mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String attemptId = extractJsonString(startBody, "mock_attempt_id");

        mockMvc.perform(get("/api/v1/mock-exams/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .param("language", "zh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language").value("zh"));
    }

    @Test
    void getAttempt_blankLanguageParam_fallsBackToSessionLanguage() throws Exception {
        fixtures.insertAccessPass(userId, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);
        String startBody = mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String attemptId = extractJsonString(startBody, "mock_attempt_id");

        // Empty string param → service falls back to attempt's stored EN.
        mockMvc.perform(get("/api/v1/mock-exams/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + userId)
                        .param("language", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language").value("en"));
    }

    @Test
    void getAttempt_crossUser_returns403() throws Exception {
        fixtures.insertAccessPass(userId, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);
        String startBody = mockMvc.perform(post("/api/v1/mock-exams/attempts")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"en"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String attemptId = extractJsonString(startBody, "mock_attempt_id");

        Long otherUser = fixtures.insertUser("other@example.com");
        mockMvc.perform(get("/api/v1/mock-exams/attempts/{id}", attemptId)
                        .header("Authorization", "Bearer " + otherUser))
                .andExpect(status().isForbidden());
    }

    private static String extractJsonStringFromQuestions(String json, String field) {
        int qStart = json.indexOf("\"questions\":[");
        if (qStart < 0) return "";
        String tail = json.substring(qStart);
        String key = "\"" + field + "\":\"";
        int kStart = tail.indexOf(key);
        if (kStart < 0) return "";
        int valStart = kStart + key.length();
        int valEnd = tail.indexOf("\"", valStart);
        return tail.substring(valStart, valEnd);
    }

    private static String extractJsonString(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return "";
        int valStart = start + key.length();
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }

    // ===== /api/v1/mock-exams/attempts/history =====

    @Test
    void getAttemptHistory_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/mock-exams/attempts/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAttemptHistory_noAttempts_emptyList() throws Exception {
        mockMvc.perform(get("/api/v1/mock-exams/attempts/history")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempts", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.data.total_in_db").value(0));
    }

    @Test
    void getAttemptHistory_returnsAttemptsNewestFirst() throws Exception {
        Long mockExamId = fixtures.insertMockExam("TEST_30Q", 30);
        Long oldAttempt = fixtures.insertMockAttemptWithScore(userId, mockExamId, 70);
        Long newAttempt = fixtures.insertMockAttemptWithScore(userId, mockExamId, 85);

        mockMvc.perform(get("/api/v1/mock-exams/attempts/history")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_in_db").value(2))
                .andExpect(jsonPath("$.data.attempts", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.attempts[0].attempt_id").value(newAttempt.toString()))
                .andExpect(jsonPath("$.data.attempts[0].score_percent").value(85))
                .andExpect(jsonPath("$.data.attempts[0].status").value("submitted"))
                .andExpect(jsonPath("$.data.attempts[1].attempt_id").value(oldAttempt.toString()))
                .andExpect(jsonPath("$.data.attempts[1].score_percent").value(70));
    }

    // ===== /api/v1/mock-exams/attempts/stats =====

    @Test
    void getAttemptStats_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/mock-exams/attempts/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAttemptStats_emptyAccount_returnsZeros() throws Exception {
        mockMvc.perform(get("/api/v1/mock-exams/attempts/stats")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_attempts").value(0))
                .andExpect(jsonPath("$.data.submitted_count").value(0))
                .andExpect(jsonPath("$.data.exited_count").value(0))
                .andExpect(jsonPath("$.data.recent_3_avg_score_percent").value(-1))
                .andExpect(jsonPath("$.data.best_score_percent").value(-1))
                .andExpect(jsonPath("$.data.latest_score_percent").value(-1));
    }

    @Test
    void getAttemptStats_withSubmittedAttempts_aggregatesCorrectly() throws Exception {
        Long mockExamId = fixtures.insertMockExam("TEST_30Q", 30);
        fixtures.insertMockAttemptWithScore(userId, mockExamId, 70);
        fixtures.insertMockAttemptWithScore(userId, mockExamId, 80);
        fixtures.insertMockAttemptWithScore(userId, mockExamId, 90);

        mockMvc.perform(get("/api/v1/mock-exams/attempts/stats")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_attempts").value(3))
                .andExpect(jsonPath("$.data.submitted_count").value(3))
                .andExpect(jsonPath("$.data.exited_count").value(0))
                .andExpect(jsonPath("$.data.best_score_percent").value(90))
                .andExpect(jsonPath("$.data.latest_score_percent").value(90))
                .andExpect(jsonPath("$.data.recent_3_avg_score_percent").value(80));
    }
}
