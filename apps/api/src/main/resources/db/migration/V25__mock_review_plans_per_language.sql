-- V25: per-language AI review-plan cache.
--
-- Bug (2026-05-31): the post-mock AI review plan was generated once in the
-- mock's language and stored in mock_attempts.ai_review_plan (single column),
-- so switching the UI language kept showing the original-language plan.
--
-- Fix: cache the plan per (attempt, language). The mock's language is generated
-- eagerly on completion; other languages are generated lazily the first time
-- they're requested. A row with plan IS NULL is a "claim" placeholder (a
-- generation is in flight) so concurrent polls don't fire duplicate LLM calls;
-- plan set = ready. The old mock_attempts.ai_review_plan column is left in
-- place (unused) to avoid touching generated jOOQ code.

CREATE TABLE mock_review_plans (
    id              BIGSERIAL    PRIMARY KEY,
    mock_attempt_id BIGINT       NOT NULL REFERENCES mock_attempts(id) ON DELETE CASCADE,
    language        VARCHAR(10)  NOT NULL,
    plan            TEXT,                       -- NULL = claimed/generating; set = ready
    model           VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mock_review_plans_attempt_lang UNIQUE (mock_attempt_id, language)
);
