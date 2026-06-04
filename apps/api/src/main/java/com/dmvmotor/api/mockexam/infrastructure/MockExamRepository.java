package com.dmvmotor.api.mockexam.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MockExamRepository {

    // V26 exam_id (dynamic ref, no jOOQ regen). Unqualified — each query that
    // uses it has a single exam_id-bearing table in scope.
    private static final Field<Long> EXAM_ID = DSL.field(DSL.name("exam_id"), Long.class);

    private final DSLContext dsl;

    public MockExamRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    // ---------------------------------------------------------------
    // Mock Exam Template
    // ---------------------------------------------------------------

    /** The active mock-exam template for a given exam (its own bank). */
    public Optional<Long> findActiveMockExamId(Long examId) {
        var me = Tables.MOCK_EXAMS;
        Record r = dsl.selectFrom(me)
                .where(me.STATUS.eq("active").and(EXAM_ID.eq(examId)))
                .orderBy(me.ID.asc())
                .limit(1)
                .fetchOne();
        return r == null ? Optional.empty() : Optional.of(r.get(me.ID));
    }

    /**
     * The exam's pass threshold (percent), resolved through the mock template's
     * parent exam. Falls back to 85 (the historical default) if unset.
     */
    public int findPassThresholdPercent(Long mockExamId) {
        var me = Tables.MOCK_EXAMS;
        var exams = DSL.table(DSL.name("exams"));
        Field<Long>    meExamId  = DSL.field(DSL.name("mock_exams", "exam_id"), Long.class);
        Field<Long>    examsId   = DSL.field(DSL.name("exams", "id"), Long.class);
        Field<Integer> threshold = DSL.field(DSL.name("exams", "pass_threshold_percent"), Integer.class);
        Integer v = dsl.select(threshold)
                .from(me)
                .join(exams).on(meExamId.eq(examsId))
                .where(me.ID.eq(mockExamId))
                .fetchOne(0, Integer.class);
        return java.util.Optional.ofNullable(v).orElse(85);  // default mirrors the old constant
    }

    /**
     * Human label of the mock's parent exam in the given language (name_zh for
     * "zh", else name_en) — e.g. "California Class C (Car)" — so the AI review-plan
     * prompt is exam-aware. Null if the mock or exam is missing.
     */
    public String findExamLabel(Long mockExamId, String language) {
        var me = Tables.MOCK_EXAMS;
        var exams = DSL.table(DSL.name("exams"));
        Field<Long>   meExamId = DSL.field(DSL.name("mock_exams", "exam_id"), Long.class);
        Field<Long>   examsId  = DSL.field(DSL.name("exams", "id"), Long.class);
        Field<String> name     = DSL.field(DSL.name("exams",
                "zh".equalsIgnoreCase(language) ? "name_zh" : "name_en"), String.class);
        return dsl.select(name)
                .from(me)
                .join(exams).on(meExamId.eq(examsId))
                .where(me.ID.eq(mockExamId))
                .fetchOne(0, String.class);
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

    public Long createAttempt(Long userId, Long mockExamId, String language,
                              int learningCycle, Long examId) {
        var ma = Tables.MOCK_ATTEMPTS;
        return dsl.insertInto(ma)
                .set(ma.USER_ID, userId)
                .set(ma.MOCK_EXAM_ID, mockExamId)
                .set(ma.LANGUAGE_CODE, language)
                .set(ma.LEARNING_CYCLE, learningCycle)
                .set(EXAM_ID, examId)
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
                r.get(ma.LEARNING_CYCLE),
                r.get(ma.SCORE_PERCENT),
                r.get(ma.CORRECT_COUNT),
                r.get(ma.WRONG_COUNT),
                r.get(ma.STARTED_AT)
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

    /** Cached AI review plan for an attempt, or empty if not yet generated. */
    public Optional<String> findReviewPlan(Long attemptId) {
        var ma = Tables.MOCK_ATTEMPTS;
        Record r = dsl.select(ma.AI_REVIEW_PLAN)
                .from(ma)
                .where(ma.ID.eq(attemptId))
                .fetchOne();
        if (r == null) return Optional.empty();
        String plan = r.get(ma.AI_REVIEW_PLAN);
        return plan == null || plan.isBlank() ? Optional.empty() : Optional.of(plan);
    }

    public void saveReviewPlan(Long attemptId, String plan, String model) {
        var ma = Tables.MOCK_ATTEMPTS;
        dsl.update(ma)
                .set(ma.AI_REVIEW_PLAN, plan)
                .set(ma.AI_REVIEW_PLAN_MODEL, model)
                .where(ma.ID.eq(attemptId))
                .execute();
    }

    /**
     * Wrong-answer detail for the review-plan prompt: each wrong answer joined
     * to its question's stem + topic + sub-topic in the requested language.
     */
    public List<WrongAnswerDetail> findWrongAnswerDetails(Long attemptId, String language) {
        var mar = Tables.MOCK_ATTEMPT_RESULTS;
        var q   = Tables.QUESTIONS;
        var qv  = Tables.QUESTION_VARIANTS;
        var t   = Tables.TOPICS;
        var st  = Tables.SUB_TOPICS;
        return dsl.select(qv.STEM_TEXT, t.NAME_EN, st.NAME_EN,
                          mar.SELECTED_CHOICE_KEY, q.CORRECT_CHOICE_KEY)
                .from(mar)
                .join(q).on(q.ID.eq(mar.QUESTION_ID))
                .join(qv).on(qv.QUESTION_ID.eq(q.ID).and(qv.LANGUAGE_CODE.eq(language)))
                .join(t).on(t.ID.eq(q.PRIMARY_TOPIC_ID))
                .leftJoin(st).on(st.ID.eq(q.SUB_TOPIC_ID))
                .where(mar.MOCK_ATTEMPT_ID.eq(attemptId).and(mar.IS_CORRECT.isFalse()))
                .fetch(r -> new WrongAnswerDetail(
                        r.get(qv.STEM_TEXT),
                        r.get(t.NAME_EN),
                        r.get(st.NAME_EN),
                        r.get(mar.SELECTED_CHOICE_KEY),
                        r.get(q.CORRECT_CHOICE_KEY)));
    }

    public record WrongAnswerDetail(
            String stem,
            String topicLabel,
            String subTopicLabel,
            String selectedChoiceKey,
            String correctChoiceKey
    ) {}

    /** Flip attempt to a terminal status with a computed score + summary +
     *  how long it took. */
    public void finalizeAttempt(Long attemptId, String status,
                                 int scorePercent, int correctCount, int wrongCount,
                                 int timeUsedSeconds) {
        var ma = Tables.MOCK_ATTEMPTS;
        dsl.update(ma)
                .set(ma.STATUS, status)
                .set(ma.SCORE_PERCENT, scorePercent)
                .set(ma.CORRECT_COUNT, correctCount)
                .set(ma.WRONG_COUNT, wrongCount)
                .set(ma.SUBMITTED_AT, OffsetDateTime.now())
                // time_used_seconds is a V21 column not in the generated schema;
                // reference it dynamically (same pattern as allow_in_free_trial).
                .set(org.jooq.impl.DSL.field(org.jooq.impl.DSL.name("time_used_seconds"),
                        Integer.class), timeUsedSeconds)
                .where(ma.ID.eq(attemptId))
                .execute();
    }

    /** Per-exam countdown length (V21 column, referenced dynamically). */
    public int findTimeLimitSeconds(Long mockExamId) {
        var me = Tables.MOCK_EXAMS;
        Integer v = dsl.select(
                        org.jooq.impl.DSL.field(
                                org.jooq.impl.DSL.name("time_limit_seconds"), Integer.class))
                .from(me)
                .where(me.ID.eq(mockExamId))
                .fetchOne(0, Integer.class);
        return v == null ? 1800 : v;
    }

    /** Persisted time-used for a finished attempt (V21 column). */
    public Integer findTimeUsedSeconds(Long attemptId) {
        var ma = Tables.MOCK_ATTEMPTS;
        return dsl.select(
                        org.jooq.impl.DSL.field(
                                org.jooq.impl.DSL.name("time_used_seconds"), Integer.class))
                .from(ma)
                .where(ma.ID.eq(attemptId))
                .fetchOne(0, Integer.class);
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

    /** Per-answered-question review detail (correct key + correctness +
     *  explanation in the requested language). Used by the post-exam review. */
    public List<AnswerDetail> findAnswerDetailsByAttemptId(Long attemptId, String language) {
        var mar = Tables.MOCK_ATTEMPT_RESULTS;
        var q   = Tables.QUESTIONS;
        var qv  = Tables.QUESTION_VARIANTS;
        return dsl.select(mar.QUESTION_ID, mar.SELECTED_CHOICE_KEY, mar.IS_CORRECT,
                        q.CORRECT_CHOICE_KEY, qv.EXPLANATION_TEXT)
                .from(mar)
                .join(q).on(q.ID.eq(mar.QUESTION_ID))
                .leftJoin(qv).on(qv.QUESTION_ID.eq(q.ID).and(qv.LANGUAGE_CODE.eq(language)))
                .where(mar.MOCK_ATTEMPT_ID.eq(attemptId))
                .orderBy(mar.ID.asc())
                .fetch()
                .map(r -> new AnswerDetail(
                        r.get(mar.QUESTION_ID),
                        r.get(mar.SELECTED_CHOICE_KEY),
                        r.get(q.CORRECT_CHOICE_KEY),
                        r.get(mar.IS_CORRECT),
                        r.get(qv.EXPLANATION_TEXT)
                ));
    }

    public record AnswerDetail(
            Long    questionId,
            String  selectedKey,
            String  correctKey,
            Boolean isCorrect,
            String  explanation
    ) {}

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
            int     learningCycle,
            Integer scorePercent,
            Integer correctCount,
            Integer wrongCount,
            OffsetDateTime startedAt
    ) {}

    public record AnswerRow(Long questionId, String selectedKey) {}

    public record WeakTopicRow(Long topicId, String label) {}

    // Study Hub history + stats reads moved to MockHistoryDao (dev-audit #3).
}
