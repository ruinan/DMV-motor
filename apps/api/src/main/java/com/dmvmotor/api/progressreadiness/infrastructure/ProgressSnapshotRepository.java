package com.dmvmotor.api.progressreadiness.infrastructure;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Progress snapshots (paid remote backup). Dynamic jOOQ field refs — the V33
 * table needs no jOOQ regen (codebase convention for post-V1 tables).
 */
@Repository
public class ProgressSnapshotRepository {

    private static final Table<?>              T          = DSL.table(DSL.name("progress_snapshots"));
    private static final Field<Long>           ID         = f("id", Long.class);
    private static final Field<Long>           USER_ID    = f("user_id", Long.class);
    private static final Field<Long>           EXAM_ID    = f("exam_id", Long.class);
    private static final Field<Integer>        READINESS  = f("readiness_score", Integer.class);
    private static final Field<Integer>        COMPLETION = f("completion_score", Integer.class);
    private static final Field<Integer>        MOCK_TOTAL = f("mock_total_attempts", Integer.class);
    private static final Field<Integer>        MOCK_BEST  = f("mock_best_score_percent", Integer.class);
    private static final Field<Integer>        MOCK_AVG   = f("mock_recent3_avg_percent", Integer.class);
    private static final Field<Integer>        PRAC_SESS  = f("practice_total_sessions", Integer.class);
    private static final Field<Integer>        PRAC_ACC   = f("practice_accuracy_percent", Integer.class);
    private static final Field<Integer>        MISTAKES   = f("active_mistakes_count", Integer.class);
    private static final Field<OffsetDateTime> CREATED_AT = f("created_at", OffsetDateTime.class);

    private static <X> Field<X> f(String col, Class<X> type) {
        return DSL.field(DSL.name("progress_snapshots", col), type);
    }

    private final DSLContext dsl;

    public ProgressSnapshotRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** Inserts a snapshot (id + created_at filled by the DB) and returns the full row. */
    public SnapshotRow insert(Long userId, Long examId, int readiness, int completion,
                              int mockTotal, Integer mockBest, Integer mockAvg,
                              int pracSessions, int pracAcc, int mistakes) {
        Record r = dsl.insertInto(T)
                .set(USER_ID,    userId)
                .set(EXAM_ID,    examId)
                .set(READINESS,  readiness)
                .set(COMPLETION, completion)
                .set(MOCK_TOTAL, mockTotal)
                .set(MOCK_BEST,  mockBest)
                .set(MOCK_AVG,   mockAvg)
                .set(PRAC_SESS,  pracSessions)
                .set(PRAC_ACC,   pracAcc)
                .set(MISTAKES,   mistakes)
                .returning(ID, USER_ID, EXAM_ID, READINESS, COMPLETION, MOCK_TOTAL,
                        MOCK_BEST, MOCK_AVG, PRAC_SESS, PRAC_ACC, MISTAKES, CREATED_AT)
                .fetchOne();
        return map(r);
    }

    /** The user's snapshots for one exam, newest first. */
    public List<SnapshotRow> findRecent(Long userId, Long examId, int limit) {
        return dsl.select(ID, USER_ID, EXAM_ID, READINESS, COMPLETION, MOCK_TOTAL,
                        MOCK_BEST, MOCK_AVG, PRAC_SESS, PRAC_ACC, MISTAKES, CREATED_AT)
                .from(T)
                .where(USER_ID.eq(userId).and(EXAM_ID.eq(examId)))
                .orderBy(CREATED_AT.desc(), ID.desc())
                .limit(limit)
                .fetch(ProgressSnapshotRepository::map);
    }

    private static SnapshotRow map(Record r) {
        return new SnapshotRow(
                r.get(ID), r.get(USER_ID), r.get(EXAM_ID),
                r.get(READINESS), r.get(COMPLETION),
                r.get(MOCK_TOTAL), r.get(MOCK_BEST), r.get(MOCK_AVG),
                r.get(PRAC_SESS), r.get(PRAC_ACC), r.get(MISTAKES),
                r.get(CREATED_AT));
    }

    public record SnapshotRow(
            Long           id,
            Long           userId,
            Long           examId,
            int            readinessScore,
            int            completionScore,
            int            mockTotalAttempts,
            Integer        mockBestScorePercent,
            Integer        mockRecent3AvgPercent,
            int            practiceTotalSessions,
            int            practiceAccuracyPercent,
            int            activeMistakesCount,
            OffsetDateTime createdAt
    ) {}
}
