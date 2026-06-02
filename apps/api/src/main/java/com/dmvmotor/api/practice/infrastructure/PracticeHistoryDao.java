package com.dmvmotor.api.practice.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read model for the Study Hub: practice-session history rows + aggregate
 * attempt totals. Extracted from {@link PracticeSessionRepository}
 * (dev-audit #3) so the read-only Study Hub queries are separate from the
 * session/attempt write + selection data access.
 */
@Repository
public class PracticeHistoryDao {

    // V26 practice_sessions.exam_id (dynamic ref, no jOOQ regen). Qualified so it
    // never collides when practice_attempts is also in scope (attemptTotals).
    private static final Field<Long> PS_EXAM_ID =
            DSL.field(DSL.name("practice_sessions", "exam_id"), Long.class);

    private final DSLContext dsl;

    public PracticeHistoryDao(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<SessionHistoryRow> findRecentByUserWithStats(Long userId, Long examId, int limit) {
        var ps = Tables.PRACTICE_SESSIONS;
        var pa = Tables.PRACTICE_ATTEMPTS;
        Field<Integer> answered = DSL.count(pa.ID).as("answered");
        // COALESCE SUM(...) so empty groups produce 0 instead of NULL.
        Field<Integer> correct = DSL.coalesce(
                DSL.sum(DSL.when(pa.IS_CORRECT.isTrue(), 1).otherwise(0)),
                0).cast(Integer.class).as("correct");
        return dsl.select(ps.ID, ps.ENTRY_TYPE, ps.LANGUAGE_CODE, ps.STATUS,
                          ps.STARTED_AT, ps.COMPLETED_AT, answered, correct)
                .from(ps)
                .leftJoin(pa).on(pa.PRACTICE_SESSION_ID.eq(ps.ID))
                .where(ps.USER_ID.eq(userId).and(PS_EXAM_ID.eq(examId)))
                .groupBy(ps.ID)
                .orderBy(ps.STARTED_AT.desc(), ps.ID.desc())
                .limit(limit)
                .fetch(r -> new SessionHistoryRow(
                        r.get(ps.ID),
                        r.get(ps.ENTRY_TYPE),
                        r.get(ps.LANGUAGE_CODE),
                        r.get(ps.STATUS),
                        r.get(ps.STARTED_AT),
                        r.get(ps.COMPLETED_AT),
                        r.get(answered),
                        r.get(correct)
                ));
    }

    public int countByUser(Long userId, Long examId) {
        var ps = Tables.PRACTICE_SESSIONS;
        // selectCount + fetchOne never returns null for an aggregate.
        return dsl.selectCount().from(ps)
                .where(ps.USER_ID.eq(userId).and(PS_EXAM_ID.eq(examId)))
                .fetchOne(0, Integer.class);
    }

    public AttemptTotals attemptTotals(Long userId, Long examId) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        var ps = Tables.PRACTICE_SESSIONS;
        Field<Integer> total = DSL.count(pa.ID);
        // SUM over zero rows = SQL NULL, so coalesce to 0 here to keep the
        // Java contract integer-typed.
        Field<Integer> correct = DSL.coalesce(
                DSL.sum(DSL.when(pa.IS_CORRECT.isTrue(), 1).otherwise(0)),
                0).cast(Integer.class);
        // practice_attempts carries no exam_id — scope through its session.
        var record = dsl.select(total, correct)
                .from(pa)
                .join(ps).on(ps.ID.eq(pa.PRACTICE_SESSION_ID))
                .where(pa.USER_ID.eq(userId).and(PS_EXAM_ID.eq(examId)))
                .fetchOne();
        return new AttemptTotals(record.get(total), record.get(correct));
    }

    public record SessionHistoryRow(
            Long           id,
            String         entryType,
            String         languageCode,
            String         status,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            int            answeredCount,
            int            correctCount
    ) {}

    public record AttemptTotals(int answered, int correct) {}
}
