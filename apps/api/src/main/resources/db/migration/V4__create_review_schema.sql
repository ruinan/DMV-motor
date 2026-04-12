-- ============================================================
-- Review Schema
-- ============================================================

-- review_packs: one per user per review cycle
-- Generated from active mistake_records when user requests a review
CREATE TABLE review_packs (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'active'
                    CHECK (status IN ('active', 'completed')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- review_tasks: one task per topic within a review pack
CREATE TABLE review_tasks (
    id                    BIGSERIAL   PRIMARY KEY,
    review_pack_id        BIGINT      NOT NULL REFERENCES review_packs(id),
    user_id               BIGINT      NOT NULL REFERENCES users(id),
    topic_id              BIGINT      NOT NULL REFERENCES topics(id),
    task_type             VARCHAR(50) NOT NULL DEFAULT 'same_topic_retry',
    status                VARCHAR(20) NOT NULL DEFAULT 'pending'
                              CHECK (status IN ('pending', 'in_progress', 'completed')),
    priority              INT         NOT NULL DEFAULT 50,
    target_question_count INT         NOT NULL DEFAULT 0,
    completed_question_count INT      NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- review_task_questions: questions assigned to each task
-- Derived from mistake_records at pack generation time
CREATE TABLE review_task_questions (
    id             BIGSERIAL PRIMARY KEY,
    review_task_id BIGINT    NOT NULL REFERENCES review_tasks(id),
    question_id    BIGINT    NOT NULL REFERENCES questions(id),
    is_answered    BOOLEAN   NOT NULL DEFAULT false,
    is_correct     BOOLEAN,
    UNIQUE (review_task_id, question_id)
);

-- Allow practice_attempts to link to either a practice session or a review task
ALTER TABLE practice_attempts
    ALTER COLUMN practice_session_id DROP NOT NULL,
    ADD COLUMN review_task_id BIGINT REFERENCES review_tasks(id);

ALTER TABLE practice_attempts
    ADD CONSTRAINT chk_attempt_source
        CHECK (practice_session_id IS NOT NULL OR review_task_id IS NOT NULL);

ALTER TABLE practice_attempts
    DROP CONSTRAINT IF EXISTS practice_attempts_practice_session_id_question_id_key;

ALTER TABLE practice_attempts
    ADD CONSTRAINT uq_session_question
        UNIQUE (practice_session_id, question_id);

ALTER TABLE practice_attempts
    ADD CONSTRAINT uq_task_question
        UNIQUE (review_task_id, question_id);
