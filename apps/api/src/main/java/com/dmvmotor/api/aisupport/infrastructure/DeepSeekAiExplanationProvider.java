package com.dmvmotor.api.aisupport.infrastructure;

import com.dmvmotor.api.aisupport.application.AiExplanationProvider;
import com.dmvmotor.api.aisupport.config.AiProperties;
import com.dmvmotor.api.common.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek-backed AI explainer — active when {@code app.ai.provider=deepseek}.
 *
 * <p>Privacy contract (progress §27.5): the wire payload carries only the
 * question content, choices, the user's pick, the correct answer key, and
 * the static explanation. No user_id, no session data, no history.
 *
 * <p>Failure semantics: anything that prevents a clean response — HTTP 4xx/5xx,
 * read timeout, connection refused, malformed JSON — is collapsed into a
 * single {@code BusinessException("AI_PROVIDER_ERROR", 503)} so the caller
 * either retries or falls back to the cache without persisting a bad row.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "deepseek")
public class DeepSeekAiExplanationProvider implements AiExplanationProvider {

    private static final String SYSTEM_PROMPT_EN =
            "You are a California Class M1 motorcycle permit test tutor. "
            + "The user picked a wrong choice. In 2-3 sentences, explain "
            + "why their choice is wrong and why the correct one is right. "
            + "Plain English. Don't repeat the question. Plain text only — no markdown.";

    private static final String SYSTEM_PROMPT_ZH =
            "你是加州 M1 摩托车驾照笔试辅导员。用户选错了。"
            + "用 2-3 句话解释为什么他们选错以及正确答案为什么对。"
            + "用简单中文。不要重复题目。纯文本输出，不要 markdown。";

    // enhance1 "深入分析": the learner already read the basic explanation and
    // tapped "go deeper". Escalate — give the underlying rule, a worked angle,
    // a memory aid, or the common confusion behind this item. Layers aren't fed
    // prior text (anti-hijack), so the depth number is the only escalation cue.
    private static final String DEEP_DIVE_PROMPT_EN =
            "You are a California Class M1 motorcycle permit test tutor. The "
            + "learner already read a basic explanation of this question and "
            + "wants to understand it more deeply (depth level %d). Give a "
            + "deeper take: the underlying rule or safety reason, a worked "
            + "example, a memory aid, or the common confusion. 3-4 sentences, "
            + "plain English. Don't just restate the basic explanation. Plain "
            + "text only — no markdown.";

    private static final String DEEP_DIVE_PROMPT_ZH =
            "你是加州 M1 摩托车驾照笔试辅导员。学员已经看过这道题的基础解释，"
            + "想更深入理解（深度层级 %d）。请给出更深的讲解：背后的规则或安全"
            + "原理、举例、记忆方法、或常见混淆点。3-4 句，简单中文。不要只是"
            + "重复基础解释。纯文本输出，不要 markdown。";

    private final RestClient restClient;
    private final String     model;
    private final int        maxTokens;

    public DeepSeekAiExplanationProvider(AiProperties props) {
        AiProperties.Deepseek ds = props.deepseek();
        int timeoutMs = (int) Duration.ofSeconds(ds.timeoutSeconds()).toMillis();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(ds.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ds.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
        this.model     = ds.model();
        this.maxTokens = ds.maxTokens();
    }

    @Override
    public Output explain(Input in) {
        boolean zh = "zh".equalsIgnoreCase(in.language());
        String systemPrompt = in.depth() > 0
                ? String.format(zh ? DEEP_DIVE_PROMPT_ZH : DEEP_DIVE_PROMPT_EN, in.depth())
                : (zh ? SYSTEM_PROMPT_ZH : SYSTEM_PROMPT_EN);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model",       model);
        body.put("messages",    List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", buildUserMessage(in))));
        body.put("max_tokens",  maxTokens);
        body.put("temperature", 0.3);
        body.put("stream",      false);

        Map<?, ?> response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new BusinessException("AI_PROVIDER_ERROR",
                    "AI provider unavailable: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (response == null) {
            throw new BusinessException("AI_PROVIDER_ERROR",
                    "AI provider returned empty response",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return parseResponse(response);
    }

    @Override
    public String modelName() {
        return model;
    }

    private static String buildUserMessage(Input in) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(in.stem()).append('\n');
        sb.append("Choices:\n");
        for (Map<String, String> c : in.choices()) {
            sb.append("  ").append(c.get("key"))
              .append(") ").append(c.get("text")).append('\n');
        }
        sb.append("User picked: ").append(in.selectedChoiceKey()).append('\n');
        sb.append("Correct: ").append(in.correctChoiceKey());
        if (in.staticExplanation() != null && !in.staticExplanation().isBlank()) {
            sb.append('\n').append("Reference: ").append(in.staticExplanation());
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Output parseResponse(Map<?, ?> response) {
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            throw new BusinessException("AI_PROVIDER_ERROR",
                    "AI provider response missing choices",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        Map<String, Object> first = (Map<String, Object>) choices.get(0);
        Map<String, Object> message = (Map<String, Object>) first.get("message");
        Object contentObj = message == null ? null : message.get("content");
        if (!(contentObj instanceof String content) || content.isBlank()) {
            throw new BusinessException("AI_PROVIDER_ERROR",
                    "AI provider response missing content",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        Integer tokensIn  = usage == null ? null : toInt(usage.get("prompt_tokens"));
        Integer tokensOut = usage == null ? null : toInt(usage.get("completion_tokens"));
        return new Output(content, tokensIn, tokensOut);
    }

    private static Integer toInt(Object o) {
        return o instanceof Number n ? n.intValue() : null;
    }
}
