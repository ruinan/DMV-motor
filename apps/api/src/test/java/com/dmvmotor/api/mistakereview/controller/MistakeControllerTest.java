package com.dmvmotor.api.mistakereview.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MistakeControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;
    private Long topicId;
    private Long topicId2;
    private Long questionId1;
    private Long questionId2;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId    = fixtures.insertUser("charlie@example.com");
        topicId   = fixtures.insertTopic("SIGNS");
        topicId2  = fixtures.insertTopic("RULES");
        questionId1 = fixtures.insertQuestion(topicId, "A");
        questionId2 = fixtures.insertQuestion(topicId2, "B");
    }

    @Test
    void listMistakes_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/mistakes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMistakes_noMistakes_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/mistakes")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.meta.total").value(0))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.page_size").value(20));
    }

    @Test
    void listMistakes_withMistakes_returnsItems() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 2, "practice");
        fixtures.insertMistakeRecord(userId, questionId2, topicId2, 1, "review");

        mockMvc.perform(get("/api/v1/mistakes")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.meta.total").value(2))
                .andExpect(jsonPath("$.data.items[0].mistake_id").isString())
                .andExpect(jsonPath("$.data.items[0].question_id").isString())
                .andExpect(jsonPath("$.data.items[0].topic_id").isString())
                .andExpect(jsonPath("$.data.items[0].wrong_count").isNumber())
                .andExpect(jsonPath("$.data.items[0].last_wrong_at").isString())
                .andExpect(jsonPath("$.data.items[0].source").isString());
    }

    @Test
    void listMistakes_withTopicFilter_returnsOnlyMatchingTopic() throws Exception {
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 2, "practice");
        fixtures.insertMistakeRecord(userId, questionId2, topicId2, 1, "practice");

        mockMvc.perform(get("/api/v1/mistakes")
                        .header("Authorization", "Bearer " + userId)
                        .param("topic_id", String.valueOf(topicId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data.items[0].topic_id").value(String.valueOf(topicId)));
    }

    @Test
    void listMistakes_pagination_respectsPageAndPageSize() throws Exception {
        Long q3 = fixtures.insertQuestion(topicId, "C");
        fixtures.insertMistakeRecord(userId, questionId1, topicId, 1, "practice");
        fixtures.insertMistakeRecord(userId, questionId2, topicId2, 1, "practice");
        fixtures.insertMistakeRecord(userId, q3, topicId, 1, "practice");

        mockMvc.perform(get("/api/v1/mistakes")
                        .header("Authorization", "Bearer " + userId)
                        .param("page", "1")
                        .param("page_size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.meta.total").value(3))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.page_size").value(2));
    }

    @Test
    void listMistakes_onlyOwnMistakes_excludesOtherUsers() throws Exception {
        Long otherUser = fixtures.insertUser("other@example.com");
        fixtures.insertMistakeRecord(otherUser, questionId1, topicId, 1, "practice");

        mockMvc.perform(get("/api/v1/mistakes")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }
}
