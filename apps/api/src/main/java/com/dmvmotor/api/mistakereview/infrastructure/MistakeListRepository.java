package com.dmvmotor.api.mistakereview.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import com.dmvmotor.api.mistakereview.domain.MistakeRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MistakeListRepository {

    private final DSLContext dsl;

    public MistakeListRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<MistakeRecord> findActiveMistakes(Long userId, Long topicId,
                                                   int page, int pageSize,
                                                   int learningCycle) {
        var mr = Tables.MISTAKE_RECORDS;
        Condition condition = mr.USER_ID.eq(userId)
                .and(mr.IS_ACTIVE.isTrue())
                .and(mr.LEARNING_CYCLE.eq(learningCycle));
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

    public int countActive(Long userId, int learningCycle) {
        var mr = Tables.MISTAKE_RECORDS;
        return dsl.selectCount()
                .from(mr)
                .where(mr.USER_ID.eq(userId)
                        .and(mr.IS_ACTIVE.isTrue())
                        .and(mr.LEARNING_CYCLE.eq(learningCycle)))
                .fetchOne(0, Integer.class);
    }

    public int countDistinctActiveTopics(Long userId, int learningCycle) {
        var mr = Tables.MISTAKE_RECORDS;
        return dsl.select(org.jooq.impl.DSL.countDistinct(mr.PRIMARY_TOPIC_ID))
                .from(mr)
                .where(mr.USER_ID.eq(userId)
                        .and(mr.IS_ACTIVE.isTrue())
                        .and(mr.LEARNING_CYCLE.eq(learningCycle)))
                .fetchOne(0, Integer.class);
    }

    public void setActive(Long userId, Long questionId, boolean isActive, int learningCycle) {
        var mr = Tables.MISTAKE_RECORDS;
        dsl.update(mr)
                .set(mr.IS_ACTIVE, isActive)
                .where(mr.USER_ID.eq(userId)
                        .and(mr.QUESTION_ID.eq(questionId))
                        .and(mr.LEARNING_CYCLE.eq(learningCycle)))
                .execute();
    }

    /**
     * Bulk-deactivate every active mistake for (user, topic, cycle). Called when
     * topic-level mastery is reached so the user does not have to "answer every
     * accumulated mistake question individually" to clear them — mastery is a
     * topic-level signal per docs/parameters.md §6.
     *
     * <p>Idempotent: re-running on an already-cleared topic is a no-op.
     *
     * @return number of rows updated (caller may log / assert)
     */
    public int deactivateForTopic(Long userId, Long topicId, int learningCycle) {
        var mr = Tables.MISTAKE_RECORDS;
        return dsl.update(mr)
                .set(mr.IS_ACTIVE, false)
                .where(mr.USER_ID.eq(userId)
                        .and(mr.PRIMARY_TOPIC_ID.eq(topicId))
                        .and(mr.LEARNING_CYCLE.eq(learningCycle))
                        .and(mr.IS_ACTIVE.isTrue()))
                .execute();
    }

    public int countActiveMistakes(Long userId, Long topicId, int learningCycle) {
        var mr = Tables.MISTAKE_RECORDS;
        Condition condition = mr.USER_ID.eq(userId)
                .and(mr.IS_ACTIVE.isTrue())
                .and(mr.LEARNING_CYCLE.eq(learningCycle));
        if (topicId != null) {
            condition = condition.and(mr.PRIMARY_TOPIC_ID.eq(topicId));
        }
        return dsl.fetchCount(mr, condition);
    }
}
