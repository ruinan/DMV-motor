package com.dmvmotor.api.aisupport.infrastructure;

import com.dmvmotor.api.aisupport.application.AiReviewPlanProvider;
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
 * DeepSeek-backed mock-exam review-plan generator — active when
 * {@code app.ai.provider=deepseek}. Same RestClient + failure semantics as
 * {@link DeepSeekAiExplanationProvider}: any non-clean response collapses to
 * a {@code BusinessException("AI_PROVIDER_ERROR", 503)} so the caller doesn't
 * persist a bad plan.
 *
 * <p>Privacy contract: the wire payload is exam content only — score, wrong
 * question stems + their topics + the user's pick vs correct key. No user_id.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "deepseek")
public class DeepSeekAiReviewPlanProvider implements AiReviewPlanProvider {

    private static final String SYSTEM_PROMPT_EN =
            "You are a California Class M1 motorcycle permit-test coach. The user just "
            + "finished a mock exam. Given their score and the questions they got wrong "
            + "(with topics), write a concise, encouraging review plan: 1) one-line "
            + "verdict on readiness, 2) the 2-3 weakest topics to focus on, 3) a short "
            + "ordered action list. Plain text, no markdown. Keep under 180 words.";

    private static final String SYSTEM_PROMPT_ZH =
            "你是加州 M1 摩托车驾照笔试教练。用户刚做完一次模拟考试。根据分数和答错的题"
            + "（含知识点），写一份简洁、鼓励性的复习计划：1) 一句话判断是否接近通过，"
            + "2) 最该补的 2-3 个知识点，3) 简短有序的行动清单。纯文本，不要 markdown，"
            + "控制在 200 字以内。";

    private final RestClient restClient;
    private final String     model;
    private final int        maxTokens;

    public DeepSeekAiReviewPlanProvider(AiProperties props) {
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
        this.maxTokens = Math.max(ds.maxTokens(), 500);
    }

    @Override
    public Output generate(Input in) {
        String systemPrompt = "zh".equalsIgnoreCase(in.language())
                ? SYSTEM_PROMPT_ZH : SYSTEM_PROMPT_EN;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model",       model);
        body.put("messages",    List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", buildUserMessage(in))));
        body.put("max_tokens",  maxTokens);
        body.put("temperature", 0.4);
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
                    "AI provider returned empty response", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return parseResponse(response);
    }

    @Override
    public String modelName() {
        return model;
    }

    private static String buildUserMessage(Input in) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score: ").append(in.scorePercent()).append("% (")
          .append(in.correctCount()).append("/").append(in.totalQuestions()).append(" correct). ")
          .append(in.passed() ? "Passed." : "Did not pass.").append('\n');
        if (in.wrongItems().isEmpty()) {
            sb.append("No wrong answers recorded.");
        } else {
            sb.append("Wrong answers:\n");
            for (WrongItem w : in.wrongItems()) {
                sb.append("- [").append(w.topicLabel());
                if (w.subTopicLabel() != null && !w.subTopicLabel().isBlank()) {
                    sb.append(" / ").append(w.subTopicLabel());
                }
                sb.append("] ").append(w.stem())
                  .append(" (picked ").append(w.selectedChoiceKey())
                  .append(", correct ").append(w.correctChoiceKey()).append(")\n");
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static Output parseResponse(Map<?, ?> response) {
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            throw new BusinessException("AI_PROVIDER_ERROR",
                    "AI provider response missing choices", HttpStatus.SERVICE_UNAVAILABLE);
        }
        Map<String, Object> first = (Map<String, Object>) choices.get(0);
        Map<String, Object> message = (Map<String, Object>) first.get("message");
        Object contentObj = message == null ? null : message.get("content");
        if (!(contentObj instanceof String content) || content.isBlank()) {
            throw new BusinessException("AI_PROVIDER_ERROR",
                    "AI provider response missing content", HttpStatus.SERVICE_UNAVAILABLE);
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
