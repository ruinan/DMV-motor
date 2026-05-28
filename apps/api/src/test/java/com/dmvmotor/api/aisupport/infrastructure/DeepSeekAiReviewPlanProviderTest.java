package com.dmvmotor.api.aisupport.infrastructure;

import com.dmvmotor.api.aisupport.application.AiReviewPlanProvider;
import com.dmvmotor.api.aisupport.config.AiProperties;
import com.dmvmotor.api.common.BusinessException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepSeekAiReviewPlanProviderTest {

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

    private DeepSeekAiReviewPlanProvider buildProvider(int timeoutSeconds) {
        AiProperties.Deepseek ds = new AiProperties.Deepseek(
                "test-api-key",
                server.url("/").toString().replaceAll("/$", ""),
                "deepseek-chat",
                500,
                timeoutSeconds);
        AiProperties props = new AiProperties(true, "deepseek", 120, 60, 300, 50, ds);
        return new DeepSeekAiReviewPlanProvider(props);
    }

    private AiReviewPlanProvider.Input sampleInput(String language) {
        return new AiReviewPlanProvider.Input(
                73, 22, 30, false,
                List.of(
                        new AiReviewPlanProvider.WrongItem(
                                "What does a flashing red light mean?",
                                "Traffic Signs & Signals",
                                "Regulatory Signs",
                                "A", "C"),
                        new AiReviewPlanProvider.WrongItem(
                                "When is lane splitting legal?",
                                "Lane Use & Positioning",
                                null,
                                "B", "D")),
                language);
    }

    @Test
    void generate_happyPath_returnsPlanAndTokens() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "choices": [{"message": {"content": "Focus on regulatory signs..."}}],
                          "usage": {"prompt_tokens": 120, "completion_tokens": 60}
                        }
                        """));

        DeepSeekAiReviewPlanProvider provider = buildProvider(5);
        AiReviewPlanProvider.Output out = provider.generate(sampleInput("en"));

        assertEquals("Focus on regulatory signs...", out.text());
        assertEquals(120, out.tokensIn());
        assertEquals(60, out.tokensOut());
        assertEquals("deepseek-chat", provider.modelName());

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        String body = req.getBody().readUtf8();
        // Privacy: never ship a user identifier in the prompt.
        assertTrue(!body.toLowerCase().contains("user_id"));
        // Content sanity: score + a wrong-question topic must be present.
        assertTrue(body.contains("73"));
        assertTrue(body.contains("Traffic Signs"));
    }

    @Test
    void generate_zhUsesChineseSystemPrompt() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"choices":[{"message":{"content":"复习计划……"}}],"usage":{}}
                        """));

        buildProvider(5).generate(sampleInput("zh"));

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertTrue(req.getBody().readUtf8().contains("笔试"));
    }

    @Test
    void generate_serverError_throwsBusinessException() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));
        DeepSeekAiReviewPlanProvider provider = buildProvider(5);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> provider.generate(sampleInput("en")));
        assertEquals("AI_PROVIDER_ERROR", ex.getErrorCode());
    }

    @Test
    void generate_missingChoices_throwsBusinessException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"usage\":{}}"));
        DeepSeekAiReviewPlanProvider provider = buildProvider(5);
        assertThrows(BusinessException.class, () -> provider.generate(sampleInput("en")));
    }

    @Test
    void generate_blankContent_throwsBusinessException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"\"}}]}"));
        DeepSeekAiReviewPlanProvider provider = buildProvider(5);
        assertThrows(BusinessException.class, () -> provider.generate(sampleInput("en")));
    }

    @Test
    void generate_nullBody_throwsBusinessException() {
        server.enqueue(new MockResponse().setResponseCode(204));
        DeepSeekAiReviewPlanProvider provider = buildProvider(5);
        assertThrows(BusinessException.class, () -> provider.generate(sampleInput("en")));
    }

    @Test
    void generate_choiceMissingMessage_throwsBusinessException() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"finish_reason\":\"stop\"}]}"));
        DeepSeekAiReviewPlanProvider provider = buildProvider(5);
        assertThrows(BusinessException.class, () -> provider.generate(sampleInput("en")));
    }

    @Test
    void generate_usageAbsent_tokensNull() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"Plan text\"}}]}"));
        AiReviewPlanProvider.Output out = buildProvider(5).generate(sampleInput("en"));
        assertEquals("Plan text", out.text());
        org.junit.jupiter.api.Assertions.assertNull(out.tokensIn());
        org.junit.jupiter.api.Assertions.assertNull(out.tokensOut());
    }

    @Test
    void generate_blankSubTopicLabel_omittedFromPrompt() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"ok\"}}],\"usage\":{}}"));
        AiReviewPlanProvider.Input in = new AiReviewPlanProvider.Input(
                50, 1, 2, false,
                List.of(new AiReviewPlanProvider.WrongItem(
                        "stem", "Topic", "   ", "A", "B")),
                "en");
        buildProvider(5).generate(in);
        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertTrue(!req.getBody().readUtf8().contains("Topic /"));
    }

    @Test
    void generate_noWrongItems_stillBuildsPrompt() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"choices":[{"message":{"content":"Great job!"}}],"usage":{}}
                        """));
        AiReviewPlanProvider.Input perfect = new AiReviewPlanProvider.Input(
                100, 30, 30, true, List.of(), "en");
        AiReviewPlanProvider.Output out = buildProvider(5).generate(perfect);
        assertEquals("Great job!", out.text());

        RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(req);
        assertTrue(req.getBody().readUtf8().contains("No wrong answers"));
    }
}
