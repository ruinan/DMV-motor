-- Phase B decision #5: a practice session can be scoped to a set of topics
-- (the "Practice these" CTA on /mistakes pre-fills the user's active-mistake
-- topics). Stored as a comma-separated list of topic ids; null = no filter
-- (the normal full personalized pool). Capped at 8 topics application-side.

ALTER TABLE practice_sessions ADD COLUMN topic_filter TEXT;
