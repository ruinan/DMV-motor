package com.dmvmotor.api.billing.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

/**
 * Stripe billing config ({@code app.billing.*}). Keys come from env / Secret
 * Manager; absent in dev/test by default. Billing is {@link #enabled()} only when
 * a secret key is present — otherwise the catalog falls back to the dev grant.
 */
@ConfigurationProperties(prefix = "app.billing")
public record BillingProperties(
        @DefaultValue("")   String stripeSecretKey,
        @DefaultValue("")   String stripeWebhookSecret,
        @DefaultValue("")   String successUrl,
        @DefaultValue("")   String cancelUrl,
        @DefaultValue("30") int    monthlyMockQuota
) {
    /** Stripe is wired only when a secret key is configured. */
    public boolean enabled() {
        return StringUtils.hasText(stripeSecretKey);
    }
}
