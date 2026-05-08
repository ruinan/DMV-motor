package com.dmvmotor.api.aisupport.application;

import java.util.List;
import java.util.Map;

/**
 * Pluggable backend that turns the structured question + user pick into
 * a short human-readable explanation. Phase A ships the {@link
 * com.dmvmotor.api.aisupport.infrastructure.StubAiExplanationProvider stub};
 * Phase B will add a DeepSeek-backed implementation guarded by
 * {@code app.ai.provider=deepseek}.
 *
 * <p>The interface intentionally takes only the minimal context that
 * docs/ai-architecture.md §11 allows — no user_id, no history. The cache
 * layer ({@link com.dmvmotor.api.aisupport.infrastructure.AiExplanationRepository})
 * sits in front of every call.
 */
public interface AiExplanationProvider {

    Output explain(Input in);

    /** Returned to callers (and persisted) so cost analytics can group by provider. */
    String modelName();

    record Input(
            Long                          questionId,
            String                        stem,
            List<Map<String, String>>     choices,
            String                        correctChoiceKey,
            String                        selectedChoiceKey,
            String                        staticExplanation,
            String                        language
    ) {}

    record Output(
            String  text,
            Integer tokensIn,
            Integer tokensOut
    ) {}
}
