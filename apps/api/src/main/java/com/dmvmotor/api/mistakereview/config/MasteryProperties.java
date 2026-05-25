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
        @DefaultValue("6")  int recentCorrectThreshold,
        @DefaultValue       Subtopic subtopic
) {

    /**
     * Sub-topic mastery uses a smaller window (4 vs 8) because each sub-topic
     * has fewer questions (~5-8 per sub-topic). Requiring 8 recent attempts
     * would force users to redo the same questions repeatedly before reaching
     * mastery. Initial values per design doc decision #6.
     */
    public record Subtopic(
            @DefaultValue("80") int correctRateThreshold,
            @DefaultValue("4")  int recentWindow,
            @DefaultValue("3")  int recentCorrectThreshold
    ) {}
}
