package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aiqgen.application.DeepSeekChatClient.AiQGenException;
import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion;
import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion.Choice;
import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion.Variant;
import com.dmvmotor.api.aiqgen.domain.GenerationGateResult;
import com.dmvmotor.api.aiqgen.domain.SubTopicSpec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerationOrchestratorTest {

    private final QuestionGenerator generator = mock(QuestionGenerator.class);
    private final FormatValidator formatValidator = mock(FormatValidator.class);
    private final CoverageJudge coverageJudge = mock(CoverageJudge.class);
    private final DifficultyJudge difficultyJudge = mock(DifficultyJudge.class);
    private final RunbookFactChecker factChecker = mock(RunbookFactChecker.class);

    private final GenerationOrchestrator orchestrator = new GenerationOrchestrator(
            generator, formatValidator, coverageJudge, difficultyJudge, factChecker);

    private static final SubTopicSpec SPEC = new SubTopicSpec(
            "LANE_SPLITTING_SHARING", "Lane Splitting", "车道分流",
            "rules", "Lane splitting is legal...");

    @Test
    void run_allCandidatesPass_returnsTargetCount() {
        when(generator.generate(any(), anyInt())).thenReturn(List.of(candidate(1), candidate(2)));
        passAllGates();

        GenerationOrchestrator.Result result = orchestrator.run(SPEC, 2);

        assertThat(result.accepted()).hasSize(2);
        assertThat(result.failureFeedback()).isEmpty();
        assertThat(result.attemptsUsed()).isEqualTo(1);
    }

    @Test
    void run_oversamples2x_underBatchCap() {
        when(generator.generate(any(), anyInt())).thenReturn(List.of(candidate(1), candidate(2), candidate(3), candidate(4)));
        passAllGates();

        orchestrator.run(SPEC, 2);

        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        verify(generator).generate(any(), sizeCap.capture());
        // 2 * target=2 = 4, under the batch cap.
        assertThat(sizeCap.getValue()).isEqualTo(4);
    }

    @Test
    void run_capsBatchSizeAtMax() {
        // target=10 → naive oversample=20, but batch cap should clamp to 4.
        when(generator.generate(any(), anyInt())).thenReturn(List.of());
        when(formatValidator.check(any())).thenReturn(GenerationGateResult.pass("ok"));
        orchestrator.run(SPEC, 10, /*retryBudget=*/0);

        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        verify(generator).generate(any(), sizeCap.capture());
        assertThat(sizeCap.getValue()).isEqualTo(4);
    }

    @Test
    void run_formatFails_retriesAndCollectsFeedback() {
        // attempt 0: returns 2 candidates, both fail format
        // attempt 1: returns 2 candidates, both pass everything
        when(generator.generate(any(), anyInt()))
                .thenReturn(List.of(candidate(1), candidate(2)))
                .thenReturn(List.of(candidate(3), candidate(4)));

        when(formatValidator.check(any()))
                .thenReturn(GenerationGateResult.fail("missing en stem"))
                .thenReturn(GenerationGateResult.fail("missing en stem"))
                .thenReturn(GenerationGateResult.pass("ok"))
                .thenReturn(GenerationGateResult.pass("ok"));
        passLlmGates();

        GenerationOrchestrator.Result result = orchestrator.run(SPEC, 2);

        assertThat(result.accepted()).hasSize(2);
        assertThat(result.failureFeedback()).contains("missing en stem");
        assertThat(result.attemptsUsed()).isEqualTo(2);
    }

    @Test
    void run_coverageFails_skipsRemainingJudges() {
        when(generator.generate(any(), anyInt())).thenReturn(List.of(candidate(1)));
        when(formatValidator.check(any())).thenReturn(GenerationGateResult.pass("ok"));
        when(coverageJudge.judge(any(), any())).thenReturn(GenerationGateResult.fail("off topic"));

        orchestrator.run(SPEC, 1, /*retryBudget=*/0);

        verify(coverageJudge).judge(any(), any());
        org.mockito.Mockito.verifyNoInteractions(difficultyJudge);
        org.mockito.Mockito.verifyNoInteractions(factChecker);
    }

    @Test
    void run_factCheckFails_candidateRejected() {
        when(generator.generate(any(), anyInt())).thenReturn(List.of(candidate(1)));
        when(formatValidator.check(any())).thenReturn(GenerationGateResult.pass("ok"));
        when(coverageJudge.judge(any(), any())).thenReturn(GenerationGateResult.pass("on topic"));
        when(difficultyJudge.judge(any())).thenReturn(GenerationGateResult.pass("hard enough"));
        // Production gates prefix the gate label, so the mock matches that shape.
        when(factChecker.judge(any(), any())).thenReturn(GenerationGateResult.fail("fact-check: handbook says different"));

        GenerationOrchestrator.Result result = orchestrator.run(SPEC, 1, 0);

        assertThat(result.accepted()).isEmpty();
        assertThat(result.failureFeedback()).contains("fact-check: handbook says different");
    }

    @Test
    void run_retryFeedbackAppendedToSpec() {
        when(generator.generate(any(), anyInt()))
                .thenReturn(List.of(candidate(1)))
                .thenReturn(List.of(candidate(2)));
        when(formatValidator.check(any()))
                .thenReturn(GenerationGateResult.fail("first batch fail"))
                .thenReturn(GenerationGateResult.pass("ok"));
        passLlmGates();

        orchestrator.run(SPEC, 1);

        ArgumentCaptor<SubTopicSpec> specCap = ArgumentCaptor.forClass(SubTopicSpec.class);
        verify(generator, org.mockito.Mockito.atLeast(2)).generate(specCap.capture(), anyInt());
        // First call uses original description, subsequent calls include feedback.
        List<SubTopicSpec> specs = specCap.getAllValues();
        assertThat(specs.get(0).description()).isEqualTo("rules");
        assertThat(specs.get(1).description()).contains("first batch fail");
    }

    @Test
    void run_generatorThrows_retriesUpToBudget() {
        when(generator.generate(any(), anyInt()))
                .thenThrow(new AiQGenException("network blip"))
                .thenReturn(List.of(candidate(1)));
        when(formatValidator.check(any())).thenReturn(GenerationGateResult.pass("ok"));
        passLlmGates();

        GenerationOrchestrator.Result result = orchestrator.run(SPEC, 1);

        assertThat(result.accepted()).hasSize(1);
        assertThat(result.attemptsUsed()).isEqualTo(2);
    }

    @Test
    void run_judgeThrows_recordsAsFail() {
        when(generator.generate(any(), anyInt())).thenReturn(List.of(candidate(1)));
        when(formatValidator.check(any())).thenReturn(GenerationGateResult.pass("ok"));
        when(coverageJudge.judge(any(), any())).thenThrow(new AiQGenException("judge crashed"));

        GenerationOrchestrator.Result result = orchestrator.run(SPEC, 1, 0);

        assertThat(result.accepted()).isEmpty();
        assertThat(result.failureFeedback()).anyMatch(s -> s.contains("judge crashed"));
    }

    @Test
    void run_retryBudgetExhausted_returnsPartialResults() {
        // Every attempt fails format, exhausting retries
        when(generator.generate(any(), anyInt())).thenReturn(List.of(candidate(1)));
        when(formatValidator.check(any())).thenReturn(GenerationGateResult.fail("always fails"));

        GenerationOrchestrator.Result result = orchestrator.run(SPEC, 3, /*retryBudget=*/2);

        assertThat(result.accepted()).isEmpty();
        assertThat(result.attemptsUsed()).isEqualTo(3); // initial + 2 retries
    }

    @Test
    void run_stopsAtTargetEvenIfBatchHasMore() {
        when(generator.generate(any(), anyInt())).thenReturn(
                List.of(candidate(1), candidate(2), candidate(3), candidate(4)));
        passAllGates();

        GenerationOrchestrator.Result result = orchestrator.run(SPEC, 2);

        assertThat(result.accepted()).hasSize(2);
    }

    // ---- helpers ----

    private void passAllGates() {
        when(formatValidator.check(any())).thenReturn(GenerationGateResult.pass("ok"));
        passLlmGates();
    }

    private void passLlmGates() {
        when(coverageJudge.judge(any(), any())).thenReturn(GenerationGateResult.pass("ok"));
        when(difficultyJudge.judge(any())).thenReturn(GenerationGateResult.pass("ok"));
        when(factChecker.judge(any(), any())).thenReturn(GenerationGateResult.pass("ok"));
    }

    private static GeneratedQuestion candidate(int n) {
        return new GeneratedQuestion(
                "LANE_SPLITTING_SHARING", "A",
                new Variant("Stem " + n, List.of(
                        new Choice("A", "alpha"),
                        new Choice("B", "beta"),
                        new Choice("C", "gamma"),
                        new Choice("D", "delta")), "explanation"),
                new Variant("题 " + n, List.of(
                        new Choice("A", "甲"),
                        new Choice("B", "乙"),
                        new Choice("C", "丙"),
                        new Choice("D", "丁")), "解释"));
    }
}
