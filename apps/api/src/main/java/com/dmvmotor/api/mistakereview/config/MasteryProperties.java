package com.dmvmotor.api.mistakereview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mastery evaluation parameters — sourced from {@code docs/parameters.md} mastery_threshold.
 * Exposed via {@code app.mastery.*} so thresholds can be tuned without a code change.
 *
 * <p>MVP implements only the two schema-backed gates. The design doc's third gate
 * ("不连续错在同一混淆点") requires a confusion-point schema that does not yet exist;
 * see {@code TODO(FUTURE_CONFUSION_SCHEMA)} in ReviewService.
 */
@ConfigurationProperties(prefix = "app.mastery")
public record MasteryProperties(
        @DefaultValue("80") int topicCorrectRateThreshold,
        @DefaultValue("8")  int recentWindow,
        @DefaultValue("6")  int recentCorrectThreshold
) {}
