package com.dmvmotor.api.content.infrastructure;

import com.dmvmotor.api.content.domain.Topic;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TopicRepository {

    private final DSLContext dsl;

    public TopicRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<Topic> findAllOrderBySortOrder() {
        return dsl.selectFrom(Tables.TOPICS)
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
