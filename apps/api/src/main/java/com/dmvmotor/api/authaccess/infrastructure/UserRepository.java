package com.dmvmotor.api.authaccess.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    // V26 column — accessed via dynamic ref (codebase convention, no jOOQ regen).
    private static final Field<Long> CURRENT_EXAM_ID =
            DSL.field(DSL.name("current_exam_id"), Long.class);

    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public record UserRow(Long id, String email, String languagePreference,
                          int resetCount, Long currentExamId) {}

    public Optional<UserRow> findById(Long userId) {
        var u = Tables.USERS;
        Record r = dsl.select(u.ID, u.EMAIL, u.LANGUAGE_PREFERENCE, u.RESET_COUNT, CURRENT_EXAM_ID)
                .from(u).where(u.ID.eq(userId)).fetchOne();
        if (r == null) return Optional.empty();
        return Optional.of(new UserRow(
                r.get(u.ID), r.get(u.EMAIL),
                r.get(u.LANGUAGE_PREFERENCE), r.get(u.RESET_COUNT),
                r.get(CURRENT_EXAM_ID)));
    }

    public void updateLanguage(Long userId, String language) {
        var u = Tables.USERS;
        dsl.update(u)
                .set(u.LANGUAGE_PREFERENCE, language)
                .where(u.ID.eq(userId))
                .execute();
    }

    /** Set the user's currently-selected exam (the one they're preparing for). */
    public void updateCurrentExam(Long userId, Long examId) {
        var u = Tables.USERS;
        dsl.update(u)
                .set(CURRENT_EXAM_ID, examId)
                .where(u.ID.eq(userId))
                .execute();
    }

    public void incrementResetCount(Long userId) {
        var u = Tables.USERS;
        dsl.update(u)
                .set(u.RESET_COUNT, u.RESET_COUNT.add(1))
                .where(u.ID.eq(userId))
                .execute();
    }

    /** Hard-delete the user row. User-owned FKs cascade (see V6 et al.). */
    public void deleteById(Long userId) {
        var u = Tables.USERS;
        dsl.deleteFrom(u).where(u.ID.eq(userId)).execute();
    }
}
