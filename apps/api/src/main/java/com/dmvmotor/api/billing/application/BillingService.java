package com.dmvmotor.api.billing.application;

import com.dmvmotor.api.billing.application.StripeGateway.CheckoutSession;
import com.dmvmotor.api.billing.application.StripeGateway.StripeEvent;
import com.dmvmotor.api.billing.infrastructure.BillingRepository;
import com.dmvmotor.api.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Billing orchestration for per-exam monthly subscriptions (hosted Checkout).
 * The Stripe-specific work is behind {@link StripeGateway}; this layer is pure
 * logic + persistence and is unit-tested with a fake gateway.
 *
 * <p>Fulfillment is webhook-driven: {@code checkout.session.completed} creates an
 * access pass for the exam; {@code customer.subscription.deleted} deactivates it.
 * (Renewal — extending the pass each billing period via {@code invoice.paid} — is
 * a follow-up; for now a fulfilled pass runs ~one period and re-checkout renews.)
 */
@Service
public class BillingService {

    private static final Logger LOG = LoggerFactory.getLogger(BillingService.class);

    private final StripeGateway      stripe;
    private final BillingRepository  repo;
    private final BillingProperties  props;

    public BillingService(StripeGateway stripe, BillingRepository repo, BillingProperties props) {
        this.stripe = stripe;
        this.repo   = repo;
        this.props  = props;
    }

    public boolean enabled() {
        return props.enabled();
    }

    /** Subscribe: returns the hosted Checkout URL the client redirects the buyer to. */
    public String createCheckoutSession(Long userId, Long examId) {
        requireEnabled();
        String priceId = repo.findStripePriceId(examId);
        if (priceId == null || priceId.isBlank()) {
            throw new BusinessException("EXAM_NOT_PURCHASABLE",
                    "This exam isn't available for subscription yet",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        String customerId = stripe.ensureCustomer(
                repo.findStripeCustomerId(userId), userId, repo.findUserEmail(userId));
        repo.setStripeCustomerId(userId, customerId);

        CheckoutSession session = stripe.createSubscriptionCheckout(
                customerId, priceId, userId, examId, props.successUrl(), props.cancelUrl());
        return session.url();
    }

    /** Unsubscribe: cancels the Stripe subscription for the exam + revokes the pass. */
    public void cancelSubscription(Long userId, Long examId) {
        requireEnabled();
        String subId = repo.findActiveSubscriptionId(userId, examId);
        if (subId == null) {
            throw new BusinessException("NO_SUBSCRIPTION",
                    "No active subscription for this exam", HttpStatus.NOT_FOUND);
        }
        stripe.cancelSubscription(subId);
        // Revoke eagerly; the customer.subscription.deleted webhook is idempotent.
        repo.cancelBySubscriptionId(subId);
    }

    /** Webhook entrypoint: verifies the signature (in the gateway) + routes the event. */
    public void handleWebhook(String payload, String signatureHeader) {
        StripeEvent e = stripe.parseWebhookEvent(payload, signatureHeader);
        switch (e.type()) {
            case "checkout.session.completed"   -> fulfill(e);
            case "customer.subscription.deleted" -> revoke(e);
            default -> LOG.debug("[billing] ignoring webhook event {}", e.type());
        }
    }

    private void fulfill(StripeEvent e) {
        if (e.userId() == null || e.examId() == null || e.subscriptionId() == null) {
            LOG.warn("[billing] checkout.session.completed missing metadata — skipping fulfillment");
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expires = e.currentPeriodEnd() != null ? e.currentPeriodEnd() : now.plusDays(31);
        repo.insertSubscriptionPass(e.userId(), e.examId(), e.subscriptionId(),
                now, expires, props.monthlyMockQuota());
        LOG.info("[billing] fulfilled subscription {} for user {} exam {}",
                e.subscriptionId(), e.userId(), e.examId());
    }

    private void revoke(StripeEvent e) {
        if (e.subscriptionId() == null) return;
        int n = repo.cancelBySubscriptionId(e.subscriptionId());
        LOG.info("[billing] revoked {} pass(es) for cancelled subscription {}", n, e.subscriptionId());
    }

    private void requireEnabled() {
        if (!props.enabled()) {
            throw new BusinessException("BILLING_NOT_CONFIGURED",
                    "Billing is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
