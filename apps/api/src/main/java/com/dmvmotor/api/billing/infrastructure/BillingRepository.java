package com.dmvmotor.api.billing.infrastructure;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/**
 * Billing reads/writes for the Stripe columns (V34) across users / exams /
 * access_passes. Self-contained with dynamic jOOQ refs so the billing module
 * owns its own persistence without touching the other repos or a jOOQ regen.
 */
@Repository
public class BillingRepository {

    private static final Table<?>      USERS         = DSL.table(DSL.name("users"));
    private static final Field<Long>   U_ID          = DSL.field(DSL.name("users", "id"), Long.class);
    private static final Field<String> U_EMAIL       = DSL.field(DSL.name("users", "email"), String.class);
    private static final Field<String> U_STRIPE_CUST = DSL.field(DSL.name("users", "stripe_customer_id"), String.class);

    private static final Table<?>      EXAMS   = DSL.table(DSL.name("exams"));
    private static final Field<Long>   E_ID    = DSL.field(DSL.name("exams", "id"), Long.class);
    private static final Field<String> E_PRICE = DSL.field(DSL.name("exams", "stripe_price_id"), String.class);

    private static final Table<?>               AP            = DSL.table(DSL.name("access_passes"));
    private static final Field<Long>            AP_ID         = DSL.field(DSL.name("access_passes", "id"), Long.class);
    private static final Field<Long>            AP_USER       = DSL.field(DSL.name("access_passes", "user_id"), Long.class);
    private static final Field<Long>            AP_EXAM       = DSL.field(DSL.name("access_passes", "exam_id"), Long.class);
    private static final Field<String>          AP_STATUS     = DSL.field(DSL.name("access_passes", "status"), String.class);
    private static final Field<OffsetDateTime>  AP_STARTS     = DSL.field(DSL.name("access_passes", "starts_at"), OffsetDateTime.class);
    private static final Field<OffsetDateTime>  AP_EXPIRES    = DSL.field(DSL.name("access_passes", "expires_at"), OffsetDateTime.class);
    private static final Field<Integer>         AP_MOCK_TOTAL = DSL.field(DSL.name("access_passes", "mock_exam_total_count"), Integer.class);
    private static final Field<Integer>         AP_MOCK_USED  = DSL.field(DSL.name("access_passes", "mock_exam_used_count"), Integer.class);
    private static final Field<String>          AP_SUB        = DSL.field(DSL.name("access_passes", "stripe_subscription_id"), String.class);

    private final DSLContext dsl;

    public BillingRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public String findStripeCustomerId(Long userId) {
        return dsl.select(U_STRIPE_CUST).from(USERS).where(U_ID.eq(userId)).fetchOne(U_STRIPE_CUST);
    }

    public String findUserEmail(Long userId) {
        return dsl.select(U_EMAIL).from(USERS).where(U_ID.eq(userId)).fetchOne(U_EMAIL);
    }

    public void setStripeCustomerId(Long userId, String customerId) {
        dsl.update(USERS).set(U_STRIPE_CUST, customerId).where(U_ID.eq(userId)).execute();
    }

    public String findStripePriceId(Long examId) {
        return dsl.select(E_PRICE).from(EXAMS).where(E_ID.eq(examId)).fetchOne(E_PRICE);
    }

    /** Inserts an active per-exam pass funded by {@code subscriptionId}. */
    public Long insertSubscriptionPass(Long userId, Long examId, String subscriptionId,
                                       OffsetDateTime startsAt, OffsetDateTime expiresAt, int mockTotal) {
        return dsl.insertInto(AP)
                .set(AP_USER,       userId)
                .set(AP_EXAM,       examId)
                .set(AP_STATUS,     "active")
                .set(AP_STARTS,     startsAt)
                .set(AP_EXPIRES,    expiresAt)
                .set(AP_MOCK_TOTAL, mockTotal)
                .set(AP_MOCK_USED,  0)
                .set(AP_SUB,        subscriptionId)
                .returningResult(AP_ID)
                .fetchOne()
                .value1();
    }

    /** Deactivates active pass(es) funded by {@code subscriptionId} (unsubscribe). */
    public int cancelBySubscriptionId(String subscriptionId) {
        return dsl.update(AP)
                .set(AP_STATUS, "inactive")
                .where(AP_SUB.eq(subscriptionId).and(AP_STATUS.eq("active")))
                .execute();
    }

    /** The Stripe subscription id behind the user's currently-active pass for the
     *  exam, or null if none — used to cancel the right subscription. */
    public String findActiveSubscriptionId(Long userId, Long examId) {
        OffsetDateTime now = OffsetDateTime.now();
        return dsl.select(AP_SUB).from(AP)
                .where(AP_USER.eq(userId)
                        .and(AP_EXAM.eq(examId))
                        .and(AP_STATUS.eq("active"))
                        .and(AP_SUB.isNotNull())
                        .and(AP_STARTS.le(now))
                        .and(AP_EXPIRES.gt(now)))
                .orderBy(AP_EXPIRES.desc())
                .limit(1)
                .fetchOne(AP_SUB);
    }
}
