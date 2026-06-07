package com.dmvmotor.api.progressreadiness.application;

import com.dmvmotor.api.authaccess.infrastructure.UserRepository;
import com.dmvmotor.api.content.application.ExamContext;
import com.dmvmotor.api.infrastructure.jooq.generated.Tables;
import com.dmvmotor.api.progressreadiness.config.ReadinessProperties;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Readiness / completion engine implementation for {@code docs/parameters.md} §7–§8.
 * All thresholds and weights come from {@link ReadinessProperties}; none are hardcoded.
 *
 * <p>Every component is scoped to the user's CURRENT exam ({@link ExamContext}) so
 * readiness, coverage, mock and practice stats are independent per exam — switching
 * exam shows that exam's own readiness, not a blend of all exams. exam_id columns
 * (V26) aren't in the generated jOOQ classes, so they're referenced as dynamic
 * fields (codebase convention for post-V1 columns). mistake_records carries no
 * exam_id, so its queries scope through topics.exam_id.
 */
@Service
public class SummaryService {

    private static final Field<Long> Q_EXAM_ID  = DSL.field(DSL.name("questions", "exam_id"), Long.class);
    private static final Field<Long> PS_EXAM_ID = DSL.field(DSL.name("practice_sessions", "exam_id"), Long.class);
    private static final Field<Long> MA_EXAM_ID = DSL.field(DSL.name("mock_attempts", "exam_id"), Long.class);
    private static final Field<Long> T_EXAM_ID  = DSL.field(DSL.name("topics", "exam_id"), Long.class);

    private final DSLContext          dsl;
    private final UserRepository      userRepo;
    private final ReadinessProperties props;
    private final ExamContext         examContext;

    public SummaryService(DSLContext dsl, UserRepository userRepo, ReadinessProperties props,
                          ExamContext examContext) {
        this.dsl         = dsl;
        this.userRepo    = userRepo;
        this.props       = props;
        this.examContext = examContext;
    }

    public SummaryResult getSummary(Long userId) {
        int cycle = cycleFor(userId);
        Long examId = examContext.resolveExamId(userId);
        Components c = computeComponents(userId, cycle, examId);

        int completionScore = completionScore(c);
        int readinessScore = readinessScore(c);

        List<String> missingGates = evaluateGates(c);
        boolean isReadyCandidate = readinessScore >= props.readyThreshold()
                && missingGates.isEmpty();

        List<WeakTopic> weakTopics = findWeakTopics(userId, cycle, examId);

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
        Long examId = examContext.resolveExamId(userId);
        Components c = computeComponents(userId, cycle, examId);

        int readinessScore = readinessScore(c);

        List<String> missingGates = evaluateGates(c);
        boolean isReady = readinessScore >= props.readyThreshold() && missingGates.isEmpty();

        return new ReadinessResult(readinessScore, isReady, missingGates);
    }

    // ---------------------------------------------------------------
    // Components
    // ---------------------------------------------------------------

