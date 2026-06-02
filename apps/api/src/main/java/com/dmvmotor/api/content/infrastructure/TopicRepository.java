package com.dmvmotor.api.content.infrastructure;

import com.dmvmotor.api.content.domain.Topic;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TopicRepository {

    // V26 topics.exam_id (dynamic ref, no jOOQ regen).
    private static final Field<Long> EXAM_ID = DSL.field(DSL.name("exam_id"), Long.class);

    private final DSLContext dsl;

    public TopicRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** Topics for a single exam (its own taxonomy), in display order. */
    public List<Topic> findByExam(Long examId) {
        return dsl.selectFrom(Tables.TOPICS)
                .where(EXAM_ID.eq(examId))
                .orderBy(Tables.TOPICS.SORT_ORDER.asc())
                .fetch(r -> new Topic(
                        r.getId(),
                        r.getParentTopicId(),
                        r.getCode(),
                        r.getNameEn(),
                        r.getNameZh(),
                        r.getIsKeyTopic(),
                        r.getRiskLevel(),
                        r.getSortOrder()
                ));
    }
}
