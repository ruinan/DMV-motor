package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aisupport.config.AiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic DeepSeek chat invocation used by every aiqgen gate + the generator.
 * Returns the raw response text — gates parse JSON themselves so they can
 * react to malformed output rather than getting an exception buried inside
 * this client.
 *
 * <p>Failures (timeout, 5xx, malformed response shape) are surfaced as
 * {@link AiQGenException} so the orchestrator can record the failure as a
 * gate {@code fail} reason and trigger the retry-with-feedback loop. We do
 * not collapse to a Spring HTTP exception because this client is invoked from
 * a CLI runner, not a web request.
 */
public class DeepSeekChatClient {

    public static final class AiQGenException extends RuntimeException {
        public AiQGenException(String msg) { super(msg); }
        public AiQGenException(String msg, Throwable cause) { super(msg, cause); }
    }

    private final RestClient restClient;
    private final String model;
    private final int maxTokens;

    public DeepSeekChatClient(AiProperties props) {
        this(buildRestClient(props.deepseek()), props.deepseek().model(), props.deepseek().maxTokens());
    }

    /** Test seam: build with a pre-configured RestClient (e.g. pointing at MockWebServer). */
    DeepSeekChatClient(RestClient restClient, String model, int maxTokens) {
        this.restClient = restClient;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)));
        body.put("max_tokens", maxTokens);
        body.put("temperature", 0.3);
        body.put("stream", false);

        Map<?, ?> response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new AiQGenException("DeepSeek request failed: " + e.getMessage(), e);
        }
        if (response == null) {
            throw new AiQGenException("DeepSeek returned empty response");
        }
        return extractContent(response);
    }

    @SuppressWarnings("unchecked")
    private static String extractContent(Map<?, ?> response) {
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            throw new AiQGenException("DeepSeek response missing choices");
        }
        Map<String, Object> first = (Map<String, Object>) choices.get(0);
        Map<String, Object> message = (Map<String, Object>) first.get("message");
        Object contentObj = message == null ? null : message.get("content");
        if (!(contentObj instanceof String content) || content.isBlank()) {
            throw new AiQGenException("DeepSeek response missing content");
        }
        return content;
    }

    private static RestClient buildRestClient(AiProperties.Deepseek ds) {
        int timeoutMs = (int) Duration.ofSeconds(ds.timeoutSeconds()).toMillis();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return RestClient.builder()
                .baseUrl(ds.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ds.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }
}
