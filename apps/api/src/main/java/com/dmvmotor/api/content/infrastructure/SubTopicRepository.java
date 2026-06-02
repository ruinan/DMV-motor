package com.dmvmotor.api.content.infrastructure;

import com.dmvmotor.api.content.domain.SubTopic;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
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

    /**
     * Sub-topics belonging to one exam, in display order. Sub-topics carry no
     * exam_id of their own — they inherit it from their parent topic, so this
     * joins through {@code topics.exam_id} (V26).
     */
    public List<SubTopic> findByExam(Long examId) {
        var st = Tables.SUB_TOPICS;
        var t  = Tables.TOPICS;
        Field<Long> topicExamId = DSL.field(DSL.name("topics", "exam_id"), Long.class);
        return dsl.select(st.ID, st.PARENT_TOPIC_ID, st.CODE, st.NAME_EN,
                          st.NAME_ZH, st.DESCRIPTION, st.SORT_ORDER)
                .from(st)
                .join(t).on(t.ID.eq(st.PARENT_TOPIC_ID))
                .where(topicExamId.eq(examId))
                .orderBy(st.SORT_ORDER.asc())
                .fetch(r -> new SubTopic(
                        r.get(st.ID),
                        r.get(st.PARENT_TOPIC_ID),
                        r.get(st.CODE),
                        r.get(st.NAME_EN),
                        r.get(st.NAME_ZH),
                        r.get(st.DESCRIPTION),
                        r.get(st.SORT_ORDER)
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
