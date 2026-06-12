-- A user has "opened" an exam at the free tier (clicked Free trial). This is a
-- UX marker only: free-trial practice is open to everyone server-side, so this
-- grants nothing beyond the free tier — it just keeps the exam unlocked (shown
-- as "Free", not "Locked") in the switcher once opened, even before any practice.
-- Paid access stays separate and authoritative in access_passes.
--
-- Why a marker and not "derive from practice activity": the OPEN action must
-- persist immediately. A user who taps Free trial but switches away before
-- answering must still see the exam as Free, not re-Locked.

CREATE TABLE exam_free_unlocks (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id     BIGINT      NOT NULL REFERENCES exams(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_exam_free_unlocks UNIQUE (user_id, exam_id)
);
