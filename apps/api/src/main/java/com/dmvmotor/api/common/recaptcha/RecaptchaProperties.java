package com.dmvmotor.api.common.recaptcha;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * reCAPTCHA Enterprise config (bot / human verification). Disabled by default —
 * so local dev and tests run without it, exactly like {@code app.billing} and
 * {@code app.auth.firebase}. Prod sets {@code app.recaptcha.enabled=true} plus
 * the project + site key (env / Secret Manager). When disabled the guard is a
 * no-op, so sensitive endpoints stay reachable in dev.
 */
@ConfigurationProperties(prefix = "app.recaptcha")
public record RecaptchaProperties(
        @DefaultValue("false") boolean enabled,
        String projectId,
        String siteKey,
        /** Minimum risk score (0.0 bot … 1.0 human) to accept. */
        @DefaultValue("0.5") double minScore
) {}
