package com.dmvmotor.api.mistakereview.review;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
                .andExpect(jsonPath("$.data.reviewPackId").isString())
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.tasks", hasSize(1)))
                .andExpect(jsonPath("$.data.tasks[0].reviewTaskId").isString())
                .andExpect(jsonPath("$.data.tasks[0].type").value("same_topic_retry"))
                .andExpect(jsonPath("$.data.tasks[0].topicId").value(String.valueOf(topicId)))
                .andExpect(jsonPath("$.data.tasks[0].status").value("pending"));
    }

    @Test
    void getReviewPack_calledTwice_returnsExistingPack() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");

        // First call creates pack
        String firstBody = mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Second call returns same pack
        mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewPackId")
                        .value(extractField(firstBody, "reviewPackId")));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/review/tasks/{id}/questions
    // ---------------------------------------------------------------

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
                .andExpect(jsonPath("$.data.reviewTaskId").value(taskId))
                .andExpect(jsonPath("$.data.questions", hasSize(1)))
                .andExpect(jsonPath("$.data.questions[0].questionId").isString())
                .andExpect(jsonPath("$.data.questions[0].stem").isString())
                .andExpect(jsonPath("$.data.questions[0].choices", hasSize(3)));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/review/tasks/{id}/answers
    // ---------------------------------------------------------------

    @Test
    void submitReviewAnswer_correct_resolvesMistake() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String packBody = mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andReturn().getResponse().getContentAsString();
        String taskId = extractTaskId(packBody);

        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isCorrect").value(true))
                .andExpect(jsonPath("$.data.correctChoiceKey").value("B"))
                .andExpect(jsonPath("$.data.taskProgress.answeredCount").value(1))
                .andExpect(jsonPath("$.data.taskProgress.targetCount").value(1));
    }

    @Test
    void submitReviewAnswer_alreadyAnswered_returns409() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String packBody = mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andReturn().getResponse().getContentAsString();
        String taskId = extractTaskId(packBody);

        // First answer
        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)));

        // Second attempt on same question
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
        // Two questions in same topic → task has 2 questions
        Long q2 = fixtures.insertQuestion(topicId, "A");
        Long v2 = fixtures.insertVariantReturningId(q2, "en", "Second question?",
                "[{\"key\":\"A\",\"text\":\"Yes\"},{\"key\":\"B\",\"text\":\"No\"}]",
                null); // null explanation to cover ReviewController null branch
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        fixtures.insertMistakeRecord(userId, q2, topicId, 1, "practice");

        String packBody = mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andReturn().getResponse().getContentAsString();
        String taskId = extractTaskId(packBody);

        // First answer → task becomes in_progress
        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId1, variantId1)));

        // Second answer → task already in_progress + null explanation
        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(q2, v2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.explanation").value(""))
                .andExpect(jsonPath("$.data.taskProgress.answeredCount").value(2));
    }

    @Test
    void submitReviewAnswer_wrong_keepsMistakeActive() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String packBody = mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andReturn().getResponse().getContentAsString();
        String taskId = extractTaskId(packBody);

        mockMvc.perform(post("/api/v1/review/tasks/{id}/answers", taskId)
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(questionId1, variantId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isCorrect").value(false))
                .andExpect(jsonPath("$.data.correctChoiceKey").value("B"));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/review/tasks/{id}/complete
    // ---------------------------------------------------------------

    @Test
    void completeTask_setsStatusCompleted() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        String packBody = mockMvc.perform(get("/api/v1/review/pack")
                        .header("Authorization", "Bearer " + userId))
                .andReturn().getResponse().getContentAsString();
        String taskId = extractTaskId(packBody);

        mockMvc.perform(post("/api/v1/review/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewTaskId").value(taskId))
                .andExpect(jsonPath("$.data.completed").value(true));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key) + key.length();
        int end   = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private String extractTaskId(String packBody) {
        String key = "\"reviewTaskId\":\"";
        int start = packBody.indexOf(key) + key.length();
        int end   = packBody.indexOf("\"", start);
        return packBody.substring(start, end);
    }
}
