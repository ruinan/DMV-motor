package com.dmvmotor.api.aisupport.application;

import java.util.List;

/**
 * Turns a completed mock-exam result into a short, actionable review plan.
 * Same plug pattern as {@link AiExplanationProvider}: a stub default + a
 * DeepSeek-backed impl guarded by {@code app.ai.provider=deepseek}.
 *
 * <p>Privacy: the wire payload carries only exam content (score, wrong
 * question stems, their topics) — never the user_id or any identifier.
 */
public interface AiReviewPlanProvider {

    Output generate(Input in);

    String modelName();

    record WrongItem(
            String stem,
            String topicLabel,
            String subTopicLabel,
            String selectedChoiceKey,
            String correctChoiceKey
    ) {}

    record Input(
            int             scorePercent,
            int             correctCount,
            int             totalQuestions,
            boolean         passed,
            List<WrongItem> wrongItems,
            String          language
    ) {}

    record Output(
            String  text,
            Integer tokensIn,
            Integer tokensOut
    ) {}
}
