package com.dmvmotor.api.progressreadiness.infrastructure;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Single-slot progress backup (bug1). One row per (user, exam) — writes upsert
 * onto the {@code uq_progress_backups_user_exam} constraint. Dynamic jOOQ field
 * refs (V36 table, no jOOQ regen — codebase convention for post-V1 tables).
 *
 * <p>The server is the source of truth: callers compute the snapshot from
 * authoritative data and hand it here. The repository never trusts a client
 * blob — it only stores what the service computed.
 */
@Repository
public class ProgressBackupRepository {

    private static final Table<?>              T          = DSL.table(DSL.name("progress_backups"));
    private static final Field<Long>           ID         = f("id", Long.class);
    private static final Field<Long>           USER_ID    = f("user_id", Long.class);
    private static final Field<Long>           EXAM_ID    = f("exam_id", Long.class);
    private static final Field<Integer>        CYCLE      = f("learning_cycle", Integer.class);
    private static final Field<Integer>        READINESS  = f("readiness_score", Integer.class);
    private static final Field<Integer>        COMPLETION = f("completion_score", Integer.class);
    private static final Field<Integer>        MOCK_TOTAL = f("mock_total_attempts", Integer.class);
    private static final Field<Integer>        MOCK_BEST  = f("mock_best_score_percent", Integer.class);
    private static final Field<Integer>        MOCK_AVG   = f("mock_recent3_avg_percent", Integer.class);
    private static final Field<Integer>        PRAC_SESS  = f("practice_total_sessions", Integer.class);
    private static final Field<Integer>        PRAC_ACC   = f("practice_accuracy_percent", Integer.class);
    private static final Field<Integer>        MISTAKES   = f("active_mistakes_count", Integer.class);
    private static final Field<JSONB>          PAYLOAD    = f("payload", JSONB.class);
    private static final Field<String>         HASH       = f("content_hash", String.class);
    private static final Field<OffsetDateTime> CREATED_AT = f("created_at", OffsetDateTime.class);
    private static final Field<OffsetDateTime> UPDATED_AT = f("updated_at", OffsetDateTime.class);
    private static final Field<OffsetDateTime> RESTORED_AT = f("restored_at", OffsetDateTime.class);

    private static <X> Field<X> f(String col, Class<X> type) {
        return DSL.field(DSL.name("progress_backups", col), type);
    }

    private final DSLContext dsl;

    public ProgressBackupRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<BackupRow> find(Long userId, Long examId) {
        Record r = dsl.select(ID, USER_ID, EXAM_ID, CYCLE, READINESS, COMPLETION,
                        MOCK_TOTAL, MOCK_BEST, MOCK_AVG, PRAC_SESS, PRAC_ACC, MISTAKES,
                        PAYLOAD, HASH, CREATED_AT, UPDATED_AT, RESTORED_AT)
                .from(T)
                .where(USER_ID.eq(userId).and(EXAM_ID.eq(examId)))
                .fetchOne();
        return r == null ? Optional.empty() : Optional.of(map(r));
    }

    /**
     * Upserts the single backup slot for (user, exam). On conflict, overwrites
     * with the new snapshot and bumps updated_at — callers only call this when
     * the content hash actually changed, so this is never a redundant write.
     */
    public BackupRow upsert(Long userId, Long examId, int cycle, int readiness, int completion,
                            int mockTotal, Integer mockBest, Integer mockAvg,
                            int pracSessions, int pracAcc, int mistakes,
                            String payloadJson, String hash) {
        OffsetDateTime now = OffsetDateTime.now();
        Record r = dsl.insertInto(T)
                .set(USER_ID,    userId)
                .set(EXAM_ID,    examId)
                .set(CYCLE,      cycle)
                .set(READINESS,  readiness)
                .set(COMPLETION, completion)
                .set(MOCK_TOTAL, mockTotal)
                .set(MOCK_BEST,  mockBest)
                .set(MOCK_AVG,   mockAvg)
                .set(PRAC_SESS,  pracSessions)
                .set(PRAC_ACC,   pracAcc)
                .set(MISTAKES,   mistakes)
                .set(PAYLOAD,    JSONB.valueOf(payloadJson))
                .set(HASH,       hash)
                .set(UPDATED_AT, now)
                .onConflict(USER_ID, EXAM_ID)
                .doUpdate()
                .set(CYCLE,      cycle)
                .set(READINESS,  readiness)
                .set(COMPLETION, completion)
                .set(MOCK_TOTAL, mockTotal)
                .set(MOCK_BEST,  mockBest)
                .set(MOCK_AVG,   mockAvg)
                .set(PRAC_SESS,  pracSessions)
                .set(PRAC_ACC,   pracAcc)
                .set(MISTAKES,   mistakes)
                .set(PAYLOAD,    JSONB.valueOf(payloadJson))
                .set(HASH,       hash)
                .set(UPDATED_AT, now)
                .returning(ID, USER_ID, EXAM_ID, CYCLE, READINESS, COMPLETION,
                        MOCK_TOTAL, MOCK_BEST, MOCK_AVG, PRAC_SESS, PRAC_ACC, MISTAKES,
                        PAYLOAD, HASH, CREATED_AT, UPDATED_AT, RESTORED_AT)
                .fetchOne();
        return map(r);
    }

    /** Stamps restored_at = now (throttle bookkeeping for the restore endpoint). */
    public void markRestored(Long userId, Long examId) {
        dsl.update(T)
                .set(RESTORED_AT, OffsetDateTime.now())
                .where(USER_ID.eq(userId).and(EXAM_ID.eq(examId)))
                .execute();
    }

    private static BackupRow map(Record r) {
        return new BackupRow(
                r.get(ID), r.get(USER_ID), r.get(EXAM_ID), r.get(CYCLE),
                r.get(READINESS), r.get(COMPLETION),
                r.get(MOCK_TOTAL), r.get(MOCK_BEST), r.get(MOCK_AVG),
                r.get(PRAC_SESS), r.get(PRAC_ACC), r.get(MISTAKES),
                r.get(PAYLOAD) == null ? null : r.get(PAYLOAD).data(),
                r.get(HASH), r.get(CREATED_AT), r.get(UPDATED_AT), r.get(RESTORED_AT));
    }

    public record BackupRow(
            Long           id,
            Long           userId,
            Long           examId,
            int            learningCycle,
            int            readinessScore,
            int            completionScore,
            int            mockTotalAttempts,
            Integer        mockBestScorePercent,
            Integer        mockRecent3AvgPercent,
            int            practiceTotalSessions,
            int            practiceAccuracyPercent,
            int            activeMistakesCount,
            String         payloadJson,
            String         contentHash,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime restoredAt
    ) {}
}
