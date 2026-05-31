package com.dmvmotor.api.aisupport.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read queries backing the deterministic "what to reinforce next"
 * recommendation (mvp.md §5 #10). No writes.
 */
@Repository
public class RecommendationRepository {

    private final DSLContext dsl;

    public RecommendationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** Active-mistake topics for the user in this cycle, most mistakes first. */
    public List<TopicMistakeCount> activeMistakeCountsByTopic(Long userId, int learningCycle) {
        var mr = Tables.MISTAKE_RECORDS;
        Field<Integer> cnt = DSL.count();
        return dsl.select(mr.PRIMARY_TOPIC_ID, cnt)
                .from(mr)
                .where(mr.USER_ID.eq(userId)
                        .and(mr.IS_ACTIVE.isTrue())
                        .and(mr.LEARNING_CYCLE.eq(learningCycle)))
                .groupBy(mr.PRIMARY_TOPIC_ID)
                .orderBy(cnt.desc(), mr.PRIMARY_TOPIC_ID.asc())
                .fetch(r -> new TopicMistakeCount(r.get(mr.PRIMARY_TOPIC_ID), r.get(cnt)));
    }

    /** Topics the user has at least one practice attempt on in this cycle. */
    public Set<Long> coveredTopicIds(Long userId, int learningCycle) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        var q  = Tables.QUESTIONS;
        var ps = Tables.PRACTICE_SESSIONS;
        return dsl.selectDistinct(q.PRIMARY_TOPIC_ID)
                .from(pa)
                .join(q).on(q.ID.eq(pa.QUESTION_ID))
                .join(ps).on(ps.ID.eq(pa.PRACTICE_SESSION_ID))
                .where(pa.USER_ID.eq(userId).and(ps.LEARNING_CYCLE.eq(learningCycle)))
                .fetch(r -> r.get(q.PRIMARY_TOPIC_ID))
                .stream().collect(Collectors.toSet());
    }

    public record TopicMistakeCount(Long topicId, int count) {}
}
