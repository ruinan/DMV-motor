package com.dmvmotor.api.mistakereview.review.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Handles review_packs, review_tasks, and review_task_questions persistence.
 */
@Repository
public class ReviewRepository {

    private final DSLContext dsl;

    public ReviewRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    // ---------------------------------------------------------------
    // Review Pack
    // ---------------------------------------------------------------

    public Optional<Long> findActivePackId(Long userId, int learningCycle) {
        var rp = Tables.REVIEW_PACKS;
        Record r = dsl.selectFrom(rp)
                .where(rp.USER_ID.eq(userId)
                        .and(rp.STATUS.eq("active"))
                        .and(rp.LEARNING_CYCLE.eq(learningCycle)))
                .orderBy(rp.CREATED_AT.desc())
                .limit(1)
                .fetchOne();
        return r == null ? Optional.empty() : Optional.of(r.get(rp.ID));
    }

    public Long createPack(Long userId, int learningCycle) {
        var rp = Tables.REVIEW_PACKS;
        return dsl.insertInto(rp)
                .set(rp.USER_ID,        userId)
                .set(rp.LEARNING_CYCLE, learningCycle)
                .returningResult(rp.ID)
                .fetchOne()
                .value1();
    }

    public void updatePackStatus(Long packId, String status) {
        var rp = Tables.REVIEW_PACKS;
        dsl.update(rp).set(rp.STATUS, status).where(rp.ID.eq(packId)).execute();
    }

    /** Returns number of tasks in the pack that are NOT yet completed. */
    public int countIncompleteTasks(Long packId) {
        var rt = Tables.REVIEW_TASKS;
        return dsl.fetchCount(rt,
                rt.REVIEW_PACK_ID.eq(packId).and(rt.STATUS.notEqual("completed")));
    }

    // ---------------------------------------------------------------
    // Review Tasks
    // ---------------------------------------------------------------

    public Long createTask(Long packId, Long userId, Long topicId, int questionCount, int priority) {
        var rt = Tables.REVIEW_TASKS;
        return dsl.insertInto(rt)
                .set(rt.REVIEW_PACK_ID,         packId)
                .set(rt.USER_ID,                userId)
                .set(rt.TOPIC_ID,               topicId)
                .set(rt.TARGET_QUESTION_COUNT,  questionCount)
                .set(rt.PRIORITY,               priority)
                .returningResult(rt.ID)
                .fetchOne()
                .value1();
    }

    public List<TaskRow> findTasksByPackId(Long packId) {
        var rt = Tables.REVIEW_TASKS;
        return dsl.selectFrom(rt)
                .where(rt.REVIEW_PACK_ID.eq(packId))
                .orderBy(rt.PRIORITY.desc(), rt.ID.asc())
                .fetch()
                .map(r -> new TaskRow(
                        r.get(rt.ID),
                        r.get(rt.USER_ID),
                        r.get(rt.TOPIC_ID),
                        r.get(rt.REVIEW_PACK_ID),
                        r.get(rt.TASK_TYPE),
                        r.get(rt.STATUS),
                        r.get(rt.PRIORITY),
                        r.get(rt.TARGET_QUESTION_COUNT),
                        r.get(rt.COMPLETED_QUESTION_COUNT)
                ));
    }

    public Optional<TaskRow> findTaskById(Long taskId) {
        var rt = Tables.REVIEW_TASKS;
        Record r = dsl.selectFrom(rt).where(rt.ID.eq(taskId)).fetchOne();
        if (r == null) return Optional.empty();
        return Optional.of(new TaskRow(
                r.get(rt.ID),
                r.get(rt.USER_ID),
                r.get(rt.TOPIC_ID),
                r.get(rt.REVIEW_PACK_ID),
                r.get(rt.TASK_TYPE),
                r.get(rt.STATUS),
                r.get(rt.PRIORITY),
                r.get(rt.TARGET_QUESTION_COUNT),
                r.get(rt.COMPLETED_QUESTION_COUNT)
        ));
    }

