-- V32: per-exam subscription schema (additive, no behavior change yet).
-- See docs/design/subscription-model.md. This lays the columns; the per-exam
-- access gate + catalog UI + checkout come in follow-up changes.
--
--  - exams.scope            : 'state' (CA, WA, …) or 'national' (FAA pilot, CDL…).
--  - exams.price_cents      : per-exam price (default $5.00 = 500).
--  - exams.currency         : ISO currency (default usd).
--  - exams.prep_cycle_days  : subscription window length (default 30 = monthly).
--  - access_passes.exam_id  : which exam a pass entitles. NULL = legacy global
--                             pass (dev grant-pass / pre-V32) — treated as
--                             all-exams until per-exam access lands.

ALTER TABLE exams ADD COLUMN IF NOT EXISTS scope           VARCHAR(16)  NOT NULL DEFAULT 'state';
ALTER TABLE exams ADD COLUMN IF NOT EXISTS price_cents     INT          NOT NULL DEFAULT 500;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS currency        VARCHAR(8)   NOT NULL DEFAULT 'usd';
ALTER TABLE exams ADD COLUMN IF NOT EXISTS prep_cycle_days INT          NOT NULL DEFAULT 30;

-- State exams have a state_code; national ones won't. Existing seed (CA-M1, CA-C)
-- are state exams — already covered by the 'state' default.

ALTER TABLE access_passes ADD COLUMN IF NOT EXISTS exam_id BIGINT REFERENCES exams(id);
CREATE INDEX IF NOT EXISTS idx_access_passes_user_exam ON access_passes (user_id, exam_id);
