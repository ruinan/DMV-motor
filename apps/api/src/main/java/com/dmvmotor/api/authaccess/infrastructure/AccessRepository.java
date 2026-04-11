package com.dmvmotor.api.authaccess.infrastructure;

import com.dmvmotor.api.authaccess.domain.AccessPass;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccessRepository {

    private final DSLContext dsl;

    public AccessRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Returns the most relevant pass for the user: active first, then most recent by created_at.
     * Returns empty for anonymous (userId null) or users with no passes.
     */
    public Optional<AccessPass> findLatestPassByUserId(Long userId) {
        if (userId == null) return Optional.empty();

        var ap = Tables.ACCESS_PASSES;
        Record r = dsl.selectFrom(ap)
                .where(ap.USER_ID.eq(userId))
                .orderBy(
                        ap.STATUS.eq("active").desc(),
                        ap.CREATED_AT.desc()
                )
                .limit(1)
                .fetchOne();

        if (r == null) return Optional.empty();

        return Optional.of(new AccessPass(
                r.get(ap.ID),
                r.get(ap.USER_ID),
                r.get(ap.STATUS),
                r.get(ap.STARTS_AT),
                r.get(ap.EXPIRES_AT),
                r.get(ap.MOCK_EXAM_TOTAL_COUNT),
                r.get(ap.MOCK_EXAM_USED_COUNT)
        ));
    }
}
