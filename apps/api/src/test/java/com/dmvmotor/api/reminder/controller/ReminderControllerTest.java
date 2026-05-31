package com.dmvmotor.api.reminder.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReminderControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;
    private Long topicId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId  = fixtures.insertUser("alice@example.com");
        topicId = fixtures.insertTopic("SIGNS");
    }

    // ---- auth ----

    @Test
    void list_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/reminders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    void generate_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/reminders/generate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    // ---- generation by learning-state signal (priority order) ----

    @Test
    void generate_inProgressSession_emitsResumePractice() throws Exception {
        fixtures.insertInProgressPracticeSession(userId, 0, "free_trial", "en");

        mockMvc.perform(post("/api/v1/reminders/generate")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated", is(true)))
                .andExpect(jsonPath("$.data.reminder.type", is("resume_practice")))
                .andExpect(jsonPath("$.data.reminder.priority", is(1)))
                .andExpect(jsonPath("$.data.reminder.status", is("pending")));
    }

    @Test
    void generate_activeMistakesNoSession_emitsReviewWeakPoints() throws Exception {
        Long q = fixtures.insertQuestion(topicId, "A");
        fixtures.insertMistakeRecord(userId, q, topicId, 2, "practice");

        mockMvc.perform(post("/api/v1/reminders/generate")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated", is(true)))
                .andExpect(jsonPath("$.data.reminder.type", is("review_weak_points")))
                .andExpect(jsonPath("$.data.reminder.priority", is(2)));
    }

    @Test
    void generate_studiedAndClean_emitsStartMock() throws Exception {
        // A completed session (studied), no in-progress, no active mistakes →
        // suggest validating with a mock.
        fixtures.insertPracticeSession(userId, 0);

        mockMvc.perform(post("/api/v1/reminders/generate")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated", is(true)))
                .andExpect(jsonPath("$.data.reminder.type", is("start_mock")))
                .andExpect(jsonPath("$.data.reminder.priority", is(3)));
    }

    @Test
    void generate_nothingApplies_emitsNone() throws Exception {
        // Brand-new user: no sessions, no mistakes, no history.
        mockMvc.perform(post("/api/v1/reminders/generate")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated", is(false)));
    }

    @Test
    void generate_dailyCap_secondCallSameDayReturnsNone() throws Exception {
        fixtures.insertInProgressPracticeSession(userId, 0, "free_trial", "en");

        mockMvc.perform(post("/api/v1/reminders/generate")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.generated", is(true)));

        // Second call within 24h — no overflow.
        mockMvc.perform(post("/api/v1/reminders/generate")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated", is(false)));
    }

    @Test
    void generate_typePausedAfter3Unresponded_fallsToNextType() throws Exception {
        // RESUME applies (in-progress) AND REVIEW applies (active mistake). Seed
        // 3 unresponded resume reminders aged > 24h (so the daily cap is clear),
        // pausing that type → generation falls through to review_weak_points.
        fixtures.insertInProgressPracticeSession(userId, 0, "free_trial", "en");
        Long q = fixtures.insertQuestion(topicId, "A");
        fixtures.insertMistakeRecord(userId, q, topicId, 2, "practice");
        fixtures.insertReminder(userId, "resume_practice", "pending", 1, 25 * 3600);
        fixtures.insertReminder(userId, "resume_practice", "pending", 1, 49 * 3600);
        fixtures.insertReminder(userId, "resume_practice", "pending", 1, 73 * 3600);

        mockMvc.perform(post("/api/v1/reminders/generate")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generated", is(true)))
                .andExpect(jsonPath("$.data.reminder.type", is("review_weak_points")));
    }

    // ---- list ----

    @Test
    void list_returnsPendingOrderedByPriority() throws Exception {
        // A lower-priority (start_mock=3) and higher-priority (resume=1) pending
        // reminder → the list leads with the higher priority.
        fixtures.insertReminder(userId, "start_mock", "pending", 3, 100);
        fixtures.insertReminder(userId, "resume_practice", "pending", 1, 50);
        // A responded one must not show.
        fixtures.insertReminder(userId, "review_weak_points", "responded", 2, 10);

        mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reminders", hasSize(2)))
                .andExpect(jsonPath("$.data.reminders[0].type", is("resume_practice")))
                .andExpect(jsonPath("$.data.reminders[1].type", is("start_mock")));
    }

    // ---- respond ----

    @Test
    void respond_marksResponded_andDropsFromActiveList() throws Exception {
        Long id = fixtures.insertReminder(userId, "resume_practice", "pending", 1, 10);

        mockMvc.perform(post("/api/v1/reminders/{id}/respond", id)
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("responded")));

        mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.reminders", hasSize(0)));
    }

    @Test
    void respond_otherUser_returns403() throws Exception {
        Long stranger = fixtures.insertUser("bob@example.com");
        Long id = fixtures.insertReminder(userId, "resume_practice", "pending", 1, 10);

        mockMvc.perform(post("/api/v1/reminders/{id}/respond", id)
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    @Test
    void respond_notFound_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/reminders/{id}/respond", "999999")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")));
    }
}
