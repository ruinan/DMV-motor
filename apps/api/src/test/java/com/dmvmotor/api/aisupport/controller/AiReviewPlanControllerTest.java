package com.dmvmotor.api.aisupport.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import com.dmvmotor.api.aisupport.application.AiReviewPlanService;
import com.dmvmotor.api.mockexam.application.MockAttemptCompletedEvent;
import com.dmvmotor.api.mockexam.application.MockExamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Review plans are now generated automatically when a mock completes (async,
 * server-driven). The client only reads via GET — there is no user-facing
 * generate trigger. These tests cover the read endpoint's status states plus
 * the auto-trigger wiring (event publish + the background job).
 */
@RecordApplicationEvents
class AiReviewPlanControllerTest extends IntegrationTestBase {

    @Autowired MockMvc            mockMvc;
    @Autowired TestFixtures       fixtures;
    @Autowired AiReviewPlanService reviewPlanService;
    @Autowired MockExamService    mockExamService;
    @Autowired ApplicationEvents  applicationEvents;

    private Long userId;
    private Long mockExamId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId     = fixtures.insertUser("planner@example.com");
        mockExamId = fixtures.insertMockExam("REVIEW_PLAN_EXAM", 2);
    }

    // ===== GET /api/v1/ai/review-plan =====

    @Test
    void reviewPlan_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/ai/review-plan").param("mock_attempt_id", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reviewPlan_terminalAttempt_noPlanYet_pending() throws Exception {
        Long attemptId = fixtures.insertMockAttemptWithScore(userId, mockExamId, 70);

        mockMvc.perform(get("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .param("mock_attempt_id", String.valueOf(attemptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.plan").value(""));
    }

    @Test
    void reviewPlan_afterGeneration_ready() throws Exception {
        Long attemptId = fixtures.insertMockAttemptWithScore(userId, mockExamId, 70);
        // Simulate the background job having run.
        reviewPlanService.generateAndCache(attemptId, userId);

        mockMvc.perform(get("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .param("mock_attempt_id", String.valueOf(attemptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ready"))
                .andExpect(jsonPath("$.data.plan").value(containsString("stub:review-plan")));
    }

    @Test
    void reviewPlan_inProgressAttempt_pending() throws Exception {
        Long attemptId = fixtures.insertInProgressMockAttempt(userId, mockExamId);

        mockMvc.perform(get("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .param("mock_attempt_id", String.valueOf(attemptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    void reviewPlan_crossUser_returns403() throws Exception {
        Long attemptId = fixtures.insertMockAttemptWithScore(userId, mockExamId, 80);
        Long otherUser = fixtures.insertUser("intruder@example.com");

        mockMvc.perform(get("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + otherUser)
                        .param("mock_attempt_id", String.valueOf(attemptId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewPlan_unknownAttempt_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .param("mock_attempt_id", "999999"))
                .andExpect(status().isNotFound());
    }

    // ===== Background job: generateAndCache =====

    @Test
    void generateAndCache_idempotent_secondCallKeepsPlan() throws Exception {
        Long attemptId = fixtures.insertMockAttemptWithScore(userId, mockExamId, 70);
        reviewPlanService.generateAndCache(attemptId, userId);
        // Second call must not throw and must leave the plan intact (cache hit).
        reviewPlanService.generateAndCache(attemptId, userId);

        mockMvc.perform(get("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .param("mock_attempt_id", String.valueOf(attemptId)))
                .andExpect(jsonPath("$.data.status").value("ready"));
    }

    @Test
    void generateAndCache_unknownAttempt_noThrow() {
        // Defensive: a stale event for a deleted attempt must not blow up the
        // background job.
        reviewPlanService.generateAndCache(999999L, userId);
    }

    @Test
    void generateAndCache_ownerMismatch_noPlan() throws Exception {
        Long attemptId = fixtures.insertMockAttemptWithScore(userId, mockExamId, 70);
        Long otherUser = fixtures.insertUser("ghost@example.com");
        // Event carries the wrong user — job must skip, leaving no plan.
        reviewPlanService.generateAndCache(attemptId, otherUser);

        mockMvc.perform(get("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .param("mock_attempt_id", String.valueOf(attemptId)))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    void generateAndCache_inProgressAttempt_noPlan() throws Exception {
        Long attemptId = fixtures.insertInProgressMockAttempt(userId, mockExamId);
        reviewPlanService.generateAndCache(attemptId, userId);

        mockMvc.perform(get("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .param("mock_attempt_id", String.valueOf(attemptId)))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    void generateAndCache_withRealQuestionsAndWrongs_buildsScoredInput() throws Exception {
        Long topicId = fixtures.insertTopic("EXAM_T", "Exam Topic", "考试", false, 5);
        Long q1 = fixtures.insertQuestion(topicId, "B");
        Long v1 = fixtures.insertVariantReturningId(q1, "en", "Q1 stem",
                "[{\"key\":\"A\",\"text\":\"a\"},{\"key\":\"B\",\"text\":\"b\"}]", "e1");
        Long q2 = fixtures.insertQuestion(topicId, "A");
        Long v2 = fixtures.insertVariantReturningId(q2, "en", "Q2 stem",
                "[{\"key\":\"A\",\"text\":\"a\"},{\"key\":\"B\",\"text\":\"b\"}]", "e2");
        Long examId = fixtures.insertMockExam("SCORED_EXAM", 2);
        fixtures.insertMockExamQuestion(examId, q1, 1);
        fixtures.insertMockExamQuestion(examId, q2, 2);

        Long attemptId = fixtures.insertMockAttemptWithScore(userId, examId, 50);
        fixtures.insertMockAttemptResult(attemptId, q1, v1, "B", true);  // correct
        fixtures.insertMockAttemptResult(attemptId, q2, v2, "B", false); // wrong

        reviewPlanService.generateAndCache(attemptId, userId);

        mockMvc.perform(get("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .param("mock_attempt_id", String.valueOf(attemptId)))
                .andExpect(jsonPath("$.data.plan").value(containsString("score=50%")))
                .andExpect(jsonPath("$.data.plan").value(containsString("wrong=1")));
    }

    // ===== Auto-trigger wiring =====

    @Test
    void mockCompletion_publishesCompletedEvent() {
        Long attemptId = fixtures.insertInProgressMockAttempt(userId, mockExamId);

        mockExamService.exitAttempt(attemptId, userId);

        long published = applicationEvents.stream(MockAttemptCompletedEvent.class)
                .filter(e -> e.attemptId().equals(attemptId) && e.userId().equals(userId))
                .count();
        assertThat(published).isEqualTo(1);
    }
}
