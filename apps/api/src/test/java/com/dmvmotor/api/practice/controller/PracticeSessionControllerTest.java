package com.dmvmotor.api.practice.controller;

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

class PracticeSessionControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired TestFixtures fixtures;

    private Long topicId;
    private Long questionId;
    private Long variantEnId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();

        topicId   = fixtures.insertTopic("TRAFFIC_SIGNS", "Traffic Signs", "交通标志", true, 1);
        questionId = fixtures.insertQuestion(topicId, "B");
        variantEnId = fixtures.insertVariantReturningId(questionId, "en",
                "What does a stop sign look like?",
                "[{\"key\":\"A\",\"text\":\"Yellow triangle\"},{\"key\":\"B\",\"text\":\"Red octagon\"},{\"key\":\"C\",\"text\":\"Green circle\"}]",
                "A stop sign is a red octagon.");
        fixtures.insertVariant(questionId, "zh",
                "停车标志是什么样子的？",
                "[{\"key\":\"A\",\"text\":\"黄色三角形\"},{\"key\":\"B\",\"text\":\"红色八角形\"},{\"key\":\"C\",\"text\":\"绿色圆形\"}]",
                "停车标志是红色八角形。");
    }

    // ---------------------------------------------------------------
    // POST /api/v1/practice/sessions
    // ---------------------------------------------------------------

    @Test
    void startSession_noLanguage_defaultsToEnglish() throws Exception {
        mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.language").value("en"));
    }

    @Test
    void startSession_freeTrial_returnsSessionWithFirstQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"en"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").isString())
                .andExpect(jsonPath("$.data.entryType").value("free_trial"))
                .andExpect(jsonPath("$.data.status").value("in_progress"))
                .andExpect(jsonPath("$.data.language").value("en"))
                .andExpect(jsonPath("$.data.nextQuestion.questionId").isString())
                .andExpect(jsonPath("$.data.nextQuestion.stem").isString())
                .andExpect(jsonPath("$.data.nextQuestion.choices", hasSize(3)));
    }

    @Test
    void startSession_missingEntryType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void startSession_noQuestionsAvailable_returns422() throws Exception {
        fixtures.truncateAll(); // remove all questions

        mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"en"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_QUESTIONS_AVAILABLE"));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/practice/sessions/{id}/next-question
    // ---------------------------------------------------------------

    @Test
    void nextQuestion_activeSession_returnsQuestion() throws Exception {
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionId").isString())
                .andExpect(jsonPath("$.data.stem").isString())
                .andExpect(jsonPath("$.data.choices", hasSize(3)))
                .andExpect(jsonPath("$.data.progress.answeredCount").value(0));
    }

    @Test
    void nextQuestion_sessionNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void nextQuestion_allQuestionsAnswered_returns404WithSessionCompleted() throws Exception {
        String sessionId = startSessionAndGetId("en");
        submitAnswer(sessionId, questionId, variantEnId, "B"); // answer the only question

        mockMvc.perform(get("/api/v1/practice/sessions/{id}/next-question", sessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SESSION_COMPLETED"));
    }

    // ---------------------------------------------------------------
    // POST /api/v1/practice/sessions/{id}/answers
    // ---------------------------------------------------------------

    @Test
    void submitAnswer_correctAnswer_returnsIsCorrectTrue() throws Exception {
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId, variantEnId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isCorrect").value(true))
                .andExpect(jsonPath("$.data.correctChoiceKey").value("B"))
                .andExpect(jsonPath("$.data.explanation").isString())
                .andExpect(jsonPath("$.data.progress.answeredCount").value(1));
    }

    @Test
    void submitAnswer_wrongAnswer_recordsMistakeAndReturnsCorrectKey() throws Exception {
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"A"}
                                """.formatted(questionId, variantEnId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isCorrect").value(false))
                .andExpect(jsonPath("$.data.correctChoiceKey").value("B"));
    }

    @Test
    void submitAnswer_nullExplanation_returnsEmptyString() throws Exception {
        fixtures.truncateAll();
        Long t2 = fixtures.insertTopic("NULL_EXPL_TOPIC");
        Long q2 = fixtures.insertQuestion(t2, "A");
        Long v2 = fixtures.insertVariantReturningId(q2, "en", "Which option?",
                "[{\"key\":\"A\",\"text\":\"Opt A\"},{\"key\":\"B\",\"text\":\"Opt B\"}]",
                null); // null explanation

        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(q2, v2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.explanation").value(""));
    }

    @Test
    void submitAnswer_alreadyAnswered_returns409() throws Exception {
        String sessionId = startSessionAndGetId("en");
        submitAnswer(sessionId, questionId, variantEnId, "A");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_id":"%s","variant_id":"%s","selected_choice_key":"B"}
                                """.formatted(questionId, variantEnId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("QUESTION_ALREADY_SUBMITTED"));
    }

    // ---------------------------------------------------------------
    // GET /api/v1/practice/sessions/{id}
    // ---------------------------------------------------------------

    @Test
    void getSession_returnsStatusAndProgress() throws Exception {
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(get("/api/v1/practice/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.status").value("in_progress"))
                .andExpect(jsonPath("$.data.answeredCount").value(0))
                .andExpect(jsonPath("$.data.totalCount").isNumber());
    }

    // ---------------------------------------------------------------
    // POST /api/v1/practice/sessions/{id}/complete
    // ---------------------------------------------------------------

    @Test
    void completeSession_setsStatusCompleted() throws Exception {
        String sessionId = startSessionAndGetId("en");

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/complete", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.status").value("completed"));
    }

    @Test
    void completeSession_alreadyCompleted_returns409() throws Exception {
        String sessionId = startSessionAndGetId("en");
        mockMvc.perform(post("/api/v1/practice/sessions/{id}/complete", sessionId));

        mockMvc.perform(post("/api/v1/practice/sessions/{id}/complete", sessionId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT_STATE"));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String startSessionAndGetId(String language) throws Exception {
        var result = mockMvc.perform(post("/api/v1/practice/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entry_type":"free_trial","language":"%s"}
                                """.formatted(language)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        // extract sessionId from JSON response
        int start = body.indexOf("\"sessionId\":\"") + 13;
        int end   = body.indexOf("\"", start);
        return body.substring(start, end);
    }

    private void submitAnswer(String sessionId, Long qId, Long vId, String choice) throws Exception {
        mockMvc.perform(post("/api/v1/practice/sessions/{id}/answers", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"question_id":"%s","variant_id":"%s","selected_choice_key":"%s"}
                        """.formatted(qId, vId, choice)));
    }
}
