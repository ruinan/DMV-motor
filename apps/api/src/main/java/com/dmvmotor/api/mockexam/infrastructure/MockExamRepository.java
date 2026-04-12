package com.dmvmotor.api.mockexam.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

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

    // ---------------------------------------------------------------
    // Mock Attempts
    // ---------------------------------------------------------------

    public Long createAttempt(Long userId, Long mockExamId, String language) {
        var ma = Tables.MOCK_ATTEMPTS;
        return dsl.insertInto(ma)
                .set(ma.USER_ID, userId)
                .set(ma.MOCK_EXAM_ID, mockExamId)
                .set(ma.LANGUAGE_CODE, language)
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
                r.get(ma.QUOTA_CONSUMED)
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
     * Upsert: overwrite if same question answered again (retry allowed).
     */
    public boolean upsertAnswer(Long attemptId, Long questionId, Long variantId,
                                  String selectedKey) {
        var mar = Tables.MOCK_ATTEMPT_RESULTS;
        boolean exists = dsl.fetchExists(mar,
                mar.MOCK_ATTEMPT_ID.eq(attemptId).and(mar.QUESTION_ID.eq(questionId)));

        if (exists) {
            dsl.update(mar)
                    .set(mar.QUESTION_VARIANT_ID, variantId)
                    .set(mar.SELECTED_CHOICE_KEY, selectedKey)
                    .where(mar.MOCK_ATTEMPT_ID.eq(attemptId).and(mar.QUESTION_ID.eq(questionId)))
                    .execute();
            return false; // existing updated
        } else {
            dsl.insertInto(mar)
                    .set(mar.MOCK_ATTEMPT_ID, attemptId)
                    .set(mar.QUESTION_ID, questionId)
                    .set(mar.QUESTION_VARIANT_ID, variantId)
                    .set(mar.SELECTED_CHOICE_KEY, selectedKey)
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

    public void consumeMockQuota(Long userId) {
        var ap = Tables.ACCESS_PASSES;
        dsl.update(ap)
                .set(ap.MOCK_EXAM_USED_COUNT, ap.MOCK_EXAM_USED_COUNT.add(1))
                .where(ap.USER_ID.eq(userId).and(ap.STATUS.eq("active")))
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
            boolean quotaConsumed
    ) {}

    public record AnswerRow(Long questionId, String selectedKey) {}

    public record WeakTopicRow(Long topicId, String label) {}
}
