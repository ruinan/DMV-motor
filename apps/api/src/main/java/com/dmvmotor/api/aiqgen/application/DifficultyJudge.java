package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion;
import com.dmvmotor.api.aiqgen.domain.GenerationGateResult;

/**
 * Binary "hard enough" gate per design decision #2: a question fails if even
 * one distractor is obviously wrong on first read. Forces the bank toward
 * questions where all four choices look defensible (close-worded distractors).
 */
public class DifficultyJudge {

    private static final String SYSTEM =
            "You are evaluating multiple-choice question difficulty for the California M1 "
            + "motorcycle permit test. The bar is whether the question genuinely tests handbook "
            + "knowledge, NOT whether every distractor is intrinsically reasonable. "
            + "Reply STRICT JSON only: {\"pass\": true|false, \"reason\": \"short explanation\"}. "
            + "pass=true if a thoughtful but unprepared reader would need handbook knowledge or "
            + "real motorcycle experience to confidently pick the correct answer. "
            + "pass=false ONLY if the correct answer is derivable from pure common sense / general "
            + "knowledge (no handbook needed), or if the question is trivially worded. "
            + "Note: in safety-procedure questions, 'dangerous distractors' are legitimate — they "
            + "test whether the reader knows the correct procedure, not whether they can recognize "
            + "danger. Do NOT reject for that reason. "
            + "No markdown, no commentary.";

    private static final String USER_TEMPLATE = """
            Question: %s

            Choices:
            %s

            Correct answer: %s

            Does answering this question correctly require handbook knowledge or motorcycle experience?
            Reply JSON: {"pass": true|false, "reason": "..."}.
            Reject ONLY if the answer is obvious from common sense alone.
            """;

    private final DeepSeekChatClient client;

    public DifficultyJudge(DeepSeekChatClient client) {
        this.client = client;
    }

    public GenerationGateResult judge(GeneratedQuestion candidate) {
        String user = USER_TEMPLATE.formatted(
                candidate.en().stem(),
                renderChoices(candidate),
                candidate.correctChoiceKey());
        String raw = client.chat(SYSTEM, user);
        JudgeVerdict v = JudgeVerdict.parse(raw);
        return v.pass()
                ? GenerationGateResult.pass("difficulty: " + v.reason())
                : GenerationGateResult.fail("difficulty: " + v.reason());
    }

    private static String renderChoices(GeneratedQuestion q) {
        StringBuilder sb = new StringBuilder();
        for (GeneratedQuestion.Choice c : q.en().choices()) {
            sb.append("  ").append(c.key()).append(") ").append(c.text()).append('\n');
        }
        return sb.toString();
    }
}
