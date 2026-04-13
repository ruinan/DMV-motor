package com.dmvmotor.api.practice.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public class MistakeRepository {

    private final DSLContext dsl;

    public MistakeRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** Upsert within the current learning cycle: increment wrong_count if exists, create if not. */
    public void upsertMistake(Long userId, Long questionId, Long topicId,
                               String entrySource, int learningCycle) {
        var mr  = Tables.MISTAKE_RECORDS;
        var now = OffsetDateTime.now();

        boolean exists = dsl.fetchExists(mr,
                mr.USER_ID.eq(userId)
                        .and(mr.QUESTION_ID.eq(questionId))
                        .and(mr.LEARNING_CYCLE.eq(learningCycle)));

        if (exists) {
            dsl.update(mr)
                    .set(mr.WRONG_COUNT,       mr.WRONG_COUNT.add(1))
                    .set(mr.LAST_WRONG_AT,     now)
                    .set(mr.LAST_ENTRY_SOURCE, entrySource)
                    .set(mr.IS_ACTIVE,         true)
                    .set(mr.UPDATED_AT,        now)
                    .where(mr.USER_ID.eq(userId)
                            .and(mr.QUESTION_ID.eq(questionId))
                            .and(mr.LEARNING_CYCLE.eq(learningCycle)))
                    .execute();
        } else {
            dsl.insertInto(mr)
                    .set(mr.USER_ID,           userId)
                    .set(mr.QUESTION_ID,       questionId)
                    .set(mr.PRIMARY_TOPIC_ID,  topicId)
                    .set(mr.LAST_ENTRY_SOURCE, entrySource)
                    .set(mr.LEARNING_CYCLE,    learningCycle)
                    .execute();
        }
    }
}
