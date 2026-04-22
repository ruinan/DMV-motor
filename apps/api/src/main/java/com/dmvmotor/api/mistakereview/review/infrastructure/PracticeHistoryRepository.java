package com.dmvmotor.api.mistakereview.review.infrastructure;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import com.dmvmotor.api.mistakereview.review.domain.MasteryEvaluator.TopicStats;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reads per-topic practice history for mastery evaluation. Sources attempts from
 * both the practice-session path (practice_attempts ⋈ practice_sessions) and the
 * review-task path (practice_attempts ⋈ review_tasks ⋈ review_packs) — per V4,
 * practice_attempts.practice_session_id is nullable and review-sourced attempts
 * carry review_task_id instead.
 */
@Repository
public class PracticeHistoryRepository {

    private final DSLContext dsl;

    public PracticeHistoryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public TopicStats topicStats(Long userId, Long topicId, int cycle) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        var q  = Tables.QUESTIONS;
        var ps = Tables.PRACTICE_SESSIONS;
        var rt = Tables.REVIEW_TASKS;
        var rp = Tables.REVIEW_PACKS;

        List<Boolean> practicePath = dsl.select(pa.IS_CORRECT)
                .from(pa)
                .join(q).on(q.ID.eq(pa.QUESTION_ID))
                .join(ps).on(ps.ID.eq(pa.PRACTICE_SESSION_ID))
                .where(pa.USER_ID.eq(userId)
                        .and(q.PRIMARY_TOPIC_ID.eq(topicId))
                        .and(ps.LEARNING_CYCLE.eq(cycle)))
                .fetch(pa.IS_CORRECT);

        List<Boolean> reviewPath = dsl.select(pa.IS_CORRECT)
                .from(pa)
                .join(q).on(q.ID.eq(pa.QUESTION_ID))
                .join(rt).on(rt.ID.eq(pa.REVIEW_TASK_ID))
                .join(rp).on(rp.ID.eq(rt.REVIEW_PACK_ID))
                .where(pa.USER_ID.eq(userId)
                        .and(q.PRIMARY_TOPIC_ID.eq(topicId))
                        .and(rp.LEARNING_CYCLE.eq(cycle)))
                .fetch(pa.IS_CORRECT);

        int total   = practicePath.size() + reviewPath.size();
        int correct = (int) (practicePath.stream().filter(Boolean.TRUE::equals).count()
                           + reviewPath.stream().filter(Boolean.TRUE::equals).count());
        return new TopicStats(total, correct);
    }

    public List<Boolean> lastNAttemptsForTopic(Long userId, Long topicId,
                                               int cycle, int n) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        var q  = Tables.QUESTIONS;
        var ps = Tables.PRACTICE_SESSIONS;
        var rt = Tables.REVIEW_TASKS;
        var rp = Tables.REVIEW_PACKS;

        List<Record2<Boolean, OffsetDateTime>> practicePath = dsl
                .select(pa.IS_CORRECT, pa.CREATED_AT)
                .from(pa)
                .join(q).on(q.ID.eq(pa.QUESTION_ID))
                .join(ps).on(ps.ID.eq(pa.PRACTICE_SESSION_ID))
                .where(pa.USER_ID.eq(userId)
                        .and(q.PRIMARY_TOPIC_ID.eq(topicId))
                        .and(ps.LEARNING_CYCLE.eq(cycle)))
                .orderBy(pa.CREATED_AT.desc())
                .limit(n)
                .fetch();

        List<Record2<Boolean, OffsetDateTime>> reviewPath = dsl
                .select(pa.IS_CORRECT, pa.CREATED_AT)
                .from(pa)
                .join(q).on(q.ID.eq(pa.QUESTION_ID))
                .join(rt).on(rt.ID.eq(pa.REVIEW_TASK_ID))
                .join(rp).on(rp.ID.eq(rt.REVIEW_PACK_ID))
                .where(pa.USER_ID.eq(userId)
                        .and(q.PRIMARY_TOPIC_ID.eq(topicId))
                        .and(rp.LEARNING_CYCLE.eq(cycle)))
                .orderBy(pa.CREATED_AT.desc())
                .limit(n)
                .fetch();

        List<Record2<Boolean, OffsetDateTime>> merged = new ArrayList<>(practicePath);
        merged.addAll(reviewPath);
        merged.sort(Comparator.comparing(
                (Record2<Boolean, OffsetDateTime> r) -> r.value2()).reversed());

        List<Boolean> result = new ArrayList<>(Math.min(n, merged.size()));
        for (int i = 0; i < merged.size() && i < n; i++) {
            result.add(merged.get(i).value1());
        }
        return result;
    }
}
