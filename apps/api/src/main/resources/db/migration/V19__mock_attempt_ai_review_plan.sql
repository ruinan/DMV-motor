-- Cache the post-exam AI review plan on the attempt itself. The attempt is
-- immutable once completed, so one plan per attempt is the natural cache key
-- (no separate table needed). Null until the user requests a plan; populated
-- once and reused on subsequent fetches (no repeat DeepSeek cost).

ALTER TABLE mock_attempts ADD COLUMN ai_review_plan       TEXT;
ALTER TABLE mock_attempts ADD COLUMN ai_review_plan_model VARCHAR(100);
