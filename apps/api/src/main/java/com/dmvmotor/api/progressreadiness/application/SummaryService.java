package com.dmvmotor.api.progressreadiness.application;

import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import com.dmvmotor.api.progressreadiness.config.ReadinessProperties;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Readiness / completion engine implementation for {@code docs/parameters.md} §7–§8.
 * All thresholds and weights come from {@link ReadinessProperties}; none are hardcoded.
 */
@Service
public class SummaryService {

    private final DSLContext          dsl;
    private final UserRepository      userRepo;
    private final ReadinessProperties props;

    public SummaryService(DSLContext dsl, UserRepository userRepo, ReadinessProperties props) {
        this.dsl      = dsl;
        this.userRepo = userRepo;
        this.props    = props;
    }

    public SummaryResult getSummary(Long userId) {
        int cycle = cycleFor(userId);
        Components c = computeComponents(userId, cycle);

        int completionScore = weighted(
                c.keyCoverageRatio,  props.completionKeyCoverageWeight(),
                c.reviewCompletionRatio, props.completionHighRiskReviewWeight(),
                c.basicPracticeRatio, props.completionBasicPracticeWeight());

        int readinessScore = weighted(
                c.mockScoreRatio,     props.mockWeight(),
                c.keyCoverageRatio,   props.keyCoverageWeight(),
                c.reviewCompletionRatio, props.highRiskReviewWeight(),
                c.recentStabilityRatio,  props.recentStabilityWeight());

        List<String> missingGates = evaluateGates(c);
        boolean isReadyCandidate = readinessScore >= props.readyThreshold()
                && missingGates.isEmpty();

        List<WeakTopic> weakTopics = findWeakTopics(userId, cycle);

        String nextActionType;
        String nextActionLabel;
        if (!weakTopics.isEmpty()) {
            nextActionType  = "review";
            nextActionLabel = "Finish review pack";
        } else if (!isReadyCandidate) {
            nextActionType  = "mock_exam";
            nextActionLabel = "Take a mock exam";
        } else {
            nextActionType  = "none";
            nextActionLabel = "You're ready!";
        }

        return new SummaryResult(completionScore, readinessScore, isReadyCandidate,
                weakTopics, nextActionType, nextActionLabel);
    }

    public ReadinessResult getReadiness(Long userId) {
        int cycle = cycleFor(userId);
        Components c = computeComponents(userId, cycle);

        int readinessScore = weighted(
                c.mockScoreRatio,     props.mockWeight(),
                c.keyCoverageRatio,   props.keyCoverageWeight(),
                c.reviewCompletionRatio, props.highRiskReviewWeight(),
                c.recentStabilityRatio,  props.recentStabilityWeight());

        List<String> missingGates = evaluateGates(c);
        boolean isReady = readinessScore >= props.readyThreshold() && missingGates.isEmpty();

        return new ReadinessResult(readinessScore, isReady, missingGates);
    }

    // ---------------------------------------------------------------
    // Components
    // ---------------------------------------------------------------

    /** Raw component ratios in [0, 1], plus extra fields gates need for threshold comparison. */
    private record Components(
            double mockScoreRatio,
            double keyCoverageRatio,
            double reviewCompletionRatio,
            double basicPracticeRatio,
            double recentStabilityRatio,
            int mockCount,
            boolean hasPersistentMistake,
            boolean hasReviewTasks,
            boolean hasKeyCoverageContent,
            int keyCoveragePercent
    ) {}

