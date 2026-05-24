-- Sub-topic dimension for fine-grained mastery tracking (Phase B / Study Hub).
-- Independent table (not topics.parent_topic_id) so /topics API surface stays
-- backward-compat (still returns 8 parent rows only). Each question gets at
-- most one sub_topic_id; nullable until B3 retag migration backfills.

CREATE TABLE sub_topics (
    id              BIGSERIAL       PRIMARY KEY,
    parent_topic_id BIGINT          NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    code            VARCHAR(100)    NOT NULL UNIQUE,
    name_en         VARCHAR(500)    NOT NULL,
    name_zh         VARCHAR(500)    NOT NULL,
    description     TEXT,
    sort_order      INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sub_topics_parent ON sub_topics(parent_topic_id);

ALTER TABLE questions ADD COLUMN sub_topic_id BIGINT REFERENCES sub_topics(id);
CREATE INDEX idx_questions_sub_topic ON questions(sub_topic_id);
