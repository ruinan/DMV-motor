package com.dmvmotor.api.billing.infrastructure;

import com.dmvmotor.api.billing.application.BillingProperties;
import com.dmvmotor.api.billing.application.StripeGateway;
import com.dmvmotor.api.common.BusinessException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The ONLY class that touches the Stripe SDK — excluded from coverage (jaCoCo)
 * since it can't be exercised without a real Stripe account. All billing LOGIC
 * lives in {@code BillingService} and is fake-tested. The global {@code Stripe.apiKey}
 * is set once from config when billing is enabled.
 */
@Component
public class StripeGatewayImpl implements StripeGateway {

    private final BillingProperties props;

    public StripeGatewayImpl(BillingProperties props) {
        this.props = props;
        if (props.enabled()) {
            Stripe.apiKey = props.stripeSecretKey();
        }
    }

    @Override
    public String ensureCustomer(String existingCustomerId, Long userId, String email) {
        if (existingCustomerId != null && !existingCustomerId.isBlank()) {
            return existingCustomerId;
        }
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .putMetadata("user_id", String.valueOf(userId))
                    .build();
            return Customer.create(params).getId();
        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public CheckoutSession createSubscriptionCheckout(String customerId, String priceId,
                                                      Long userId, Long examId,
                                                      String successUrl, String cancelUrl) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .putMetadata("user_id", String.valueOf(userId))
                    .putMetadata("exam_id", String.valueOf(examId))
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .putMetadata("user_id", String.valueOf(userId))
                            .putMetadata("exam_id", String.valueOf(examId))
                            .build())
                    .build();
            Session session = Session.create(params);
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public void cancelSubscription(String subscriptionId) {
        try {
            Subscription.retrieve(subscriptionId).cancel();
        } catch (StripeException e) {
            throw stripeError(e);
        }
    }

    @Override
    public StripeEvent parseWebhookEvent(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, props.stripeWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new BusinessException("INVALID_SIGNATURE",
                    "Invalid Stripe webhook signature", HttpStatus.BAD_REQUEST);
        }
        String type = event.getType();
        StripeObject obj = event.getDataObjectDeserializer().getObject().orElse(null);

        if ("checkout.session.completed".equals(type) && obj instanceof Session s) {
            Map<String, String> md = s.getMetadata();
            return new StripeEvent(type, metaLong(md, "user_id"), metaLong(md, "exam_id"),
                    s.getSubscription(), null);
        }
        if ("customer.subscription.deleted".equals(type) && obj instanceof Subscription sub) {
            Map<String, String> md = sub.getMetadata();
            return new StripeEvent(type, metaLong(md, "user_id"), metaLong(md, "exam_id"),
                    sub.getId(), null);
        }
        return new StripeEvent(type, null, null, null, null);
    }

    private static Long metaLong(Map<String, String> md, String key) {
        if (md == null) return null;
        String v = md.get(key);
        if (v == null || v.isBlank()) return null;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BusinessException stripeError(StripeException e) {
        return new BusinessException("STRIPE_ERROR",
                "Payment provider error", HttpStatus.BAD_GATEWAY);
    }
}
