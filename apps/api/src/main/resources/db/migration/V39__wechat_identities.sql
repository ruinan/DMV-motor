-- WeChat mini-program login identities. Maps a WeChat user (openid) to an
-- internal account, so a returning WeChat login resolves to the same account
-- (which may have been created via email/password). Email stays the universal
-- key on users; this side table keeps the existing firebase_uid auth path and
-- UserProvisioner untouched.
--
--   openid   : the user's id within OUR mini-program (per-mini-program).
--   unionid  : cross-property id, only present if an Open Platform account binds
--              several WeChat apps — nullable until/unless that exists.
CREATE TABLE wechat_identities (
    openid      VARCHAR(64)  PRIMARY KEY,
    unionid     VARCHAR(64),
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wechat_identities_user ON wechat_identities (user_id);
