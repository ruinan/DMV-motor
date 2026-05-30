package com.dmvmotor.api.practice.infrastructure;

import com.dmvmotor.api.content.domain.Choice;
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
import java.util.ArrayList;
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

    /**
     * Creates a practice session. {@code topicFilter} is the (already
     * server-capped, non-null) list of topic ids to scope the pool to; pass
     * an empty list for the normal full pool.
     */
    public Long create(Long userId, String entryType, String languageCode,
                        int learningCycle, List<Long> topicFilter) {
        var ps = Tables.PRACTICE_SESSIONS;
        return dsl.insertInto(ps)
                .set(ps.USER_ID,        userId)
                .set(ps.ENTRY_TYPE,     entryType)
                .set(ps.LANGUAGE_CODE,  languageCode)
                .set(ps.LEARNING_CYCLE, learningCycle)
                .set(ps.TOPIC_FILTER,   encodeTopicFilter(topicFilter))
                .returningResult(ps.ID)
                .fetchOne()
                .value1();
    }

    /** Comma-joined topic ids, or null for "no filter" (empty list). */
    static String encodeTopicFilter(List<Long> topicFilter) {
        if (topicFilter.isEmpty()) return null;
        return topicFilter.stream().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
    }

    static List<Long> decodeTopicFilter(String raw) {
        // encodeTopicFilter only ever writes null or a non-empty CSV, so a
        // null check is the only guard needed.
        if (raw == null) return List.of();
        List<Long> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            out.add(Long.valueOf(part.trim()));
        }
        return out;
    }

    public Optional<PracticeSession> findById(Long sessionId) {
        var ps = Tables.PRACTICE_SESSIONS;
        Record r = dsl.selectFrom(ps).where(ps.ID.eq(sessionId)).fetchOne();
        if (r == null) return Optional.empty();
        return Optional.of(map(r));
    }

    // Next-question selection (the personalization query) moved to
    // PracticeQuestionSelector (dev-audit #3) — this repository now keeps only
    // session/attempt CRUD and pool sizing.

    /**
     * Max questions served per practice session, by entry type. A session is a
     * bounded study chunk; once this many are answered it completes. Paid full
     * practice gives more per round (30) than the free taste (15) — free is
     * deliberately the smaller sampler. Displayed total is min(cap, pool) so a
     * small pool (narrow topic filter) still shows its real size.
     */
    public static final int FREE_TRIAL_QUESTION_CAP = 15;
    public static final int FULL_QUESTION_CAP       = 30;

    public static int capFor(String entryType) {
        return "free_trial".equals(entryType) ? FREE_TRIAL_QUESTION_CAP : FULL_QUESTION_CAP;
    }

    public int countAnswered(Long sessionId) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        return dsl.fetchCount(pa, pa.PRACTICE_SESSION_ID.eq(sessionId));
    }

    public int countTotal(String languageCode, String entryType) {
        return countTotal(languageCode, entryType, List.of());
    }

    /**
     * Size of the session pool, optionally scoped to a topic filter. The pool
     * condition mirrors {@link #findNextUnansweredQuestion} (active +
     * allow_in_practice, plus allow_in_free_trial for free-trial, plus the
     * topic filter) so the displayed total matches what can actually be served.
     * An empty {@code topicFilter} means the full pool.
     */
    public int countTotal(String languageCode, String entryType, List<Long> topicFilter) {
        var q  = Tables.QUESTIONS;
        var qv = Tables.QUESTION_VARIANTS;

        var condition = q.ALLOW_IN_PRACTICE.isTrue().and(q.STATUS.eq("active"));
        if ("free_trial".equals(entryType)) {
            condition = condition.and(
                    org.jooq.impl.DSL.field(
                            org.jooq.impl.DSL.name("allow_in_free_trial"),
                            Boolean.class).isTrue());
        }
        if (!topicFilter.isEmpty()) {
            condition = condition.and(q.PRIMARY_TOPIC_ID.in(topicFilter));
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
                              ps.TOPIC_FILTER, ps.LAST_ACTIVE_AT, answered)
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
        // Topic-scoped sessions ("Practice these") report the filtered pool
        // size, not min(cap, full bank).
        int total = Math.min(capFor(r.get(ps.ENTRY_TYPE)),
                countTotal(r.get(ps.LANGUAGE_CODE), r.get(ps.ENTRY_TYPE),
                        decodeTopicFilter(r.get(ps.TOPIC_FILTER))));
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
                r.get(ps.COMPLETED_AT),
                decodeTopicFilter(r.get(ps.TOPIC_FILTER))
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
