-- ============================================================
-- Mock Exam Attempt Schema
-- ============================================================

-- mock_attempts: one per user per mock session
CREATE TABLE mock_attempts (
    id                  BIGSERIAL   PRIMARY KEY,
    user_id             BIGINT      NOT NULL REFERENCES users(id),
    mock_exam_id        BIGINT      NOT NULL REFERENCES mock_exams(id),
    language_code       VARCHAR(10) NOT NULL DEFAULT 'en',
    status              VARCHAR(30) NOT NULL DEFAULT 'in_progress'
                            CHECK (status IN ('in_progress', 'submitted', 'ended_by_exit', 'expired')),
    score_percent       INT,
    correct_count       INT,
    wrong_count         INT,
    answered_count      INT         NOT NULL DEFAULT 0,
    quota_consumed      BOOLEAN     NOT NULL DEFAULT false,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- mock_attempt_results: one row per question in the attempt
-- Answers are persisted incrementally; no correctness returned until submit
CREATE TABLE mock_attempt_results (
    id                  BIGSERIAL   PRIMARY KEY,
    mock_attempt_id     BIGINT      NOT NULL REFERENCES mock_attempts(id),
    question_id         BIGINT      NOT NULL REFERENCES questions(id),
    question_variant_id BIGINT      NOT NULL REFERENCES question_variants(id),
    selected_choice_key VARCHAR(10) NOT NULL,
    is_correct          BOOLEAN,                    -- filled in at submit time
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (mock_attempt_id, question_id)            -- allows upsert for retry
);
