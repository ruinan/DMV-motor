-- Seed 16 sub-topics (2 per parent topic) per docs/sub-topics.md frozen catalog.
-- sort_order uses parent_sort * 10 + sub_index so sub-topics stay grouped under
-- their parent in default ordering.

-- TRAFFIC_CONTROLS (parent sort_order=10)
INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'TRAFFIC_REGULATORY_SIGNS',
       'Regulatory Signs (Stop, Yield, Speed)',
       '法规标志（停车、让行、限速）',
       'Stop signs, yield signs, posted speed limits, no-turn restrictions, one-way signs. Includes the blind-intersection stop-then-edge-then-stop-again procedure from the motorcycle handbook.',
       101
FROM topics WHERE code = 'TRAFFIC_CONTROLS';

INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'TRAFFIC_LANE_MARKINGS',
       'Lane Markings & Pavement Signals',
       '车道标线与路面信号',
       'Solid and dashed white/yellow lines, two-way left-turn center lanes, HOV markings, crosswalks, stop bars, curb-color parking rules including the 45-90 degree motorcycle parking angle.',
       102
FROM topics WHERE code = 'TRAFFIC_CONTROLS';

-- RIGHT_OF_WAY (parent sort_order=20)
INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'ROW_INTERSECTIONS',
       'Right of Way at Intersections',
       '路口通行权',
       'Who proceeds first at four-way stops, uncontrolled intersections, T-intersections, left turns against oncoming traffic. The motorcycle handbook flags driver-entering-rider-ROW as the #1 collision cause.',
       201
FROM topics WHERE code = 'RIGHT_OF_WAY';

INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'ROW_VULNERABLE_USERS',
       'Pedestrians, Bicycles, Emergency Vehicles',
       '行人、自行车、应急车辆',
       'Yielding to pedestrians at crosswalks, sharing the road with bicyclists, responding to emergency vehicles. Plus the motorcycle-specific door-zone and pedestrian-from-between-cars hazards when passing parked vehicles.',
       202
FROM topics WHERE code = 'RIGHT_OF_WAY';

-- SPEED_DISTANCE (parent sort_order=30)
INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'SPEED_FOLLOWING_DISTANCE',
       'Following Distance (2-second rule)',
       '跟车距离（2 秒规则）',
       'The 2-second following rule and its extension in adverse conditions. Responding when followed too closely: move over, change lanes, or let the tailgater pass.',
       301
FROM topics WHERE code = 'SPEED_DISTANCE';

INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'SPEED_LIMITS_PASSING',
       'Speed Limits & Passing',
       '限速与超车',
       'Posted vs basic speed law, legal passing zones, completing a pass quickly and returning to lane, being passed (don''t speed up), freeway merging.',
       302
FROM topics WHERE code = 'SPEED_DISTANCE';

-- LANE_POSITION (parent sort_order=40)
INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'LANE_POSITION_SELECTION',
       'Three-Position Lane Selection',
       '三车道位置选择',
       'Choosing left, center, or right portion of the lane based on traffic flow, visibility, road conditions, and surface hazards. Motorcycle-specific skill not present in passenger-vehicle exams.',
       401
FROM topics WHERE code = 'LANE_POSITION';

INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'LANE_SPLITTING_SHARING',
       'Lane Splitting & Sharing (CA-specific)',
       '车道分流与共享（加州特有）',
       'California''s lane-splitting rules: legal but requires caution, reasonable speed differentials, avoid splitting near intersections or large vehicles. Lane sharing between motorcycles.',
       402
FROM topics WHERE code = 'LANE_POSITION';

-- TURNING_MANEUVERS (parent sort_order=50)
INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'TURNING_CURVES',
       'Cornering (Constant / Decreasing / Widening Curves)',
       '过弯（等弯/收弯/扩弯）',
       'Identifying curve types, slowing before entry vs accelerating through, outside-inside-outside lane line, countersteering basics.',
       501
FROM topics WHERE code = 'TURNING_MANEUVERS';

INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'MANEUVERS_SWERVE_BRAKE',
       'Swerving & Emergency Braking',
       '紧急规避与制动',
       'Separating swerve from brake (never both at once, especially front brake). Swerve-then-brake vs brake-then-swerve choice. Both-brake straight-line stopping and threshold braking.',
       502
FROM topics WHERE code = 'TURNING_MANEUVERS';

-- ALCOHOL_DRUGS (parent sort_order=60)
INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'ALCOHOL_BAC_LAW',
       'BAC Limits & DUI Consequences',
       'BAC 限值与酒驾后果',
       'California BAC limits (0.08% adult, 0.04% commercial, 0.01% under-21), DUI penalties (suspension, fines, jail), implied consent, zero-tolerance for minors.',
       601
FROM topics WHERE code = 'ALCOHOL_DRUGS';

INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'DRUGS_IMPAIRMENT',
       'Drug Impairment & Risk Minimization',
       '药物影响与风险防范',
       'Drug impairment beyond alcohol (prescription, OTC, cannabis), minimize-the-risks guidance, motorcyclists'' heightened vulnerability to even small amounts of impairment.',
       602
FROM topics WHERE code = 'ALCOHOL_DRUGS';

-- SPECIAL_CONDITIONS (parent sort_order=70)
INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'SURFACES_SLIPPERY_TRACKS',
       'Slippery Surfaces, Tracks & Pavement Seams',
       '滑面、铁轨与路面接缝',
       'Handling wet roads, leaves, gravel, metal grates, painted lines, oil spots. Crossing railroad and trolley tracks at as close to 90 degrees as practical. Riding over pavement seams.',
       701
FROM topics WHERE code = 'SPECIAL_CONDITIONS';

INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'EMERGENCIES_MECHANICAL',
       'Mechanical Emergencies (Tire/Throttle/Chain/Engine)',
       '机械应急（爆胎/油门卡死/链条/引擎死火）',
       'Tire failure (don''t grab brake on flat), stuck throttle (clutch + kill switch), chain breakage, engine seizure, safely coasting off the road to the shoulder.',
       702
FROM topics WHERE code = 'SPECIAL_CONDITIONS';

-- MOTORCYCLE_BASICS (parent sort_order=80)
INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'BASICS_PPE',
       'Protective Gear (Helmet, Eye, Clothing)',
       '防护装备（头盔、眼脸、服装）',
       'U.S. DOT helmet requirement and verification, helmet types and trade-offs, face/eye protection hierarchy (face shield > goggles > glasses), proper clothing (jacket, long pants, over-the-ankle boots, gloves).',
       801
FROM topics WHERE code = 'MOTORCYCLE_BASICS';

INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order)
SELECT id, 'BASICS_CONTROL_INSPECTION',
       'Pre-Ride Inspection & Basic Controls',
       '行前检查与基本操控',
       'Pre-trip inspection checklist (tires, controls, lights, oil, chassis, stand), control identification (clutch, throttle, brakes, gear shift), body position (arms relaxed, knees against tank, feet on pegs), smooth braking with both brakes.',
       802
FROM topics WHERE code = 'MOTORCYCLE_BASICS';
