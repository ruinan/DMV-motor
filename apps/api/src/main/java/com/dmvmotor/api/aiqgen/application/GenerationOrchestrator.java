package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aiqgen.application.DeepSeekChatClient.AiQGenException;
import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion;
import com.dmvmotor.api.aiqgen.domain.GenerationGateResult;
import com.dmvmotor.api.aiqgen.domain.SubTopicSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the negative-feedback control loop end-to-end:
 *
 * <ol>
 *   <li>Generator oversamples 2× the target count.</li>
 *   <li>FormatValidator filters malformed candidates (cheap, no LLM).</li>
 *   <li>Three LLM judges (coverage / difficulty / fact-check) each veto a
 *       candidate. Order matters — coverage first (cheapest to fail fast),
 *       then difficulty, then fact-check (most expensive).</li>
 *   <li>If pass count {@literal <} target, re-generate with an attached
 *       feedback list (reasons from failed candidates) and rerun gates,
 *       up to the retry budget.</li>
 * </ol>
 *
 * <p>The orchestrator does NOT persist anything. It returns a list of passing
 * candidates; the CLI layer renders the V16 seed SQL.
 */
public class GenerationOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(GenerationOrchestrator.class);
    private static final int OVERSAMPLE_FACTOR = 2;
    // Cap per-attempt batch so the generator response stays inside max_tokens
    // (each bilingual MCQ ≈ 600 output tokens; a 4-question batch fits in 4000).
    private static final int MAX_BATCH_SIZE = 4;
    private static final int DEFAULT_RETRY_BUDGET = 3;

    private final QuestionGenerator generator;
    private final FormatValidator formatValidator;
    private final CoverageJudge coverageJudge;
    private final DifficultyJudge difficultyJudge;
    private final RunbookFactChecker factChecker;

    public GenerationOrchestrator(
            QuestionGenerator generator,
            FormatValidator formatValidator,
            CoverageJudge coverageJudge,
            DifficultyJudge difficultyJudge,
            RunbookFactChecker factChecker) {
        this.generator = generator;
        this.formatValidator = formatValidator;
        this.coverageJudge = coverageJudge;
        this.difficultyJudge = difficultyJudge;
        this.factChecker = factChecker;
    }

    public Result run(SubTopicSpec spec, int targetCount) {
        return run(spec, targetCount, DEFAULT_RETRY_BUDGET);
    }

    public Result run(SubTopicSpec spec, int targetCount, int retryBudget) {
        List<GeneratedQuestion> accepted = new ArrayList<>();
        List<String> failureFeedback = new ArrayList<>();
        int attempt = 0;
        while (accepted.size() < targetCount && attempt <= retryBudget) {
            int needed = targetCount - accepted.size();
            int batchSize = Math.min(needed * OVERSAMPLE_FACTOR, MAX_BATCH_SIZE);
            LOG.info("[{}] attempt {} — requesting {} candidates (need {} more)",
                    spec.code(), attempt, batchSize, needed);
            List<GeneratedQuestion> batch;
            try {
                batch = generator.generate(specWithFeedback(spec, failureFeedback), batchSize);
            } catch (AiQGenException e) {
                LOG.warn("[{}] generator threw on attempt {}: {}", spec.code(), attempt, e.getMessage());
                attempt++;
                continue;
            }
            for (GeneratedQuestion candidate : batch) {
                if (accepted.size() >= targetCount) break;
                runGates(candidate, spec, accepted, failureFeedback);
            }
            attempt++;
        }
        return new Result(accepted, failureFeedback, attempt);
    }

    private void runGates(
            GeneratedQuestion candidate,
            SubTopicSpec spec,
            List<GeneratedQuestion> accepted,
            List<String> failureFeedback) {

        GenerationGateResult fmt = formatValidator.check(candidate);
        if (!fmt.passed()) { failureFeedback.add(fmt.reason()); return; }

        GenerationGateResult cov = safeJudge(() -> coverageJudge.judge(candidate, spec), "coverage");
        if (!cov.passed()) { failureFeedback.add(cov.reason()); return; }

        GenerationGateResult dif = safeJudge(() -> difficultyJudge.judge(candidate), "difficulty");
        if (!dif.passed()) { failureFeedback.add(dif.reason()); return; }

        GenerationGateResult fac = safeJudge(() -> factChecker.judge(candidate, spec), "fact-check");
        if (!fac.passed()) { failureFeedback.add(fac.reason()); return; }

        accepted.add(candidate);
        LOG.info("[{}] accepted candidate: {}", spec.code(), truncate(candidate.en().stem(), 60));
    }

    private static GenerationGateResult safeJudge(JudgeRun r, String label) {
        try {
            return r.invoke();
        } catch (AiQGenException e) {
            LOG.warn("{} judge threw: {}", label, e.getMessage());
            return GenerationGateResult.fail(label + ": judge errored — " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface JudgeRun {
        GenerationGateResult invoke();
    }

    /**
     * Append failure feedback to the sub-topic description so the next
     * generation call sees what went wrong. Keeps feedback bounded.
     */
    private static SubTopicSpec specWithFeedback(SubTopicSpec base, List<String> feedback) {
        if (feedback.isEmpty()) return base;
        int take = Math.min(feedback.size(), 5);
        StringBuilder sb = new StringBuilder(base.description());
        sb.append("\n\nPrevious attempts failed because:\n");
        for (int i = feedback.size() - take; i < feedback.size(); i++) {
            sb.append("- ").append(feedback.get(i)).append('\n');
        }
        sb.append("Avoid these failure modes in the new batch.");
        return new SubTopicSpec(base.code(), base.nameEn(), base.nameZh(), sb.toString(), base.runbookExcerpt());
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    public record Result(
            List<GeneratedQuestion> accepted,
            List<String> failureFeedback,
            int attemptsUsed
    ) {}
}
