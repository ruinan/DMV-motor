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
            String                        language,
            // enhance1: 0 = base explanation; 1..N = "深入分析" layer. Providers
            // escalate the prompt for depth ≥ 1. Layers aren't fed prior text
            // (anti-hijack) — the depth just nudges "go deeper".
            int                           depth
    ) {}

    record Output(
            String  text,
            Integer tokensIn,
            Integer tokensOut
    ) {}
}
