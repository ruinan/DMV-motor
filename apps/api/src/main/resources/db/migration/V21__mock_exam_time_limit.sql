-- Mock exams are now timed. time_limit_seconds drives a client countdown and a
-- server-enforced auto-submit; default is 60 seconds per question. Existing
-- templates get their limit from question_count. time_used_seconds records how
-- long a finished attempt actually took (for history display).

ALTER TABLE mock_exams ADD COLUMN time_limit_seconds INT NOT NULL DEFAULT 1800;
UPDATE mock_exams SET time_limit_seconds = question_count * 60;

ALTER TABLE mock_attempts ADD COLUMN time_used_seconds INT;
