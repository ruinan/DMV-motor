package com.dmvmotor.api.aisupport.infrastructure;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Append-only metadata log of "深入分析" (deep-dive) LLM calls — NO explanation
 * text (that lives in the client's localStorage; see V23 / memory §35).
 *
 * <p>Uses dynamic jOOQ field refs (same pattern as the V21 timer columns) so a
 * jOOQ regen isn't required for this small table. Drives two things:
 * <ul>
 *   <li>the per-(user, question, language) depth cap (anti-abuse / cost), which
 *       persists across a client cache clear so re-burning still counts;</li>
 *   <li>the daily-cap + cooldown rate-limit, combined with ai_explanations so a
 *       deep-dive is billed like any other LLM call.</li>
 * </ul>
 */
@Repository
public class AiDeepDiveLogRepository {

    private static final Table<?>        T           = DSL.table(DSL.name("ai_deep_dive_log"));
    private static final Field<Long>     USER_ID     = DSL.field(DSL.name("user_id"), Long.class);
    private static final Field<Long>     QUESTION_ID = DSL.field(DSL.name("question_id"), Long.class);
    private static final Field<String>   LANGUAGE    = DSL.field(DSL.name("language"), String.class);
    private static final Field<Integer>  DEPTH       = DSL.field(DSL.name("depth"), Integer.class);
    private static final Field<OffsetDateTime> CREATED_AT =
            DSL.field(DSL.name("created_at"), OffsetDateTime.class);

    private final DSLContext dsl;

    public AiDeepDiveLogRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** Deep-dive LLM calls by this user in the rate-limit window. */
    public int countByUserSince(Long userId, OffsetDateTime since) {
        return dsl.fetchCount(T, USER_ID.eq(userId).and(CREATED_AT.greaterOrEqual(since)));
    }

    /** Timestamp of this user's most recent deep-dive, for the cooldown gate. */
    public Optional<OffsetDateTime> findLatestCreatedAtByUser(Long userId) {
        return dsl.select(CREATED_AT)
                .from(T)
                .where(USER_ID.eq(userId))
                .orderBy(CREATED_AT.desc())
                .limit(1)
                .fetchOptional()
                .map(r -> r.get(CREATED_AT));
    }

    /** Total deep-dive calls already spent on this question — the depth cap. */
    public int countByUserQuestionLanguage(Long userId, Long questionId, String language) {
        return dsl.fetchCount(T, USER_ID.eq(userId)
                .and(QUESTION_ID.eq(questionId))
                .and(LANGUAGE.eq(language)));
    }

    public void insert(Long userId, Long questionId, String language, int depth) {
        dsl.insertInto(T)
                .columns(USER_ID, QUESTION_ID, LANGUAGE, DEPTH)
                .values(userId, questionId, language, depth)
                .execute();
    }
}
