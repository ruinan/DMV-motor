-- V12: AI explanation cache + audit trail.
--
-- Per docs/ai-architecture.md §6 (reuse-first) + §12 (题目解释缓存) + 决策点 27.2 #1/#3:
--   * UNIQUE (user_id, question_id, language) is the cost ceiling — a free-trial
--     user re-grinding the same 30-question pool can only ever drive 30 LLM
--     calls (per language) regardless of replay count.
--   * (user_id, created_at) drives the rate-limit lookup (思考时间冷却 + per-day cap).
--   * tokens_in / tokens_out are nullable so the stub provider can omit them
--     without violating NOT NULL — Phase B (DeepSeek) will populate.
--   * model is required so cost analytics can group by provider.

CREATE TABLE ai_explanations (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id         BIGINT       NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    language            VARCHAR(10)  NOT NULL,
    selected_choice_key VARCHAR(10),
    explanation         TEXT         NOT NULL,
    model               VARCHAR(100) NOT NULL,
    tokens_in           INT,
    tokens_out          INT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ai_explanations_user_question_lang
        UNIQUE (user_id, question_id, language)
);

CREATE INDEX idx_ai_explanations_user_created_at
    ON ai_explanations (user_id, created_at DESC);
