-- V24: reminder tasks (docs/reminder-and-readiness.md + features.md §2 + api.md §11).
--
-- Reminders are a BACKEND capability (spec rule 1): the server decides, based on
-- learning state, the single most-worthwhile task to pull the user back to. The
-- client only surfaces the in-app list. MVP channel = in-app (站内 > email).
--
-- One row per generated reminder. Lifecycle: pending → responded (the user acted
-- on or dismissed it). The generator enforces the spec's frequency rules:
--   * at most one new reminder per 24h per user (每天最多 1 次, no overflow);
--   * a type is paused once its last 3 reminders are all still unresponded
--     (连续 3 次未响应 → 暂停该类); responding to any breaks the streak.
-- priority encodes the trigger order (1 highest): resume practice > review weak
-- points > start mock.

CREATE TABLE reminder_tasks (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         VARCHAR(40)  NOT NULL,   -- resume_practice / review_weak_points / start_mock
    status       VARCHAR(20)  NOT NULL DEFAULT 'pending',  -- pending / responded
    priority     INT          NOT NULL,   -- 1 = highest
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMPTZ
);

-- Daily-cap + per-type-streak lookups, and the active-list read, are all scoped
-- by user and ordered by recency.
CREATE INDEX idx_reminder_user_created ON reminder_tasks (user_id, created_at DESC);
