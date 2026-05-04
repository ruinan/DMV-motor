package com.dmvmotor.api.content.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security contract for {@code GET /api/v1/questions/{id}}:
 *   - The endpoint NEVER returns {@code correct_choice_key} or
 *     {@code explanation}. Answers flow only through Practice / Review /
 *     Mock submit endpoints once the user has committed an answer.
 *   - Anonymous + free-trial + expired users see only
 *     {@code allow_in_free_trial=true AND status='active'} questions
 *     (the documented "fixed free-trial set" per api-contract §3-§4).
 *   - Active-pass users see any {@code status='active'} question.
 *   - Inactive / draft questions never leak (404 even to paid users).
 */
class QuestionControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired TestFixtures fixtures;

    private Long topicId;
    private Long freeTrialQuestionId;
    private Long paidOnlyQuestionId;
    private Long inactiveQuestionId;
    private Long freeTrialUserId;
    private Long paidUserId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();

        topicId = fixtures.insertTopic("TRAFFIC_SIGNS", "Traffic Signs", "交通标志", true, 1);

        freeTrialQuestionId = fixtures.insertQuestion(topicId, "B");
        fixtures.insertVariant(freeTrialQuestionId, "en",
                "What does a red octagon mean?",
                "[{\"key\":\"A\",\"text\":\"Slow down\"},{\"key\":\"B\",\"text\":\"Stop\"},{\"key\":\"C\",\"text\":\"Yield\"}]",
                "A red octagon is a stop sign.");
        fixtures.insertVariant(freeTrialQuestionId, "zh",
                "红色八角形标志代表什么？",
                "[{\"key\":\"A\",\"text\":\"减速\"},{\"key\":\"B\",\"text\":\"停车\"},{\"key\":\"C\",\"text\":\"让行\"}]",
                "红色八角形是停止标志。");

        paidOnlyQuestionId = fixtures.insertPaidOnlyQuestion(topicId, "C");
        fixtures.insertVariant(paidOnlyQuestionId, "en",
                "Premium-only question stem",
                "[{\"key\":\"A\",\"text\":\"a\"},{\"key\":\"B\",\"text\":\"b\"},{\"key\":\"C\",\"text\":\"c\"}]",
                "Premium-only explanation.");

        inactiveQuestionId = fixtures.insertInactiveQuestion(topicId, "A");
        fixtures.insertVariant(inactiveQuestionId, "en",
                "Inactive stem", "[{\"key\":\"A\",\"text\":\"x\"}]", "irrelevant");

        freeTrialUserId = fixtures.insertUser("free@example.com");

        paidUserId = fixtures.insertUser("paid@example.com");
        fixtures.insertAccessPass(paidUserId, "active",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusDays(30),
                5, 0);
    }

    // ---------------------------------------------------------------
    // Answer + explanation are NEVER in the response, regardless of caller.
    // ---------------------------------------------------------------

    @Test
    void getQuestion_anonymous_freeTrialQuestion_returnsStemAndChoicesWithoutAnswer() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", freeTrialQuestionId)
                        .param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.question_id").value(freeTrialQuestionId.toString()))
                .andExpect(jsonPath("$.data.topic_id").value(topicId.toString()))
                .andExpect(jsonPath("$.data.language").value("en"))
                .andExpect(jsonPath("$.data.stem").value("What does a red octagon mean?"))
                .andExpect(jsonPath("$.data.choices", hasSize(3)))
                .andExpect(jsonPath("$.data.correct_choice_key").doesNotExist())
                .andExpect(jsonPath("$.data.explanation").doesNotExist());
    }

    @Test
    void getQuestion_paidUser_freeTrialQuestion_stillOmitsAnswer() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", freeTrialQuestionId)
                        .param("language", "en")
                        .header("Authorization", "Bearer " + paidUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct_choice_key").doesNotExist())
                .andExpect(jsonPath("$.data.explanation").doesNotExist());
    }

    // ---------------------------------------------------------------
    // Anonymous / free-trial cannot see paid-only content (404, not 403,
    // so the existence of a paid-only ID is not leaked).
    // ---------------------------------------------------------------

    @Test
    void getQuestion_anonymous_paidOnlyQuestion_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", paidOnlyQuestionId)
                        .param("language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getQuestion_freeTrialUser_paidOnlyQuestion_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", paidOnlyQuestionId)
                        .param("language", "en")
                        .header("Authorization", "Bearer " + freeTrialUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getQuestion_paidUser_paidOnlyQuestion_returnsStemWithoutAnswer() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", paidOnlyQuestionId)
                        .param("language", "en")
                        .header("Authorization", "Bearer " + paidUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stem").value("Premium-only question stem"))
                .andExpect(jsonPath("$.data.correct_choice_key").doesNotExist())
                .andExpect(jsonPath("$.data.explanation").doesNotExist());
    }

    // ---------------------------------------------------------------
    // Inactive questions never leak, even to paid users.
    // ---------------------------------------------------------------

    @Test
    void getQuestion_inactive_anonymous_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", inactiveQuestionId)
                        .param("language", "en"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getQuestion_inactive_paidUser_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", inactiveQuestionId)
                        .param("language", "en")
                        .header("Authorization", "Bearer " + paidUserId))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // Language + standard not-found cases still work.
    // ---------------------------------------------------------------

    @Test
    void getQuestion_chineseVariant_anonymous_returnsZhStem() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", freeTrialQuestionId)
                        .param("language", "zh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language").value("zh"))
                .andExpect(jsonPath("$.data.stem").value("红色八角形标志代表什么？"))
                .andExpect(jsonPath("$.data.choices[1].text").value("停车"));
    }

    @Test
    void getQuestion_defaultLanguageIsEn() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", freeTrialQuestionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language").value("en"));
    }

    @Test
    void getQuestion_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/questions/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}