    private Components computeComponents(Long userId, int cycle) {
        MockStats mock = mockStats(userId, cycle);
        CoverageStats key = keyCoverageStats(userId, cycle);
        ReviewStats rev = reviewStats(userId, cycle);
        double practice = basicPracticeRatio(userId, cycle);
        double stability = recentStabilityRatio(userId, cycle);
        boolean persistent = hasPersistentMistake(userId, cycle);

        // Mock: 0 when user hasn't taken any mocks; otherwise average score as a ratio of target.
        double mockRatio = mock.count == 0
                ? 0.0
                : Math.min(1.0, mock.avgScore / (double) props.mockAverageThreshold());

        // Key coverage: answered/total, OR trivially 1.0 when there's nothing to measure
        // (degenerate "no key questions in catalog" case; the gate handles this the same way).
        double keyRatio = key.total == 0 ? 1.0 : (double) key.answered / key.total;

        // Review completion: done/target, OR trivially 1.0 when the user has no review work
        // (no mistakes → nothing to review → this axis is satisfied).
        double reviewRatio = rev.totalQuestions == 0
                ? 1.0
                : (double) rev.completedQuestions / rev.totalQuestions;

        return new Components(mockRatio, keyRatio, reviewRatio, practice, stability,
                mock.count, persistent, rev.totalQuestions > 0,
                key.total > 0, key.total == 0 ? 0 : key.answered * 100 / key.total);
    }

    private List<String> evaluateGates(Components c) {
        List<String> gates = new ArrayList<>();

        // "最近 2 次 mock exam 平均成绩达到 85%" (docs/parameters.md §8)
        if (c.mockCount < props.mockMinimumCount()
                || c.mockScoreRatio < 1.0) {
            gates.add("MOCK_SCORE_NOT_STABLE");
        }

        // "关键 topic 覆盖达到 90%" — only fires when there are key-coverage questions to measure.
        if (c.hasKeyCoverageContent
                && c.keyCoveragePercent < props.keyCoverageThreshold()) {
            gates.add("KEY_COVERAGE_INCOMPLETE");
        }

        // "高风险复习完成达到 80%" — only fires when the user actually has review work.
        if (c.hasReviewTasks
                && c.reviewCompletionRatio * 100 < props.reviewCompletionThreshold()) {
            gates.add("HIGH_RISK_REVIEW_LOW");
        }

        // "当前不存在持续未解决的关键薄弱点"
        if (c.hasPersistentMistake) {
            gates.add("PERSISTENT_WEAK_POINT");
        }

        return gates;
    }

    // ---------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------

    private record MockStats(int count, double avgScore) {}
    private record CoverageStats(int answered, int total) {}
    private record ReviewStats(int completedQuestions, int totalQuestions) {}

    private MockStats mockStats(Long userId, int cycle) {
        var ma = Tables.MOCK_ATTEMPTS;
        List<Integer> recent = dsl.select(ma.SCORE_PERCENT)
                .from(ma)
                .where(ma.USER_ID.eq(userId)
                        .and(ma.STATUS.eq("submitted"))
                        .and(ma.LEARNING_CYCLE.eq(cycle))
                        .and(ma.SCORE_PERCENT.isNotNull()))
                .orderBy(ma.CREATED_AT.desc())
                .limit(props.mockMinimumCount())
                .fetch(ma.SCORE_PERCENT);
        if (recent.isEmpty()) return new MockStats(0, 0.0);
        double avg = recent.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        return new MockStats(recent.size(), avg);
    }

    private CoverageStats keyCoverageStats(Long userId, int cycle) {
        var q  = Tables.QUESTIONS;
        var pa = Tables.PRACTICE_ATTEMPTS;
        var ps = Tables.PRACTICE_SESSIONS;

        int total = dsl.fetchCount(q,
                q.IS_KEY_COVERAGE.isTrue().and(q.STATUS.eq("active")));
        if (total == 0) return new CoverageStats(0, 0);

        int answered = dsl.fetchCount(
                dsl.selectDistinct(pa.QUESTION_ID)
                   .from(pa)
                   .join(ps).on(ps.ID.eq(pa.PRACTICE_SESSION_ID))
                   .join(q).on(q.ID.eq(pa.QUESTION_ID))
                   .where(pa.USER_ID.eq(userId)
                           .and(ps.LEARNING_CYCLE.eq(cycle))
                           .and(q.IS_KEY_COVERAGE.isTrue())));
        return new CoverageStats(answered, total);
    }

