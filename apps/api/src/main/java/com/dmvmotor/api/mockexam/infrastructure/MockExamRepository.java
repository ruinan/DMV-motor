package com.dmvmotor.api.mockexam.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MockExamRepository {

    private final DSLContext dsl;

    public MockExamRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    // ---------------------------------------------------------------
    // Mock Exam Template
    // ---------------------------------------------------------------

    public Optional<Long> findActiveMockExamId() {
        var me = Tables.MOCK_EXAMS;
        Record r = dsl.selectFrom(me)
                .where(me.STATUS.eq("active"))
                .orderBy(me.ID.asc())
                .limit(1)
                .fetchOne();
        return r == null ? Optional.empty() : Optional.of(r.get(me.ID));
    }

    public List<Long> findQuestionIdsByMockExamId(Long mockExamId) {
        var meq = Tables.MOCK_EXAM_QUESTIONS;
        return dsl.select(meq.QUESTION_ID)
                .from(meq)
                .where(meq.MOCK_EXAM_ID.eq(mockExamId))
                .orderBy(meq.SORT_ORDER.asc())
                .fetch(meq.QUESTION_ID);
    }

    public int findMockExamQuestionCount(Long mockExamId) {
        var meq = Tables.MOCK_EXAM_QUESTIONS;
        return dsl.fetchCount(meq, meq.MOCK_EXAM_ID.eq(mockExamId));
    }

    public boolean existsInMockExam(Long mockExamId, Long questionId) {
        var meq = Tables.MOCK_EXAM_QUESTIONS;
        return dsl.fetchExists(meq,
                meq.MOCK_EXAM_ID.eq(mockExamId).and(meq.QUESTION_ID.eq(questionId)));
    }

    // ---------------------------------------------------------------
    // Mock Attempts
    // ---------------------------------------------------------------

    public Long createAttempt(Long userId, Long mockExamId, String language, int learningCycle) {
        var ma = Tables.MOCK_ATTEMPTS;
        return dsl.insertInto(ma)
                .set(ma.USER_ID, userId)
                .set(ma.MOCK_EXAM_ID, mockExamId)
                .set(ma.LANGUAGE_CODE, language)
                .set(ma.LEARNING_CYCLE, learningCycle)
                .returningResult(ma.ID)
                .fetchOne()
                .value1();
    }

    public Optional<AttemptRow> findAttemptById(Long attemptId) {
        var ma = Tables.MOCK_ATTEMPTS;
        Record r = dsl.selectFrom(ma).where(ma.ID.eq(attemptId)).fetchOne();
        if (r == null) return Optional.empty();
        return Optional.of(new AttemptRow(
                r.get(ma.ID),
                r.get(ma.USER_ID),
                r.get(ma.MOCK_EXAM_ID),
                r.get(ma.LANGUAGE_CODE),
                r.get(ma.STATUS),
                r.get(ma.ANSWERED_COUNT),
                r.get(ma.QUOTA_CONSUMED),
                r.get(ma.LEARNING_CYCLE)
        ));
    }

    public void updateAttemptStatus(Long attemptId, String status) {
        var ma = Tables.MOCK_ATTEMPTS;
        dsl.update(ma).set(ma.STATUS, status).where(ma.ID.eq(attemptId)).execute();
    }

    public void scoreAttempt(Long attemptId, int correctCount, int wrongCount, int scorePercent) {
        var ma = Tables.MOCK_ATTEMPTS;
        dsl.update(ma)
                .set(ma.STATUS, "submitted")
                .set(ma.CORRECT_COUNT, correctCount)
                .set(ma.WRONG_COUNT, wrongCount)
                .set(ma.SCORE_PERCENT, scorePercent)
                .where(ma.ID.eq(attemptId))
                .execute();
    }

    // ---------------------------------------------------------------
    // Mock Attempt Results (per-question answers)
    // ---------------------------------------------------------------

    /**
     * Upsert with immediate correctness — new mock-exam UX scores each answer
     * inline so the user sees right/wrong before advancing. Returns whether
     * this is a NEW answer (false = retry of an already-answered question).
     */
    public boolean upsertAnswer(Long attemptId, Long questionId, Long variantId,
                                  String selectedKey, boolean isCorrect) {
        var mar = Tables.MOCK_ATTEMPT_RESULTS;
        boolean exists = dsl.fetchExists(mar,
                mar.MOCK_ATTEMPT_ID.eq(attemptId).and(mar.QUESTION_ID.eq(questionId)));

        if (exists) {
            dsl.update(mar)
                    .set(mar.QUESTION_VARIANT_ID, variantId)
                    .set(mar.SELECTED_CHOICE_KEY, selectedKey)
                    .set(mar.IS_CORRECT, isCorrect)
                    .where(mar.MOCK_ATTEMPT_ID.eq(attemptId).and(mar.QUESTION_ID.eq(questionId)))
                    .execute();
            return false; // existing updated
        } else {
            dsl.insertInto(mar)
                    .set(mar.MOCK_ATTEMPT_ID, attemptId)
                    .set(mar.QUESTION_ID, questionId)
                    .set(mar.QUESTION_VARIANT_ID, variantId)
                    .set(mar.SELECTED_CHOICE_KEY, selectedKey)
                    .set(mar.IS_CORRECT, isCorrect)
                    .execute();
            // Increment answered_count on attempt
            var ma = Tables.MOCK_ATTEMPTS;
            dsl.update(ma)
                    .set(ma.ANSWERED_COUNT, ma.ANSWERED_COUNT.add(1))
                    .where(ma.ID.eq(attemptId))
                    .execute();
            return true; // new answer
        }
    }

    /** Count wrong answers (is_correct=false) for an attempt. */
    public int countWrongAnswers(Long attemptId) {
        var mar = Tables.MOCK_ATTEMPT_RESULTS;
        return dsl.selectCount()
                .from(mar)
                .where(mar.MOCK_ATTEMPT_ID.eq(attemptId).and(mar.IS_CORRECT.isFalse()))
                .fetchOne(0, Integer.class);
    }

    /** Count correct answers (is_correct=true) for an attempt. */
    public int countCorrectAnswers(Long attemptId) {
        var mar = Tables.MOCK_ATTEMPT_RESULTS;
        return dsl.selectCount()
                .from(mar)
                .where(mar.MOCK_ATTEMPT_ID.eq(attemptId).and(mar.IS_CORRECT.isTrue()))
                .fetchOne(0, Integer.class);
    }

    /** Flip attempt to a terminal status with a computed score + summary. */
    public void finalizeAttempt(Long attemptId, String status,
                                 int scorePercent, int correctCount, int wrongCount) {
        var ma = Tables.MOCK_ATTEMPTS;
        dsl.update(ma)
                .set(ma.STATUS, status)
                .set(ma.SCORE_PERCENT, scorePercent)
                .set(ma.CORRECT_COUNT, correctCount)
                .set(ma.WRONG_COUNT, wrongCount)
                .set(ma.SUBMITTED_AT, OffsetDateTime.now())
                .where(ma.ID.eq(attemptId))
                .execute();
    }

    public List<AnswerRow> findAnswersByAttemptId(Long attemptId) {
        var mar = Tables.MOCK_ATTEMPT_RESULTS;
        return dsl.selectFrom(mar)
                .where(mar.MOCK_ATTEMPT_ID.eq(attemptId))
                .fetch()
                .map(r -> new AnswerRow(
                        r.get(mar.QUESTION_ID),
                        r.get(mar.SELECTED_CHOICE_KEY)
                ));
    }

    public void markAnswerCorrectness(Long attemptId, Long questionId, boolean isCorrect) {
        var mar = Tables.MOCK_ATTEMPT_RESULTS;
        dsl.update(mar)
                .set(mar.IS_CORRECT, isCorrect)
                .where(mar.MOCK_ATTEMPT_ID.eq(attemptId).and(mar.QUESTION_ID.eq(questionId)))
                .execute();
    }

    // ---------------------------------------------------------------
    // Access Pass quota management
    // ---------------------------------------------------------------

    /**
     * Decrements quota on a specific pass row. Caller (MockExamService) must
     * resolve the currently-active pass id before calling — the previous
     * implementation matched on {@code user_id + status='active'} which
     * incremented every active row, double-counting if a user happened to
     * own more than one active pass.
     */
    public void consumeMockQuotaForPass(Long passId) {
        var ap = Tables.ACCESS_PASSES;
        dsl.update(ap)
                .set(ap.MOCK_EXAM_USED_COUNT, ap.MOCK_EXAM_USED_COUNT.add(1))
                .where(ap.ID.eq(passId))
                .execute();
    }

    // ---------------------------------------------------------------
    // Value objects
    // ---------------------------------------------------------------

    public List<WeakTopicRow> findWeakTopicsByAttemptId(Long attemptId) {
        var mar = Tables.MOCK_ATTEMPT_RESULTS;
        var q   = Tables.QUESTIONS;
        var t   = Tables.TOPICS;
        return dsl.select(t.ID, t.NAME_EN, org.jooq.impl.DSL.count())
                .from(mar)
                .join(q).on(q.ID.eq(mar.QUESTION_ID))
                .join(t).on(t.ID.eq(q.PRIMARY_TOPIC_ID))
                .where(mar.MOCK_ATTEMPT_ID.eq(attemptId).and(mar.IS_CORRECT.isFalse()))
                .groupBy(t.ID, t.NAME_EN)
                .orderBy(org.jooq.impl.DSL.count().desc())
                .limit(5)
                .fetch()
                .map(r -> new WeakTopicRow(r.get(t.ID), r.get(t.NAME_EN)));
    }

    // ---------------------------------------------------------------
    // Value objects
    // ---------------------------------------------------------------

    public record AttemptRow(
            Long    id,
            Long    userId,
            Long    mockExamId,
            String  language,
            String  status,
            int     answeredCount,
            boolean quotaConsumed,
            int     learningCycle
    ) {}

    public record AnswerRow(Long questionId, String selectedKey) {}

    public record WeakTopicRow(Long topicId, String label) {}

    // ---------------------------------------------------------------
    // Study Hub history + stats
    // ---------------------------------------------------------------

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
        // best/latest CAN legitimately be null (no submitted attempts yet).
        Integer best = dsl.select(org.jooq.impl.DSL.max(ma.SCORE_PERCENT)).from(ma)
                .where(ma.USER_ID.eq(userId).and(ma.STATUS.eq("submitted")))
                .fetchOne(0, Integer.class);
        Integer latest = dsl.select(ma.SCORE_PERCENT).from(ma)
                .where(ma.USER_ID.eq(userId).and(ma.STATUS.eq("submitted")))
                .orderBy(ma.SUBMITTED_AT.desc(), ma.ID.desc())
                .limit(1)
                .fetchOne(0, Integer.class);
        List<Integer> recent3 = dsl.select(ma.SCORE_PERCENT).from(ma)
                .where(ma.USER_ID.eq(userId).and(ma.STATUS.eq("submitted")))
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
