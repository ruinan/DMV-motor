package com.dmvmotor.api.reminder.infrastructure;

import com.dmvmotor.api.reminder.domain.Reminder;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access for {@code reminder_tasks}. Dynamic jOOQ field refs (same pattern
 * as AiDeepDiveLogRepository) so the new table needs no jOOQ regen.
 */
@Repository
public class ReminderRepository {

    private static final Table<?>       T            = DSL.table(DSL.name("reminder_tasks"));
    private static final Field<Long>    ID           = DSL.field(DSL.name("id"), Long.class);
    private static final Field<Long>    USER_ID      = DSL.field(DSL.name("user_id"), Long.class);
    private static final Field<String>  TYPE         = DSL.field(DSL.name("type"), String.class);
    private static final Field<String>  STATUS       = DSL.field(DSL.name("status"), String.class);
    private static final Field<Integer> PRIORITY     = DSL.field(DSL.name("priority"), Integer.class);
    private static final Field<OffsetDateTime> CREATED_AT =
            DSL.field(DSL.name("created_at"), OffsetDateTime.class);
    private static final Field<OffsetDateTime> RESPONDED_AT =
            DSL.field(DSL.name("responded_at"), OffsetDateTime.class);

    private final DSLContext dsl;

    public ReminderRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Long insert(Long userId, String type, int priority) {
        return dsl.insertInto(T)
                .columns(USER_ID, TYPE, STATUS, PRIORITY)
                .values(DSL.val(userId), DSL.val(type), DSL.val("pending"), DSL.val(priority))
                .returningResult(ID)
                .fetchOne()
                .value1();
    }

    /** Daily cap: was any reminder created for this user since {@code since}? */
    public boolean existsCreatedSince(Long userId, OffsetDateTime since) {
        return dsl.fetchExists(T, USER_ID.eq(userId).and(CREATED_AT.greaterOrEqual(since)));
    }

    /** Active (pending) reminders, highest priority first then newest. */
    public List<Reminder> findActiveByUser(Long userId) {
        return dsl.selectFrom(T)
                .where(USER_ID.eq(userId).and(STATUS.eq("pending")))
                .orderBy(PRIORITY.asc(), CREATED_AT.desc())
                .fetch(ReminderRepository::map);
    }

    /** Statuses of this user's last {@code limit} reminders of a type, newest
     *  first — drives the "3 consecutive unresponded → pause" rule. */
    public List<String> recentStatusesByType(Long userId, String type, int limit) {
        return dsl.select(STATUS)
                .from(T)
                .where(USER_ID.eq(userId).and(TYPE.eq(type)))
                .orderBy(CREATED_AT.desc())
                .limit(limit)
                .fetch(r -> r.get(STATUS));
    }

    public Optional<Reminder> findById(Long id) {
        Record r = dsl.selectFrom(T).where(ID.eq(id)).fetchOne();
        return r == null ? Optional.empty() : Optional.of(map(r));
    }

    /** Mark a pending reminder responded. Returns rows updated (0 if it wasn't
     *  pending — i.e. already responded, treated as idempotent by the caller). */
    public int markResponded(Long id) {
        return dsl.update(T)
                .set(STATUS, "responded")
                .set(RESPONDED_AT, OffsetDateTime.now())
                .where(ID.eq(id).and(STATUS.eq("pending")))
                .execute();
    }

    private static Reminder map(Record r) {
        return new Reminder(
                r.get(ID), r.get(USER_ID), r.get(TYPE), r.get(STATUS),
                r.get(PRIORITY), r.get(CREATED_AT), r.get(RESPONDED_AT));
    }
}
