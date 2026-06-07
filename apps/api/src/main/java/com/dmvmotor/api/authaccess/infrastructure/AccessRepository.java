package com.dmvmotor.api.authaccess.infrastructure;

import com.dmvmotor.api.authaccess.domain.AccessPass;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class AccessRepository {

    // V32 access_passes.exam_id (dynamic ref, no jOOQ regen). NULL = legacy /
    // dev grant-pass = a global pass that unlocks any exam.
    private static final Field<Long> AP_EXAM_ID =
            DSL.field(DSL.name("access_passes", "exam_id"), Long.class);

    private final DSLContext dsl;

    public AccessRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Returns the pass that should drive the user's current access state.
     *
     * Selection priority (to avoid the bug where a future or
     * expired-but-still-status-active row could mask a currently-valid one):
     *   1. status='active' AND now ∈ [starts_at, expires_at) — currently in
     *      window. Tiebreak: latest expires_at, then latest created_at.
     *   2. fall back to the user's most recent pass overall, ordered by
     *      expires_at DESC then created_at DESC, so the UI can still say
     *      "expired on X" for a recently-lapsed pass.
     *
     * Returns empty for anonymous (userId null) or users with no passes.
     *
     * Scoped to {@code examId}: a per-exam pass counts only for its exam; a
     * legacy/dev pass (exam_id NULL) is global and counts for any exam.
     */
    public Optional<AccessPass> findRelevantPassByUserId(Long userId, Long examId, OffsetDateTime now) {
        if (userId == null) return Optional.empty();

        var ap = Tables.ACCESS_PASSES;
        Record r = dsl.selectFrom(ap)
                .where(ap.USER_ID.eq(userId)
                        .and(AP_EXAM_ID.eq(examId).or(AP_EXAM_ID.isNull())))
                .orderBy(
                        // Tier 1: currently in window — priority 0 (best),
                        // everything else priority 1.
                        DSL.case_()
                                .when(ap.STATUS.eq("active")
                                                .and(ap.STARTS_AT.le(now))
                                                .and(ap.EXPIRES_AT.gt(now)),
                                        0)
                                .otherwise(1)
                                .asc(),
                        // Tier 2: prefer rows still flagged status='active'.
                        ap.STATUS.eq("active").desc(),
                        // Tier 3: longest-running window first (latest expires_at).
                        ap.EXPIRES_AT.desc().nullsLast(),
                        // Tiebreak: most-recent insert.
                        ap.CREATED_AT.desc())
                .limit(1)
                .fetchOne();

        if (r == null) return Optional.empty();

        return Optional.of(toDomain(r));
    }

    private AccessPass toDomain(Record r) {
        var ap = Tables.ACCESS_PASSES;
        return new AccessPass(
                r.get(ap.ID),
                r.get(ap.USER_ID),
                r.get(ap.STATUS),
                r.get(ap.STARTS_AT),
                r.get(ap.EXPIRES_AT),
                r.get(ap.MOCK_EXAM_TOTAL_COUNT),
                r.get(ap.MOCK_EXAM_USED_COUNT));
    }

    /**
     * Inserts a fresh active pass for the given user. Used by the dev-only
     * grant endpoint and by future paid-checkout flows. Returns the inserted
     * row's id.
     */
    public Long insertActivePass(Long userId,
                                  OffsetDateTime startsAt,
                                  OffsetDateTime expiresAt,
                                  int mockExamTotalCount) {
        return insertActivePass(userId, null, startsAt, expiresAt, mockExamTotalCount);
    }

    /** Insert a pass scoped to {@code examId} (null = global). The per-exam form
     *  is what the dev grant + future checkout use (V32 subscription model). */
    public Long insertActivePass(Long userId,
                                  Long examId,
                                  OffsetDateTime startsAt,
                                  OffsetDateTime expiresAt,
                                  int mockExamTotalCount) {
        var ap = Tables.ACCESS_PASSES;
        return dsl.insertInto(ap)
                .set(ap.USER_ID,               userId)
                .set(AP_EXAM_ID,               examId)
                .set(ap.STATUS,                "active")
                .set(ap.STARTS_AT,             startsAt)
                .set(ap.EXPIRES_AT,            expiresAt)
                .set(ap.MOCK_EXAM_TOTAL_COUNT, mockExamTotalCount)
                .set(ap.MOCK_EXAM_USED_COUNT,  0)
                .returningResult(ap.ID)
                .fetchOne()
                .value1();
    }
}
