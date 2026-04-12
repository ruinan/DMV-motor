package com.dmvmotor.api.progressreadiness.application;

import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SummaryService {

    /** Minimum mock score (%) to be considered "passed" for readiness. */
    private static final int MOCK_PASS_THRESHOLD = 83;

    private final DSLContext dsl;

    public SummaryService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public SummaryResult getSummary(Long userId) {
        ReadinessResult readiness = getReadiness(userId);
        List<WeakTopic> weakTopics = findWeakTopics(userId);
        int completionScore = calcCompletionScore(userId);

        return new SummaryResult(completionScore, readiness.readinessScore(),
                readiness.isReadyCandidate(), weakTopics);
    }

    public ReadinessResult getReadiness(Long userId) {
        List<String> missingGates = new ArrayList<>();

        boolean hasPassing = hasPassingMock(userId);
        if (!hasPassing) missingGates.add("MOCK_SCORE_NOT_STABLE");

        int activeMistakes = countActiveMistakes(userId);
        if (activeMistakes > 0) missingGates.add("KEY_TOPIC_COVERAGE_LOW");

        boolean isReady      = missingGates.isEmpty();
        int     readinessScore = calcReadinessScore(hasPassing, activeMistakes);

        return new ReadinessResult(readinessScore, isReady, missingGates);
    }

    // ---------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------

    private int calcCompletionScore(Long userId) {
        var q  = Tables.QUESTIONS;
        var pa = Tables.PRACTICE_ATTEMPTS;

        int totalActive = dsl.fetchCount(q,
                q.ALLOW_IN_PRACTICE.isTrue().and(q.STATUS.eq("active")));
        if (totalActive == 0) return 0;

        int answered = dsl.fetchCount(
                dsl.selectDistinct(pa.QUESTION_ID)
                   .from(pa)
                   .where(pa.USER_ID.eq(userId)));

        return Math.min(100, (int) Math.round(100.0 * answered / totalActive));
    }

    private boolean hasPassingMock(Long userId) {
        var ma = Tables.MOCK_ATTEMPTS;
        return dsl.fetchExists(ma,
                ma.USER_ID.eq(userId)
                        .and(ma.STATUS.eq("submitted"))
                        .and(ma.SCORE_PERCENT.greaterOrEqual(MOCK_PASS_THRESHOLD)));
    }

    private int countActiveMistakes(Long userId) {
        var mr = Tables.MISTAKE_RECORDS;
        return dsl.fetchCount(mr,
                mr.USER_ID.eq(userId).and(mr.IS_ACTIVE.isTrue()));
    }

    private int calcReadinessScore(boolean hasPassing, int activeMistakes) {
        int score = 0;
        if (hasPassing) score += 50;
        if (activeMistakes == 0) score += 50;
        else score += Math.max(0, 50 - activeMistakes * 5);
        return Math.min(100, score);
    }

    private List<WeakTopic> findWeakTopics(Long userId) {
        var mr = Tables.MISTAKE_RECORDS;
        var t  = Tables.TOPICS;
        return dsl.select(t.ID, t.NAME_EN, DSL.sum(mr.WRONG_COUNT))
                .from(mr)
                .join(t).on(t.ID.eq(mr.PRIMARY_TOPIC_ID))
                .where(mr.USER_ID.eq(userId).and(mr.IS_ACTIVE.isTrue()))
                .groupBy(t.ID, t.NAME_EN)
                .orderBy(DSL.sum(mr.WRONG_COUNT).desc())
                .limit(5)
                .fetch()
                .map(r -> new WeakTopic(r.get(t.ID), r.get(t.NAME_EN)));
    }

    // ---------------------------------------------------------------
    // Result types
    // ---------------------------------------------------------------

    public record SummaryResult(
            int completionScore,
            int readinessScore,
            boolean isReadyCandidate,
            List<WeakTopic> weakTopics
    ) {}

    public record ReadinessResult(
            int readinessScore,
            boolean isReadyCandidate,
            List<String> missingGates
    ) {}

    public record WeakTopic(Long topicId, String label) {}
}
