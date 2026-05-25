package com.dmvmotor.api.content.infrastructure;

import com.dmvmotor.api.content.domain.SubTopic;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SubTopicRepository {

    private final DSLContext dsl;

    public SubTopicRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<SubTopic> findAllOrderBySortOrder() {
        return dsl.selectFrom(Tables.SUB_TOPICS)
                .orderBy(Tables.SUB_TOPICS.SORT_ORDER.asc())
                .fetch(r -> new SubTopic(
                        r.getId(),
                        r.getParentTopicId(),
                        r.getCode(),
                        r.getNameEn(),
                        r.getNameZh(),
                        r.getDescription(),
                        r.getSortOrder()
                ));
    }

    /**
     * Returns a map of sub_topic_id → count of active questions assigned to it.
     * Used to surface "bank size" in mastery views so the user sees what's
     * available vs what they've attempted.
     */
    public Map<Long, Integer> countActiveQuestionsBySubTopic() {
        var q = Tables.QUESTIONS;
        Map<Long, Integer> result = new HashMap<>();
        dsl.select(q.SUB_TOPIC_ID, org.jooq.impl.DSL.count())
                .from(q)
                .where(q.STATUS.eq("active"))
                .groupBy(q.SUB_TOPIC_ID)
                .fetch()
                .forEach(r -> result.put(r.value1(), r.value2()));
        return result;
    }
}
