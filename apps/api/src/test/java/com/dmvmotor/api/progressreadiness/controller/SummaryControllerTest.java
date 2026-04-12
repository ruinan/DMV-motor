package com.dmvmotor.api.progressreadiness.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SummaryControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;
    private Long topicId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId  = fixtures.insertUser("summary@example.com");
        topicId = fixtures.insertTopic("SUMMARY_TOPIC");
    }

    // ---------------------------------------------------------------
    // GET /api/v1/summary
    // ---------------------------------------------------------------

    @Test
    void getSummary_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSummary_newUser_returnsZeroScores() throws Exception {
        mockMvc.perform(get("/api/v1/summary")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completion_score").isNumber())
                .andExpect(jsonPath("$.data.readiness_score").isNumber())
                .andExpect(jsonPath("$.data.is_ready_candidate").value(false))
                .andExpect(jsonPath("$.data.weak_topics").isArray())
                .andExpect(jsonPath("$.data.next_action.type").isString())
                .andExpect(jsonPath("$.data.next_action.label").isString());
    }

    @Test
    void getSummary_withMistakes_showsWeakTopics() throws Exception {
        Long q1 = fixtures.insertQuestion(topicId, "A");
        fixtures.insertMistakeRecord(userId, q1, topicId, 3, "practice");

        mockMvc.perform(get("/api/v1/summary")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weak_topics[0].topic_id")
                        .value(String.valueOf(topicId)))
                .andExpect(jsonPath("$.data.weak_topics[0].label").isString());
    }

    // ---------------------------------------------------------------
    // GET /api/v1/readiness
    // ---------------------------------------------------------------

    @Test
    void getReadiness_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/readiness"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReadiness_newUser_returnsNotReady() throws Exception {
        mockMvc.perform(get("/api/v1/readiness")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readiness_score").isNumber())
                .andExpect(jsonPath("$.data.is_ready_candidate").value(false))
                .andExpect(jsonPath("$.data.missing_gates").isArray());
    }

    @Test
    void getReadiness_userWithPassedMockAndNoMistakes_returnsReady() throws Exception {
        Long mockExamId = fixtures.insertMockExam("READINESS_TEST_V1", 1);
        Long q1 = fixtures.insertQuestion(topicId, "A");
        fixtures.insertMockExamQuestion(mockExamId, q1, 1);
        fixtures.insertVariant(q1, "en", "Test?",
                "[{\"key\":\"A\",\"text\":\"Yes\"}]", "Explanation");

        fixtures.insertMockAttemptWithScore(userId, mockExamId, 90);

        mockMvc.perform(get("/api/v1/readiness")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_ready_candidate").value(true));
    }
}
