-- V8: Add learning_cycle to mock_attempts
-- Mock exam results are now cycle-scoped: a passing score in a previous
-- cycle does not satisfy the readiness gate in the current cycle.

ALTER TABLE mock_attempts ADD COLUMN learning_cycle INT NOT NULL DEFAULT 0;

CREATE INDEX idx_mock_attempts_user_cycle ON mock_attempts(user_id, learning_cycle);
