-- Mock exam now auto-terminates when wrong_count exceeds the pass threshold
-- (e.g. 4 wrong on a 30-question / 85% pass-rate exam). The new status
-- 'ended_by_failure' distinguishes this from user-initiated 'ended_by_exit'
-- and the natural 'submitted' / 'expired' paths.

ALTER TABLE mock_attempts DROP CONSTRAINT IF EXISTS mock_attempts_status_check;

-- V5 created the constraint inline so PostgreSQL named it
-- mock_attempts_status_check. Drop both possible names to be safe.
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
    CHECK (status IN ('in_progress', 'submitted', 'ended_by_exit', 'expired', 'ended_by_failure'));
