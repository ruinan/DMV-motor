package com.dmvmotor.api.mockexam.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read model for the Study Hub: mock-attempt history rows + aggregate stats.
 * Extracted from {@link MockExamRepository} (dev-audit #3) so the read-only
 * Study Hub queries are separate from the template/attempt/answer write data
 * access.
 */
@Repository
public class MockHistoryDao {

    private final DSLContext dsl;

    public MockHistoryDao(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<AttemptHistoryRow> findRecentByUser(Long userId, int limit) {
        var ma = Tables.MOCK_ATTEMPTS;
        var me = Tables.MOCK_EXAMS;
        return dsl.select(ma.ID, ma.MOCK_EXAM_ID, me.CODE, ma.STATUS, ma.SCORE_PERCENT,
                          ma.CORRECT_COUNT, ma.ANSWERED_COUNT, ma.STARTED_AT, ma.SUBMITTED_AT)
                .from(ma)
                .join(me).on(me.ID.eq(ma.MOCK_EXAM_ID))
                .where(ma.USER_ID.eq(userId))
                .orderBy(ma.STARTED_AT.desc(), ma.ID.desc())
                .limit(limit)
                .fetch(r -> new AttemptHistoryRow(
                        r.get(ma.ID),
                        r.get(ma.MOCK_EXAM_ID),
                        r.get(me.CODE),
                        r.get(ma.STATUS),
                        r.get(ma.SCORE_PERCENT),
                        r.get(ma.CORRECT_COUNT),
                        r.get(ma.ANSWERED_COUNT),
                        r.get(ma.STARTED_AT),
                        r.get(ma.SUBMITTED_AT)
                ));
    }

    public AttemptStats aggregateStats(Long userId) {
        var ma = Tables.MOCK_ATTEMPTS;
        int total = dsl.selectCount().from(ma).where(ma.USER_ID.eq(userId))
                .fetchOne(0, Integer.class);
        int submitted = dsl.selectCount().from(ma)
                .where(ma.USER_ID.eq(userId).and(ma.STATUS.eq("submitted")))
                .fetchOne(0, Integer.class);
        int exited = dsl.selectCount().from(ma)
                .where(ma.USER_ID.eq(userId).and(ma.STATUS.eq("ended_by_exit")))
                .fetchOne(0, Integer.class);
        // Scored attempts = clean submits + timeouts (a timeout still produces a
        // real score). Fail-outs / exits are excluded from best/avg/latest.
        var scored = ma.STATUS.in("submitted", "ended_by_timeout");
        // best/latest CAN legitimately be null (no scored attempts yet).
        Integer best = dsl.select(org.jooq.impl.DSL.max(ma.SCORE_PERCENT)).from(ma)
                .where(ma.USER_ID.eq(userId).and(scored))
                .fetchOne(0, Integer.class);
        Integer latest = dsl.select(ma.SCORE_PERCENT).from(ma)
                .where(ma.USER_ID.eq(userId).and(scored))
                .orderBy(ma.SUBMITTED_AT.desc(), ma.ID.desc())
                .limit(1)
                .fetchOne(0, Integer.class);
        List<Integer> recent3 = dsl.select(ma.SCORE_PERCENT).from(ma)
                .where(ma.USER_ID.eq(userId).and(scored))
                .orderBy(ma.SUBMITTED_AT.desc(), ma.ID.desc())
                .limit(3)
                .fetch(ma.SCORE_PERCENT);
        Integer recentAvg = recent3.isEmpty() ? null :
                (int) Math.round(recent3.stream().mapToInt(Integer::intValue).average().orElse(0));
        return new AttemptStats(total, submitted, exited, recentAvg, best, latest);
    }

    public int countAttemptsByUser(Long userId) {
        var ma = Tables.MOCK_ATTEMPTS;
        return dsl.selectCount().from(ma).where(ma.USER_ID.eq(userId))
                .fetchOne(0, Integer.class);
    }

    public record AttemptHistoryRow(
            Long                       id,
            Long                       mockExamId,
            String                     mockExamCode,
            String                     status,
            Integer                    scorePercent,
            Integer                    correctCount,
            int                        answeredCount,
            java.time.OffsetDateTime   startedAt,
            java.time.OffsetDateTime   submittedAt
    ) {}

    public record AttemptStats(
            int     totalAttempts,
            int     submittedCount,
            int     exitedCount,
            Integer recent3AvgScorePercent,
            Integer bestScorePercent,
            Integer latestScorePercent
    ) {}
}
