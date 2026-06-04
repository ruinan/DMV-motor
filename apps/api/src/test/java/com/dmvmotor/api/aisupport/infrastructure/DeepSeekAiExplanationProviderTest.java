package com.dmvmotor.api.aisupport.infrastructure;

import com.dmvmotor.api.aisupport.application.AiExplanationProvider;
import com.dmvmotor.api.aisupport.config.AiProperties;
import com.dmvmotor.api.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepSeekAiExplanationProviderTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    /**
     * Build the provider against a {@link MockWebServer} URL so request/response
     * round-trips are exercised end-to-end without a real DeepSeek dependency.
     */
    private DeepSeekAiExplanationProvider buildProvider(int timeoutSeconds) {
        AiProperties.Deepseek ds = new AiProperties.Deepseek(
                "test-api-key",
                server.url("/").toString().replaceAll("/$", ""), // strip trailing slash
                "deepseek-chat",
                400,
                timeoutSeconds);
        AiProperties props = new AiProperties(true, "deepseek", 120, 60, 300, 50, 10, ds);
        return new DeepSeekAiExplanationProvider(props);
    }

    private AiExplanationProvider.Input sampleInput(String language) {
        return new AiExplanationProvider.Input(
                42L,
                "What does a stop sign mean?",
                List.of(
                        Map.of("key", "A", "text", "Slow down"),
                        Map.of("key", "B", "text", "Stop fully"),
                        Map.of("key", "C", "text", "Yield")),
                "B",
                "A",
                "A stop sign requires a full stop.",
                language,
                0, null, null, "California Class C (Car)");
    }

    private static String chatCompletionsJson(String content,
                                              int promptTokens,
                                              int completionTokens) {
        return "{"
                + "\"id\":\"x\","
                + "\"model\":\"deepseek-chat\","
                + "\"choices\":[{\"index\":0,"
                + "\"message\":{\"role\":\"assistant\",\"content\":\"" + content + "\"},"
                + "\"finish_reason\":\"stop\"}],"
                + "\"usage\":{\"prompt_tokens\":" + promptTokens
                + ",\"completion_tokens\":" + completionTokens
                + ",\"total_tokens\":" + (promptTokens + completionTokens) + "}"
                + "}";
    }

    // ---------------- 1. happy path EN ----------------

    @Test
    void explain_happyPath_en_parsesContentAndTokens() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(chatCompletionsJson("Wrong: A only slows. Right: B requires a full stop.", 120, 45)));

        DeepSeekAiExplanationProvider provider = buildProvider(30);
        AiExplanationProvider.Output out = provider.explain(sampleInput("en"));

        assertEquals("Wrong: A only slows. Right: B requires a full stop.", out.text());
        assertEquals(120, out.tokensIn());
        assertEquals(45, out.tokensOut());
        assertEquals("deepseek-chat", provider.modelName());

        // Sanity-check the wire format: model, messages, max_tokens, stream=false.
        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertEquals("/chat/completions", req.getPath());
        assertEquals("Bearer test-api-key", req.getHeader("Authorization"));
        JsonNode body = JSON.readTree(req.getBody().readUtf8());
        assertEquals("deepseek-chat", body.get("model").asText());
        assertFalse(body.get("stream").asBoolean());
        assertEquals(400, body.get("max_tokens").asInt());
        assertEquals(2, body.get("messages").size());
        assertEquals("system", body.get("messages").get(0).get("role").asText());
        assertEquals("user", body.get("messages").get(1).get("role").asText());
    }

    // ---------------- 2. ZH switches the system prompt ----------------

    @Test
    void explain_zh_usesChineseSystemPrompt() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(chatCompletionsJson("A 仅减速，B 必须完全停止。", 130, 50)));

        DeepSeekAiExplanationProvider provider = buildProvider(30);
        AiExplanationProvider.Output out = provider.explain(sampleInput("zh"));

        assertEquals("A 仅减速，B 必须完全停止。", out.text());

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        JsonNode body = JSON.readTree(req.getBody().readUtf8());
        String systemPrompt = body.get("messages").get(0).get("content").asText();
        // System prompt must be the ZH variant — keyword "笔试" appears only there.
        assertTrue(systemPrompt.contains("笔试"),
                "ZH request should carry the Chinese system prompt, got: " + systemPrompt);
    }

    @Test
    void explain_en_usesEnglishSystemPrompt() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(chatCompletionsJson("ok", 1, 1)));

        DeepSeekAiExplanationProvider provider = buildProvider(30);
        provider.explain(sampleInput("en"));

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        JsonNode body = JSON.readTree(req.getBody().readUtf8());
        String systemPrompt = body.get("messages").get(0).get("content").asText();
        // "tutor" appears only in the EN system prompt (ZH uses 辅导员).
        assertTrue(systemPrompt.toLowerCase().contains("tutor"),
                "EN request should carry the English system prompt, got: " + systemPrompt);
    }

    @Test
    void explain_systemPrompt_isExamAware_notHardcodedToOneLicenseType() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(chatCompletionsJson("ok", 1, 1)));

        DeepSeekAiExplanationProvider provider = buildProvider(30);
        provider.explain(sampleInput("en")); // examLabel = "California Class C (Car)"

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        JsonNode body = JSON.readTree(req.getBody().readUtf8());
        String systemPrompt = body.get("messages").get(0).get("content").asText();
        // The exam label drives the persona — no hardcoded motorcycle/M1 wording.
        assertTrue(systemPrompt.contains("California Class C (Car)"),
                "system prompt should name the exam, got: " + systemPrompt);
        assertFalse(systemPrompt.toLowerCase().contains("motorcycle"),
                "system prompt must not hardcode motorcycle for a car exam, got: " + systemPrompt);
    }

    @Test
    void explain_nullOrBlankExamLabel_usesGenericFallbackPersona() throws Exception {
        DeepSeekAiExplanationProvider provider = buildProvider(30);

        // null label (en) → generic "California DMV"
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody(chatCompletionsJson("ok", 1, 1)));
        provider.explain(new AiExplanationProvider.Input(
                1L, "Q?", List.of(Map.of("key", "A", "text", "x")),
                "A", "B", null, "en", 0, null, null, null));
        String enPrompt = JSON.readTree(server.takeRequest(2, TimeUnit.SECONDS).getBody().readUtf8())
                .get("messages").get(0).get("content").asText();
        assertTrue(enPrompt.contains("California DMV"), enPrompt);

        // blank label (zh) → generic "加州 DMV"
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody(chatCompletionsJson("ok", 1, 1)));
        provider.explain(new AiExplanationProvider.Input(
                1L, "Q?", List.of(Map.of("key", "A", "text", "x")),
                "A", "B", null, "zh", 0, null, null, "   "));
        String zhPrompt = JSON.readTree(server.takeRequest(2, TimeUnit.SECONDS).getBody().readUtf8())
                .get("messages").get(0).get("content").asText();
        assertTrue(zhPrompt.contains("加州 DMV"), zhPrompt);
    }

    // ---------------- 3. privacy — no user_id leak ----------------

    @Test
    void explain_requestBody_doesNotLeakUserId() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(chatCompletionsJson("ok", 10, 5)));

        DeepSeekAiExplanationProvider provider = buildProvider(30);
        provider.explain(sampleInput("en"));

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        String raw = req.getBody().readUtf8();
        // The provider must never forward anything that identifies the user.
        assertFalse(raw.toLowerCase().contains("user_id"),
                "request body must not contain user_id, got: " + raw);
        assertFalse(raw.toLowerCase().contains("userid"),
                "request body must not contain userid, got: " + raw);
    }

    // ---------------- 4. 5xx surfaces as AI_PROVIDER_ERROR / 503 ----------------

    @Test
    void explain_providerReturns500_throwsBusinessException503() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("upstream boom"));

        DeepSeekAiExplanationProvider provider = buildProvider(30);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> provider.explain(sampleInput("en")));
        assertEquals("AI_PROVIDER_ERROR", ex.getErrorCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
    }

    // ---------------- 5. timeout surfaces the same way ----------------

    @Test
    void explain_providerTimesOut_throwsBusinessException503() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBodyDelay(3, TimeUnit.SECONDS)
                .setBody(chatCompletionsJson("late", 1, 1)));

        // 1s read-timeout against a 3s server delay → forced timeout path.
        DeepSeekAiExplanationProvider provider = buildProvider(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> provider.explain(sampleInput("en")));
        assertEquals("AI_PROVIDER_ERROR", ex.getErrorCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
    }

    // ---------------- 6. malformed response: missing choices ----------------

    @Test
    void explain_responseMissingChoices_throwsBusinessException503() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"x\",\"model\":\"deepseek-chat\"}"));

        DeepSeekAiExplanationProvider provider = buildProvider(30);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> provider.explain(sampleInput("en")));
        assertEquals("AI_PROVIDER_ERROR", ex.getErrorCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
    }

    // ---------------- 7. malformed response: empty choices array ----------------

    @Test
    void explain_responseEmptyChoices_throwsBusinessException503() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1}}"));

        DeepSeekAiExplanationProvider provider = buildProvider(30);

        assertThrows(BusinessException.class,
                () -> provider.explain(sampleInput("en")));
    }

    // ---------------- 8. malformed response: missing message.content ----------------

    @Test
    void explain_responseMissingContent_throwsBusinessException503() {
        // Message present but content key absent — exercises the !(content instanceof String) branch.
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\"}}]}"));

        DeepSeekAiExplanationProvider provider = buildProvider(30);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> provider.explain(sampleInput("en")));
        assertEquals("AI_PROVIDER_ERROR", ex.getErrorCode());
    }

    // ---------------- 9. usage block absent → tokens null ----------------

    @Test
    void explain_responseWithoutUsage_returnsNullTokens() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"index\":0,"
                        + "\"message\":{\"role\":\"assistant\",\"content\":\"answer\"}}]}"));

        DeepSeekAiExplanationProvider provider = buildProvider(30);
        AiExplanationProvider.Output out = provider.explain(sampleInput("en"));

        assertEquals("answer", out.text());
        assertEquals(null, out.tokensIn());
        assertEquals(null, out.tokensOut());
    }

    // ---------------- 10. user message omits Reference when no static explanation ----------------

    @Test
    void explain_inputWithoutStaticExplanation_omitsReferenceLine() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(chatCompletionsJson("ok", 1, 1)));

        AiExplanationProvider.Input noRef = new AiExplanationProvider.Input(
                7L, "Q?", List.of(Map.of("key", "A", "text", "x")),
                "A", "B", null, "en", 0, null, null, "California Class C (Car)");

        DeepSeekAiExplanationProvider provider = buildProvider(30);
        provider.explain(noRef);

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        JsonNode body = JSON.readTree(req.getBody().readUtf8());
        String userMsg = body.get("messages").get(1).get("content").asText();
        assertFalse(userMsg.contains("Reference:"),
                "user message must omit Reference when no static explanation, got: " + userMsg);
    }

    // enhance1: deep-dive (depth ≥ 1) is aspect-directed + progressive. Cover the
    // per-aspect focus switch (both languages, incl. the default fallback) and
    // that the thread-so-far is fed back as "do not repeat" context.
    @Test
    void deepDive_allAspectsAndLanguages_feedPriorContext() throws Exception {
        String[] aspects = {"example", "mnemonic", "distractors", "rule", "unknown"};
        String[] langs = {"en", "zh"};
        DeepSeekAiExplanationProvider provider = buildProvider(30);

        for (String lang : langs) {
            for (String aspect : aspects) {
                server.enqueue(new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(chatCompletionsJson("deeper", 5, 5)));

                AiExplanationProvider.Output out = provider.explain(
                        new AiExplanationProvider.Input(
                                9L, "Q?",
                                List.of(Map.of("key", "A", "text", "x"),
                                        Map.of("key", "B", "text", "y")),
                                "A", "B", "ref", lang, 2, aspect,
                                "earlier explanation of the basics", "California Class C (Car)"));

                assertEquals("deeper", out.text());
                RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
                JsonNode body = JSON.readTree(req.getBody().readUtf8());
                String userMsg = body.get("messages").get(1).get("content").asText();
                assertTrue(userMsg.contains("earlier explanation of the basics"),
                        "deep dive must feed prior context for " + lang + "/" + aspect);
                // system prompt was built for a deep dive (layer interpolated)
                String systemMsg = body.get("messages").get(0).get("content").asText();
                assertTrue(systemMsg.contains("2"),
                        "deep-dive prompt should reference the layer for " + lang + "/" + aspect);
            }
        }
    }

    @Test
    void deepDive_longPriorContext_isTruncated() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(chatCompletionsJson("ok", 1, 1)));

        String longContext = "z".repeat(5000);
        DeepSeekAiExplanationProvider provider = buildProvider(30);
        provider.explain(new AiExplanationProvider.Input(
                9L, "Q?", List.of(Map.of("key", "A", "text", "x")),
                "A", "B", null, "en", 1, "example", longContext, "California Class C (Car)"));

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        JsonNode body = JSON.readTree(req.getBody().readUtf8());
        String userMsg = body.get("messages").get(1).get("content").asText();
        // The 5000-char context is capped well under its original length.
        assertTrue(userMsg.length() < 4000,
                "long prior context must be truncated, got length " + userMsg.length());
    }
}
