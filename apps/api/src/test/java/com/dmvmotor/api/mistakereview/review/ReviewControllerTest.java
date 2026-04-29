package com.dmvmotor.api.mistakereview.review;

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

class ReviewControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;
    private Long topicId;
    private Long questionId1;
    private Long variantId1;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId     = fixtures.insertUser("dave@example.com");
        // Active pass required for review access
        fixtures.insertAccessPass(userId, "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);
        topicId    = fixtures.insertTopic("SIGNALS");
        questionId1 = fixtures.insertQuestion(topicId, "B");
        variantId1  = fixtures.insertVariantReturningId(questionId1, "en",
                "What does a flashing yellow light mean?",
                "[{\"key\":\"A\",\"text\":\"Stop\"},{\"key\":\"B\",\"text\":\"Caution\"},{\"key\":\"C\",\"text\":\"Go\"}]",
                "Flashing yellow means proceed with caution.");
    }

    // ---------------------------------------------------------------
    // GET /api/v1/review/pack
    // ---------------------------------------------------------------

    @Test
    void getReviewPack_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/review/pack"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReviewPack_noAccessPass_returns403() throws Exception {
        Long noPassUser = fixtures.insertUser("nopass@example.com");
        mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + noPassUser))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void getReviewPack_noMistakes_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NO_MISTAKES_TO_REVIEW"));
    }

    @Test
    void getReviewPack_withMistakes_createsAndReturnsPack() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 2, "practice");

        mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_pack_id").isString())
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.target_question_count").value(1))
                .andExpect(jsonPath("$.data.completed_question_count").value(0))
                .andExpect(jsonPath("$.data.tasks", hasSize(1)))
                .andExpect(jsonPath("$.data.tasks[0].review_task_id").isString())
                .andExpect(jsonPath("$.data.tasks[0].type").value("same_topic_retry"))
                .andExpect(jsonPath("$.data.tasks[0].topic_id").value(String.valueOf(topicId)))
                .andExpect(jsonPath("$.data.tasks[0].status").value("pending"))
                .andExpect(jsonPath("$.data.tasks[0].target_question_count").value(1))
                .andExpect(jsonPath("$.data.tasks[0].completed_question_count").value(0));
    }

    @Test
    void getReviewPack_calledTwice_returnsExistingPack() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");

        String firstBody = mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_pack_id")
                        .value(extractField(firstBody, "review_pack_id")));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/review/tasks/{id}/questions
    // ---------------------------------------------------------------

    @Test
    void getTaskQuestions_noAccessPass_returns403() throws Exception {
        // Defense in depth: even with a valid taskId, a user whose pass expires must not
        // be able to continue working on it. api-contract.md §15: "需要允许使用 review".
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String taskId = getTaskId();

        Long noPassUser = fixtures.insertUser("nopass2@example.com");
        mockMvc.perform(get("/api/v1/review/tasks/{id}/questions", taskId)
                        .header("Authorization", "Bearer " + noPassUser)
                        .param("language", "en"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void submitAnswer_noAccessPass_returns403() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String taskId = getTaskId();

        Long noPassUser = fixtures.insertUser("nopass3@example.com");
        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + noPassUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void completeTask_noAccessPass_returns403() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String taskId = getTaskId();

        Long noPassUser = fixtures.insertUser("nopass4@example.com");
        mockMvc.perform(post("/api/v1/review/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + noPassUser))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void getTaskQuestions_invalidTaskId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/review/tasks/{id}/questions", "999999")
                        .header("Authorization", "Bearer " + userId)
                        .param("language", "en"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTaskQuestions_validTask_returnsQuestions() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");

        String packBody = mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andReturn().getResponse().getContentAsString();

        String taskId = extractTaskId(packBody);

        mockMvc.perform(get("/api/v1/review/tasks/{id}/questions", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_task_id").value(taskId))
                .andExpect(jsonPath("$.data.questions", hasSize(1)))
                .andExpect(jsonPath("$.data.questions[0].question_id").isString())
                .andExpect(jsonPath("$.data.questions[0].stem").isString())
                .andExpect(jsonPath("$.data.questions[0].choices", hasSize(3)));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/review/tasks/{id}/answers
    // ---------------------------------------------------------------

    @Test
    void submitReviewAnswer_correct_resolvesMistake() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String taskId = getTaskId();

        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_correct").value(true))
                .andExpect(jsonPath("$.data.correct_choice_key").value("B"))
                .andExpect(jsonPath("$.data.task_progress.answered_count").value(1))
                .andExpect(jsonPath("$.data.task_progress.target_count").value(1));
    }

    @Test
    void submitReviewAnswer_correct_doesNotImmediatelyDeactivateMistake() throws Exception {
        // Correct answer during review must NOT deactivate the mistake right away.
        // Deactivation happens only after completeTask, per review-and-readiness-engine.md.
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String packBody = mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andReturn().getResponse().getContentAsString();
        String taskId = extractTaskId(packBody);

        // Answer correctly
        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_correct").value(true));

        // Mistake NOT yet deactivated → pack still exists (same pack is returned)
        mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_pack_id")
                        .value(extractField(packBody, "review_pack_id")));
    }

    @Test
    void submitReviewAnswer_withLanguageParam_usesSpecifiedLanguage() throws Exception {
        // Insert a Chinese variant for the question
        fixtures.insertVariantReturningId(questionId1, "zh", "这道题是什么意思？",
                "[{\"key\":\"A\",\"text\":\"停止\"},{\"key\":\"B\",\"text\":\"注意\"}]",
                "闪烁黄灯表示谨慎前行。");
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String taskId = getTaskId();

        // Submit with language=zh; answer must be judged against zh variant
        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B","language":"zh"}
                                """.formatted(questionId1, variantId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_correct").value(true))
                .andExpect(jsonPath("$.data.correct_choice_key").value("B"));
    }

    @Test
    void submitReviewAnswer_questionNotInTask_returns400_andDoesNotBumpProgress() throws Exception {
        // B1: Submit a question that's not part of this task. Bug under audit: 400 returns,
        // but markQuestionAnswered's UPDATE matches 0 rows while incrementCompletedCount still
        // bumps the counter and saveReviewAttempt writes a bogus row. Fix must reject upfront.
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String taskId = getTaskId();

        // A question on a different topic — guaranteed not in the same-topic task.
        Long otherTopic = fixtures.insertTopic("OTHER_TOPIC");
        Long otherQid   = fixtures.insertQuestion(otherTopic, "A");
        Long otherVid   = fixtures.insertVariantReturningId(otherQid, "en", "Off-topic?",
                "[{\"key\":\"A\",\"text\":\"x\"},{\"key\":\"B\",\"text\":\"y\"}]", "expl");

        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(otherQid, otherVid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("QUESTION_NOT_IN_TASK"));

        // Counter must NOT have been bumped — the legit follow-up should report 1, not 2.
        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task_progress.answered_count").value(1));
    }

    @Test
    void submitReviewAnswer_completedTask_returns409() throws Exception {
        // B2: After a task is marked completed, no further answers should be accepted —
        // currently slips through because requireTask only checks ownership.
        Long q2 = fixtures.insertQuestion(topicId, "A");
        Long v2 = fixtures.insertVariantReturningId(q2, "en", "Second?",
                "[{\"key\":\"A\",\"text\":\"Yes\"},{\"key\":\"B\",\"text\":\"No\"}]", "expl");
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        fixtures.insertMistakeRecord(userId, q2,          topicId, 1, "practice");

        String taskId = getTaskId();
        // Answer q1, then complete the task while q2 remains unanswered.
        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)));
        mockMvc.perform(post("/api/v1/review/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());

        // Try to submit the still-pending q2 on the completed task → should reject.
        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(q2, v2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT_STATE"));
    }

    @Test
    void submitReviewAnswer_alreadyAnswered_returns409() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String taskId = getTaskId();

        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)));

        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("QUESTION_ALREADY_SUBMITTED"));
    }

    @Test
    void submitReviewAnswer_secondQuestion_taskAlreadyInProgress() throws Exception {
        Long q2 = fixtures.insertQuestion(topicId, "A");
        Long v2 = fixtures.insertVariantReturningId(q2, "en", "Second question?",
                "[{\"key\":\"A\",\"text\":\"Yes\"},{\"key\":\"B\",\"text\":\"No\"}]",
                null);
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        fixtures.insertMistakeRecord(userId, q2, topicId, 1, "practice");

        String taskId = getTaskId();

        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)));

        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(q2, v2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.explanation").value(""))
                .andExpect(jsonPath("$.data.task_progress.answered_count").value(2));
    }

    @Test
    void submitReviewAnswer_wrong_keepsMistakeActive() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String taskId = getTaskId();

        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(questionId1, variantId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_correct").value(false))
                .andExpect(jsonPath("$.data.correct_choice_key").value("B"));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/review/tasks/{id}/complete
    // ---------------------------------------------------------------

    @Test
    void completeTask_setsStatusCompleted() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String taskId = getTaskId();

        mockMvc.perform(post("/api/v1/review/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_task_id").value(taskId))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.next_action.type").value("continue_review"));
    }

    @Test
    void completeTask_masteryReached_deactivatesMistake() throws Exception {
        // Seed a mastery-qualifying practice history: 10 attempts in this topic, 9 correct,
        // last 8 all correct. Then complete a review task → mistake gets deactivated.
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        seedPracticeHistory(topicId, /* correct */ 9, /* incorrect */ 1);

        String packBody = mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andReturn().getResponse().getContentAsString();
        String taskId = extractTaskId(packBody);

        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)));

        mockMvc.perform(post("/api/v1/review/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true));

        // Mastery reached → mistake deactivated → getMistakes returns empty, getPack 404
        mockMvc.perform(get("/api/v1/mistakes")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));
        mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NO_MISTAKES_TO_REVIEW"));
    }

    @Test
    void completeTask_topicUnderThreshold_keepsMistakeActive() throws Exception {
        // 10 attempts, 7 correct → 70% < 80% threshold → not mastered despite task completion.
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        seedPracticeHistory(topicId, /* correct */ 7, /* incorrect */ 3);

        String taskId = getTaskId();
        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)));
        mockMvc.perform(post("/api/v1/review/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true));

        // Mistake still active — user hasn't demonstrated topic mastery
        mockMvc.perform(get("/api/v1/mistakes")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].question_id")
                        .value(String.valueOf(questionId1)));
    }

    @Test
    void completeTask_insufficientHistory_keepsMistakeActive() throws Exception {
        // Only 5 practice attempts — below the recent-window size (8). Even all-correct
        // is insufficient evidence per MVP mastery spec.
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        seedPracticeHistory(topicId, /* correct */ 5, /* incorrect */ 0);

        String taskId = getTaskId();
        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)));
        mockMvc.perform(post("/api/v1/review/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/mistakes")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Seeds {@code correct + incorrect} practice attempts for {@code targetTopicId} in
     * the user's current learning cycle (0). Each attempt uses a fresh question to satisfy
     * the {@code uq_session_question} constraint. Incorrect attempts are inserted first so
     * correct attempts dominate the recent window regardless of timestamp tie-breaking.
     */
    private void seedPracticeHistory(Long targetTopicId, int correct, int incorrect) {
        Long sessionId = fixtures.insertPracticeSession(userId, 0);
        String choicesJson =
                "[{\"key\":\"A\",\"text\":\"x\"},{\"key\":\"B\",\"text\":\"y\"}]";
        for (int i = 0; i < incorrect; i++) {
            Long q = fixtures.insertQuestion(targetTopicId, "A");
            Long v = fixtures.insertVariantReturningId(q, "en",
                    "Wrong " + i, choicesJson, null);
            fixtures.insertPracticeAttempt(userId, sessionId, q, v, "B", false);
        }
        for (int i = 0; i < correct; i++) {
            Long q = fixtures.insertQuestion(targetTopicId, "A");
            Long v = fixtures.insertVariantReturningId(q, "en",
                    "Right " + i, choicesJson, null);
            fixtures.insertPracticeAttempt(userId, sessionId, q, v, "A", true);
        }
    }

    private String getTaskId() throws Exception {
        String packBody = mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andReturn().getResponse().getContentAsString();
        return extractTaskId(packBody);
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key) + key.length();
        int end   = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private String extractTaskId(String packBody) {
        String key = "\"review_task_id\":\"";
        int start = packBody.indexOf(key) + key.length();
        int end   = packBody.indexOf("\"", start);
        return packBody.substring(start, end);
    }
}
