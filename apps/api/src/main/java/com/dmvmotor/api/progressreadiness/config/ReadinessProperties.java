package com.dmvmotor.api.progressreadiness.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Readiness / completion engine parameters — sourced from {@code docs/parameters.md}.
 * Exposed via {@code app.readiness.*} so weights and thresholds can be tuned without
 * a code change (docs/parameters.md §7 explicitly forbids hardcoding these).
 */
@ConfigurationProperties(prefix = "app.readiness")
public record ReadinessProperties(
        // --- Readiness hard gates ---
        @DefaultValue("85") int mockAverageThreshold,
        @DefaultValue("2")  int mockMinimumCount,
        @DefaultValue("90") int keyCoverageThreshold,
        @DefaultValue("80") int reviewCompletionThreshold,
        @DefaultValue("2")  int persistentMistakeWrongCount,

        // --- Readiness score weights (must sum to 100) ---
        @DefaultValue("40") int mockWeight,
        @DefaultValue("25") int keyCoverageWeight,
        @DefaultValue("20") int highRiskReviewWeight,
        @DefaultValue("15") int recentStabilityWeight,

        // --- Completion score weights (must sum to 100) ---
        @DefaultValue("50") int completionKeyCoverageWeight,
        @DefaultValue("30") int completionHighRiskReviewWeight,
        @DefaultValue("20") int completionBasicPracticeWeight,

        // --- Candidate threshold + recent stability window ---
        @DefaultValue("80") int readyThreshold,
        @DefaultValue("20") int recentPracticeWindow,
        @DefaultValue("85") int recentAccuracyThreshold
) {}
