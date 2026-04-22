-- V11: add firebase_uid to users for third-party auth (Firebase Authentication).
-- Users are provisioned just-in-time on first call carrying a valid Firebase ID token:
-- UserIdResolver verifies the token, UserProvisioner looks up the matching row (or
-- inserts one), and returns the internal bigint id. No backfill needed — pre-V11 rows
-- in dev/test remain reachable only through legacy TestFixtures helpers that now set
-- firebase_uid explicitly.

ALTER TABLE users
    ADD COLUMN firebase_uid VARCHAR(128);

CREATE UNIQUE INDEX uq_users_firebase_uid ON users (firebase_uid);
