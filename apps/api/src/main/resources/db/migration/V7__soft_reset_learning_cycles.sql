-- V7: Soft reset via learning_cycle
-- Reset operation: UPDATE users SET reset_count = reset_count + 1  (O(1), no row deletes)
-- Queries filter by learning_cycle = users.reset_count to scope data to current cycle.

-- users: how many times the user has reset their learning progress
ALTER TABLE users ADD COLUMN reset_count INT NOT NULL DEFAULT 0;

-- practice_sessions: which learning cycle this session belongs to
ALTER TABLE practice_sessions ADD COLUMN learning_cycle INT NOT NULL DEFAULT 0;

-- mistake_records: which learning cycle this mistake was recorded in
-- Also update unique constraint to allow same question in different cycles
ALTER TABLE mistake_records DROP CONSTRAINT uq_mistake_records_user_question;
ALTER TABLE mistake_records ADD COLUMN learning_cycle INT NOT NULL DEFAULT 0;
ALTER TABLE mistake_records
    ADD CONSTRAINT uq_mistake_records_user_question_cycle
        UNIQUE (user_id, question_id, learning_cycle);

-- review_packs: which learning cycle this review pack belongs to
ALTER TABLE review_packs ADD COLUMN learning_cycle INT NOT NULL DEFAULT 0;

-- Composite indexes: cover (user_id, learning_cycle) filters in all user-scoped queries
CREATE INDEX idx_practice_sessions_user_cycle ON practice_sessions(user_id, learning_cycle);
CREATE INDEX idx_mistake_records_user_cycle   ON mistake_records(user_id, learning_cycle);
CREATE INDEX idx_review_packs_user_cycle      ON review_packs(user_id, learning_cycle);
