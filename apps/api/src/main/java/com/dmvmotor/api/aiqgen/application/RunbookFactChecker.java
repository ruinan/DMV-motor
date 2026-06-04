package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion;
import com.dmvmotor.api.aiqgen.domain.GenerationGateResult;
import com.dmvmotor.api.aiqgen.domain.SubTopicSpec;

/**
 * Anchors the correct answer to the provided handbook excerpt. The judge is
 * given a curated handbook excerpt for the sub-topic and asked whether the
 * candidate's marked-correct answer matches what the handbook says. If model
 * opinion disagrees with handbook, handbook wins.
 *
 * <p>This is the last gate before persistence — its bar is the highest because
 * a wrong "correct answer" is the most user-visible failure mode.
 */
public class RunbookFactChecker {

    private static final String SYSTEM =
            "You are a fact-checker for DMV written knowledge test questions. "
            + "You are given a candidate question + the marked correct answer + an excerpt from "
            + "the official driver handbook. "
            + "Decide whether the marked correct answer is supported by the handbook excerpt. "
            + "Use the handbook as the SOLE authority — if you personally believe a different "
            + "answer is correct, defer to the handbook. "
            + "Reply STRICT JSON only: {\"pass\": true|false, \"reason\": \"short explanation\"}. "
            + "No markdown, no commentary.";

    private static final String USER_TEMPLATE = """
            DMV Handbook excerpt:
            ---
            %s
            ---

            Candidate question (EN): %s
            Choices:
            %s
            Marked correct answer: %s
            Static explanation: %s

            Is the marked correct answer supported by the handbook excerpt?
            Reply JSON: {"pass": true|false, "reason": "..."}.
            """;

    private final DeepSeekChatClient client;

    public RunbookFactChecker(DeepSeekChatClient client) {
        this.client = client;
    }

    public GenerationGateResult judge(GeneratedQuestion candidate, SubTopicSpec spec) {
        String user = USER_TEMPLATE.formatted(
                spec.runbookExcerpt(),
                candidate.en().stem(),
                renderChoices(candidate),
                candidate.correctChoiceKey(),
                candidate.en().explanation());
        String raw = client.chat(SYSTEM, user);
        JudgeVerdict v = JudgeVerdict.parse(raw);
        return v.pass()
                ? GenerationGateResult.pass("fact-check: " + v.reason())
                : GenerationGateResult.fail("fact-check: " + v.reason());
    }

    private static String renderChoices(GeneratedQuestion q) {
        StringBuilder sb = new StringBuilder();
        for (GeneratedQuestion.Choice c : q.en().choices()) {
            sb.append("  ").append(c.key()).append(") ").append(c.text()).append('\n');
        }
        return sb.toString();
    }
}
