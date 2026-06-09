package com.dmvmotor.api.practice.infrastructure;

import com.dmvmotor.api.content.domain.Choice;
import com.dmvmotor.api.content.domain.QuestionDetail;
import com.dmvmotor.api.content.infrastructure.QuestionRepository;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Picks the next question for a practice session. Extracted from
 * {@link PracticeSessionRepository} (dev-audit #3) so the intricate
 * personalization query lives on its own, separate from plain session/attempt
 * CRUD. Pure read access — no writes, no session-lifecycle concerns.
 */
@Repository
public class PracticeQuestionSelector {

    // V26 questions.exam_id, qualified so it never collides with topics.exam_id
    // (the main query joins both). Dynamic ref — no jOOQ regen.
    private static final Field<Long> Q_EXAM_ID =
            DSL.field(DSL.name("questions", "exam_id"), Long.class);

    private final DSLContext   dsl;
    private final ObjectMapper objectMapper;

    public PracticeQuestionSelector(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl          = dsl;
        this.objectMapper = objectMapper;
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
            Long userId, int learningCycle, Long examId) {
        return findNextUnansweredQuestion(sessionId, languageCode, entryType,
                userId, learningCycle, examId, List.of(), "random");
    }

    /**
     * {@code selectionMode} (bug4) controls ordering/pool:
     * <ul>
     *   <li>{@code weak_points} — the weighted personalization below (active
     *       mistakes first, then uncovered key topics). Paid.</li>
     *   <li>{@code random} — no weak-point weighting: just the recency penalty
     *       (don't repeat the just-served topic) + stable id. The only mode
     *       free users get, so they're not locked onto one weak topic.</li>
     *   <li>{@code review_learned} — like {@code random} but the pool is
     *       restricted to topics the user has already covered this cycle. Paid.</li>
     * </ul>
     */
    public Optional<QuestionDetail> findNextUnansweredQuestion(
            Long sessionId, String languageCode, String entryType,
            Long userId, int learningCycle, Long examId, List<Long> topicFilter,
            String selectionMode) {
        var q  = Tables.QUESTIONS;
        var qv = Tables.QUESTION_VARIANTS;
        var t  = Tables.TOPICS;
        var pa = Tables.PRACTICE_ATTEMPTS;
        var mr = Tables.MISTAKE_RECORDS;

        // -------- pool filter (unchanged contract: never widen) --------
        // Exam scope first: a session only ever serves its own exam's bank.
        var condition = q.ALLOW_IN_PRACTICE.isTrue()
                .and(q.STATUS.eq("active"))
                .and(Q_EXAM_ID.eq(examId))
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
        // Topic-scoped session (the /mistakes "Practice these" CTA): restrict
        // the pool to the chosen topics. Empty filter = no restriction.
        if (!topicFilter.isEmpty()) {
            condition = condition.and(q.PRIMARY_TOPIC_ID.in(topicFilter));
        }
        // review_learned mode (paid): only serve topics the user has already
        // covered this cycle — a consolidation round over learned material.
        if ("review_learned".equals(selectionMode) && userId != null) {
            var qcov = Tables.QUESTIONS.as("qcov");
            condition = condition.and(q.PRIMARY_TOPIC_ID.in(
                    dsl.selectDistinct(qcov.PRIMARY_TOPIC_ID)
                       .from(pa).join(qcov).on(qcov.ID.eq(pa.QUESTION_ID))
                       .where(pa.PRACTICE_SESSION_ID.in(
                               dsl.select(Tables.PRACTICE_SESSIONS.ID)
                                  .from(Tables.PRACTICE_SESSIONS)
                                  .where(Tables.PRACTICE_SESSIONS.USER_ID.eq(userId)
                                          .and(Tables.PRACTICE_SESSIONS.LEARNING_CYCLE.eq(learningCycle)))))
            ));
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
        // Mode-aware ordering. weak_points uses the full weighted key (active
        // mistakes → recency → uncovered key topics → wrong-count peak); random
        // and review_learned drop the weak-point weighting entirely so the user
        // isn't locked onto one hot topic — just the recency penalty + stable id.
        // The unused weighting Fields above aren't referenced here, so jOOQ never
        // renders them for the non-weighted modes.
        List<org.jooq.OrderField<?>> order = new ArrayList<>();
        if ("weak_points".equals(selectionMode)) {
            order.add(mistakePriority.asc());
            order.add(recencyPenalty.asc());
            order.add(keyTopicPriority.asc());
            order.add(mistakeWrongPeak.asc());
        } else {
            order.add(recencyPenalty.asc());
        }
        order.add(q.ID.asc());

        Record r = dsl.select()
                .from(q)
                .join(qv).on(qv.QUESTION_ID.eq(q.ID).and(qv.LANGUAGE_CODE.eq(languageCode)))
                .join(t).on(t.ID.eq(q.PRIMARY_TOPIC_ID))
                .where(condition)
                .orderBy(order)
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
}
