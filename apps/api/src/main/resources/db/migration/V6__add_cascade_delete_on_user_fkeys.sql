-- V6: Add ON DELETE CASCADE to all user-owned FK constraints.
-- This ensures that deleting a user automatically removes all their data
-- (sessions, attempts, mistakes, review packs, mock attempts, access passes).

-- access_passes
ALTER TABLE access_passes
    DROP CONSTRAINT access_passes_user_id_fkey,
    ADD  CONSTRAINT access_passes_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- practice_sessions
ALTER TABLE practice_sessions
    DROP CONSTRAINT practice_sessions_user_id_fkey,
    ADD  CONSTRAINT practice_sessions_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- practice_attempts: cascade from practice_sessions and from review_tasks
ALTER TABLE practice_attempts
    DROP CONSTRAINT practice_attempts_user_id_fkey,
    ADD  CONSTRAINT practice_attempts_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE practice_attempts
    DROP CONSTRAINT practice_attempts_practice_session_id_fkey,
    ADD  CONSTRAINT practice_attempts_practice_session_id_fkey
        FOREIGN KEY (practice_session_id) REFERENCES practice_sessions(id) ON DELETE CASCADE;

ALTER TABLE practice_attempts
    DROP CONSTRAINT practice_attempts_review_task_id_fkey,
    ADD  CONSTRAINT practice_attempts_review_task_id_fkey
        FOREIGN KEY (review_task_id) REFERENCES review_tasks(id) ON DELETE CASCADE;

-- mistake_records
ALTER TABLE mistake_records
    DROP CONSTRAINT mistake_records_user_id_fkey,
    ADD  CONSTRAINT mistake_records_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- review_packs → review_tasks → review_task_questions
ALTER TABLE review_packs
    DROP CONSTRAINT review_packs_user_id_fkey,
    ADD  CONSTRAINT review_packs_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE review_tasks
    DROP CONSTRAINT review_tasks_user_id_fkey,
    ADD  CONSTRAINT review_tasks_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE review_tasks
    DROP CONSTRAINT review_tasks_review_pack_id_fkey,
    ADD  CONSTRAINT review_tasks_review_pack_id_fkey
        FOREIGN KEY (review_pack_id) REFERENCES review_packs(id) ON DELETE CASCADE;

ALTER TABLE review_task_questions
    DROP CONSTRAINT review_task_questions_review_task_id_fkey,
    ADD  CONSTRAINT review_task_questions_review_task_id_fkey
        FOREIGN KEY (review_task_id) REFERENCES review_tasks(id) ON DELETE CASCADE;

-- mock_attempts → mock_attempt_results
ALTER TABLE mock_attempts
    DROP CONSTRAINT mock_attempts_user_id_fkey,
    ADD  CONSTRAINT mock_attempts_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE mock_attempt_results
    DROP CONSTRAINT mock_attempt_results_mock_attempt_id_fkey,
    ADD  CONSTRAINT mock_attempt_results_mock_attempt_id_fkey
        FOREIGN KEY (mock_attempt_id) REFERENCES mock_attempts(id) ON DELETE CASCADE;