    public void updateTaskStatus(Long taskId, String status) {
        var rt = Tables.REVIEW_TASKS;
        dsl.update(rt).set(rt.STATUS, status).where(rt.ID.eq(taskId)).execute();
    }

    public void incrementCompletedCount(Long taskId) {
        var rt = Tables.REVIEW_TASKS;
        dsl.update(rt)
                .set(rt.COMPLETED_QUESTION_COUNT, rt.COMPLETED_QUESTION_COUNT.add(1))
                .where(rt.ID.eq(taskId))
                .execute();
    }

    // ---------------------------------------------------------------
    // Review Task Questions
    // ---------------------------------------------------------------

    public void addTaskQuestion(Long taskId, Long questionId) {
        var rtq = Tables.REVIEW_TASK_QUESTIONS;
        dsl.insertInto(rtq)
                .set(rtq.REVIEW_TASK_ID, taskId)
                .set(rtq.QUESTION_ID,    questionId)
                .execute();
    }

    public List<Long> findQuestionIdsByTaskId(Long taskId) {
        var rtq = Tables.REVIEW_TASK_QUESTIONS;
        return dsl.select(rtq.QUESTION_ID)
                .from(rtq)
                .where(rtq.REVIEW_TASK_ID.eq(taskId))
                .fetch(rtq.QUESTION_ID);
    }

    public List<Long> findCorrectlyAnsweredQuestionIds(Long taskId) {
        var rtq = Tables.REVIEW_TASK_QUESTIONS;
        return dsl.select(rtq.QUESTION_ID)
                .from(rtq)
                .where(rtq.REVIEW_TASK_ID.eq(taskId)
                        .and(rtq.IS_ANSWERED.isTrue())
                        .and(rtq.IS_CORRECT.isTrue()))
                .fetch(rtq.QUESTION_ID);
    }

    public boolean hasAnswer(Long taskId, Long questionId) {
        var rtq = Tables.REVIEW_TASK_QUESTIONS;
        return dsl.fetchExists(rtq,
                rtq.REVIEW_TASK_ID.eq(taskId)
                        .and(rtq.QUESTION_ID.eq(questionId))
                        .and(rtq.IS_ANSWERED.isTrue()));
    }

    public boolean existsInTask(Long taskId, Long questionId) {
        var rtq = Tables.REVIEW_TASK_QUESTIONS;
        return dsl.fetchExists(rtq,
                rtq.REVIEW_TASK_ID.eq(taskId).and(rtq.QUESTION_ID.eq(questionId)));
    }

    public void markQuestionAnswered(Long taskId, Long questionId, boolean isCorrect) {
        var rtq = Tables.REVIEW_TASK_QUESTIONS;
        dsl.update(rtq)
                .set(rtq.IS_ANSWERED, true)
                .set(rtq.IS_CORRECT,  isCorrect)
                .where(rtq.REVIEW_TASK_ID.eq(taskId).and(rtq.QUESTION_ID.eq(questionId)))
                .execute();
    }

    public void saveReviewAttempt(Long taskId, Long userId, Long questionId,
                                   Long variantId, String selectedKey, boolean isCorrect) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        dsl.insertInto(pa)
                .set(pa.REVIEW_TASK_ID,        taskId)
                .set(pa.USER_ID,               userId)
                .set(pa.QUESTION_ID,           questionId)
                .set(pa.QUESTION_VARIANT_ID,   variantId)
                .set(pa.SELECTED_CHOICE_KEY,   selectedKey)
                .set(pa.IS_CORRECT,            isCorrect)
                .set(pa.ENTRY_SOURCE,          "review")
                .execute();
    }

    // ---------------------------------------------------------------
    // Value objects
    // ---------------------------------------------------------------

    public record TaskRow(
            Long   id,
            Long   userId,
            Long   topicId,
            Long   reviewPackId,
            String taskType,
            String status,
            int    priority,
            int    targetQuestionCount,
            int    completedQuestionCount
    ) {}
}
