-- V26: multi-exam foundation — an "exam" = (state × license type) dimension so
-- content can be scoped per exam. CA-M1 is the only seeded exam at launch; this
-- migration makes the system multi-exam-READY without adding new content.
--
-- jOOQ note: per the codebase convention (every post-V1 column/table — V9/V21/
-- V23/V24/V25 — is accessed via dynamic DSL.field/table refs, NOT regenerated
-- code), the new exams table + exam_id columns are used the same way. No regen.

-- 1. The exam catalog (state × license type), each with its own pass standard.
CREATE TABLE exams (
    id                    BIGSERIAL    PRIMARY KEY,
    state_code            VARCHAR(8)   NOT NULL,   -- e.g. 'CA', 'TX'
    license_class         VARCHAR(16)  NOT NULL,   -- e.g. 'M1', 'C', 'CDL'
    name_en               VARCHAR(120) NOT NULL,
    name_zh               VARCHAR(120) NOT NULL,
    pass_threshold_percent INT         NOT NULL DEFAULT 85,
    status                VARCHAR(20)  NOT NULL DEFAULT 'active',
    sort_order            INT          NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_exams_state_class UNIQUE (state_code, license_class)
);

-- The one exam that exists today. Threshold 85% matches the old hardcoded
-- MockScoringPolicy.PASS_THRESHOLD.
INSERT INTO exams (state_code, license_class, name_en, name_zh, pass_threshold_percent, status, sort_order)
VALUES ('CA', 'M1', 'California M1 (Motorcycle)', '加州 M1（摩托车）', 85, 'active', 0);

-- 2. Add exam_id to content + per-user snapshot tables (nullable first for backfill).
ALTER TABLE topics            ADD COLUMN exam_id BIGINT REFERENCES exams(id);
ALTER TABLE questions         ADD COLUMN exam_id BIGINT REFERENCES exams(id);
ALTER TABLE mock_exams        ADD COLUMN exam_id BIGINT REFERENCES exams(id);
ALTER TABLE practice_sessions ADD COLUMN exam_id BIGINT REFERENCES exams(id);
ALTER TABLE mock_attempts     ADD COLUMN exam_id BIGINT REFERENCES exams(id);
ALTER TABLE users             ADD COLUMN current_exam_id BIGINT REFERENCES exams(id);

-- 3. Backfill everything that exists today to CA-M1.
UPDATE topics            SET exam_id = (SELECT id FROM exams WHERE state_code='CA' AND license_class='M1') WHERE exam_id IS NULL;
UPDATE questions         SET exam_id = (SELECT id FROM exams WHERE state_code='CA' AND license_class='M1') WHERE exam_id IS NULL;
UPDATE mock_exams        SET exam_id = (SELECT id FROM exams WHERE state_code='CA' AND license_class='M1') WHERE exam_id IS NULL;
UPDATE practice_sessions SET exam_id = (SELECT id FROM exams WHERE state_code='CA' AND license_class='M1') WHERE exam_id IS NULL;
UPDATE mock_attempts     SET exam_id = (SELECT id FROM exams WHERE state_code='CA' AND license_class='M1') WHERE exam_id IS NULL;
UPDATE users             SET current_exam_id = (SELECT id FROM exams WHERE state_code='CA' AND license_class='M1') WHERE current_exam_id IS NULL;

-- 4. Content + session/attempt always belong to an exam → NOT NULL. users.current_exam_id
--    stays nullable (new users have no exam until onboarding picks one).
ALTER TABLE topics            ALTER COLUMN exam_id SET NOT NULL;
ALTER TABLE questions         ALTER COLUMN exam_id SET NOT NULL;
ALTER TABLE mock_exams        ALTER COLUMN exam_id SET NOT NULL;
ALTER TABLE practice_sessions ALTER COLUMN exam_id SET NOT NULL;
ALTER TABLE mock_attempts     ALTER COLUMN exam_id SET NOT NULL;

-- 5. Scoping-filter indexes.
CREATE INDEX idx_questions_exam   ON questions (exam_id);
CREATE INDEX idx_topics_exam      ON topics (exam_id);
CREATE INDEX idx_mock_exams_exam  ON mock_exams (exam_id);
