package com.dmvmotor.api.billing.application;

import java.time.OffsetDateTime;

/**
 * Thin seam over the Stripe SDK so the billing LOGIC stays unit-testable with a
 * fake. The real implementation ({@code StripeGatewayImpl}) is the only place
 * that touches {@code com.stripe.*} and is excluded from coverage — like the
 * Firebase token verifier.
 */
public interface StripeGateway {

    /** Returns {@code existingCustomerId} if non-null, else creates a Stripe
     *  Customer for the user and returns the new id. */
    String ensureCustomer(String existingCustomerId, Long userId, String email);

    /** Creates a subscription-mode hosted Checkout Session and returns its id + URL.
     *  The session and resulting subscription both carry {user_id, exam_id} metadata
     *  so webhooks can map events back to the right pass. */
    CheckoutSession createSubscriptionCheckout(String customerId, String priceId,
                                               Long userId, Long examId,
                                               String successUrl, String cancelUrl);

    /** Cancels a subscription immediately (the {@code customer.subscription.deleted}
     *  webhook then revokes the pass; callers may also revoke eagerly). */
    void cancelSubscription(String subscriptionId);

    /** Verifies the webhook signature and projects the payload to the minimal set
     *  of fields the fulfillment logic needs. Throws on an invalid signature. */
    StripeEvent parseWebhookEvent(String payload, String signatureHeader);

    /** Hosted Checkout Session: its id and the URL to redirect the buyer to. */
    record CheckoutSession(String id, String url) {}

    /**
     * Minimal projection of a Stripe webhook event. {@code userId}/{@code examId}
     * come from the metadata we set at checkout; fields are null when not
     * applicable to the event type.
     */
    record StripeEvent(
            String         type,
            Long           userId,
            Long           examId,
            String         subscriptionId,
            OffsetDateTime currentPeriodEnd
    ) {}
}
