package com.dmvmotor.api.aiqgen.domain;

import java.util.List;

/**
 * A candidate question produced by the AI generator, before any gates have
 * inspected it. Carries both language variants so we never need a separate
 * round-trip to translate.
 *
 * <p>Field naming follows the SQL seed format so {@link com.dmvmotor.api.aiqgen.application.GenerationOrchestrator}
 * can hand a passing list straight to the V16 SQL emitter.
 */
public record GeneratedQuestion(
        String subTopicCode,
        String correctChoiceKey,
        Variant en,
        Variant zh
) {

    public record Variant(
            String stem,
            List<Choice> choices,
            String explanation
    ) {}

    public record Choice(
            String key,
            String text
    ) {}
}
