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
                .andExpect(jsonPath("$.data.completionScore").isNumber())
                .andExpect(jsonPath("$.data.readinessScore").isNumber())
                .andExpect(jsonPath("$.data.isReadyCandidate").value(false))
                .andExpect(jsonPath("$.data.weakTopics").isArray());
    }

    @Test
    void getSummary_withMistakes_showsWeakTopics() throws Exception {
        Long q1 = fixtures.insertQuestion(topicId, "A");
        fixtures.insertMistakeRecord(userId, q1, topicId, 3, "practice");

        mockMvc.perform(get("/api/v1/summary")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weakTopics[0].topicId")
                        .value(String.valueOf(topicId)));
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
                .andExpect(jsonPath("$.data.readinessScore").isNumber())
                .andExpect(jsonPath("$.data.isReadyCandidate").value(false))
                .andExpect(jsonPath("$.data.missingGates").isArray());
    }

    @Test
    void getReadiness_userWithPassedMockAndNoMistakes_returnsReady() throws Exception {
        // Insert a passed mock attempt (≥83% score)
        Long mockExamId = fixtures.insertMockExam("READINESS_TEST_V1", 1);
        Long q1 = fixtures.insertQuestion(topicId, "A");
        fixtures.insertMockExamQuestion(mockExamId, q1, 1);
        fixtures.insertVariant(q1, "en", "Test?",
                "[{\"key\":\"A\",\"text\":\"Yes\"}]", "Explanation");

        fixtures.insertMockAttemptWithScore(userId, mockExamId, 90);

        mockMvc.perform(get("/api/v1/readiness")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isReadyCandidate").value(true));
    }
}
