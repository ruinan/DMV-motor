package com.dmvmotor.api.aisupport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * AI explanation parameters — sourced from progress §27 (decision points 1, 6).
 *
 * <p>Cooldown algorithm: given {@code N} = AI calls in the past 24h, the
 * cooldown required before the next call is {@code min(base + N * increment,
 * max)} seconds. With the relaxed defaults (base 10 / increment 0) this is a
 * flat ~10s floor that only blocks double-click / machine-gun bursts; the
 * daily cap is the real cost ceiling. (The original 120s + 60s ramp punished
 * real learners reviewing several wrong answers in a row.)
 *
 * <p>Cache hits ({@code UNIQUE(user_id, question_id, language)} match) skip
 * the rate-limit check entirely — no LLM call, no cost.
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        @DefaultValue("true")  boolean   enabled,
        @DefaultValue("stub")  String    provider,
        @DefaultValue("3")     int       baseCooldownSeconds,
        @DefaultValue("0")     int       cooldownIncrementSeconds,
        @DefaultValue("10")    int       maxCooldownSeconds,
        @DefaultValue("50")    int       maxCallsPerDay,
        // enhance1: a single question can be "深入分析"-ed at most this many
        // times (per language). The cap is enforced server-side and survives a
        // client localStorage clear (the deep-dive log persists), so repeatedly
        // clearing + re-burning still hits the ceiling. Beyond it the next
        // deep-dive is refused (RATE_LIMITED) until the daily window rolls.
        @DefaultValue("10")    int       maxDeepDivesPerQuestion,
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
