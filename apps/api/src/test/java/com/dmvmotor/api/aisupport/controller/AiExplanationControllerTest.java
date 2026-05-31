package com.dmvmotor.api.aisupport.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import com.dmvmotor.api.aisupport.infrastructure.StubAiExplanationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pin the cooldown params to the original 120/60/300 so the time-offset
// assertions below (e.g. "30s ago → still cooling") test the rate-limit LOGIC
// independent of the production defaults, which were later relaxed to 10/0/30
// for UX. The logic is what matters here; the live values live in application.yml.
@TestPropertySource(properties = {
        "app.ai.base-cooldown-seconds=120",
        "app.ai.cooldown-increment-seconds=60",
        "app.ai.max-cooldown-seconds=300"
})
class AiExplanationControllerTest extends IntegrationTestBase {

    @Autowired MockMvc                    mockMvc;
    @Autowired TestFixtures               fixtures;
    @Autowired StubAiExplanationProvider  stubProvider;

    private Long userId;
    private Long otherUserId;
    private Long topicId;
    private Long freeTrialQuestionId;
    private Long paidOnlyQuestionId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        stubProvider.resetCallCount();

        userId      = fixtures.insertUser("alice@example.com");
        otherUserId = fixtures.insertUser("bob@example.com");
        topicId     = fixtures.insertTopic("SIGNS");

        freeTrialQuestionId = fixtures.insertQuestion(topicId, "A");
        fixtures.insertEnVariant(freeTrialQuestionId, "What does a stop sign mean?", "Stop fully.");
        fixtures.insertZhVariant(freeTrialQuestionId, "停车标志什么意思？", "完全停下。");