    private ReviewStats reviewStats(Long userId, int cycle) {
        var rt = Tables.REVIEW_TASKS;
        var rp = Tables.REVIEW_PACKS;

        var row = dsl.select(
                        DSL.coalesce(DSL.sum(rt.TARGET_QUESTION_COUNT), 0).cast(Integer.class),
                        DSL.coalesce(DSL.sum(rt.COMPLETED_QUESTION_COUNT), 0).cast(Integer.class))
                .from(rt)
                .join(rp).on(rp.ID.eq(rt.REVIEW_PACK_ID))
                .where(rp.USER_ID.eq(userId).and(rp.LEARNING_CYCLE.eq(cycle)))
                .fetchOne();
        if (row == null) return new ReviewStats(0, 0);
        int target = row.value1() == null ? 0 : row.value1();
        int done   = row.value2() == null ? 0 : row.value2();
        return new ReviewStats(done, target);
    }

    private double basicPracticeRatio(Long userId, int cycle) {
        var q  = Tables.QUESTIONS;
        var pa = Tables.PRACTICE_ATTEMPTS;
        var ps = Tables.PRACTICE_SESSIONS;

        int totalActive = dsl.fetchCount(q,
                q.ALLOW_IN_PRACTICE.isTrue().and(q.STATUS.eq("active")));
        if (totalActive == 0) return 0.0;

        int answered = dsl.fetchCount(
                dsl.selectDistinct(pa.QUESTION_ID)
                   .from(pa)
                   .join(ps).on(ps.ID.eq(pa.PRACTICE_SESSION_ID))
                   .where(pa.USER_ID.eq(userId).and(ps.LEARNING_CYCLE.eq(cycle))));
        return Math.min(1.0, (double) answered / totalActive);
    }

    private double recentStabilityRatio(Long userId, int cycle) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        var ps = Tables.PRACTICE_SESSIONS;

        List<Boolean> recent = dsl.select(pa.IS_CORRECT)
                .from(pa)
                .join(ps).on(ps.ID.eq(pa.PRACTICE_SESSION_ID))
                .where(pa.USER_ID.eq(userId).and(ps.LEARNING_CYCLE.eq(cycle)))
                .orderBy(pa.CREATED_AT.desc())
                .limit(props.recentPracticeWindow())
                .fetch(pa.IS_CORRECT);
        if (recent.isEmpty()) return 0.0;
        long correct = recent.stream().filter(Boolean.TRUE::equals).count();
        double accuracy = 100.0 * correct / recent.size();
        // Normalize against the configured accuracy target so the component stays in [0, 1].
        return Math.min(1.0, accuracy / (double) props.recentAccuracyThreshold());
    }

    private boolean hasPersistentMistake(Long userId, int cycle) {
        var mr = Tables.MISTAKE_RECORDS;
        return dsl.fetchExists(mr,
                mr.USER_ID.eq(userId)
                        .and(mr.IS_ACTIVE.isTrue())
                        .and(mr.LEARNING_CYCLE.eq(cycle))
                        .and(mr.WRONG_COUNT.greaterOrEqual(props.persistentMistakeWrongCount())));
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
    // Helpers
    // ---------------------------------------------------------------

    private int cycleFor(Long userId) {
        return userRepo.findById(userId).map(u -> u.resetCount()).orElse(0);
    }

    /** Weighted sum of ratio-in-[0,1] × weight-in-%. */
    private static int weighted(double r1, int w1, double r2, int w2, double r3, int w3) {
        double score = r1 * w1 + r2 * w2 + r3 * w3;
        return (int) Math.min(100, Math.max(0, Math.round(score)));
    }

    private static int weighted(double r1, int w1, double r2, int w2,
                                double r3, int w3, double r4, int w4) {
        double score = r1 * w1 + r2 * w2 + r3 * w3 + r4 * w4;
        return (int) Math.min(100, Math.max(0, Math.round(score)));
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