    /** Raw component ratios in [0, 1], plus data-presence flags. An axis with no
     *  data to measure (no scored mock, no key-coverage questions, no review work,
     *  no recent practice) is EXCLUDED from the weighted score rather than scored
     *  1.0 — otherwise a brand-new user gets free points (B30: 45% after one
     *  failed mock, because key-coverage + review defaulted to 1.0). */
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
            int keyCoveragePercent,
            boolean hasBasicPracticeData,
            boolean hasStabilityData
    ) {}

    private Components computeComponents(Long userId, int cycle, Long examId) {
        MockStats mock = mockStats(userId, cycle, examId);
        CoverageStats key = keyCoverageStats(userId, cycle, examId);
        ReviewStats rev = reviewStats(userId, cycle);
        RatioStat practice = basicPracticeRatio(userId, cycle, examId);
        RatioStat stability = recentStabilityRatio(userId, cycle, examId);
        boolean persistent = hasPersistentMistake(userId, cycle, examId);

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

        return new Components(mockRatio, keyRatio, reviewRatio, practice.ratio(), stability.ratio(),
                mock.count, persistent, rev.totalQuestions > 0,
                key.total > 0, key.total == 0 ? 0 : key.answered * 100 / key.total,
                practice.hasData(), stability.hasData());
    }

    /** Renormalized weighted score: average the ratios of the axes that have data,
     *  weighted, scaled to [0,100]. No axes with data → 0 (the user has done
     *  nothing measurable for this exam yet). */
    private int readinessScore(Components c) {
        return weightedNorm(
                new double[]{c.mockScoreRatio, c.keyCoverageRatio, c.reviewCompletionRatio, c.recentStabilityRatio},
                new int[]{props.mockWeight(), props.keyCoverageWeight(), props.highRiskReviewWeight(), props.recentStabilityWeight()},
                new boolean[]{c.mockCount > 0, c.hasKeyCoverageContent, c.hasReviewTasks, c.hasStabilityData});
    }

    private int completionScore(Components c) {
        return weightedNorm(
                new double[]{c.keyCoverageRatio, c.reviewCompletionRatio, c.basicPracticeRatio},
                new int[]{props.completionKeyCoverageWeight(), props.completionHighRiskReviewWeight(), props.completionBasicPracticeWeight()},
                new boolean[]{c.hasKeyCoverageContent, c.hasReviewTasks, c.hasBasicPracticeData});
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
    // Queries (all scoped to the current exam)
    // ---------------------------------------------------------------

    private record MockStats(int count, double avgScore) {}
    private record CoverageStats(int answered, int total) {}
    private record ReviewStats(int completedQuestions, int totalQuestions) {}
    /** A ratio in [0,1] plus whether there was any data behind it. */
    private record RatioStat(double ratio, boolean hasData) {}

    private MockStats mockStats(Long userId, int cycle, Long examId) {
        var ma = Tables.MOCK_ATTEMPTS;
        List<Integer> recent = dsl.select(ma.SCORE_PERCENT)
                .from(ma)
                // Scored attempts: clean submits + timeouts (both produce a real
                // score). Fail-outs / exits don't count toward readiness.
                .where(ma.USER_ID.eq(userId)
                        .and(ma.STATUS.in("submitted", "ended_by_timeout"))
                        .and(ma.LEARNING_CYCLE.eq(cycle))
                        .and(MA_EXAM_ID.eq(examId))
                        .and(ma.SCORE_PERCENT.isNotNull()))
                .orderBy(ma.CREATED_AT.desc())
                .limit(props.mockMinimumCount())
                .fetch(ma.SCORE_PERCENT);
        if (recent.isEmpty()) return new MockStats(0, 0.0);
        double avg = recent.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        return new MockStats(recent.size(), avg);
    }

    private CoverageStats keyCoverageStats(Long userId, int cycle, Long examId) {
        var q  = Tables.QUESTIONS;
        var pa = Tables.PRACTICE_ATTEMPTS;
        var ps = Tables.PRACTICE_SESSIONS;

        int total = dsl.fetchCount(q,
                q.IS_KEY_COVERAGE.isTrue().and(q.STATUS.eq("active")).and(Q_EXAM_ID.eq(examId)));
        if (total == 0) return new CoverageStats(0, 0);

        int answered = dsl.fetchCount(
                dsl.selectDistinct(pa.QUESTION_ID)
                   .from(pa)
                   .join(ps).on(ps.ID.eq(pa.PRACTICE_SESSION_ID))
                   .join(q).on(q.ID.eq(pa.QUESTION_ID))
                   .where(pa.USER_ID.eq(userId)
                           .and(ps.LEARNING_CYCLE.eq(cycle))
                           .and(q.IS_KEY_COVERAGE.isTrue())
                           .and(Q_EXAM_ID.eq(examId))));
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

    private RatioStat basicPracticeRatio(Long userId, int cycle, Long examId) {
        var q  = Tables.QUESTIONS;
        var pa = Tables.PRACTICE_ATTEMPTS;
        var ps = Tables.PRACTICE_SESSIONS;

        int totalActive = dsl.fetchCount(q,
                q.ALLOW_IN_PRACTICE.isTrue().and(q.STATUS.eq("active")).and(Q_EXAM_ID.eq(examId)));
        // No practice questions for this exam → axis isn't measurable.
        if (totalActive == 0) return new RatioStat(0.0, false);

        int answered = dsl.fetchCount(
                dsl.selectDistinct(pa.QUESTION_ID)
                   .from(pa)
                   .join(ps).on(ps.ID.eq(pa.PRACTICE_SESSION_ID))
                   .where(pa.USER_ID.eq(userId)
                           .and(ps.LEARNING_CYCLE.eq(cycle))
                           .and(PS_EXAM_ID.eq(examId))));
        // The exam HAS practice content, so 0 answered is real data (0% covered).
        return new RatioStat(Math.min(1.0, (double) answered / totalActive), true);
    }

    private RatioStat recentStabilityRatio(Long userId, int cycle, Long examId) {
        var pa = Tables.PRACTICE_ATTEMPTS;
        var ps = Tables.PRACTICE_SESSIONS;

        List<Boolean> recent = dsl.select(pa.IS_CORRECT)
                .from(pa)
                .join(ps).on(ps.ID.eq(pa.PRACTICE_SESSION_ID))
                .where(pa.USER_ID.eq(userId)
                        .and(ps.LEARNING_CYCLE.eq(cycle))
                        .and(PS_EXAM_ID.eq(examId)))
                .orderBy(pa.CREATED_AT.desc())
                .limit(props.recentPracticeWindow())
                .fetch(pa.IS_CORRECT);
        // No recent practice → nothing to measure stability against.
        if (recent.isEmpty()) return new RatioStat(0.0, false);
        long correct = recent.stream().filter(Boolean.TRUE::equals).count();
        double accuracy = 100.0 * correct / recent.size();
        // Normalize against the configured accuracy target so the component stays in [0, 1].
        return new RatioStat(Math.min(1.0, accuracy / (double) props.recentAccuracyThreshold()), true);
    }

    private boolean hasPersistentMistake(Long userId, int cycle, Long examId) {
        var mr = Tables.MISTAKE_RECORDS;
        var t  = Tables.TOPICS;
        // mistake_records has no exam_id — scope through its primary topic's exam.
        return dsl.fetchExists(
                dsl.selectOne()
                   .from(mr)
                   .join(t).on(t.ID.eq(mr.PRIMARY_TOPIC_ID))
                   .where(mr.USER_ID.eq(userId)
                           .and(mr.IS_ACTIVE.isTrue())
                           .and(mr.LEARNING_CYCLE.eq(cycle))
                           .and(mr.WRONG_COUNT.greaterOrEqual(props.persistentMistakeWrongCount()))
                           .and(T_EXAM_ID.eq(examId))));
    }

    private List<WeakTopic> findWeakTopics(Long userId, int cycle, Long examId) {
        var mr = Tables.MISTAKE_RECORDS;
        var t  = Tables.TOPICS;
        return dsl.select(t.ID, t.NAME_EN, DSL.sum(mr.WRONG_COUNT))
                .from(mr)
                .join(t).on(t.ID.eq(mr.PRIMARY_TOPIC_ID))
                .where(mr.USER_ID.eq(userId)
                        .and(mr.IS_ACTIVE.isTrue())
                        .and(mr.LEARNING_CYCLE.eq(cycle))
                        .and(T_EXAM_ID.eq(examId)))
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

    /**
     * Weighted average of the axis ratios that have data, scaled to [0,100].
     * Axes with no data are dropped from BOTH numerator and denominator, so a
     * non-existent / not-yet-engaged axis neither helps nor hurts (vs. the old
     * "score it 1.0" which gifted points). All axes have data → identical to the
     * old Σ(ratio×weight) since the weights sum to 100. No axes → 0.
     */
    private static int weightedNorm(double[] ratios, int[] weights, boolean[] hasData) {
        double num = 0;
        int den = 0;
        for (int i = 0; i < ratios.length; i++) {
            if (hasData[i]) {
                num += ratios[i] * weights[i];
                den += weights[i];
            }
        }
        if (den == 0) return 0;
        return (int) Math.min(100, Math.max(0, Math.round(num / den * 100.0)));
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
