package com.dmvmotor.api.aisupport.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import com.dmvmotor.api.aisupport.application.AiReviewPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * When the AI feature is switched off, the read endpoint reports
 * {@code unavailable} and the background job is a no-op (no plan persisted).
 */
@TestPropertySource(properties = "app.ai.enabled=false")
class AiReviewPlanDisabledTest extends IntegrationTestBase {

    @Autowired MockMvc            mockMvc;
    @Autowired TestFixtures       fixtures;
    @Autowired AiReviewPlanService reviewPlanService;

    private Long userId;
    private Long mockExamId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId     = fixtures.insertUser("disabled@example.com");
        mockExamId = fixtures.insertMockExam("DISABLED_EXAM", 2);
    }

    @Test
    void reviewPlan_aiDisabled_unavailable() throws Exception {
        Long attemptId = fixtures.insertMockAttemptWithScore(userId, mockExamId, 70);
        // Background job is a no-op while disabled.
        reviewPlanService.generateAndCache(attemptId, userId);

        mockMvc.perform(get("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .param("mock_attempt_id", String.valueOf(attemptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("unavailable"));
    }
}
