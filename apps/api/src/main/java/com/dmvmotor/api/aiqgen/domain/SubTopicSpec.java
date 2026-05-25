package com.dmvmotor.api.aiqgen.domain;

/**
 * What the generator needs to know to draft questions for one sub-topic:
 * the bilingual identity (so generated variants stay aligned with V14 seed),
 * a one-paragraph description from {@code docs/sub-topics.md}, and a curated
 * runbook excerpt that the fact-checker uses as ground truth.
 *
 * <p>Curated excerpts (vs full handbook) keep each LLM call cheap and focused.
 * One excerpt per sub-topic is built in
 * {@link com.dmvmotor.api.aiqgen.infrastructure.RunbookExcerpts}.
 */
public record SubTopicSpec(
        String code,
        String nameEn,
        String nameZh,
        String description,
        String runbookExcerpt
) {}
