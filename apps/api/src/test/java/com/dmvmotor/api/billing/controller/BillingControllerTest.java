package com.dmvmotor.api.billing.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import com.dmvmotor.api.billing.application.StripeGateway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Billing flow with a FAKE Stripe gateway (the real SDK adapter is smoke-tested
 * manually). Stripe is "enabled" via a dummy secret key. The fake parses webhook
 * payloads from a pipe-delimited {@code type|userId|examId|subId} string so the
 * fulfillment/revocation logic is driven deterministically.
 */
@TestPropertySource(properties = "app.billing.stripe-secret-key=sk_test_dummy")
@Import(BillingControllerTest.FakeStripeConfig.class)
class BillingControllerTest extends IntegrationTestBase {

    @Autowired MockMvc       mockMvc;
    @Autowired TestFixtures  fixtures;
    @Autowired StripeGateway gateway; // the @Primary fake

    private Long userId;
    private Long examId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId = fixtures.insertUser("pay@example.com");
        examId = fixtures.defaultExamId();
        ((FakeStripeGateway) gateway).reset();
    }

    private void fulfill(String subId) throws Exception {
        mockMvc.perform(post("/api/v1/billing/webhook")
                        .header("Stripe-Signature", "valid")
                        .content("checkout.session.completed|" + userId + "|" + examId + "|" + subId))
                .andExpect(status().isOk());
    }

    @Test
    void config_reportsEnabledWhenKeyPresent() throws Exception {
        mockMvc.perform(get("/api/v1/billing/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void checkout_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/billing/checkout-session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkout_examWithoutPrice_returns422() throws Exception {
        mockMvc.perform(post("/api/v1/billing/checkout-session")
                        .param("exam_id", String.valueOf(examId))
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("EXAM_NOT_PURCHASABLE"));
    }

    @Test
    void checkout_returnsHostedUrl() throws Exception {
        fixtures.setExamStripePrice(examId, "price_test_123");
        mockMvc.perform(post("/api/v1/billing/checkout-session")
                        .param("exam_id", String.valueOf(examId))
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(containsString("checkout.stripe.test")));
    }

    @Test
    void checkout_noExamParam_resolvesCurrentExam() throws Exception {
        // No exam_id → resolves the user's current exam (default CA-M1) and prices it.
        fixtures.setExamStripePrice(examId, "price_test_123");
        mockMvc.perform(post("/api/v1/billing/checkout-session")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(containsString("checkout.stripe.test")));
    }

    @Test
    void webhook_missingMetadata_doesNotFulfil() throws Exception {
        // checkout.session.completed with no user/exam/sub → fulfillment skipped.
        mockMvc.perform(post("/api/v1/billing/webhook")
                        .header("Stripe-Signature", "valid")
                        .content("checkout.session.completed|||"))
                .andExpect(status().isOk());
        fixtures.setUserCurrentExam(userId, examId);
        mockMvc.perform(get("/api/v1/access").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_active_pass").value(false));
    }

    @Test
    void webhook_unhandledEventType_isIgnored() throws Exception {
        mockMvc.perform(post("/api/v1/billing/webhook")
                        .header("Stripe-Signature", "valid")
                        .content("invoice.paid|||sub_123"))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_subscriptionDeletedWithoutSubId_isNoOp() throws Exception {
        mockMvc.perform(post("/api/v1/billing/webhook")
                        .header("Stripe-Signature", "valid")
                        .content("customer.subscription.deleted|||"))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_checkoutCompleted_fulfillsPass() throws Exception {
        mockMvc.perform(get("/api/v1/access").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_active_pass").value(false));

        fulfill("sub_abc");

        fixtures.setUserCurrentExam(userId, examId);
        mockMvc.perform(get("/api/v1/access").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_active_pass").value(true));
    }

    @Test
    void webhook_subscriptionDeleted_revokesPass() throws Exception {
        fulfill("sub_xyz");
        fixtures.setUserCurrentExam(userId, examId);
        mockMvc.perform(get("/api/v1/access").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_active_pass").value(true));

        mockMvc.perform(post("/api/v1/billing/webhook")
                        .header("Stripe-Signature", "valid")
                        .content("customer.subscription.deleted|||sub_xyz"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/access").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_active_pass").value(false));
    }

    @Test
    void cancelEndpoint_cancelsStripeSubAndRevokes() throws Exception {
        fulfill("sub_cancelme");
        fixtures.setUserCurrentExam(userId, examId);

        mockMvc.perform(post("/api/v1/billing/cancel")
                        .param("exam_id", String.valueOf(examId))
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cancelled").value(true));

        Assertions.assertTrue(((FakeStripeGateway) gateway).cancelled.contains("sub_cancelme"),
                "the Stripe subscription should have been cancelled");
        mockMvc.perform(get("/api/v1/access").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_active_pass").value(false));
    }

    @Test
    void cancelEndpoint_noSubscription_returns404() throws Exception {
        // No exam_id → resolves the current exam; no active subscription → 404.
        mockMvc.perform(post("/api/v1/billing/cancel")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NO_SUBSCRIPTION"));
    }

    // ---------------------------------------------------------------

    @TestConfiguration
    static class FakeStripeConfig {
        @Bean
        @Primary
        StripeGateway fakeStripeGateway() {
            return new FakeStripeGateway();
        }
    }

    static class FakeStripeGateway implements StripeGateway {
        final List<String> cancelled = new ArrayList<>();

        void reset() {
            cancelled.clear();
        }

        @Override
        public String ensureCustomer(String existingCustomerId, Long userId, String email) {
            return existingCustomerId != null ? existingCustomerId : "cus_fake_" + userId;
        }

        @Override
        public CheckoutSession createSubscriptionCheckout(String customerId, String priceId,
                                                          Long userId, Long examId,
                                                          String successUrl, String cancelUrl) {
            return new CheckoutSession("cs_fake", "https://checkout.stripe.test/cs_fake");
        }

        @Override
        public void cancelSubscription(String subscriptionId) {
            cancelled.add(subscriptionId);
        }

        @Override
        public StripeEvent parseWebhookEvent(String payload, String signatureHeader) {
            String[] p = payload.split("\\|", -1);
            String type = p[0];
            Long uid = p.length > 1 && !p[1].isEmpty() ? Long.parseLong(p[1]) : null;
            Long eid = p.length > 2 && !p[2].isEmpty() ? Long.parseLong(p[2]) : null;
            String sub = p.length > 3 && !p[3].isEmpty() ? p[3] : null;
            return new StripeEvent(type, uid, eid, sub, null);
        }
    }
}
