-- Activation / redemption codes: a code grants the redeeming user an access
-- pass for an exam — an alternative to paid checkout (gift, launch promo, or
-- offline activation). Keeps the same pass shape the dev grant / future Stripe
-- checkout produce, so the rest of the access gate is unchanged.
--
--  - exam_id NULL          : redeem against the user's current exam.
--  - max_redemptions       : how many DISTINCT users may redeem the code.
--  - code_redemptions      : one row per (code, user) — a user can't redeem the
--                            same code twice, and it links to the granted pass.

CREATE TABLE redemption_codes (
    id                BIGSERIAL   PRIMARY KEY,
    code              VARCHAR(64) NOT NULL,
    exam_id           BIGINT      REFERENCES exams(id),       -- NULL = current exam
    duration_days     INT         NOT NULL DEFAULT 30,
    mock_quota        INT         NOT NULL DEFAULT 5,
    max_redemptions   INT         NOT NULL DEFAULT 1,
    redemption_count  INT         NOT NULL DEFAULT 0,
    status            VARCHAR(16) NOT NULL DEFAULT 'active',  -- active | disabled
    expires_at        TIMESTAMPTZ,                            -- NULL = never expires
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_redemption_codes_status
        CHECK (status IN ('active', 'disabled')),
    CONSTRAINT chk_redemption_counts
        CHECK (redemption_count >= 0 AND max_redemptions >= 1
               AND redemption_count <= max_redemptions)
);

-- Codes are matched case-insensitively, so uniqueness is on the upper form.
CREATE UNIQUE INDEX uq_redemption_codes_code ON redemption_codes (UPPER(code));

CREATE TABLE code_redemptions (
    id            BIGSERIAL   PRIMARY KEY,
    code_id       BIGINT      NOT NULL REFERENCES redemption_codes(id),
    user_id       BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id       BIGINT      NOT NULL REFERENCES exams(id),
    pass_id       BIGINT      NOT NULL REFERENCES access_passes(id),
    redeemed_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_code_redemptions_code_user UNIQUE (code_id, user_id)
);

-- Seed launch / founder activation codes (one per current exam). max_redemptions
-- high so they double as a launch promo; disable or expire later from the DB.
INSERT INTO redemption_codes (code, exam_id, duration_days, mock_quota, max_redemptions, status)
VALUES
  ('DMVPREP-M1-FOUNDER-K7Q2',
   (SELECT id FROM exams WHERE state_code = 'CA' AND license_class = 'M1'),
   30, 5, 1000, 'active'),
  ('DMVPREP-CAC-FOUNDER-R3W8',
   (SELECT id FROM exams WHERE state_code = 'CA' AND license_class = 'C'),
   30, 5, 1000, 'active');
