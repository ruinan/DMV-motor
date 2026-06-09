-- bug4: practice modes. A practice session remembers HOW it picks questions so
-- next-question calls (and resume-after-refresh) stay consistent:
--   random         - unweighted pick from the pool (the only mode free users get)
--   weak_points    - weighted toward active mistakes + uncovered key topics (paid)
--   review_learned - unweighted, restricted to already-covered topics (paid)
-- Default 'random' so existing rows and any un-specified session behave as the
-- plain free-trial flow did.
ALTER TABLE practice_sessions
    ADD COLUMN selection_mode VARCHAR(20) NOT NULL DEFAULT 'random';

ALTER TABLE practice_sessions
    ADD CONSTRAINT chk_practice_selection_mode
        CHECK (selection_mode IN ('random', 'weak_points', 'review_learned'));
