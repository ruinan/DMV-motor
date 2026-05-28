package com.dmvmotor.api.aisupport.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiReviewPlanControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;
    private Long mockExamId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId     = fixtures.insertUser("planner@example.com");
        mockExamId = fixtures.insertMockExam("REVIEW_PLAN_EXAM", 2);
    }

    @Test
    void reviewPlan_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/review-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mock_attempt_id":"1"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reviewPlan_completedAttempt_returnsPlanThenCached() throws Exception {
        Long attemptId = fixtures.insertMockAttemptWithScore(userId, mockExamId, 70);

        // First call — generates via stub provider, cached=false
        mockMvc.perform(post("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mock_attempt_id":"%s","language":"en"}
                                """.formatted(attemptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached").value(false))
                .andExpect(jsonPath("$.data.plan").value(containsString("stub:review-plan")));

        // Second call — served from the persisted column, cached=true
        mockMvc.perform(post("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mock_attempt_id":"%s","language":"en"}
                                """.formatted(attemptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached").value(true));
    }

    @Test
    void reviewPlan_inProgressAttempt_returns409() throws Exception {
        Long attemptId = fixtures.insertInProgressMockAttempt(userId, mockExamId);

        mockMvc.perform(post("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mock_attempt_id":"%s"}
                                """.formatted(attemptId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MOCK_NOT_COMPLETED"));
    }

    @Test
    void reviewPlan_crossUser_returns403() throws Exception {
        Long attemptId = fixtures.insertMockAttemptWithScore(userId, mockExamId, 80);
        Long otherUser = fixtures.insertUser("intruder@example.com");

        mockMvc.perform(post("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + otherUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mock_attempt_id":"%s"}
                                """.formatted(attemptId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewPlan_unknownAttempt_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mock_attempt_id":"999999"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void reviewPlan_withRealQuestionsAndWrongs_buildsScoredInput() throws Exception {
        // Wire a 2-question exam with one correct + one wrong answered result so
        // the service computes total=2, score=50, passed=false, and joins one
        // wrong-answer detail (exercises the real scoring + wrong-join path).
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

        mockMvc.perform(post("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mock_attempt_id":"%s"}
                                """.formatted(attemptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached").value(false))
                .andExpect(jsonPath("$.data.plan").value(containsString("score=50%")))
                .andExpect(jsonPath("$.data.plan").value(containsString("wrong=1")));
    }

    @Test
    void reviewPlan_perfectScore_passedTrueNoWrongs() throws Exception {
        Long topicId = fixtures.insertTopic("EXAM_T2", "Exam Topic 2", "考试2", false, 6);
        Long q1 = fixtures.insertQuestion(topicId, "A");
        Long v1 = fixtures.insertVariantReturningId(q1, "en", "Q1",
                "[{\"key\":\"A\",\"text\":\"a\"},{\"key\":\"B\",\"text\":\"b\"}]", "e1");
        Long examId = fixtures.insertMockExam("PERFECT_EXAM", 1);
        fixtures.insertMockExamQuestion(examId, q1, 1);

        Long attemptId = fixtures.insertMockAttemptWithScore(userId, examId, 100);
        fixtures.insertMockAttemptResult(attemptId, q1, v1, "A", true);

        mockMvc.perform(post("/api/v1/ai/review-plan")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mock_attempt_id":"%s","language":""}
                                """.formatted(attemptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan").value(containsString("score=100%")))
                .andExpect(jsonPath("$.data.plan").value(containsString("passed=true")));
    }
}
