-- Phase 1: Content & Base Data Layer
-- Tables: users, access_passes, topics, questions, question_variants,
--         question_related_topics, mock_exams, mock_exam_questions

-- ============================================================
-- users
-- ============================================================
CREATE TABLE users (
    id                  BIGSERIAL       PRIMARY KEY,
    email               VARCHAR(255),
    language_preference VARCHAR(10)     NOT NULL DEFAULT 'en',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- access_passes
-- ============================================================
CREATE TABLE access_passes (
    id                      BIGSERIAL   PRIMARY KEY,
    user_id                 BIGINT      NOT NULL REFERENCES users(id),
    status                  VARCHAR(50) NOT NULL,   -- inactive | active | expired | consumed_out
    starts_at               TIMESTAMPTZ,
    expires_at              TIMESTAMPTZ,
    mock_exam_total_count   INT         NOT NULL DEFAULT 0,
    mock_exam_used_count    INT         NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_access_passes_status
        CHECK (status IN ('inactive', 'active', 'expired', 'consumed_out')),
    CONSTRAINT chk_mock_exam_counts
        CHECK (mock_exam_used_count >= 0 AND mock_exam_total_count >= 0
               AND mock_exam_used_count <= mock_exam_total_count)
);

-- ============================================================
-- topics
-- ============================================================
CREATE TABLE topics (
    id              BIGSERIAL       PRIMARY KEY,
    parent_topic_id BIGINT          REFERENCES topics(id),
    code            VARCHAR(100)    NOT NULL UNIQUE,
    name_en         VARCHAR(500)    NOT NULL,
    name_zh         VARCHAR(500)    NOT NULL,
    is_key_topic    BOOLEAN         NOT NULL DEFAULT FALSE,
    risk_level      VARCHAR(50),
    sort_order      INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- questions
-- ============================================================
CREATE TABLE questions (
    id                  BIGSERIAL   PRIMARY KEY,
    primary_topic_id    BIGINT      NOT NULL REFERENCES topics(id),
    correct_choice_key  VARCHAR(10) NOT NULL,
    learning_level      VARCHAR(50),
    difficulty_level    VARCHAR(50),
    risk_flag           BOOLEAN     NOT NULL DEFAULT FALSE,
    is_key_coverage     BOOLEAN     NOT NULL DEFAULT FALSE,
    allow_in_practice   BOOLEAN     NOT NULL DEFAULT TRUE,
    allow_in_review     BOOLEAN     NOT NULL DEFAULT TRUE,
    allow_in_mock_exam  BOOLEAN     NOT NULL DEFAULT TRUE,
    status              VARCHAR(50) NOT NULL DEFAULT 'active',  -- active | inactive | draft
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_questions_status
        CHECK (status IN ('active', 'inactive', 'draft'))
);

-- ============================================================
-- question_variants
-- choices_payload format: [{"key": "A", "text": "..."}, ...]
-- ============================================================
CREATE TABLE question_variants (
    id              BIGSERIAL   PRIMARY KEY,
    question_id     BIGINT      NOT NULL REFERENCES questions(id),
    language_code   VARCHAR(10) NOT NULL,
    stem_text       TEXT        NOT NULL,
    choices_payload JSONB       NOT NULL,
    explanation_text TEXT,
    status          VARCHAR(50) NOT NULL DEFAULT 'active',  -- active | inactive | draft
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_question_variants_question_lang UNIQUE (question_id, language_code),
    CONSTRAINT chk_question_variants_status
        CHECK (status IN ('active', 'inactive', 'draft'))
);

-- ============================================================
-- question_related_topics
-- ============================================================
CREATE TABLE question_related_topics (
    id            BIGSERIAL    PRIMARY KEY,
    question_id   BIGINT       NOT NULL REFERENCES questions(id),
    topic_id      BIGINT       NOT NULL REFERENCES topics(id),
    relation_type VARCHAR(100)
);

-- ============================================================
-- mock_exams  (exam templates)
-- ============================================================
CREATE TABLE mock_exams (
    id             BIGSERIAL   PRIMARY KEY,
    code           VARCHAR(100) NOT NULL UNIQUE,
    version        INT         NOT NULL DEFAULT 1,
    status         VARCHAR(50) NOT NULL DEFAULT 'active',  -- active | inactive | draft
    question_count INT         NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_mock_exams_status
        CHECK (status IN ('active', 'inactive', 'draft')),
    CONSTRAINT chk_mock_exams_question_count
        CHECK (question_count > 0)
);

-- ============================================================
-- mock_exam_questions
-- ============================================================
CREATE TABLE mock_exam_questions (
    id           BIGSERIAL PRIMARY KEY,
    mock_exam_id BIGINT    NOT NULL REFERENCES mock_exams(id),
    question_id  BIGINT    NOT NULL REFERENCES questions(id),
    sort_order   INT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_mock_exam_questions UNIQUE (mock_exam_id, question_id)
);
