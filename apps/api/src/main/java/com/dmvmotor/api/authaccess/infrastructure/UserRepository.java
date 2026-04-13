package com.dmvmotor.api.authaccess.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public record UserRow(Long id, String email, String languagePreference, int resetCount) {}

    public Optional<UserRow> findById(Long userId) {
        var u = Tables.USERS;
        Record r = dsl.selectFrom(u).where(u.ID.eq(userId)).fetchOne();
        if (r == null) return Optional.empty();
        return Optional.of(new UserRow(
                r.get(u.ID), r.get(u.EMAIL),
                r.get(u.LANGUAGE_PREFERENCE), r.get(u.RESET_COUNT)));
    }

    public void updateLanguage(Long userId, String language) {
        var u = Tables.USERS;
        dsl.update(u)
                .set(u.LANGUAGE_PREFERENCE, language)
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
}
