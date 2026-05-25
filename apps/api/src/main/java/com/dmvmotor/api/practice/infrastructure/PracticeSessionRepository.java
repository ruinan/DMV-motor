package com.dmvmotor.api.practice.infrastructure;

import com.dmvmotor.api.content.domain.Choice;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.content.infrastructure.QuestionRepository;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import com.dmvmotor.api.practice.domain.PracticeSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PracticeSessionRepository {

    private final DSLContext    dsl;
    private final ObjectMapper  objectMapper;

    public PracticeSessionRepository(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl          = dsl;
        this.objectMapper = objectMapper;
    }

    public Long create(Long userId, String entryType, String languageCode, int learningCycle) {
        var ps = Tables.PRACTICE_SESSIONS;
        return dsl.insertInto(ps)
                .set(ps.USER_ID,        userId)
                .set(ps.ENTRY_TYPE,     entryType)
                .set(ps.LANGUAGE_CODE,  languageCode)
                .set(ps.LEARNING_CYCLE, learningCycle)
                .returningResult(ps.ID)
                .fetchOne()
                .value1();
    }

    public Optional<PracticeSession> findById(Long sessionId) {
        var ps = Tables.PRACTICE_SESSIONS;
        Record r = dsl.selectFrom(ps).where(ps.ID.eq(sessionId)).fetchOne();
        if (r == null) return Optional.empty();
        return Optional.of(map(r));
    }

    /**
     * Returns the next unanswered question for this session, or empty if all
     * answered. The pool is filtered first (active + allow_in_practice, plus
     * allow_in_free_trial for free_trial sessions); only inside that pool is
     * the order personalized.
     *
     * <p>Personalization (docs/review-and-readiness-engine.md
     * "按用户薄弱点投放") ranks the remaining questions by a composite key:
     *
     * <ol>
     *   <li>Priority 0 — the question's topic has an active MistakeRecord for
     *       this user in the current learning cycle. Within this bucket,
     *       topics with a higher peak {@code wrong_count} surface first.</li>
     *   <li>Priority 1 — the topic is a key topic ({@code topics.is_key_topic
     *       = true}) and the user has not yet answered a question in that
     *       topic in the current learning cycle.</li>
     *   <li>Priority 2 — everything else.</li>
     * </ol>
     *
     * <p>Recency penalty: if the topic of the question matches the topic of
     * one of the user's last 2 answers in <em>this session</em>, it is pushed
     * to the back of its priority bucket. This prevents the user from being
     * locked into a single hot topic when several are equally salient.
     *
     * <p>Anonymous sessions ({@code userId == null}) keep the same shape but
     * have no mistake records and a per-session-only coverage view —
     * effectively the recency penalty and key-topic coverage carry the
     * personalization weight.
     */
    public Optional<QuestionDetail> findNextUnansweredQuestion(
            Long sessionId, String languageCode, String entryType,
            Long userId, int learningCycle) {
        var q  = Tables.QUESTIONS;
        var qv = Tables.QUESTION_VARIANTS;
        var t  = Tables.TOPICS;
        var pa = Tables.PRACTICE_ATTEMPTS;
        var mr = Tables.MISTAKE_RECORDS;

        // -------- pool filter (unchanged contract: never widen) --------
        var condition = q.ALLOW_IN_PRACTICE.isTrue()
                .and(q.STATUS.eq("active"))
                .and(q.ID.notIn(
                        dsl.select(pa.QUESTION_ID)
                           .from(pa)
                           .where(pa.PRACTICE_SESSION_ID.eq(sessionId))
                ));
        if ("free_trial".equals(entryType)) {
            condition = condition.and(
                    DSL.field(DSL.name("allow_in_free_trial"),
                            Boolean.class).isTrue());
        }

        // -------- priority 0: active mistake on this topic --------
        // Anonymous sessions have no mistake records, so this collapses to
        // "always false" naturally (the subquery yields no rows).
        Field<Integer> mistakePriority;
        Field<Integer> mistakeWrongPeak;
        if (userId != null) {
            mistakePriority = DSL.when(
                    DSL.exists(
                            dsl.selectOne().from(mr)
                                    .where(mr.USER_ID.eq(userId))
                                    .and(mr.IS_ACTIVE.isTrue())
                                    .and(mr.LEARNING_CYCLE.eq(learningCycle))
                                    .and(mr.PRIMARY_TOPIC_ID.eq(q.PRIMARY_TOPIC_ID))
                    ),
                    DSL.inline(0)
            ).otherwise(DSL.inline(99));
            // Negated so that higher wrong_count produces a smaller (better) sort key.
            Field<Integer> peakSubquery = DSL.select(DSL.max(mr.WRONG_COUNT).neg())
                    .from(mr)
                    .where(mr.USER_ID.eq(userId))
                    .and(mr.IS_ACTIVE.isTrue())
                    .and(mr.LEARNING_CYCLE.eq(learningCycle))
                    .and(mr.PRIMARY_TOPIC_ID.eq(q.PRIMARY_TOPIC_ID))
                    .asField();
            mistakeWrongPeak = DSL.coalesce(peakSubquery, DSL.inline(0));
        } else {
            // Anonymous: no MistakeRecord data. A bare DSL.inline(0) here
            // would render as an integer literal in ORDER BY, which Postgres
            // interprets as a column ordinal — wrap in a CASE so jOOQ emits a
            // proper expression that doesn't collide with that syntax.
            mistakePriority  = DSL.when(DSL.trueCondition(), DSL.inline(99))
                                  .otherwise(DSL.inline(99));
            mistakeWrongPeak = DSL.when(DSL.trueCondition(), DSL.inline(0))
                                  .otherwise(DSL.inline(0));
        }

        // -------- priority 1: uncovered key topic --------
        // "Covered" = the user has at least one practice_attempt on that topic
        // in the current learning_cycle (across sessions for owned users; for
        // anonymous sessions the same-session view is the best proxy — same
        // intent applies: don't keep serving a topic the user has already
        // touched).
        var qa = Tables.QUESTIONS.as("qa");
        org.jooq.Condition coverageMatch = qa.PRIMARY_TOPIC_ID.eq(q.PRIMARY_TOPIC_ID);
        org.jooq.SelectConditionStep<?> coverageScopedAttempts;
        if (userId != null) {
            coverageScopedAttempts = dsl.selectOne()
                    .from(pa).join(qa).on(qa.ID.eq(pa.QUESTION_ID))
                    .where(pa.USER_ID.eq(userId))
                    .and(coverageMatch)
                    .and(pa.PRACTICE_SESSION_ID.in(
                            dsl.select(Tables.PRACTICE_SESSIONS.ID)
                                    .from(Tables.PRACTICE_SESSIONS)
                                    .where(Tables.PRACTICE_SESSIONS.USER_ID.eq(userId))
                                    .and(Tables.PRACTICE_SESSIONS.LEARNING_CYCLE.eq(learningCycle))
                    ));
        } else {
            coverageScopedAttempts = dsl.selectOne()
                    .from(pa).join(qa).on(qa.ID.eq(pa.QUESTION_ID))
                    .where(pa.PRACTICE_SESSION_ID.eq(sessionId))
                    .and(coverageMatch);
        }
        Field<Integer> keyTopicPriority = DSL.when(
                t.IS_KEY_TOPIC.isTrue().and(DSL.notExists(coverageScopedAttempts)),
                DSL.inline(0)
        ).otherwise(DSL.inline(1));

        // -------- recency penalty: last 2 answered topics in this session --------
        // A question whose topic is in that window scores 1 (worse); otherwise 0.
        var qb = Tables.QUESTIONS.as("qb");
        var recentTopicIds = dsl
                .select(qb.PRIMARY_TOPIC_ID)
                .from(pa).join(qb).on(qb.ID.eq(pa.QUESTION_ID))
                .where(pa.PRACTICE_SESSION_ID.eq(sessionId))
                .orderBy(pa.SUBMITTED_AT.desc(), pa.ID.desc())
                .limit(2);
        Field<Integer> recencyPenalty = DSL.when(
                q.PRIMARY_TOPIC_ID.in(recentTopicIds),
                DSL.inline(1)
        ).otherwise(DSL.inline(0));

        // -------- final selection --------
        // Composite ORDER BY:
        //   1. mistakePriority     (0 = active mistake topic, else 99)
        //   2. recencyPenalty      (0 = not just-served, 1 = just-served)
        //   3. keyTopicPriority    (0 = uncovered key topic, else 1)
        //   4. mistakeWrongPeak    (smaller = bigger -wrong_count = bigger wrong_count)
        //   5. q.id                (stable tiebreaker)
        // Recency is placed above keyTopicPriority so the user is never served
        // the same topic twice in a row even if that topic is the only key
        // gap — the next-best topic gets a turn first.
        Record r = dsl.select()
                .from(q)
                .join(qv).on(qv.QUESTION_ID.eq(q.ID).and(qv.LANGUAGE_CODE.eq(languageCode)))
                .join(t).on(t.ID.eq(q.PRIMARY_TOPIC_ID))
                .where(condition)
                .orderBy(
                        mistakePriority.asc(),
                        recencyPenalty.asc(),
                        keyTopicPriority.asc(),
                        mistakeWrongPeak.asc(),
                        q.ID.asc()
                )
                .limit(1)
                .fetchOne();

        if (r == null) return Optional.empty();

        List<Choice> choices = QuestionRepository.parseChoices(objectMapper,
                r.get(qv.CHOICES_PAYLOAD).data());

        return Optional.of(new QuestionDetail(
                r.get(q.ID),
                r.get(qv.ID),
                r.get(q.PRIMARY_TOPIC_ID),
                r.get(q.CORRECT_CHOICE_KEY),
                languageCode,
                r.get(qv.STEM_TEXT),
                choices,
                r.get(qv.EXPLANATION_TEXT)
        ));
    }

    public int countAnswered(Long sessionId) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        return dsl.fetchCount(pa, pa.PRACTICE_SESSION_ID.eq(sessionId));
    }

    public int countTotal(String languageCode, String entryType) {
        var q  = Tables.QUESTIONS;
        var qv = Tables.QUESTION_VARIANTS;

        var condition = q.ALLOW_IN_PRACTICE.isTrue().and(q.STATUS.eq("active"));
        if ("free_trial".equals(entryType)) {
            condition = condition.and(
                    org.jooq.impl.DSL.field(
                            org.jooq.impl.DSL.name("allow_in_free_trial"),
                            Boolean.class).isTrue());
        }

        return dsl.fetchCount(
                dsl.select().from(q)
                   .join(qv).on(qv.QUESTION_ID.eq(q.ID).and(qv.LANGUAGE_CODE.eq(languageCode)))
                   .where(condition)
        );
    }

    public boolean hasAttempt(Long sessionId, Long questionId) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        return dsl.fetchExists(pa,
                pa.PRACTICE_SESSION_ID.eq(sessionId).and(pa.QUESTION_ID.eq(questionId)));
    }

    /**
     * Whether the question is part of this session's pool.
     * Mirrors the filter in {@link #findNextUnansweredQuestion}: active + allow_in_practice,
     * plus allow_in_free_trial when the session is free_trial.
     */
    public boolean existsInSessionPool(Long questionId, String entryType) {
        var q = Tables.QUESTIONS;
        var condition = q.ID.eq(questionId)
                .and(q.STATUS.eq("active"))
                .and(q.ALLOW_IN_PRACTICE.isTrue());
        if ("free_trial".equals(entryType)) {
            condition = condition.and(
                    org.jooq.impl.DSL.field(
                            org.jooq.impl.DSL.name("allow_in_free_trial"),
                            Boolean.class).isTrue());
        }
        return dsl.fetchExists(q, condition);
    }

    public void saveAttempt(Long sessionId, Long userId, Long questionId,
                             Long variantId, String selectedKey, boolean isCorrect) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        dsl.insertInto(pa)
                .set(pa.PRACTICE_SESSION_ID,   sessionId)
                .set(pa.USER_ID,               userId)
                .set(pa.QUESTION_ID,           questionId)
                .set(pa.QUESTION_VARIANT_ID,   variantId)
                .set(pa.SELECTED_CHOICE_KEY,   selectedKey)
                .set(pa.IS_CORRECT,            isCorrect)
                .execute();
    }

    public boolean existsInProgressByUserId(Long userId, int learningCycle) {
        var ps = Tables.PRACTICE_SESSIONS;
        return dsl.fetchExists(ps,
                ps.USER_ID.eq(userId)
                        .and(ps.STATUS.eq("in_progress"))
                        .and(ps.LEARNING_CYCLE.eq(learningCycle)));
    }

    /**
     * Finds the user's most-recently-active in-progress practice session
     * (within the current learning cycle) along with attempts answered so
     * far. Used by /me to power the Study Hub Resume CTA.
     */
    public Optional<InProgressSession> findInProgressByUser(Long userId, int learningCycle) {
        var ps = Tables.PRACTICE_SESSIONS;
        var pa = Tables.PRACTICE_ATTEMPTS;
        Field<Integer> answered = DSL.count(pa.ID).as("answered");
        Record r = dsl.select(ps.ID, ps.ENTRY_TYPE, ps.LANGUAGE_CODE,
                              ps.LAST_ACTIVE_AT, answered)
                .from(ps)
                .leftJoin(pa).on(pa.PRACTICE_SESSION_ID.eq(ps.ID))
                .where(ps.USER_ID.eq(userId)
                        .and(ps.STATUS.eq("in_progress"))
                        .and(ps.LEARNING_CYCLE.eq(learningCycle)))
                .groupBy(ps.ID)
                .orderBy(ps.LAST_ACTIVE_AT.desc(), ps.ID.desc())
                .limit(1)
                .fetchOne();
        if (r == null) return Optional.empty();
        int answeredCount = r.get(answered);
        int total = countTotal(r.get(ps.LANGUAGE_CODE), r.get(ps.ENTRY_TYPE));
        return Optional.of(new InProgressSession(
                r.get(ps.ID),
                r.get(ps.ENTRY_TYPE),
                r.get(ps.LANGUAGE_CODE),
                answeredCount,
                total,
                r.get(ps.LAST_ACTIVE_AT)
        ));
    }

    public record InProgressSession(
            Long           sessionId,
            String         entryType,
            String         language,
            int            answeredCount,
            int            totalCount,
            OffsetDateTime lastActivityAt
    ) {}

    public void updateStatus(Long sessionId, String status) {
        var ps = Tables.PRACTICE_SESSIONS;
        dsl.update(ps)
                .set(ps.STATUS, status)
                .where(ps.ID.eq(sessionId))
                .execute();
    }

    /**
     * Past attempts in this session, joined with the question's stem +
     * choices + explanation in the requested display language.
     * Order: chronological (earliest submission first).
     */
    public List<AttemptDetail> findAttemptsBySessionId(Long sessionId, String languageCode) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        var q  = Tables.QUESTIONS;
        var qv = Tables.QUESTION_VARIANTS;

        return dsl.select(
                        pa.QUESTION_ID,
                        pa.QUESTION_VARIANT_ID,
                        q.PRIMARY_TOPIC_ID,
                        q.CORRECT_CHOICE_KEY,
                        pa.SELECTED_CHOICE_KEY,
                        pa.IS_CORRECT,
                        pa.SUBMITTED_AT,
                        qv.STEM_TEXT,
                        qv.CHOICES_PAYLOAD,
                        qv.EXPLANATION_TEXT)
                .from(pa)
                .join(q).on(q.ID.eq(pa.QUESTION_ID))
                .join(qv).on(qv.QUESTION_ID.eq(pa.QUESTION_ID)
                        .and(qv.LANGUAGE_CODE.eq(languageCode)))
                .where(pa.PRACTICE_SESSION_ID.eq(sessionId))
                .orderBy(pa.SUBMITTED_AT.asc(), pa.ID.asc())
                .fetch()
                .map(r -> new AttemptDetail(
                        r.get(pa.QUESTION_ID),
                        r.get(pa.QUESTION_VARIANT_ID),
                        r.get(q.PRIMARY_TOPIC_ID),
                        languageCode,
                        r.get(qv.STEM_TEXT),
                        QuestionRepository.parseChoices(objectMapper,
                                r.get(qv.CHOICES_PAYLOAD).data()),
                        r.get(q.CORRECT_CHOICE_KEY),
                        r.get(pa.SELECTED_CHOICE_KEY),
                        r.get(qv.EXPLANATION_TEXT),
                        r.get(pa.IS_CORRECT),
                        r.get(pa.SUBMITTED_AT)));
    }

    public record AttemptDetail(
            Long           questionId,
            Long           variantId,
            Long           topicId,
            String         language,
            String         stem,
            List<Choice>   choices,
            String         correctChoiceKey,
            String         selectedChoiceKey,
            String         explanation,
            boolean        isCorrect,
            OffsetDateTime submittedAt
    ) {}

    private PracticeSession map(Record r) {
        var ps = Tables.PRACTICE_SESSIONS;
        return new PracticeSession(
                r.get(ps.ID),
                r.get(ps.USER_ID),
                r.get(ps.STATUS),
                r.get(ps.ENTRY_TYPE),
                r.get(ps.LANGUAGE_CODE),
                r.get(ps.STARTED_AT),
                r.get(ps.COMPLETED_AT)
        );
    }

    // ===== Study Hub history + stats =====

    public List<SessionHistoryRow> findRecentByUserWithStats(Long userId, int limit) {
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
                .where(ps.USER_ID.eq(userId))
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

    public int countByUser(Long userId) {
        var ps = Tables.PRACTICE_SESSIONS;
        // selectCount + fetchOne never returns null for an aggregate.
        return dsl.selectCount().from(ps)
                .where(ps.USER_ID.eq(userId))
                .fetchOne(0, Integer.class);
    }

    public AttemptTotals attemptTotals(Long userId) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        Field<Integer> total = DSL.count(pa.ID);
        // SUM over zero rows = SQL NULL, so coalesce to 0 here to keep the
        // Java contract integer-typed.
        Field<Integer> correct = DSL.coalesce(
                DSL.sum(DSL.when(pa.IS_CORRECT.isTrue(), 1).otherwise(0)),
                0).cast(Integer.class);
        var record = dsl.select(total, correct)
                .from(pa)
                .where(pa.USER_ID.eq(userId))
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
