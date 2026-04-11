package com.dmvmotor.api.mistakereview.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import com.dmvmotor.api.mistakereview.domain.MistakeRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MistakeListRepository {

    private final DSLContext dsl;

    public MistakeListRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<MistakeRecord> findActiveMistakes(Long userId, Long topicId,
                                                   int page, int pageSize) {
        var mr = Tables.MISTAKE_RECORDS;
        Condition condition = mr.USER_ID.eq(userId).and(mr.IS_ACTIVE.isTrue());
        if (topicId != null) {
            condition = condition.and(mr.PRIMARY_TOPIC_ID.eq(topicId));
        }

        int offset = (page - 1) * pageSize;

        return dsl.selectFrom(mr)
                .where(condition)
                .orderBy(mr.LAST_WRONG_AT.desc().nullsLast(), mr.ID.asc())
                .limit(pageSize)
                .offset(offset)
                .fetch()
                .map(r -> new MistakeRecord(
                        r.get(mr.ID),
                        r.get(mr.QUESTION_ID),
                        r.get(mr.PRIMARY_TOPIC_ID),
                        r.get(mr.WRONG_COUNT),
                        r.get(mr.LAST_WRONG_AT),
                        r.get(mr.LAST_ENTRY_SOURCE)
                ));
    }

    public int countActiveMistakes(Long userId, Long topicId) {
        var mr = Tables.MISTAKE_RECORDS;
        Condition condition = mr.USER_ID.eq(userId).and(mr.IS_ACTIVE.isTrue());
        if (topicId != null) {
            condition = condition.and(mr.PRIMARY_TOPIC_ID.eq(topicId));
        }
        return dsl.fetchCount(mr, condition);
    }
}
