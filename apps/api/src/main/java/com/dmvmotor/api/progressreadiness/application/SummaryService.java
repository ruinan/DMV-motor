package com.dmvmotor.api.progressreadiness.application;

import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
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

    private final DSLContext     dsl;
    private final UserRepository userRepo;

    public SummaryService(DSLContext dsl, UserRepository userRepo) {
        this.dsl      = dsl;
        this.userRepo = userRepo;
    }

    public SummaryResult getSummary(Long userId) {
        int cycle = cycleFor(userId);
        ReadinessResult readiness = getReadiness(userId, cycle);
        List<WeakTopic> weakTopics = findWeakTopics(userId, cycle);
        int completionScore = calcCompletionScore(userId, cycle);

        String nextActionType;
        String nextActionLabel;
        if (!weakTopics.isEmpty()) {
            nextActionType  = "review";
            nextActionLabel = "Finish review pack";
        } else if (!readiness.isReadyCandidate()) {
            nextActionType  = "mock_exam";
            nextActionLabel = "Take a mock exam";
        } else {
            nextActionType  = "none";
            nextActionLabel = "You're ready!";
        }

        return new SummaryResult(completionScore, readiness.readinessScore(),
                readiness.isReadyCandidate(), weakTopics, nextActionType, nextActionLabel);
    }

    public ReadinessResult getReadiness(Long userId) {
        return getReadiness(userId, cycleFor(userId));
    }

    private ReadinessResult getReadiness(Long userId, int cycle) {
        List<String> missingGates = new ArrayList<>();

        boolean hasPassing = hasPassingMock(userId);
        if (!hasPassing) missingGates.add("MOCK_SCORE_NOT_STABLE");

        int activeMistakes = countActiveMistakes(userId, cycle);
        if (activeMistakes > 0) missingGates.add("KEY_TOPIC_COVERAGE_LOW");

        boolean isReady      = missingGates.isEmpty();
        int     readinessScore = calcReadinessScore(hasPassing, activeMistakes);

        return new ReadinessResult(readinessScore, isReady, missingGates);
    }

    // ---------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------

    private int cycleFor(Long userId) {
        return userRepo.findById(userId).map(u -> u.resetCount()).orElse(0);
    }

    private int calcCompletionScore(Long userId, int cycle) {
        var q  = Tables.QUESTIONS;
        var pa = Tables.PRACTICE_ATTEMPTS;
        var ps = Tables.PRACTICE_SESSIONS;

        int totalActive = dsl.fetchCount(q,
                q.ALLOW_IN_PRACTICE.isTrue().and(q.STATUS.eq("active")));
        if (totalActive == 0) return 0;

        int answered = dsl.fetchCount(
                dsl.selectDistinct(pa.QUESTION_ID)
                   .from(pa)
                   .join(ps).on(ps.ID.eq(pa.PRACTICE_SESSION_ID))
                   .where(pa.USER_ID.eq(userId).and(ps.LEARNING_CYCLE.eq(cycle))));

        return Math.min(100, (int) Math.round(100.0 * answered / totalActive));
    }

    private boolean hasPassingMock(Long userId) {
        var ma = Tables.MOCK_ATTEMPTS;
        return dsl.fetchExists(ma,
                ma.USER_ID.eq(userId)
                        .and(ma.STATUS.eq("submitted"))
                        .and(ma.SCORE_PERCENT.greaterOrEqual(MOCK_PASS_THRESHOLD)));
    }

    private int countActiveMistakes(Long userId, int cycle) {
        var mr = Tables.MISTAKE_RECORDS;
        return dsl.fetchCount(mr,
                mr.USER_ID.eq(userId)
                        .and(mr.IS_ACTIVE.isTrue())
                        .and(mr.LEARNING_CYCLE.eq(cycle)));
    }

    private int calcReadinessScore(boolean hasPassing, int activeMistakes) {
        int score = 0;
        if (hasPassing) score += 50;
        if (activeMistakes == 0) score += 50;
        else score += Math.max(0, 50 - activeMistakes * 5);
        return Math.min(100, score);
    }

    private List<WeakTopic> findWeakTopics(Long userId, int cycle) {
        var mr = Tables.MISTAKE_RECORDS;
        var t  = Tables.TOPICS;
        return dsl.select(t.ID, t.NAME_EN, DSL.sum(mr.WRONG_COUNT))
                .from(mr)
                .join(t).on(t.ID.eq(mr.PRIMARY_TOPIC_ID))
                .where(mr.USER_ID.eq(userId)
                        .and(mr.IS_ACTIVE.isTrue())
                        .and(mr.LEARNING_CYCLE.eq(cycle)))
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
            List<WeakTopic> weakTopics,
            String nextActionType,
            String nextActionLabel
    ) {}

    public record ReadinessResult(
            int readinessScore,
            boolean isReadyCandidate,
            List<String> missingGates
    ) {}

    public record WeakTopic(Long topicId, String label) {}
}
