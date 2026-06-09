-- bug1: single-slot, restorable progress backup (paid cloud-save).
--
-- Supersedes the V33 append-list progress_snapshots: the user wants ONE latest
-- backup per (user, exam), not a growing history. Source of truth stays the
-- SERVER (practice_*/mock_*/mistake_* tables) — this snapshot is server-COMPUTED
-- from that authoritative data, never an unvalidated client upload, so progress
-- can't be forged via the backup/restore service. It's a durable copy a client
-- (web now, mini-program / app later) downloads to rehydrate after the local
-- cache is deleted or on a new platform, and the basis for a hardened restore.
--
-- payload holds the full restorable snapshot (the active-mistake list etc.) as
-- JSON; the summary columns mirror the dashboard for cheap display without
-- parsing. content_hash lets sync no-op when nothing changed (incremental — we
-- don't rewrite the row on every call). UNIQUE(user_id, exam_id) = single slot;
-- writes upsert.
CREATE TABLE progress_backups (
    id                         BIGSERIAL   PRIMARY KEY,
    user_id                    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id                    BIGINT      NOT NULL REFERENCES exams(id),
    learning_cycle             INT         NOT NULL DEFAULT 0,
    readiness_score            INT         NOT NULL DEFAULT 0,
    completion_score           INT         NOT NULL DEFAULT 0,
    mock_total_attempts        INT         NOT NULL DEFAULT 0,
    mock_best_score_percent    INT,                      -- null when no scored mocks
    mock_recent3_avg_percent   INT,                      -- null when no scored mocks
    practice_total_sessions    INT         NOT NULL DEFAULT 0,
    practice_accuracy_percent  INT         NOT NULL DEFAULT 0,
    active_mistakes_count      INT         NOT NULL DEFAULT 0,
    payload                    JSONB       NOT NULL,      -- full restorable snapshot
    content_hash               TEXT        NOT NULL,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    restored_at                TIMESTAMPTZ,               -- last restore; throttles the restore endpoint
    CONSTRAINT uq_progress_backups_user_exam UNIQUE (user_id, exam_id)
);

-- The V33 append-list is retired by this single-slot model. Drop it (it was
-- never deployed to prod — prod is at V32) so we don't keep a dead, unbounded
-- table around; the periodic compaction job operates on progress_backups only.
DROP TABLE IF EXISTS progress_snapshots;
