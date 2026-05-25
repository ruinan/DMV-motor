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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JudgesTest {

    private final DeepSeekChatClient client = mock(DeepSeekChatClient.class);

    private static final SubTopicSpec SPEC = new SubTopicSpec(
            "LANE_SPLITTING_SHARING",
            "Lane Splitting & Sharing",
            "车道分流与共享",
            "CA lane-splitting rules",
            "Lane splitting is legal in California with caution.");

    private static final GeneratedQuestion CANDIDATE = new GeneratedQuestion(
            "LANE_SPLITTING_SHARING", "B",
            new Variant("Is lane splitting legal in California?",
                    List.of(new Choice("A", "No, never"),
                            new Choice("B", "Yes, with caution"),
                            new Choice("C", "Only on freeways"),
                            new Choice("D", "Only at low speeds")),
                    "Lane splitting is legal in CA when done safely."),
            new Variant("加州车道分流合法吗？",
                    List.of(new Choice("A", "不可以"),
                            new Choice("B", "合法但谨慎"),
                            new Choice("C", "仅高速"),
                            new Choice("D", "仅低速")),
                    "在加州安全的车道分流合法。"));

    // ---------- CoverageJudge ----------

    @Test
    void coverage_pass_returnsPass() {
        when(client.chat(any(), any())).thenReturn("{\"pass\": true, \"reason\": \"on topic\"}");
        GenerationGateResult result = new CoverageJudge(client).judge(CANDIDATE, SPEC);
        assertThat(result.passed()).isTrue();
        assertThat(result.reason()).contains("coverage").contains("on topic");
    }

    @Test
    void coverage_fail_returnsFail() {
        when(client.chat(any(), any())).thenReturn("{\"pass\": false, \"reason\": \"about speed not splitting\"}");
        GenerationGateResult result = new CoverageJudge(client).judge(CANDIDATE, SPEC);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("about speed");
    }

    @Test
    void coverage_promptIncludesSubTopicAndStem() {
        when(client.chat(any(), any())).thenReturn("{\"pass\": true, \"reason\": \"\"}");
        new CoverageJudge(client).judge(CANDIDATE, SPEC);

        ArgumentCaptor<String> userCap = ArgumentCaptor.forClass(String.class);
        verify(client).chat(any(), userCap.capture());
        String user = userCap.getValue();
        assertThat(user).contains("LANE_SPLITTING_SHARING");
        assertThat(user).contains("Is lane splitting legal in California");
    }

    // ---------- DifficultyJudge ----------

    @Test
    void difficulty_pass_returnsPass() {
        when(client.chat(any(), any())).thenReturn("{\"pass\": true, \"reason\": \"all 4 plausible\"}");
        GenerationGateResult result = new DifficultyJudge(client).judge(CANDIDATE);
        assertThat(result.passed()).isTrue();
        assertThat(result.reason()).contains("difficulty");
    }

    @Test
    void difficulty_fail_returnsFail() {
        when(client.chat(any(), any())).thenReturn("{\"pass\": false, \"reason\": \"choice C obviously wrong\"}");
        GenerationGateResult result = new DifficultyJudge(client).judge(CANDIDATE);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("choice C");
    }

    @Test
    void difficulty_promptHasNoSubTopicReference() {
        // Difficulty judge looks at choices in isolation — must not bias on sub-topic.
        when(client.chat(any(), any())).thenReturn("{\"pass\": true, \"reason\": \"\"}");
        new DifficultyJudge(client).judge(CANDIDATE);
        ArgumentCaptor<String> userCap = ArgumentCaptor.forClass(String.class);
        verify(client).chat(any(), userCap.capture());
        assertThat(userCap.getValue()).doesNotContain("LANE_SPLITTING_SHARING");
    }

    // ---------- RunbookFactChecker ----------

    @Test
    void factCheck_pass_returnsPass() {
        when(client.chat(any(), any())).thenReturn("{\"pass\": true, \"reason\": \"handbook says legal\"}");
        GenerationGateResult result = new RunbookFactChecker(client).judge(CANDIDATE, SPEC);
        assertThat(result.passed()).isTrue();
        assertThat(result.reason()).contains("fact-check");
    }

    @Test
    void factCheck_fail_returnsFail() {
        when(client.chat(any(), any())).thenReturn("{\"pass\": false, \"reason\": \"handbook says different\"}");
        GenerationGateResult result = new RunbookFactChecker(client).judge(CANDIDATE, SPEC);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("handbook says different");
    }

    @Test
    void factCheck_promptIncludesRunbookExcerpt() {
        when(client.chat(any(), any())).thenReturn("{\"pass\": true, \"reason\": \"\"}");
        new RunbookFactChecker(client).judge(CANDIDATE, SPEC);
        ArgumentCaptor<String> userCap = ArgumentCaptor.forClass(String.class);
        verify(client).chat(any(), userCap.capture());
        assertThat(userCap.getValue()).contains("Lane splitting is legal in California");
    }

    // ---------- JudgeVerdict.parse coverage ----------

    @Test
    void verdict_stripsCodeFence() {
        when(client.chat(any(), any())).thenReturn("```json\n{\"pass\": true, \"reason\": \"ok\"}\n```");
        GenerationGateResult result = new CoverageJudge(client).judge(CANDIDATE, SPEC);
        assertThat(result.passed()).isTrue();
    }

    @Test
    void verdict_stripsSingleLineFence() {
        when(client.chat(any(), any())).thenReturn("```{\"pass\": true, \"reason\": \"ok\"}```");
        GenerationGateResult result = new CoverageJudge(client).judge(CANDIDATE, SPEC);
        assertThat(result.passed()).isTrue();
    }

    @Test
    void verdict_notJson_throwsAiQGenException() {
        when(client.chat(any(), any())).thenReturn("just some prose, no JSON");
        assertThatThrownBy(() -> new CoverageJudge(client).judge(CANDIDATE, SPEC))
                .isInstanceOf(AiQGenException.class)
                .hasMessageContaining("Judge output not JSON");
    }

    @Test
    void verdict_notObject_throwsAiQGenException() {
        when(client.chat(any(), any())).thenReturn("[]");
        assertThatThrownBy(() -> new CoverageJudge(client).judge(CANDIDATE, SPEC))
                .isInstanceOf(AiQGenException.class)
                .hasMessageContaining("not a JSON object");
    }

    @Test
    void verdict_missingPassField_defaultsToFail() {
        when(client.chat(any(), any())).thenReturn("{\"reason\":\"unclear\"}");
        GenerationGateResult result = new CoverageJudge(client).judge(CANDIDATE, SPEC);
        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("unclear");
    }

    @Test
    void verdict_nullPassField_defaultsToFail() {
        when(client.chat(any(), any())).thenReturn("{\"pass\":null,\"reason\":\"x\"}");
        GenerationGateResult result = new CoverageJudge(client).judge(CANDIDATE, SPEC);
        assertThat(result.passed()).isFalse();
    }

    @Test
    void verdict_missingReason_returnsEmptyString() {
        when(client.chat(any(), any())).thenReturn("{\"pass\": true}");
        GenerationGateResult result = new CoverageJudge(client).judge(CANDIDATE, SPEC);
        assertThat(result.passed()).isTrue();
    }

    @Test
    void verdict_longInvalidJson_truncatedInError() {
        when(client.chat(any(), any())).thenReturn("X".repeat(500));
        assertThatThrownBy(() -> new CoverageJudge(client).judge(CANDIDATE, SPEC))
                .isInstanceOf(AiQGenException.class)
                .hasMessageContaining("...");
    }
}
