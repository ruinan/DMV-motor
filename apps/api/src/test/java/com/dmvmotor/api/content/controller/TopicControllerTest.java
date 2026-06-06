package com.dmvmotor.api.content.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TopicControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
    }

    @Test
    void listTopics_empty_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    void listTopics_withData_returnsAllTopics() throws Exception {
        fixtures.insertTopic("TRAFFIC_SIGNS", "Traffic Signs", "交通标志", true, 1);
        fixtures.insertTopic("RIGHT_OF_WAY", "Right of Way", "通行权", false, 2);

        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].code").value("TRAFFIC_SIGNS"))
                .andExpect(jsonPath("$.data.items[0].name_en").value("Traffic Signs"))
                .andExpect(jsonPath("$.data.items[0].name_zh").value("交通标志"))
                .andExpect(jsonPath("$.data.items[0].is_key_topic").value(true))
                .andExpect(jsonPath("$.data.items[1].code").value("RIGHT_OF_WAY"));
    }

    @Test
    void listTopics_withChildTopic_returnsParentTopicId() throws Exception {
        Long parentId = fixtures.insertTopic("PARENT", "Parent", "父", false, 1);
        fixtures.insertChildTopic("CHILD", parentId);

        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.code=='CHILD')].parent_topic_id")
                        .value(parentId.toString()));
    }

    @Test
    void listTopics_sortedBySortOrder() throws Exception {
        fixtures.insertTopic("B_TOPIC", "B Topic", "B", false, 20);
        fixtures.insertTopic("A_TOPIC", "A Topic", "A", false, 10);

        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("A_TOPIC"))
                .andExpect(jsonPath("$.data.items[1].code").value("B_TOPIC"));
    }

    // ===== /topics/mastery =====

    @Test
    void getMastery_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/topics/mastery"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void getMastery_noAttempts_allSubTopicsNotMastered() throws Exception {
        Long userId = fixtures.insertUser("mastery-test@test.com");
        Long topicId = fixtures.insertTopic("LANES", "Lane Use", "车道", false, 10);
        fixtures.insertQuestion(topicId, "A");
        fixtures.insertQuestion(topicId, "B");

        mockMvc.perform(get("/api/v1/topics/mastery").header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.total_sub_topics").value(1))
                .andExpect(jsonPath("$.data.summary.mastered_sub_topics").value(0))
                .andExpect(jsonPath("$.data.topics", hasSize(1)))
                .andExpect(jsonPath("$.data.topics[0].is_mastered").value(false))
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].is_mastered").value(false))
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].attempted_count").value(0))
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].bank_size").value(2));
    }

    @Test
    void getMastery_meetsThresholds_subTopicMastered() throws Exception {
        Long userId = fixtures.insertUser("mastery-test@test.com");
        Long topicId = fixtures.insertTopic("LANES", "Lane Use", "车道", false, 10);
        Long q1 = fixtures.insertQuestion(topicId, "A");
        Long v1 = fixtures.insertEnVariantReturningId(q1, "stem 1", "expl 1");
        Long q2 = fixtures.insertQuestion(topicId, "A");
        Long v2 = fixtures.insertEnVariantReturningId(q2, "stem 2", "expl 2");
        Long q3 = fixtures.insertQuestion(topicId, "A");
        Long v3 = fixtures.insertEnVariantReturningId(q3, "stem 3", "expl 3");
        Long q4 = fixtures.insertQuestion(topicId, "A");
        Long v4 = fixtures.insertEnVariantReturningId(q4, "stem 4", "expl 4");

        Long sessionId = fixtures.insertPracticeSession(userId, 0);
        // 4 attempts, 4 correct → 100% overall, 4/4 recent → mastered
        fixtures.insertPracticeAttempt(userId, sessionId, q1, v1, "A", true);
        fixtures.insertPracticeAttempt(userId, sessionId, q2, v2, "A", true);
        fixtures.insertPracticeAttempt(userId, sessionId, q3, v3, "A", true);
        fixtures.insertPracticeAttempt(userId, sessionId, q4, v4, "A", true);

        mockMvc.perform(get("/api/v1/topics/mastery").header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.mastered_sub_topics").value(1))
                .andExpect(jsonPath("$.data.topics[0].is_mastered").value(true))
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].is_mastered").value(true))
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].attempted_count").value(4))
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].correct_count").value(4));
    }

    @Test
    void getMastery_countsMockAnswers_towardCoverage() throws Exception {
        // "模考也算覆盖" — answers submitted in a mock exam count toward sub-topic
        // coverage + mastery just like practice, so a learner who only took mocks
        // doesn't see an empty 0/16 donut.
        Long userId = fixtures.insertUser("mastery-mock@test.com");
        Long topicId = fixtures.insertTopic("LANES", "Lane Use", "车道", false, 10);
        Long q1 = fixtures.insertQuestion(topicId, "A");
        Long v1 = fixtures.insertEnVariantReturningId(q1, "stem 1", "expl 1");
        Long q2 = fixtures.insertQuestion(topicId, "A");
        Long v2 = fixtures.insertEnVariantReturningId(q2, "stem 2", "expl 2");
        Long q3 = fixtures.insertQuestion(topicId, "A");
        Long v3 = fixtures.insertEnVariantReturningId(q3, "stem 3", "expl 3");
        Long q4 = fixtures.insertQuestion(topicId, "A");
        Long v4 = fixtures.insertEnVariantReturningId(q4, "stem 4", "expl 4");

        // No practice at all — only a (scored) mock attempt with 4 correct answers.
        Long mockExamId = fixtures.insertMockExam("COVERAGE_MOCK", 4);
        Long attemptId = fixtures.insertMockAttemptWithScore(userId, mockExamId, 100);
        fixtures.insertMockAttemptResult(attemptId, q1, v1, "A", true);
        fixtures.insertMockAttemptResult(attemptId, q2, v2, "A", true);
        fixtures.insertMockAttemptResult(attemptId, q3, v3, "A", true);
        fixtures.insertMockAttemptResult(attemptId, q4, v4, "A", true);

        mockMvc.perform(get("/api/v1/topics/mastery").header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].attempted_count").value(4))
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].correct_count").value(4))
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].is_mastered").value(true));
    }

    @Test
    void getMastery_belowRecentCorrect_subTopicNotMastered() throws Exception {
        Long userId = fixtures.insertUser("mastery-test@test.com");
        Long topicId = fixtures.insertTopic("LANES", "Lane Use", "车道", false, 10);
        Long q1 = fixtures.insertQuestion(topicId, "A");
        Long v1 = fixtures.insertEnVariantReturningId(q1, "s1", "e1");
        Long q2 = fixtures.insertQuestion(topicId, "A");
        Long v2 = fixtures.insertEnVariantReturningId(q2, "s2", "e2");
        Long q3 = fixtures.insertQuestion(topicId, "A");
        Long v3 = fixtures.insertEnVariantReturningId(q3, "s3", "e3");
        Long q4 = fixtures.insertQuestion(topicId, "A");
        Long v4 = fixtures.insertEnVariantReturningId(q4, "s4", "e4");

        Long sessionId = fixtures.insertPracticeSession(userId, 0);
        // 4 attempts, 2 correct → 50% overall (below 80%)
        fixtures.insertPracticeAttempt(userId, sessionId, q1, v1, "A", true);
        fixtures.insertPracticeAttempt(userId, sessionId, q2, v2, "A", true);
        fixtures.insertPracticeAttempt(userId, sessionId, q3, v3, "B", false);
        fixtures.insertPracticeAttempt(userId, sessionId, q4, v4, "B", false);

        mockMvc.perform(get("/api/v1/topics/mastery").header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.mastered_sub_topics").value(0))
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].is_mastered").value(false))
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].attempted_count").value(4))
                .andExpect(jsonPath("$.data.topics[0].sub_topics[0].correct_count").value(2));
    }

    @Test
    void getMastery_parentMasteryRequiresAllChildren() throws Exception {
        // Two topics with one sub-topic each. Master one → only that topic is_mastered.
        Long userId = fixtures.insertUser("mastery-test@test.com");
        Long topic1 = fixtures.insertTopic("LANES", "Lane Use", "车道", false, 10);
        Long topic2 = fixtures.insertTopic("SPEED", "Speed", "速度", false, 20);
        Long sessionId = fixtures.insertPracticeSession(userId, 0);

        for (int i = 0; i < 4; i++) {
            Long q = fixtures.insertQuestion(topic1, "A");
            Long v = fixtures.insertEnVariantReturningId(q, "s" + i, "e" + i);
            fixtures.insertPracticeAttempt(userId, sessionId, q, v, "A", true);
        }
        fixtures.insertQuestion(topic2, "A"); // no attempts

        mockMvc.perform(get("/api/v1/topics/mastery").header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.mastered_sub_topics").value(1))
                .andExpect(jsonPath("$.data.topics[?(@.code=='LANES')].is_mastered").value(hasItem(true)))
                .andExpect(jsonPath("$.data.topics[?(@.code=='SPEED')].is_mastered").value(hasItem(false)));
    }
}
