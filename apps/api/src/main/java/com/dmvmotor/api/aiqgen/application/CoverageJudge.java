package com.dmvmotor.api.aiqgen.application;

import com.dmvmotor.api.aiqgen.domain.GeneratedQuestion;
import com.dmvmotor.api.aiqgen.domain.GenerationGateResult;
import com.dmvmotor.api.aiqgen.domain.SubTopicSpec;

/**
 * Asks DeepSeek: does this candidate actually test the named sub-topic?
 * Rejects questions that drift onto adjacent topics (e.g. a "speed limits"
 * question slipped into the LANE_SPLITTING_SHARING batch).
 */
public class CoverageJudge {

    private static final String SYSTEM =
            "You are an exam content reviewer. Given a sub-topic description and a candidate question, "
            + "decide whether the question primarily tests that sub-topic. "
            + "Reply STRICT JSON only: {\"pass\": true|false, \"reason\": \"short explanation\"}. "
            + "No markdown, no commentary.";

    private static final String USER_TEMPLATE = """
            Sub-topic: %s (%s)
            Description: %s

            Candidate question (EN): %s
            Choices (EN): %s
            Correct answer: %s

            Does this candidate primarily test the sub-topic above?
            Reply JSON: {"pass": true|false, "reason": "..."}
            """;

    private final DeepSeekChatClient client;

    public CoverageJudge(DeepSeekChatClient client) {
        this.client = client;
    }

    public GenerationGateResult judge(GeneratedQuestion candidate, SubTopicSpec spec) {
        String user = USER_TEMPLATE.formatted(
                spec.code(),
                spec.nameEn(),
                spec.description(),
                candidate.en().stem(),
                renderChoices(candidate),
                candidate.correctChoiceKey());
        String raw = client.chat(SYSTEM, user);
        JudgeVerdict v = JudgeVerdict.parse(raw);
        return v.pass()
                ? GenerationGateResult.pass("coverage: " + v.reason())
                : GenerationGateResult.fail("coverage: " + v.reason());
    }

    private static String renderChoices(GeneratedQuestion q) {
        StringBuilder sb = new StringBuilder();
        for (GeneratedQuestion.Choice c : q.en().choices()) {
            sb.append(c.key()).append(") ").append(c.text()).append("; ");
        }
        return sb.toString();
    }
}
