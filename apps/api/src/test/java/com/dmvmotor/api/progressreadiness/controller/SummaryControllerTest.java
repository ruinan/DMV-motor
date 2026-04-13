package com.dmvmotor.api.progressreadiness.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.hasItem;
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

    @Test
    void getReadiness_withUnattendedKeyCoverageQuestions_hasKeyCoverageGate() throws Exception {
        // Insert a key-coverage question the user has never answered.
        // KEY_COVERAGE_INCOMPLETE gate must fire.
        Long kq = fixtures.insertKeyCoverageQuestion(topicId, "A");
        fixtures.insertVariant(kq, "en", "Key question?",
                "[{\"key\":\"A\",\"text\":\"Yes\"},{\"key\":\"B\",\"text\":\"No\"}]",
                "Key explanation");

        mockMvc.perform(get("/api/v1/readiness")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_ready_candidate").value(false))
                .andExpect(jsonPath("$.data.missing_gates", hasItem("KEY_COVERAGE_INCOMPLETE")));
    }

    @Test
    void getReadiness_passedMockInPreviousCycle_notReady() throws Exception {
        // Insert a passing mock in learning_cycle=0, then reset the user (cycle becomes 1).
        // The old mock score must NOT satisfy the readiness gate in the new cycle.
        Long mockExamId = fixtures.insertMockExam("READINESS_CYCLE_TEST_V1", 1);
        Long q1 = fixtures.insertQuestion(topicId, "A");
        fixtures.insertMockExamQuestion(mockExamId, q1, 1);
        fixtures.insertVariant(q1, "en", "Cycle test?",
                "[{\"key\":\"A\",\"text\":\"Yes\"}]", "Explanation");

        fixtures.insertMockAttemptWithScore(userId, mockExamId, 90, /* learningCycle */ 0);

        // Simulate soft reset: user is now on cycle 1
        fixtures.incrementUserResetCount(userId);

        mockMvc.perform(get("/api/v1/readiness")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_ready_candidate").value(false))
                .andExpect(jsonPath("$.data.missing_gates", hasItem("MOCK_SCORE_NOT_STABLE")));
    }
}
