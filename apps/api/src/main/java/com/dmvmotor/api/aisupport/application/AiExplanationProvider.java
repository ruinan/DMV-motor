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
            // enhance1: 0 = base explanation; 1..N = "深入分析" layer.
            int                           depth,
            // Deep-dive direction the learner tapped (example / mnemonic /
            // distractors / rule), or null for the base. A fixed preset — never
            // free user text — so the click-only anti-abuse stance holds.
            String                        aspect,
            // The thread so far (base + prior layers, the AI's OWN earlier
            // output) fed back so the next layer is progressive and doesn't
            // repeat. Server-truncated; null/blank for the base.
            String                        priorContext,
            // The exam this question belongs to, as a human label in the request
            // language (e.g. "California Class C (Car)"), so the prompt is
            // exam-aware instead of hardcoded to one license type. Resolved by
            // the service; null/blank → the provider uses a generic fallback.
            String                        examLabel
    ) {}

    record Output(
            String  text,
            Integer tokensIn,
            Integer tokensOut
    ) {}
}
