-- Timed mock exams auto-submit when the clock runs out. The new status
-- 'ended_by_timeout' distinguishes that from a clean 'submitted', the wrong-cap
-- 'ended_by_failure', and the user-initiated 'ended_by_exit'.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints
         WHERE constraint_name LIKE 'mock_attempts%status%'
    ) THEN
        EXECUTE 'ALTER TABLE mock_attempts DROP CONSTRAINT '
            || (SELECT constraint_name FROM information_schema.check_constraints
                 WHERE constraint_name LIKE 'mock_attempts%status%' LIMIT 1);
    END IF;
END $$;

ALTER TABLE mock_attempts
    ADD CONSTRAINT mock_attempts_status_check
    CHECK (status IN ('in_progress', 'submitted', 'ended_by_exit', 'expired',
                      'ended_by_failure', 'ended_by_timeout'));
