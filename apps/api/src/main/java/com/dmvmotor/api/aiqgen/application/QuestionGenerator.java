package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aiqgen.application.DeepSeekChatClient.AiQGenException;
import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion;
import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion.Choice;
import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion.Variant;
import com.dmvmotor.api.aiqgen.domain.SubTopicSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Calls DeepSeek to draft bilingual M1 questions for a sub-topic. The
 * generator does NO validation — it just parses what DeepSeek returns into
 * domain objects. Downstream gates ({@link FormatValidator}, the three LLM
 * judges) reject malformed or off-target candidates.
 *
 * <p>Per design doc decision #2, the prompt asks for "hard" questions where
 * all four choices look defensible at first glance. The DifficultyJudge gate
 * later enforces this with a binary check.
 *
 * <p>Privacy: the prompt is purely about content (sub-topic, handbook
 * excerpt). No user data is ever sent to DeepSeek.
 */
public class QuestionGenerator {

    private static final String SYSTEM_PROMPT = """
            You are a California Class M1 motorcycle permit test question author.
            Generate exam-quality multiple choice questions, each with EXACTLY 4 choices keyed A/B/C/D.
            The DMV Motorcycle Handbook is the sole source of truth — do not invent rules not stated there.
            All four distractors must be defensible at first glance; reject any choice that is obviously wrong.
            Output strict JSON only. No markdown fences, no commentary, no trailing text.
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            Sub-topic code: %s
            Sub-topic (EN): %s
            Sub-topic (ZH): %s
            Description: %s

            Relevant DMV Motorcycle Handbook excerpt:
            ---
            %s
            ---

            Produce %d distinct multiple-choice questions on this sub-topic.

            For EACH question output an object with these fields (snake_case keys):
              "stem_en": one-sentence English question stem
              "stem_zh": one-sentence Chinese question stem (semantic match, not literal)
              "choices_en": array of 4 objects {"key": "A"|"B"|"C"|"D", "text": "..."}
              "choices_zh": array of 4 objects with the same keys as choices_en, Chinese text
              "correct_choice_key": one of "A"/"B"/"C"/"D"
              "explanation_en": 50-500 chars, plain text, no markdown
              "explanation_zh": 50-300 Chinese chars, plain text, no markdown

            Rules:
              - All four choices must look plausible. If even one distractor is obviously wrong, redraft.
              - Distractors must be different from each other (no duplicates).
              - Tests fine distinctions: e.g., 30 mph vs 25 mph speed limits, exact rules from the handbook.
              - Correct answer must be supported by the handbook excerpt above.
              - choices_zh keys must match choices_en keys 1:1 (same key = same answer in both languages).
              - Output JSON array of exactly %d question objects. NO other content.

            Example output shape (for guidance, do not copy content):
            [
              {
                "stem_en": "...",
                "stem_zh": "...",
                "choices_en": [{"key":"A","text":"..."}, {"key":"B","text":"..."}, {"key":"C","text":"..."}, {"key":"D","text":"..."}],
                "choices_zh": [{"key":"A","text":"..."}, {"key":"B","text":"..."}, {"key":"C","text":"..."}, {"key":"D","text":"..."}],
                "correct_choice_key": "B",
                "explanation_en": "...",
                "explanation_zh": "..."
              }
            ]
            """;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DeepSeekChatClient client;

    public QuestionGenerator(DeepSeekChatClient client) {
        this.client = client;
    }

    public List<GeneratedQuestion> generate(SubTopicSpec spec, int targetCount) {
        String userPrompt = USER_PROMPT_TEMPLATE.formatted(
                spec.code(),
                spec.nameEn(),
                spec.nameZh(),
                spec.description(),
                spec.runbookExcerpt(),
                targetCount,
                targetCount);
        String raw = client.chat(SYSTEM_PROMPT, userPrompt);
        return parse(raw, spec.code());
    }

    private static List<GeneratedQuestion> parse(String raw, String subTopicCode) {
        String json = stripCodeFence(raw).trim();
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (Exception e) {
            throw new AiQGenException("Generator output was not valid JSON: " + truncate(raw, 200), e);
        }
        if (!root.isArray()) {
            throw new AiQGenException("Generator output was not a JSON array: " + truncate(raw, 200));
        }
        List<GeneratedQuestion> out = new ArrayList<>(root.size());
        for (JsonNode node : root) {
            out.add(parseOne(node, subTopicCode));
        }
        return out;
    }

    private static GeneratedQuestion parseOne(JsonNode node, String subTopicCode) {
        String correctKey = textOrNull(node, "correct_choice_key");
        Variant en = new Variant(
                textOrNull(node, "stem_en"),
                parseChoices(node.get("choices_en")),
                textOrNull(node, "explanation_en"));
        Variant zh = new Variant(
                textOrNull(node, "stem_zh"),
                parseChoices(node.get("choices_zh")),
                textOrNull(node, "explanation_zh"));
        return new GeneratedQuestion(subTopicCode, correctKey, en, zh);
    }

    private static List<Choice> parseChoices(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        List<Choice> choices = new ArrayList<>(arrayNode.size());
        for (JsonNode c : arrayNode) {
            choices.add(new Choice(textOrNull(c, "key"), textOrNull(c, "text")));
        }
        return choices;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        return v.asText();
    }

    private static String stripCodeFence(String s) {
        String trimmed = s.trim();
        if (trimmed.startsWith("```")) {
            int firstNl = trimmed.indexOf('\n');
            // With newline → drop the fence line (handles ```json\n...).
            // Without newline → drop just the leading ``` so a single-line fence works.
            trimmed = (firstNl > 0) ? trimmed.substring(firstNl + 1) : trimmed.substring(3);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
