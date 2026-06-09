package com.dmvmotor.api.aisupport.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecommendationControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId = fixtures.insertUser("alice@example.com");
        // Next-step recommendations are a paid perk (bug4). Grant a pass for the
        // user's current (default) exam so the ranking tests exercise the logic;
        // the free-user empty case is covered separately.
        fixtures.insertAccessPassForExam(userId, fixtures.defaultExamId(), "active",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(30), 3, 0);
    }

    @Test
    void recommendations_freeUser_returnsEmpty() throws Exception {
        // No pass → next-step suggestions are gated off (backend-enforced).
        Long freeUser = fixtures.insertUser("free@example.com");
        Long t = fixtures.insertTopic("FT", "Free Topic", "免费", false, 1);
        Long q = fixtures.insertQuestion(t, "A");
        fixtures.insertMistakeRecord(freeUser, q, t, 1, "practice");

        mockMvc.perform(get("/api/v1/ai/recommendations")
                        .header("Authorization", "Bearer " + freeUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations", hasSize(0)));
    }

    @Test
    void recommendations_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/ai/recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    void recommendations_weakTopicsRankedByMistakeCount() throws Exception {
        Long topicA = fixtures.insertTopic("TA", "Topic A", "话题A", false, 1);
        Long topicB = fixtures.insertTopic("TB", "Topic B", "话题B", false, 2);
        // topic A: 3 active mistakes; topic B: 1.
        for (int i = 0; i < 3; i++) {
            Long q = fixtures.insertQuestion(topicA, "A");
            fixtures.insertMistakeRecord(userId, q, topicA, 1, "practice");
        }
        Long qb = fixtures.insertQuestion(topicB, "A");
        fixtures.insertMistakeRecord(userId, qb, topicB, 1, "practice");

        mockMvc.perform(get("/api/v1/ai/recommendations")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations", hasSize(2)))
                .andExpect(jsonPath("$.data.recommendations[0].topic_id", is(topicA.toString())))
                .andExpect(jsonPath("$.data.recommendations[0].reason_code", is("active_mistakes")))
                .andExpect(jsonPath("$.data.recommendations[0].mistake_count", is(3)))
                .andExpect(jsonPath("$.data.recommendations[0].topic_filter[0]", is(topicA.toString())))
                .andExpect(jsonPath("$.data.recommendations[1].topic_id", is(topicB.toString())))
                .andExpect(jsonPath("$.data.recommendations[1].mistake_count", is(1)));
    }

    @Test
    void recommendations_uncoveredKeyTopicsFillWhenNoMistakes() throws Exception {
        fixtures.insertTopic("KT", "Key Topic", "关键考点", true, 1);

        mockMvc.perform(get("/api/v1/ai/recommendations")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.data.recommendations[0].reason_code", is("uncovered_key_topic")))
                .andExpect(jsonPath("$.data.recommendations[0].mistake_count", is(0)));
    }

    @Test
    void recommendations_mistakeTopicsRankBeforeKeyTopics() throws Exception {
        Long weak = fixtures.insertTopic("WK", "Weak", "薄弱", false, 1);
        fixtures.insertTopic("KT", "Key", "关键", true, 2);  // uncovered key topic
        Long q = fixtures.insertQuestion(weak, "A");
        fixtures.insertMistakeRecord(userId, q, weak, 1, "practice");

        mockMvc.perform(get("/api/v1/ai/recommendations")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations", hasSize(2)))
                .andExpect(jsonPath("$.data.recommendations[0].topic_id", is(weak.toString())))
                .andExpect(jsonPath("$.data.recommendations[0].reason_code", is("active_mistakes")))
                .andExpect(jsonPath("$.data.recommendations[1].reason_code", is("uncovered_key_topic")));
    }

    @Test
    void recommendations_coveredKeyTopicExcluded() throws Exception {
        Long key = fixtures.insertTopic("KT", "Key", "关键", true, 1);
        Long q = fixtures.insertQuestion(key, "A");
        Long v = fixtures.insertEnVariantReturningId(q, "stem", "expl");
        // Practiced this topic this cycle → covered → must not be recommended.
        Long session = fixtures.insertPracticeSession(userId, 0);
        fixtures.insertPracticeAttempt(userId, session, q, v, "A", true);

        mockMvc.perform(get("/api/v1/ai/recommendations")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations", hasSize(0)));
    }

    @Test
    void recommendations_respectsLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            Long t = fixtures.insertTopic("T" + i, "Topic " + i, "话题" + i, false, i);
            Long q = fixtures.insertQuestion(t, "A");
            fixtures.insertMistakeRecord(userId, q, t, 1, "practice");
        }

        mockMvc.perform(get("/api/v1/ai/recommendations?limit=2")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations", hasSize(2)));
    }

    @Test
    void recommendations_languageZh_returnsZhLabel() throws Exception {
        Long t = fixtures.insertTopic("TA", "Topic A", "话题A", false, 1);
        Long q = fixtures.insertQuestion(t, "A");
        fixtures.insertMistakeRecord(userId, q, t, 1, "practice");

        mockMvc.perform(get("/api/v1/ai/recommendations?language=zh")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations[0].label", is("话题A")));
    }
}
