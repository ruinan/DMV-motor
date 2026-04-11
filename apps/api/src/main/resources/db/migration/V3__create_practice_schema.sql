-- V3: Practice main flow tables
-- Tables: practice_sessions, practice_attempts, mistake_records

-- ============================================================
-- practice_sessions
-- ============================================================
CREATE TABLE practice_sessions (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          REFERENCES users(id),   -- NULL for anonymous free trial
    status          VARCHAR(50)     NOT NULL DEFAULT 'in_progress',
    entry_type      VARCHAR(50)     NOT NULL,               -- free_trial | full
    language_code   VARCHAR(10)     NOT NULL DEFAULT 'en',
    started_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMPTZ,
    last_active_at  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_practice_sessions_status
        CHECK (status IN ('in_progress', 'completed', 'abandoned')),
    CONSTRAINT chk_practice_sessions_entry_type
        CHECK (entry_type IN ('free_trial', 'full'))
);

CREATE INDEX idx_practice_sessions_user_id ON practice_sessions(user_id);
CREATE INDEX idx_practice_sessions_status  ON practice_sessions(status);

-- ============================================================
-- practice_attempts
-- ============================================================
CREATE TABLE practice_attempts (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          REFERENCES users(id),
    practice_session_id BIGINT          NOT NULL REFERENCES practice_sessions(id),
    question_id         BIGINT          NOT NULL REFERENCES questions(id),
    question_variant_id BIGINT          NOT NULL REFERENCES question_variants(id),
    entry_source        VARCHAR(50)     NOT NULL DEFAULT 'practice',  -- practice | review
    selected_choice_key VARCHAR(10)     NOT NULL,
    is_correct          BOOLEAN         NOT NULL,
    submitted_at        TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_practice_attempts_entry_source
        CHECK (entry_source IN ('practice', 'review')),
    CONSTRAINT uq_practice_attempts_session_question
        UNIQUE (practice_session_id, question_id)
);

CREATE INDEX idx_practice_attempts_session  ON practice_attempts(practice_session_id);
CREATE INDEX idx_practice_attempts_user     ON practice_attempts(user_id);
CREATE INDEX idx_practice_attempts_question ON practice_attempts(question_id);

-- ============================================================
-- mistake_records
-- ============================================================
CREATE TABLE mistake_records (
    id                BIGSERIAL       PRIMARY KEY,
    user_id           BIGINT          NOT NULL REFERENCES users(id),
    question_id       BIGINT          NOT NULL REFERENCES questions(id),
    primary_topic_id  BIGINT          NOT NULL REFERENCES topics(id),
    first_wrong_at    TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_wrong_at     TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    wrong_count       INT             NOT NULL DEFAULT 1,
    last_entry_source VARCHAR(50)     NOT NULL DEFAULT 'practice',
    is_active         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mistake_records_user_question UNIQUE (user_id, question_id),
    CONSTRAINT chk_mistake_records_wrong_count  CHECK (wrong_count > 0)
);

CREATE INDEX idx_mistake_records_user         ON mistake_records(user_id);
CREATE INDEX idx_mistake_records_user_active  ON mistake_records(user_id, is_active);
CREATE INDEX idx_mistake_records_topic        ON mistake_records(primary_topic_id);
