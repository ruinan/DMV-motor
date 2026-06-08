package com.dmvmotor.api.progressreadiness.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Daily-activity aggregation for the engagement strip (streak + daily goal).
 * Buckets practice answers into the USER's local calendar day — the client
 * sends its UTC offset — so "today" and "consecutive days" line up with the
 * clock the user reads, not the server's UTC midnight. Counts practice attempts
 * across ALL exams: a streak is about showing up, not which test you opened.
 */
@Repository
public class EngagementRepository {

    private final DSLContext dsl;

    public EngagementRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Local-day → number of practice answers that day for the user, most recent
     * first. {@code offsetMinutes} = minutes to ADD to UTC to reach the user's
     * local time (e.g. -480 for US Pacific). Empty when there's no activity.
     */
    public Map<LocalDate, Integer> answeredCountByLocalDay(Long userId, int offsetMinutes) {
        var ap = Tables.PRACTICE_ATTEMPTS;
        // (submitted_at AT TIME ZONE 'UTC') yields the UTC wall-clock as a plain
        // timestamp; shifting by the offset and casting to date gives the local
        // calendar day independent of the SERVER timezone. The offset is INLINED
        // (it's a server-clamped int — no injection risk) so the SELECT, GROUP BY
        // and ORDER BY copies of the expression are byte-identical; a bind param
        // renders as distinct placeholders and Postgres then rejects the GROUP BY.
        Field<LocalDate> localDay = DSL.field(
                "(({0} AT TIME ZONE 'UTC') + interval '1 minute' * {1})::date",
                LocalDate.class, ap.SUBMITTED_AT, DSL.inline(offsetMinutes));

        Map<LocalDate, Integer> byDay = new LinkedHashMap<>();
        for (Record2<LocalDate, Integer> r : dsl
                .select(localDay, DSL.count())
                .from(ap)
                .where(ap.USER_ID.eq(userId))
                .groupBy(localDay)
                .orderBy(localDay.desc())
                .fetch()) {
            byDay.put(r.value1(), r.value2());
        }
        return byDay;
    }
}
