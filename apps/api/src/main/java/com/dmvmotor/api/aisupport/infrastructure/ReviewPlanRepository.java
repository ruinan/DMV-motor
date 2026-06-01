package com.dmvmotor.api.aisupport.infrastructure;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Per-(attempt, language) AI review-plan cache (V25). Dynamic jOOQ field refs
 * (no regen needed). A row with {@code plan IS NULL} is a claim placeholder —
 * a generation is in flight — so concurrent reads don't fire duplicate LLM
 * calls; a non-null {@code plan} is ready.
 */
@Repository
public class ReviewPlanRepository {

    private static final Table<?>       T          = DSL.table(DSL.name("mock_review_plans"));
    private static final Field<Long>    ATTEMPT_ID = DSL.field(DSL.name("mock_attempt_id"), Long.class);
    private static final Field<String>  LANGUAGE   = DSL.field(DSL.name("language"), String.class);
    private static final Field<String>  PLAN       = DSL.field(DSL.name("plan"), String.class);
    private static final Field<String>  MODEL      = DSL.field(DSL.name("model"), String.class);

    private final DSLContext dsl;

    public ReviewPlanRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** The ready plan for this attempt+language, or empty if absent / still generating. */
    public Optional<String> findReadyPlan(Long attemptId, String language) {
        return dsl.select(PLAN)
                .from(T)
                .where(ATTEMPT_ID.eq(attemptId).and(LANGUAGE.eq(language)).and(PLAN.isNotNull()))
                .fetchOptional()
                .map(r -> r.get(PLAN));
    }

    /**
     * Stake a claim to generate this language: insert a placeholder row.
     * Returns true if THIS caller claimed it (so it should generate); false if a
     * row already existed (someone else is generating, or it's already ready).
     */
    public boolean claim(Long attemptId, String language) {
        return dsl.insertInto(T)
                .columns(ATTEMPT_ID, LANGUAGE)
                .values(DSL.val(attemptId), DSL.val(language))
                .onConflictDoNothing()
                .execute() == 1;
    }

    /** Fill in a claimed row with the generated plan. */
    public void markReady(Long attemptId, String language, String plan, String model) {
        dsl.update(T)
                .set(PLAN, plan)
                .set(MODEL, model)
                .where(ATTEMPT_ID.eq(attemptId).and(LANGUAGE.eq(language)))
                .execute();
    }

    /** Release a claim that failed to generate (plan still null) so it can retry. */
    public void releaseClaim(Long attemptId, String language) {
        dsl.deleteFrom(T)
                .where(ATTEMPT_ID.eq(attemptId).and(LANGUAGE.eq(language)).and(PLAN.isNull()))
                .execute();
    }
}
