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

class QuestionControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired TestFixtures fixtures;

    private Long topicId;
    private Long questionId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();

        topicId = fixtures.insertTopic("TRAFFIC_SIGNS", "Traffic Signs", "交通标志", true, 1);
        questionId = fixtures.insertQuestion(topicId, "B");
        fixtures.insertVariant(questionId, "en",
                "What does a red octagon mean?",
                "[{\"key\":\"A\",\"text\":\"Slow down\"},{\"key\":\"B\",\"text\":\"Stop\"},{\"key\":\"C\",\"text\":\"Yield\"}]",
                "A red octagon is a stop sign.");
        fixtures.insertVariant(questionId, "zh",
                "红色八角形标志代表什么？",
                "[{\"key\":\"A\",\"text\":\"减速\"},{\"key\":\"B\",\"text\":\"停车\"},{\"key\":\"C\",\"text\":\"让行\"}]",
                "红色八角形是停止标志。");
    }

    @Test
    void getQuestion_englishVariant_returnsCorrectData() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", questionId)
                        .param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questionId").value(questionId.toString()))
                .andExpect(jsonPath("$.data.topicId").value(topicId.toString()))
                .andExpect(jsonPath("$.data.correctChoiceKey").value("B"))
                .andExpect(jsonPath("$.data.language").value("en"))
                .andExpect(jsonPath("$.data.stem").value("What does a red octagon mean?"))
                .andExpect(jsonPath("$.data.choices", hasSize(3)))
                .andExpect(jsonPath("$.data.choices[0].key").value("A"))
                .andExpect(jsonPath("$.data.choices[1].key").value("B"))
                .andExpect(jsonPath("$.data.explanation").value("A red octagon is a stop sign."));
    }

    @Test
    void getQuestion_chineseVariant_returnsChineseContent() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", questionId)
                        .param("language", "zh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language").value("zh"))
                .andExpect(jsonPath("$.data.stem").value("红色八角形标志代表什么？"))
                .andExpect(jsonPath("$.data.choices[1].text").value("停车"));
    }

    @Test
    void getQuestion_defaultLanguageIsEn() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language").value("en"));
    }

    @Test
    void getQuestion_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}
