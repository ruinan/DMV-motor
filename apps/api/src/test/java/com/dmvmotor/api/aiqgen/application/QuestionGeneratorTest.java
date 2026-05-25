package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aiqgen.application.DeepSeekChatClient.AiQGenException;
import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion;
import com.dmvmotor.api.aiqgen.domain.SubTopicSpec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestionGeneratorTest {

    private final DeepSeekChatClient client = mock(DeepSeekChatClient.class);
    private final QuestionGenerator generator = new QuestionGenerator(client);

    private static final SubTopicSpec SPEC = new SubTopicSpec(
            "LANE_SPLITTING_SHARING",
            "Lane Splitting & Sharing",
            "车道分流与共享",
            "CA lane-splitting rules",
            "Lane splitting is legal in California with caution...");

    @Test
    void happyPath_parsesBilingualArray() {
        when(client.chat(any(), any())).thenReturn(SAMPLE_JSON);

        List<GeneratedQuestion> result = generator.generate(SPEC, 1);

        assertThat(result).hasSize(1);
        GeneratedQuestion q = result.get(0);
        assertThat(q.subTopicCode()).isEqualTo("LANE_SPLITTING_SHARING");
        assertThat(q.correctChoiceKey()).isEqualTo("B");
        assertThat(q.en().stem()).contains("lane splitting");
        assertThat(q.zh().stem()).contains("车道");
        assertThat(q.en().choices()).hasSize(4);
        assertThat(q.zh().choices()).hasSize(4);
        assertThat(q.en().explanation()).startsWith("Lane splitting is legal");
    }

    @Test
    void stripsCodeFence_whenLlmWrapsInMarkdown() {
        String fenced = "```json\n" + SAMPLE_JSON + "\n```";
        when(client.chat(any(), any())).thenReturn(fenced);

        List<GeneratedQuestion> result = generator.generate(SPEC, 1);

        assertThat(result).hasSize(1);
    }

    @Test
    void notJson_throwsAiQGenException() {
        when(client.chat(any(), any())).thenReturn("This is not JSON at all, just prose.");

        assertThatThrownBy(() -> generator.generate(SPEC, 1))
                .isInstanceOf(AiQGenException.class)
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void notArray_throwsAiQGenException() {
        when(client.chat(any(), any())).thenReturn("{\"questions\":[]}");

        assertThatThrownBy(() -> generator.generate(SPEC, 1))
                .isInstanceOf(AiQGenException.class)
                .hasMessageContaining("not a JSON array");
    }

    @Test
    void promptCarriesRunbookExcerpt_andRequestedCount() {
        when(client.chat(any(), any())).thenReturn("[]");

        generator.generate(SPEC, 5);

        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(client).chat(any(), userCaptor.capture());
        String userPrompt = userCaptor.getValue();
        assertThat(userPrompt).contains("LANE_SPLITTING_SHARING");
        assertThat(userPrompt).contains("Lane splitting is legal");
        assertThat(userPrompt).contains("Produce 5 distinct");
    }

    @Test
    void promptCarriesNoUserData() {
        when(client.chat(any(), any())).thenReturn("[]");

        generator.generate(SPEC, 1);

        ArgumentCaptor<String> systemCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userCap = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(client).chat(systemCap.capture(), userCap.capture());
        String combined = systemCap.getValue() + "\n" + userCap.getValue();
        // Privacy: no user identifiers, no session refs, no answers history.
        assertThat(combined).doesNotContainIgnoringCase("user_id");
        assertThat(combined).doesNotContainIgnoringCase("session");
        assertThat(combined).doesNotContainIgnoringCase("firebase_uid");
    }

    @Test
    void emptyObject_yieldsAllNullFields() {
        // Triggers textOrNull's `v == null` branch on every field.
        when(client.chat(any(), any())).thenReturn("[{}]");
        List<GeneratedQuestion> result = generator.generate(SPEC, 1);
        assertThat(result.get(0).correctChoiceKey()).isNull();
        assertThat(result.get(0).en().stem()).isNull();
        assertThat(result.get(0).en().choices()).isEmpty();
    }

    @Test
    void longInvalidJson_isTruncatedInErrorMessage() {
        String big = "X".repeat(500); // not valid JSON, > 200 chars triggers truncate path
        when(client.chat(any(), any())).thenReturn(big);
        assertThatThrownBy(() -> generator.generate(SPEC, 1))
                .isInstanceOf(AiQGenException.class)
                .hasMessageContaining("...");
    }

    @Test
    void missingChoicesField_yieldsEmptyChoicesList() {
        // Question without choices_en / choices_zh — generator returns it as-is,
        // FormatValidator will reject downstream. Generator is non-validating.
        when(client.chat(any(), any())).thenReturn("""
                [{
                  "stem_en": "Q",
                  "stem_zh": "问",
                  "correct_choice_key": "A",
                  "explanation_en": "x",
                  "explanation_zh": "y"
                }]
                """);
        List<GeneratedQuestion> result = generator.generate(SPEC, 1);
        assertThat(result.get(0).en().choices()).isEmpty();
        assertThat(result.get(0).zh().choices()).isEmpty();
    }

    @Test
    void explicitJsonNullFields_yieldNull() {
        when(client.chat(any(), any())).thenReturn("""
                [{
                  "stem_en": null,
                  "stem_zh": null,
                  "choices_en": [],
                  "choices_zh": [],
                  "correct_choice_key": null,
                  "explanation_en": null,
                  "explanation_zh": null
                }]
                """);
        List<GeneratedQuestion> result = generator.generate(SPEC, 1);
        assertThat(result.get(0).correctChoiceKey()).isNull();
        assertThat(result.get(0).en().stem()).isNull();
    }

    @Test
    void singleLineCodeFence_isStripped() {
        // No newline between opening fence and content.
        when(client.chat(any(), any())).thenReturn("```[]```");
        List<GeneratedQuestion> result = generator.generate(SPEC, 1);
        assertThat(result).isEmpty();
    }

    @Test
    void emptyArray_returnsEmptyList() {
        when(client.chat(any(), any())).thenReturn("[]");
        assertThat(generator.generate(SPEC, 1)).isEmpty();
    }

    private static final String SAMPLE_JSON = """
            [{
              "stem_en": "Is lane splitting legal in California?",
              "stem_zh": "在加州车道分流合法吗？",
              "choices_en": [
                {"key":"A","text":"No, never"},
                {"key":"B","text":"Yes, with caution"},
                {"key":"C","text":"Only on freeways"},
                {"key":"D","text":"Only during commute hours"}
              ],
              "choices_zh": [
                {"key":"A","text":"完全不可以"},
                {"key":"B","text":"合法，但需谨慎"},
                {"key":"C","text":"仅高速公路上可以"},
                {"key":"D","text":"仅通勤时段可以"}
              ],
              "correct_choice_key": "B",
              "explanation_en": "Lane splitting is legal in California when done safely at a reasonable speed differential.",
              "explanation_zh": "在加州，安全且速度差合理的车道分流是合法的。"
            }]
            """;
}
