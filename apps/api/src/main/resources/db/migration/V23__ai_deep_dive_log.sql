-- V23: deep-dive ("深入分析") call log — metadata only, NO explanation text.
--
-- enhance1 decision (memory §35, 2026-05-30): deep-dive history lives in the
-- client's localStorage ("clear cache 就没"); the server only records that a
-- deep-dive call happened, so it can:
--   * enforce a per-(user,question,language) depth cap (anti-abuse / cost) that
--     survives a localStorage clear — re-burning after clear still counts here;
--   * fold deep-dives into the existing daily cap + cooldown rate-limit
--     (each row is one billable LLM call).
-- The base (depth 0) explanation keeps its full-text cache in ai_explanations
-- (the cost ceiling for the common case). Deep-dive text is deliberately NOT
-- persisted here — that's the "减服务器压力" half of the decision.

CREATE TABLE ai_deep_dive_log (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id BIGINT       NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    language    VARCHAR(10)  NOT NULL,
    depth       INT          NOT NULL,   -- requested layer (1..N); informational
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Rate-limit lookup (daily cap + cooldown) mirrors ai_explanations' index.
CREATE INDEX idx_ai_deep_dive_user_created
    ON ai_deep_dive_log (user_id, created_at DESC);

-- Per-question depth-cap lookup.
CREATE INDEX idx_ai_deep_dive_user_question_lang
    ON ai_deep_dive_log (user_id, question_id, language);
