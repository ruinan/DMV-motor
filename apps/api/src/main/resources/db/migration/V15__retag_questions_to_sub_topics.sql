-- Retag all 99 existing questions (V2 46 + V10 53) to one of the 16 sub-topics
-- from V14. Mapping respects each question's primary_topic_id — every sub-topic
-- assigned belongs to a sub_topics row whose parent_topic_id matches the
-- question's primary_topic_id.
--
-- Manual classification (not AI) based on stem inspection against the
-- docs/sub-topics.md frozen catalog. Coverage gaps that B4 AI Q-gen must fill:
--   LANE_SPLITTING_SHARING:  1 question
--   MANEUVERS_SWERVE_BRAKE:  1 question
--   EMERGENCIES_MECHANICAL:  0 questions
--
-- After this migration, sub_topic_id is also constrained NOT NULL so any
-- future INSERT (V16 AI-generated batch and beyond) must specify it.

-- ============================================================
-- TRAFFIC_CONTROLS
-- ============================================================
UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'TRAFFIC_REGULATORY_SIGNS')
  WHERE id IN (1, 2, 3, 4, 7, 8, 47, 48, 49, 50, 51);

UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'TRAFFIC_LANE_MARKINGS')
  WHERE id IN (5, 6, 52, 53);

-- ============================================================
-- RIGHT_OF_WAY
-- ============================================================
UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'ROW_INTERSECTIONS')
  WHERE id IN (9, 10, 11, 13, 15, 54, 55, 58);

UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'ROW_VULNERABLE_USERS')
  WHERE id IN (12, 14, 56, 57, 59);

-- ============================================================
-- SPEED_DISTANCE
-- ============================================================
UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'SPEED_FOLLOWING_DISTANCE')
  WHERE id IN (18, 21, 60);

UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'SPEED_LIMITS_PASSING')
  WHERE id IN (16, 17, 19, 20, 61, 62, 63, 64);

-- ============================================================
-- LANE_POSITION
-- ============================================================
UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'LANE_POSITION_SELECTION')
  WHERE id IN (22, 23, 24, 25, 26, 66, 67, 68, 69);

UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'LANE_SPLITTING_SHARING')
  WHERE id IN (65);

-- ============================================================
-- TURNING_MANEUVERS
-- ============================================================
UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'TURNING_CURVES')
  WHERE id IN (27, 28, 29, 30, 31, 70, 71, 72);

UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'MANEUVERS_SWERVE_BRAKE')
  WHERE id IN (73);

-- ============================================================
-- ALCOHOL_DRUGS
-- ============================================================
UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'ALCOHOL_BAC_LAW')
  WHERE id IN (32, 34, 36, 74, 75, 76, 77);

UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'DRUGS_IMPAIRMENT')
  WHERE id IN (33, 35, 37, 78);

-- ============================================================
-- SPECIAL_CONDITIONS
-- ============================================================
-- Note: SURFACES_SLIPPERY_TRACKS catalog description focuses on slippery
-- surfaces and tracks, but in absence of a third "environmental visibility"
-- sub-topic, night / fog / glare / drowsy questions are bucketed here under
-- the broader interpretation of "environmental conditions reducing safety
-- margin". EMERGENCIES_MECHANICAL stays strictly bike-mechanical and gets 0
-- existing questions — B4 must generate these.
UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'SURFACES_SLIPPERY_TRACKS')
  WHERE id IN (38, 39, 40, 41, 42, 79, 80, 81, 82, 83);

-- ============================================================
-- MOTORCYCLE_BASICS
-- ============================================================
UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'BASICS_PPE')
  WHERE id IN (43, 46, 84, 85, 86, 97);

UPDATE questions SET sub_topic_id = (SELECT id FROM sub_topics WHERE code = 'BASICS_CONTROL_INSPECTION')
  WHERE id IN (44, 45, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 98, 99);

-- ============================================================
-- Verify + enforce NOT NULL
-- ============================================================
DO $$
DECLARE
    untagged INT;
BEGIN
    SELECT COUNT(*) INTO untagged FROM questions WHERE sub_topic_id IS NULL;
    IF untagged > 0 THEN
        RAISE EXCEPTION 'V15 retag incomplete: % questions still have NULL sub_topic_id', untagged;
    END IF;
END $$;

ALTER TABLE questions ALTER COLUMN sub_topic_id SET NOT NULL;
