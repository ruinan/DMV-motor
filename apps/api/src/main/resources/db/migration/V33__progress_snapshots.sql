-- Progress snapshots (paid "remote backup of progress").
-- A point-in-time, server-stored capture of a user's progress FOR ONE EXAM:
-- the headline readiness/completion scores plus the recent-mock and
-- practice summaries the user cares about (近3模考 / 近N练习 / 总进度评分).
--
-- The live progress already lives across practice_*/mock_*/mistake_* tables;
-- this is a durable, restorable record that survives a learning-cycle reset or
-- a new device, and is a paid perk. Creating a snapshot is gated on an active
-- pass; viewing past snapshots is always allowed (kept after a downgrade).
CREATE TABLE progress_snapshots (
    id                         BIGSERIAL   PRIMARY KEY,
    user_id                    BIGINT      NOT NULL REFERENCES users(id),
    exam_id                    BIGINT      NOT NULL REFERENCES exams(id),
    readiness_score            INT         NOT NULL,
    completion_score           INT         NOT NULL,
    mock_total_attempts        INT         NOT NULL DEFAULT 0,
    mock_best_score_percent    INT,                      -- null when no scored mocks
    mock_recent3_avg_percent   INT,                      -- null when no scored mocks
    practice_total_sessions    INT         NOT NULL DEFAULT 0,
    practice_accuracy_percent  INT         NOT NULL DEFAULT 0,
    active_mistakes_count      INT         NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_progress_snapshots_user_exam
    ON progress_snapshots (user_id, exam_id, created_at DESC);
