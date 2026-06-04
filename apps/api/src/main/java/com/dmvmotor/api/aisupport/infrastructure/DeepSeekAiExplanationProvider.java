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

    // Exam-aware persona: %s = exam label (e.g. "California Class C (Car)"), so
    // the same provider serves any state × license type rather than hardcoding M1.
    private static final String SYSTEM_PROMPT_EN =
            "You are a tutor for the %s written knowledge test. "
            + "The user picked a wrong choice. In 2-3 sentences, explain "
            + "why their choice is wrong and why the correct one is right. "
            + "Plain English. Don't repeat the question. Plain text only — no markdown.";

    private static final String SYSTEM_PROMPT_ZH =
            "你是 %s 笔试辅导员。用户选错了。"
            + "用 2-3 句话解释为什么他们选错以及正确答案为什么对。"
            + "用简单中文。不要重复题目。纯文本输出，不要 markdown。";

    // enhance1 "深入分析": the learner tapped a direction (aspect) to go deeper.
    // The prompt is built from the exam label + a per-aspect focus + the thread
    // so far (fed back so the layer is progressive and doesn't repeat).
    // %1$s = exam label, %2$d = depth, %3$s = aspect focus instruction.
    private static final String DEEP_DIVE_PROMPT_EN =
            "You are a tutor for the %1$s written knowledge test. The "
            + "learner already read an explanation of this question and tapped "
            + "to go deeper (layer %2$d). %3$s Build on what was already said — "
            + "do NOT repeat earlier points; add something new and more specific. "
            + "3-4 sentences, plain English, plain text only (no markdown).";

    private static final String DEEP_DIVE_PROMPT_ZH =
            "你是 %1$s 笔试辅导员。学员已看过这道题的解释并点击继续"
            + "深入（第 %2$d 层）。%3$s 要在已说内容的基础上递进——**不要重复**"
            + "前面的要点，补充更新、更具体的内容。3-4 句，简单中文，纯文本（不要 markdown）。";

    private static String examLabelOrDefault(String label, boolean zh) {
        if (label != null && !label.isBlank()) return label;
        return zh ? "加州 DMV" : "California DMV";
    }

    /** Per-aspect focus instruction. Keys must match the frontend buttons. */
    private static String aspectFocusEn(String aspect) {
        return switch (aspect == null ? "" : aspect) {
            case "example"     -> "Focus: give a concrete real-world driving scenario that illustrates the rule.";
            case "mnemonic"    -> "Focus: give a memory aid or mnemonic that makes the answer stick.";
            case "distractors" -> "Focus: explain why each of the other (wrong) choices is tempting but wrong.";
            case "rule"        -> "Focus: explain the underlying law/regulation and the safety reason behind it.";
            default            -> "Focus: go one level deeper than the basic explanation.";
        };
    }

    private static String aspectFocusZh(String aspect) {
        return switch (aspect == null ? "" : aspect) {
            case "example"     -> "重点：举一个具体的真实驾驶场景来说明这条规则。";
            case "mnemonic"    -> "重点：给一个让答案好记的记忆方法或口诀。";
            case "distractors" -> "重点：逐个说明其他（错误）选项为什么有迷惑性、为什么错。";
            case "rule"        -> "重点：讲清楚背后的法规和安全原理。";
            default            -> "重点：比基础解释再深入一层。";
        };
    }

    private static final int PRIOR_CONTEXT_MAX = 1200;

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
        String examLabel = examLabelOrDefault(in.examLabel(), zh);
        String systemPrompt = in.depth() > 0
                ? String.format(zh ? DEEP_DIVE_PROMPT_ZH : DEEP_DIVE_PROMPT_EN,
                        examLabel, in.depth(), zh ? aspectFocusZh(in.aspect()) : aspectFocusEn(in.aspect()))
                : String.format(zh ? SYSTEM_PROMPT_ZH : SYSTEM_PROMPT_EN, examLabel);

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
        // Deep-dive: feed the thread so far so the next layer builds on it and
        // doesn't repeat. Truncated to bound prompt cost; this is the AI's own
        // earlier output, framed as context (not a free-text user instruction).
        if (in.priorContext() != null && !in.priorContext().isBlank()) {
            String prior = in.priorContext();
            if (prior.length() > PRIOR_CONTEXT_MAX) {
                prior = prior.substring(prior.length() - PRIOR_CONTEXT_MAX);
            }
            sb.append('\n').append("Already explained (do not repeat):\n").append(prior);
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