        paidOnlyQuestionId = fixtures.insertPaidOnlyQuestion(topicId, "B");
        fixtures.insertEnVariant(paidOnlyQuestionId, "Right-of-way at uncontrolled intersection?", "Yield to right.");
    }

    private String body(String questionId, String selectedChoiceKey, String language) {
        return "{\"question_id\":\"" + questionId + "\","
                + "\"selected_choice_key\":\"" + selectedChoiceKey + "\","
                + "\"language\":\"" + language + "\"}";
    }

    private String bodyWithDepth(String questionId, String selectedChoiceKey,
                                 String language, int depth) {
        return "{\"question_id\":\"" + questionId + "\","
                + "\"selected_choice_key\":\"" + selectedChoiceKey + "\","
                + "\"language\":\"" + language + "\","
                + "\"depth\":" + depth + "}";
    }

    // --------------- 1. anonymous ---------------

    @Test
    void explain_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(freeTrialQuestionId.toString(), "B", "en")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));

        assertEquals(0, stubProvider.callCount(), "provider must not be called");
    }

    // --------------- 2-4. access gates ---------------

    @Test
    void explain_freeTrial_freeTrialQuestion_returnsExplanation() throws Exception {
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(freeTrialQuestionId.toString(), "B", "en")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.explanation", containsString("stub:explanation")))
                .andExpect(jsonPath("$.data.cached", is(false)))
                .andExpect(jsonPath("$.data.model", is("stub")))
                .andExpect(jsonPath("$.data.language", is("en")));

        assertEquals(1, stubProvider.callCount());
        assertEquals(1, fixtures.countAiExplanationsForUser(userId));
    }

    @Test
    void explain_freeTrial_paidOnlyQuestion_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(paidOnlyQuestionId.toString(), "C", "en")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")));

        assertEquals(0, stubProvider.callCount(), "provider must not be called for hidden question");
        assertEquals(0, fixtures.countAiExplanationsForUser(userId));
    }

    @Test
    void explain_paid_paidOnlyQuestion_returnsExplanation() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        fixtures.insertAccessPass(userId, "active", now.minusHours(1), now.plusDays(30), 5, 0);

        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(paidOnlyQuestionId.toString(), "C", "en")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.explanation", notNullValue()))
                .andExpect(jsonPath("$.data.cached", is(false)));

        assertEquals(1, stubProvider.callCount());
    }

    // --------------- 5-7. cache behaviour ---------------

    @Test
    void explain_cacheHit_secondCallReturnsCached_providerNotCalled() throws Exception {
        // First call — populates cache, provider is hit once.
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(freeTrialQuestionId.toString(), "B", "en")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached", is(false)));

        assertEquals(1, stubProvider.callCount());

        // Second call — same user/question/language → cache hit, provider count unchanged.
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(freeTrialQuestionId.toString(), "C", "en")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached", is(true)));

        assertEquals(1, stubProvider.callCount(), "cache hit must short-circuit provider");
        assertEquals(1, fixtures.countAiExplanationsForUser(userId));
    }

    @Test
    void explain_crossUser_cacheIsolated_userBStillCallsProvider() throws Exception {
        // user A primes the cache.
        fixtures.insertAiExplanation(userId, freeTrialQuestionId, "en", 0);

        // user B asks for the same question — no cache row for B → provider must be called.
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + otherUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(freeTrialQuestionId.toString(), "B", "en")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached", is(false)));

        assertEquals(1, stubProvider.callCount());
        assertEquals(1, fixtures.countAiExplanationsForUser(userId), "A's row untouched");
        assertEquals(1, fixtures.countAiExplanationsForUser(otherUserId), "B got own row");
    }

    @Test
    void explain_crossLanguage_enAndZhAreSeparateCacheRows() throws Exception {
        // Pre-seed EN row 200s ago (past base cooldown 120s) so the ZH call below
        // is admitted on its own merits — this test is about the cache key, not
        // the cooldown timer.
        fixtures.insertAiExplanation(userId, freeTrialQuestionId, "en", 200);

        // ZH — same user / same question / different language → cache miss,
        // provider called, a second row is persisted.
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(freeTrialQuestionId.toString(), "B", "zh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached", is(false)))
                .andExpect(jsonPath("$.data.language", is("zh")));

        assertEquals(1, stubProvider.callCount(),
                "ZH call hits the provider; EN row was pre-seeded by the fixture");
        assertEquals(2, fixtures.countAiExplanationsForUser(userId),
                "EN fixture + ZH new row → 2 cache rows for the user");
    }

    // --------------- 8-9. rate limiting ---------------

    @Test
    void explain_rateLimit_withinCooldown_returns429() throws Exception {
        // Recent call 30s ago — base cooldown 120s, so a new (uncached) call is rejected.
        Long anotherFreeTrialQ = fixtures.insertQuestion(topicId, "A");
        fixtures.insertEnVariant(anotherFreeTrialQ, "Another stem", "Another explanation");
        fixtures.insertAiExplanation(userId, anotherFreeTrialQ, "en", 30);

        // freeTrialQuestionId has no cache row for this user → would normally call the provider.
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(freeTrialQuestionId.toString(), "B", "en")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code", is("RATE_LIMITED")));

        assertEquals(0, stubProvider.callCount(), "rate limit must short-circuit provider");
    }

    @Test
    void explain_rateLimit_afterCooldown_succeeds() throws Exception {
        // Last call was 200s ago — past the 120s base cooldown.
        Long anotherFreeTrialQ = fixtures.insertQuestion(topicId, "A");
        fixtures.insertEnVariant(anotherFreeTrialQ, "Another stem", "Another explanation");
        fixtures.insertAiExplanation(userId, anotherFreeTrialQ, "en", 200);

        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(freeTrialQuestionId.toString(), "B", "en")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached", is(false)));

        assertEquals(1, stubProvider.callCount());
    }

    @Test
    void explain_cacheHit_skipsRateLimit() throws Exception {
        // Both signals active: cache row exists AND a recent call is inside the cooldown window.
        // Cache must win — no LLM cost, so no rate-limit need to apply.
        fixtures.insertAiExplanation(userId, freeTrialQuestionId, "en", 5);

        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(freeTrialQuestionId.toString(), "B", "en")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached", is(true)));

        assertEquals(0, stubProvider.callCount());
    }

    // --------------- 10-11. validation ---------------

    @Test
    void explain_validation_missingQuestionId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selected_choice_key\":\"B\",\"language\":\"en\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_ERROR")));
    }

    @Test
    void explain_validation_invalidIdFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("abc", "B", "en")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("INVALID_ID_FORMAT")));
    }

    // --------------- 12. defaulting ---------------

    @Test
    void explain_omittedLanguage_defaultsToEn() throws Exception {
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question_id\":\"" + freeTrialQuestionId + "\",\"selected_choice_key\":\"B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language", is("en")));

        assertNotEquals(0, stubProvider.callCount());
    }

    @Test
    void explain_explicitVariantId_isAccepted() throws Exception {
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question_id\":\"" + freeTrialQuestionId + "\","
                                + "\"variant_id\":\"42\","
                                + "\"selected_choice_key\":\"B\","
                                + "\"language\":\"en\"}"))
                .andExpect(status().isOk());

        assertEquals(1, stubProvider.callCount());
    }

    // --------------- 13. daily cap ---------------

    @Test
    void explain_dailyCapExceeded_returns429() throws Exception {
        // Default cap is 50 calls/day. Pre-seed 50 rows comfortably within the
        // 24h window — the first new request must be rejected.
        for (int i = 0; i < 50; i++) {
            Long q = fixtures.insertQuestion(topicId, "A");
            fixtures.insertEnVariant(q, "stem " + i, "exp " + i);
            // 600s ago — well past the per-call cooldown but inside 24h.
            fixtures.insertAiExplanation(userId, q, "en", 600L + i);
        }

        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(freeTrialQuestionId.toString(), "B", "en")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code", is("RATE_LIMITED")))
                .andExpect(jsonPath("$.error.message", containsString("Daily")));

        assertEquals(0, stubProvider.callCount());
    }

    // --------------- 14. deep dive ("深入分析") ---------------

    @Test
    void deepDive_freshQuestion_returnsDeeperLayer_notCachedNotPersisted() throws Exception {
        // No prior calls → no cooldown gate, cap not reached. depth=1 escalates
        // the prompt; the layer is NOT written to ai_explanations (client keeps
        // it), only a metadata row lands in ai_deep_dive_log.
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithDepth(freeTrialQuestionId.toString(), "B", "en", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.explanation", containsString(":depth=1")))
                .andExpect(jsonPath("$.data.cached", is(false)))
                .andExpect(jsonPath("$.data.depth", is(1)))
                .andExpect(jsonPath("$.data.depth_remaining", is(9)));

        assertEquals(1, stubProvider.callCount());
        assertEquals(0, fixtures.countAiExplanationsForUser(userId),
                "deep-dive text is not persisted server-side");
        assertEquals(1, fixtures.countDeepDiveLogForUser(userId),
                "deep-dive logs one metadata row");
    }

    @Test
    void deepDive_perQuestionCapReached_returns429() throws Exception {
        // Seed maxDeepDivesPerQuestion (default 10) prior deep-dives on this
        // question, aged 600s so the cooldown can't be the cause — the cap is.
        for (int i = 1; i <= 10; i++) {
            fixtures.insertDeepDiveLog(userId, freeTrialQuestionId, "en", i, 600L + i);
        }

        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithDepth(freeTrialQuestionId.toString(), "B", "en", 11)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code", is("RATE_LIMITED")))
                .andExpect(jsonPath("$.error.message", containsString("Deep-dive limit")));

        assertEquals(0, stubProvider.callCount(), "cap must short-circuit the provider");
    }

    @Test
    void deepDive_paidOnlyQuestion_freeTrialUser_returns404() throws Exception {
        // Access gate applies to deep dives too — a free-trial user can't deep
        // dive a paid-only question (same ID-leak hardening as the base path).
        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithDepth(paidOnlyQuestionId.toString(), "C", "en", 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")));

        assertEquals(0, stubProvider.callCount());
        assertEquals(0, fixtures.countDeepDiveLogForUser(userId));
    }

    @Test
    void deepDive_respectsCooldownFromRecentBaseCall_returns429() throws Exception {
        // A recent base call (30s ago, cooldown 120s) must also gate a deep dive
        // — deep dives are billable LLM calls, so they share the rate-limit.
        Long otherQ = fixtures.insertQuestion(topicId, "A");
        fixtures.insertEnVariant(otherQ, "Other stem", "Other exp");
        fixtures.insertAiExplanation(userId, otherQ, "en", 30);

        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithDepth(freeTrialQuestionId.toString(), "B", "en", 1)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code", is("RATE_LIMITED")))
                .andExpect(jsonPath("$.error.message", containsString("cooling")));

        assertEquals(0, stubProvider.callCount());
    }

    @Test
    void baseCall_respectsCooldownFromRecentDeepDive_returns429() throws Exception {
        // The reverse: a recent deep dive (30s ago) must gate a new base call —
        // proves the base path's rate-limit counts deep dives too.
        Long otherQ = fixtures.insertQuestion(topicId, "A");
        fixtures.insertEnVariant(otherQ, "Other stem", "Other exp");
        fixtures.insertDeepDiveLog(userId, otherQ, "en", 1, 30);

        mockMvc.perform(post("/api/v1/ai/explain")
                        .header("Authorization", "Bearer " + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(freeTrialQuestionId.toString(), "B", "en")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code", is("RATE_LIMITED")));

        assertEquals(0, stubProvider.callCount());
    }
}
