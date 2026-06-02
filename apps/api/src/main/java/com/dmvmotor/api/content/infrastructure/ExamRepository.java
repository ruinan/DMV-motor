package com.dmvmotor.api.content.infrastructure;

import com.dmvmotor.api.content.domain.Exam;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The exam catalog (state × license type). Dynamic jOOQ field refs (the
 * codebase convention for post-V1 tables — see AiDeepDiveLogRepository), so the
 * V26 table needs no jOOQ regen.
 */
@Repository
public class ExamRepository {

    private static final Table<?>       T          = DSL.table(DSL.name("exams"));
    private static final Field<Long>    ID         = DSL.field(DSL.name("id"), Long.class);
    private static final Field<String>  STATE      = DSL.field(DSL.name("state_code"), String.class);
    private static final Field<String>  CLASS      = DSL.field(DSL.name("license_class"), String.class);
    private static final Field<String>  NAME_EN    = DSL.field(DSL.name("name_en"), String.class);
    private static final Field<String>  NAME_ZH    = DSL.field(DSL.name("name_zh"), String.class);
    private static final Field<Integer> THRESHOLD  = DSL.field(DSL.name("pass_threshold_percent"), Integer.class);
    private static final Field<String>  STATUS     = DSL.field(DSL.name("status"), String.class);
    private static final Field<Integer> SORT_ORDER = DSL.field(DSL.name("sort_order"), Integer.class);

    private final DSLContext dsl;

    public ExamRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** Active exams a user can pick from, in display order. */
    public List<Exam> findAllActive() {
        return dsl.select(ID, STATE, CLASS, NAME_EN, NAME_ZH, THRESHOLD, STATUS, SORT_ORDER)
                .from(T)
                .where(STATUS.eq("active"))
                .orderBy(SORT_ORDER.asc(), ID.asc())
                .fetch(ExamRepository::map);
    }

    public Optional<Exam> findById(Long id) {
        Record r = dsl.select(ID, STATE, CLASS, NAME_EN, NAME_ZH, THRESHOLD, STATUS, SORT_ORDER)
                .from(T).where(ID.eq(id)).fetchOne();
        return r == null ? Optional.empty() : Optional.of(map(r));
    }

    /**
     * The exam a user/anonymous session falls back to when no current exam is
     * set — the first active exam in display order. Today that's the single
     * seeded CA-M1 exam; once content for more exams exists, onboarding picks
     * one explicitly and this only backstops the unset case.
     */
    public Optional<Long> findDefaultActiveId() {
        return Optional.ofNullable(
                dsl.select(ID).from(T)
                        .where(STATUS.eq("active"))
                        .orderBy(SORT_ORDER.asc(), ID.asc())
                        .limit(1)
                        .fetchOne(ID));
    }

    private static Exam map(Record r) {
        return new Exam(
                r.get(ID), r.get(STATE), r.get(CLASS), r.get(NAME_EN), r.get(NAME_ZH),
                r.get(THRESHOLD), r.get(STATUS), r.get(SORT_ORDER));
    }
}
