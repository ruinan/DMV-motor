package com.dmvmotor.api.billing.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Default context = no Stripe key, so billing is DISABLED. The catalog learns
 * this from /config and the subscribe/cancel endpoints refuse (503) before any
 * Stripe call. Uses the shared context (no property override), so it's cheap.
 */
class BillingDisabledTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId = fixtures.insertUser("nokey@example.com");
    }

    @Test
    void config_reportsDisabledWithoutKey() throws Exception {
        mockMvc.perform(get("/api/v1/billing/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void checkout_whenDisabled_returns503() throws Exception {
        mockMvc.perform(post("/api/v1/billing/checkout-session")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("BILLING_NOT_CONFIGURED"));
    }

    @Test
    void cancel_whenDisabled_returns503() throws Exception {
        mockMvc.perform(post("/api/v1/billing/cancel")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("BILLING_NOT_CONFIGURED"));
    }
}
