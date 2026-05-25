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
            "You are evaluating multiple-choice question difficulty. "
            + "Look at all 4 choices for a single question and judge whether any one of them is "
            + "OBVIOUSLY wrong at first read — meaning a casual reader would dismiss it without thinking. "
            + "A good hard question has all 4 choices look defensible. "
            + "Reply STRICT JSON only: {\"pass\": true|false, \"reason\": \"short explanation\"}. "
            + "pass=true means NO distractor is obviously wrong. "
            + "pass=false means at least one choice is too easy to dismiss. "
            + "No markdown, no commentary.";

    private static final String USER_TEMPLATE = """
            Question: %s

            Choices:
            %s

            Correct answer: %s

            Is at least one distractor obviously wrong on first read?
            Reply JSON: {"pass": true|false, "reason": "..."}.
            Remember: pass=true ONLY if all distractors look defensible.
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
