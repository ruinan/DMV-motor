-- V9: Add allow_in_free_trial flag to questions
-- The free trial set is a fixed pool of questions accessible to all users (including anonymous).
-- Per docs/parameters.md free_trial_set_size = 30 questions.
-- Existing questions (from V2 seed) are marked true for the first 30 by sort order.

ALTER TABLE questions ADD COLUMN allow_in_free_trial BOOLEAN NOT NULL DEFAULT FALSE;

-- Mark the first 30 seed questions (lowest IDs) as the free trial pool.
-- In production, this set is curated manually; here we initialize from the seed order.
UPDATE questions
SET allow_in_free_trial = TRUE
WHERE id IN (
    SELECT id FROM questions ORDER BY id LIMIT 30
);

CREATE INDEX idx_questions_allow_in_free_trial ON questions(allow_in_free_trial)
    WHERE allow_in_free_trial = TRUE;
