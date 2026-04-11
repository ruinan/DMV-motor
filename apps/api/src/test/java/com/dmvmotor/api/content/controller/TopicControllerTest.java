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
                .andExpect(jsonPath("$.data.items[0].nameEn").value("Traffic Signs"))
                .andExpect(jsonPath("$.data.items[0].nameZh").value("交通标志"))
                .andExpect(jsonPath("$.data.items[0].isKeyTopic").value(true))
                .andExpect(jsonPath("$.data.items[1].code").value("RIGHT_OF_WAY"));
    }

    @Test
    void listTopics_withChildTopic_returnsParentTopicId() throws Exception {
        Long parentId = fixtures.insertTopic("PARENT", "Parent", "父", false, 1);
        fixtures.insertChildTopic("CHILD", parentId);

        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.code=='CHILD')].parentTopicId")
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
}
