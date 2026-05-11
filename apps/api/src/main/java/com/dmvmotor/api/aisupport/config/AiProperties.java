package com.dmvmotor.api.aisupport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * AI explanation parameters — sourced from progress §27 (decision points 1, 6).
 *
 * <p>Cooldown algorithm (decision point #6, "thinking-time" semantics):
 * given {@code N} = AI calls in the past 24h, the cooldown required before
 * the next call is {@code min(base + N * increment, max)} seconds. A bot
 * scanning the bank trips the cap quickly; a human pacing through real
 * questions never even brushes the floor.
 *
 * <p>Cache hits ({@code UNIQUE(user_id, question_id, language)} match) skip
 * the rate-limit check entirely — no LLM call, no cost.
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        @DefaultValue("true")  boolean   enabled,
        @DefaultValue("stub")  String    provider,
        @DefaultValue("120")   int       baseCooldownSeconds,
        @DefaultValue("60")    int       cooldownIncrementSeconds,
        @DefaultValue("300")   int       maxCooldownSeconds,
        @DefaultValue("50")    int       maxCallsPerDay,
        @DefaultValue          Deepseek  deepseek
) {

    /**
     * DeepSeek provider config — bound only when {@code app.ai.provider=deepseek}.
     * The empty default for {@code apiKey} lets Spring Boot start under the stub
     * profile (dev/test) without a real key.
     */
    public record Deepseek(
            @DefaultValue("")                          String apiKey,
            @DefaultValue("https://api.deepseek.com")  String baseUrl,
            @DefaultValue("deepseek-chat")             String model,
            @DefaultValue("400")                       int    maxTokens,
            @DefaultValue("30")                        int    timeoutSeconds
    ) {}
}
