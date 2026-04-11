-- V2: Seed fake content for development
-- 8 topics, 46 questions (1 mock exam worth), en + zh variants
-- NOTE: This is dev placeholder data. Real M1 questions will be AI-generated later.

DO $$
DECLARE
    -- topic IDs
    t_traffic   BIGINT;
    t_row       BIGINT;
    t_speed     BIGINT;
    t_lane      BIGINT;
    t_turn      BIGINT;
    t_alcohol   BIGINT;
    t_special   BIGINT;
    t_moto      BIGINT;

    -- question IDs (q1..q46)
    q1  BIGINT; q2  BIGINT; q3  BIGINT; q4  BIGINT; q5  BIGINT;
    q6  BIGINT; q7  BIGINT; q8  BIGINT; q9  BIGINT; q10 BIGINT;
    q11 BIGINT; q12 BIGINT; q13 BIGINT; q14 BIGINT; q15 BIGINT;
    q16 BIGINT; q17 BIGINT; q18 BIGINT; q19 BIGINT; q20 BIGINT;
    q21 BIGINT; q22 BIGINT; q23 BIGINT; q24 BIGINT; q25 BIGINT;
    q26 BIGINT; q27 BIGINT; q28 BIGINT; q29 BIGINT; q30 BIGINT;
    q31 BIGINT; q32 BIGINT; q33 BIGINT; q34 BIGINT; q35 BIGINT;
    q36 BIGINT; q37 BIGINT; q38 BIGINT; q39 BIGINT; q40 BIGINT;
    q41 BIGINT; q42 BIGINT; q43 BIGINT; q44 BIGINT; q45 BIGINT;
    q46 BIGINT;

    -- mock exam
    exam1 BIGINT;

BEGIN

-- ============================================================
-- TOPICS
-- ============================================================
INSERT INTO topics (code, name_en, name_zh, is_key_topic, risk_level, sort_order)
VALUES ('TRAFFIC_CONTROLS', 'Traffic Signs & Signals', '交通标志与信号', TRUE, 'high', 10)
RETURNING id INTO t_traffic;

INSERT INTO topics (code, name_en, name_zh, is_key_topic, risk_level, sort_order)
VALUES ('RIGHT_OF_WAY', 'Right of Way', '通行权', TRUE, 'high', 20)
RETURNING id INTO t_row;

INSERT INTO topics (code, name_en, name_zh, is_key_topic, risk_level, sort_order)
VALUES ('SPEED_DISTANCE', 'Speed & Following Distance', '速度与跟车距离', TRUE, 'high', 30)
RETURNING id INTO t_speed;

INSERT INTO topics (code, name_en, name_zh, is_key_topic, risk_level, sort_order)
VALUES ('LANE_POSITION', 'Lane Use & Positioning', '车道使用与定位', FALSE, 'medium', 40)
RETURNING id INTO t_lane;

INSERT INTO topics (code, name_en, name_zh, is_key_topic, risk_level, sort_order)
VALUES ('TURNING_MANEUVERS', 'Turning & Maneuvers', '转弯与机动', FALSE, 'medium', 50)
RETURNING id INTO t_turn;

INSERT INTO topics (code, name_en, name_zh, is_key_topic, risk_level, sort_order)
VALUES ('ALCOHOL_DRUGS', 'Alcohol & Drugs', '酒精与药物', TRUE, 'high', 60)
RETURNING id INTO t_alcohol;

INSERT INTO topics (code, name_en, name_zh, is_key_topic, risk_level, sort_order)
VALUES ('SPECIAL_CONDITIONS', 'Special Driving Conditions', '特殊驾驶条件', FALSE, 'medium', 70)
RETURNING id INTO t_special;

INSERT INTO topics (code, name_en, name_zh, is_key_topic, risk_level, sort_order)
VALUES ('MOTORCYCLE_BASICS', 'Motorcycle Basics', '摩托车基础', TRUE, 'high', 80)
RETURNING id INTO t_moto;

-- ============================================================
-- QUESTIONS — TRAFFIC_CONTROLS (q1–q8)
-- ============================================================
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_traffic, 'C', TRUE, TRUE) RETURNING id INTO q1;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage)
VALUES (t_traffic, 'A', TRUE) RETURNING id INTO q2;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_traffic, 'B') RETURNING id INTO q3;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_traffic, 'A') RETURNING id INTO q4;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage)
VALUES (t_traffic, 'C', TRUE) RETURNING id INTO q5;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_traffic, 'B') RETURNING id INTO q6;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_traffic, 'A') RETURNING id INTO q7;

INSERT INTO questions (primary_topic_id, correct_choice_key, risk_flag)
VALUES (t_traffic, 'C', TRUE) RETURNING id INTO q8;

-- ============================================================
-- QUESTIONS — RIGHT_OF_WAY (q9–q15)
-- ============================================================
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_row, 'B', TRUE, TRUE) RETURNING id INTO q9;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage)
VALUES (t_row, 'C', TRUE) RETURNING id INTO q10;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_row, 'A') RETURNING id INTO q11;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage)
VALUES (t_row, 'B', TRUE) RETURNING id INTO q12;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_row, 'C') RETURNING id INTO q13;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_row, 'A') RETURNING id INTO q14;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_row, 'B') RETURNING id INTO q15;

-- ============================================================
-- QUESTIONS — SPEED_DISTANCE (q16–q21)
-- ============================================================
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_speed, 'C', TRUE, TRUE) RETURNING id INTO q16;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage)
VALUES (t_speed, 'A', TRUE) RETURNING id INTO q17;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_speed, 'B') RETURNING id INTO q18;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_speed, 'C') RETURNING id INTO q19;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_speed, 'A') RETURNING id INTO q20;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_speed, 'B') RETURNING id INTO q21;

-- ============================================================
-- QUESTIONS — LANE_POSITION (q22–q26)
-- ============================================================
INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_lane, 'A') RETURNING id INTO q22;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage)
VALUES (t_lane, 'C', TRUE) RETURNING id INTO q23;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_lane, 'B') RETURNING id INTO q24;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_lane, 'A') RETURNING id INTO q25;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_lane, 'C') RETURNING id INTO q26;

-- ============================================================
-- QUESTIONS — TURNING_MANEUVERS (q27–q31)
-- ============================================================
INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_turn, 'B') RETURNING id INTO q27;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage)
VALUES (t_turn, 'C', TRUE) RETURNING id INTO q28;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_turn, 'A') RETURNING id INTO q29;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_turn, 'B') RETURNING id INTO q30;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_turn, 'C') RETURNING id INTO q31;

-- ============================================================
-- QUESTIONS — ALCOHOL_DRUGS (q32–q37)
-- ============================================================
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_alcohol, 'A', TRUE, TRUE) RETURNING id INTO q32;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_alcohol, 'C', TRUE, TRUE) RETURNING id INTO q33;

INSERT INTO questions (primary_topic_id, correct_choice_key, risk_flag)
VALUES (t_alcohol, 'B', TRUE) RETURNING id INTO q34;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage)
VALUES (t_alcohol, 'A', TRUE) RETURNING id INTO q35;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_alcohol, 'C') RETURNING id INTO q36;

INSERT INTO questions (primary_topic_id, correct_choice_key, risk_flag)
VALUES (t_alcohol, 'B', TRUE) RETURNING id INTO q37;

-- ============================================================
-- QUESTIONS — SPECIAL_CONDITIONS (q38–q42)
-- ============================================================
INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_special, 'C') RETURNING id INTO q38;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage)
VALUES (t_special, 'A', TRUE) RETURNING id INTO q39;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_special, 'B') RETURNING id INTO q40;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_special, 'C') RETURNING id INTO q41;

INSERT INTO questions (primary_topic_id, correct_choice_key)
VALUES (t_special, 'A') RETURNING id INTO q42;

-- ============================================================
-- QUESTIONS — MOTORCYCLE_BASICS (q43–q46)
-- ============================================================
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'B', TRUE, TRUE) RETURNING id INTO q43;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage)
VALUES (t_moto, 'C', TRUE) RETURNING id INTO q44;

INSERT INTO questions (primary_topic_id, correct_choice_key, risk_flag)
VALUES (t_moto, 'A', TRUE) RETURNING id INTO q45;

INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage)
VALUES (t_moto, 'B', TRUE) RETURNING id INTO q46;

-- ============================================================
-- QUESTION VARIANTS — English + Chinese
-- ============================================================

-- q1: red octagon
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q1, 'en', 'A red octagonal sign always means:',
 '[{"key":"A","text":"Slow down and proceed with caution"},{"key":"B","text":"Yield to oncoming traffic"},{"key":"C","text":"Come to a complete stop"}]'::jsonb,
 'A red octagon is a stop sign. You must come to a complete stop at the marked stop line.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q1, 'zh', '红色八角形标志总是意味着：',
 '[{"key":"A","text":"减速并小心通行"},{"key":"B","text":"让行对向来车"},{"key":"C","text":"完全停车"}]'::jsonb,
 '红色八角形是停车标志。您必须在划定的停车线处完全停车。');

-- q2: yellow diamond sign
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q2, 'en', 'A yellow diamond-shaped sign indicates:',
 '[{"key":"A","text":"A warning of a hazard or change in road conditions ahead"},{"key":"B","text":"A regulatory requirement you must obey"},{"key":"C","text":"Information about services ahead"}]'::jsonb,
 'Yellow diamond signs are warning signs. They alert drivers to potential hazards ahead.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q2, 'zh', '黄色菱形标志表示：',
 '[{"key":"A","text":"前方有危险或路况变化的警告"},{"key":"B","text":"必须遵守的法规要求"},{"key":"C","text":"前方服务设施的信息"}]'::jsonb,
 '黄色菱形标志是警告标志，提醒驾驶员前方可能存在危险。');

-- q3: green signal light
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q3, 'en', 'When a traffic signal turns green, you should:',
 '[{"key":"A","text":"Accelerate immediately to maintain traffic flow"},{"key":"B","text":"Check that the intersection is clear before proceeding"},{"key":"C","text":"Sound your horn to alert pedestrians"}]'::jsonb,
 'Even on green, always check the intersection is clear before entering. Other drivers may run red lights.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q3, 'zh', '当交通信号灯变绿时，您应该：',
 '[{"key":"A","text":"立即加速以保持交通流畅"},{"key":"B","text":"确认路口畅通后再通行"},{"key":"C","text":"鸣笛提醒行人"}]'::jsonb,
 '即使在绿灯时，进入路口前也要确认路口畅通。其他驾驶员可能闯红灯。');

-- q4: flashing red light
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q4, 'en', 'A flashing red traffic signal means:',
 '[{"key":"A","text":"Treat it as a stop sign — stop completely, then proceed when safe"},{"key":"B","text":"Slow down and proceed with caution"},{"key":"C","text":"The signal is broken — proceed normally"}]'::jsonb,
 'A flashing red is treated the same as a stop sign. Stop completely, then yield before proceeding.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q4, 'zh', '闪烁的红色交通信号灯意味着：',
 '[{"key":"A","text":"按停车标志处理——完全停车，确认安全后再通行"},{"key":"B","text":"减速并小心通行"},{"key":"C","text":"信号灯故障——正常通行"}]'::jsonb,
 '闪烁的红灯与停车标志处理方式相同。完全停车后让行，再继续前进。');

-- q5: double yellow lines
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q5, 'en', 'Double solid yellow center lines mean:',
 '[{"key":"A","text":"Passing is allowed only when the left line is dashed"},{"key":"B","text":"Passing is allowed at reduced speed"},{"key":"C","text":"No passing in either direction"}]'::jsonb,
 'Double solid yellow lines prohibit passing in either direction. They indicate a no-passing zone.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q5, 'zh', '双实黄色中心线意味着：',
 '[{"key":"A","text":"仅当左线为虚线时允许超车"},{"key":"B","text":"降速后允许超车"},{"key":"C","text":"双向均禁止超车"}]'::jsonb,
 '双实黄线禁止双向超车，表示禁止超车区。');

-- q6: white edge line
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q6, 'en', 'A solid white line on the right edge of the road marks:',
 '[{"key":"A","text":"The center of a two-way road"},{"key":"B","text":"The right boundary of the travel lane"},{"key":"C","text":"A bike lane separator"}]'::jsonb,
 'A solid white edge line marks the right boundary of the roadway, separating the travel lane from the shoulder.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q6, 'zh', '道路右边缘的白色实线标志：',
 '[{"key":"A","text":"双向道路的中心"},{"key":"B","text":"行车道的右边界"},{"key":"C","text":"自行车道分隔线"}]'::jsonb,
 '白色边线标记道路右边界，将行车道与路肩分开。');

-- q7: pedestrian crossing sign
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q7, 'en', 'When you see a pedestrian crossing sign, you should:',
 '[{"key":"A","text":"Slow down and be prepared to stop for pedestrians"},{"key":"B","text":"Sound your horn to warn pedestrians"},{"key":"C","text":"Maintain your speed — pedestrians must wait"}]'::jsonb,
 'Pedestrian crossing signs warn of areas where people may be crossing. Slow down and yield to pedestrians.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q7, 'zh', '当您看到行人穿越标志时，您应该：',
 '[{"key":"A","text":"减速并准备为行人停车"},{"key":"B","text":"鸣笛提醒行人"},{"key":"C","text":"保持速度——行人必须等待"}]'::jsonb,
 '行人穿越标志警告该区域可能有人穿越。减速并为行人让行。');

-- q8: railroad crossing
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q8, 'en', 'When approaching a railroad crossing with flashing lights, you must:',
 '[{"key":"A","text":"Slow down to 15 mph and proceed carefully"},{"key":"B","text":"Stop only if you can see a train"},{"key":"C","text":"Stop at least 15 feet from the nearest rail and wait until the lights stop"}]'::jsonb,
 'Flashing lights at a railroad crossing mean a train is approaching. Stop at least 15 feet from the tracks and wait.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q8, 'zh', '靠近有闪烁灯光的铁路道口时，您必须：',
 '[{"key":"A","text":"减速至15英里/小时并小心通行"},{"key":"B","text":"只有看到火车才停车"},{"key":"C","text":"在距最近铁轨至少15英尺处停车，等到灯光停止"}]'::jsonb,
 '铁路道口闪烁灯光表示火车正在接近。在距铁轨至少15英尺处停车等待。');

-- q9: uncontrolled intersection
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q9, 'en', 'At an uncontrolled intersection, you must yield to:',
 '[{"key":"A","text":"Only vehicles coming from your left"},{"key":"B","text":"Vehicles already in the intersection and those on your right"},{"key":"C","text":"Only larger vehicles"}]'::jsonb,
 'At an uncontrolled intersection, yield to vehicles already in the intersection and to traffic on your right.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q9, 'zh', '在无控制交叉路口，您必须让行于：',
 '[{"key":"A","text":"只有来自您左侧的车辆"},{"key":"B","text":"已在路口的车辆和来自您右侧的车辆"},{"key":"C","text":"只有较大的车辆"}]'::jsonb,
 '在无控制路口，让行于已在路口的车辆和右侧来车。');

-- q10: 4-way stop
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q10, 'en', 'At a four-way stop, when two vehicles arrive at the same time, which one goes first?',
 '[{"key":"A","text":"The larger vehicle"},{"key":"B","text":"The vehicle going straight"},{"key":"C","text":"The vehicle on the right"}]'::jsonb,
 'When two vehicles reach a four-way stop at the same time, the vehicle to the right has the right of way.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q10, 'zh', '在四向停车路口，两辆车同时到达时，哪辆车先行？',
 '[{"key":"A","text":"较大的车辆"},{"key":"B","text":"直行的车辆"},{"key":"C","text":"右侧的车辆"}]'::jsonb,
 '两辆车同时到达四向停车路口时，右侧车辆享有通行权。');

-- q11: turning vehicle yield
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q11, 'en', 'When making a left turn at a green light, you must yield to:',
 '[{"key":"A","text":"Oncoming traffic and pedestrians in the crosswalk"},{"key":"B","text":"Only pedestrians"},{"key":"C","text":"No one — you have the green light"}]'::jsonb,
 'On a green light left turn, you must still yield to oncoming traffic and pedestrians crossing.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q11, 'zh', '在绿灯时左转，您必须让行于：',
 '[{"key":"A","text":"对向来车和人行横道上的行人"},{"key":"B","text":"只有行人"},{"key":"C","text":"无需让行——您有绿灯"}]'::jsonb,
 '绿灯左转时，仍须让行于对向来车和过街行人。');

-- q12: emergency vehicle
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q12, 'en', 'When an emergency vehicle with lights and sirens approaches, you should:',
 '[{"key":"A","text":"Accelerate to clear the intersection ahead"},{"key":"B","text":"Pull to the right edge of the road and stop"},{"key":"C","text":"Slow down and move to the center lane"}]'::jsonb,
 'When an emergency vehicle approaches with lights/sirens, pull to the right and stop until it passes.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q12, 'zh', '当带有警灯和警报声的紧急车辆靠近时，您应该：',
 '[{"key":"A","text":"加速通过前方路口"},{"key":"B","text":"靠右行驶并停车"},{"key":"C","text":"减速并移至中间车道"}]'::jsonb,
 '当紧急车辆开灯鸣笛靠近时，靠右停车直到其通过。');

-- q13: yield sign
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q13, 'en', 'A yield sign requires you to:',
 '[{"key":"A","text":"Stop completely before proceeding"},{"key":"B","text":"Maintain your current speed"},{"key":"C","text":"Slow down and give the right of way to traffic in the intersection"}]'::jsonb,
 'A yield sign means slow down, and give the right of way to traffic in the intersection or roadway.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q13, 'zh', '让行标志要求您：',
 '[{"key":"A","text":"完全停车后再通行"},{"key":"B","text":"保持当前速度"},{"key":"C","text":"减速并在路口让行"}]'::jsonb,
 '让行标志表示减速，并在路口或道路上让行。');

-- q14: pedestrian in crosswalk
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q14, 'en', 'A pedestrian is crossing at a marked crosswalk. You should:',
 '[{"key":"A","text":"Wait until the pedestrian is completely across before proceeding"},{"key":"B","text":"Proceed if the pedestrian is on the far half"},{"key":"C","text":"Sound your horn to make the pedestrian walk faster"}]'::jsonb,
 'You must wait until the pedestrian has completely crossed before proceeding, not just past your lane.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q14, 'zh', '有行人在标记的人行横道上过街。您应该：',
 '[{"key":"A","text":"等行人完全过街后再通行"},{"key":"B","text":"如果行人在远半边则可以通行"},{"key":"C","text":"鸣笛让行人走快些"}]'::jsonb,
 '必须等行人完全过街后再通行，不仅仅是过了您的车道。');

-- q15: blind intersection
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q15, 'en', 'At a blind intersection where you cannot see 100 feet in either direction, the speed limit is:',
 '[{"key":"A","text":"25 mph"},{"key":"B","text":"15 mph"},{"key":"C","text":"20 mph"}]'::jsonb,
 'At a blind intersection (visibility under 100 feet in either direction), the maximum speed is 15 mph.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q15, 'zh', '在两个方向均无法看到100英尺的盲交叉路口，限速为：',
 '[{"key":"A","text":"25英里/小时"},{"key":"B","text":"15英里/小时"},{"key":"C","text":"20英里/小时"}]'::jsonb,
 '在盲交叉路口（两方向能见度不足100英尺），最高速度为15英里/小时。');

-- q16: basic speed law
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q16, 'en', 'California''s Basic Speed Law states that you must:',
 '[{"key":"A","text":"Never exceed posted speed limits under any conditions"},{"key":"B","text":"Always drive at exactly the posted speed limit"},{"key":"C","text":"Never drive faster than is safe for current conditions"}]'::jsonb,
 'The Basic Speed Law requires driving at a speed that is safe for current conditions, even below the posted limit.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q16, 'zh', '加利福尼亚州基本速度法规定您必须：',
 '[{"key":"A","text":"任何情况下都不超过规定限速"},{"key":"B","text":"始终以规定限速行驶"},{"key":"C","text":"不得以超过当前条件安全速度行驶"}]'::jsonb,
 '基本速度法要求在当前条件下安全行驶，即使低于规定限速。');

-- q17: residential speed limit
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q17, 'en', 'The speed limit in a residential district when no sign is posted is:',
 '[{"key":"A","text":"25 mph"},{"key":"B","text":"35 mph"},{"key":"C","text":"20 mph"}]'::jsonb,
 'In a residential district with no posted speed limit, the default is 25 mph.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q17, 'zh', '住宅区在未张贴标志时的限速为：',
 '[{"key":"A","text":"25英里/小时"},{"key":"B","text":"35英里/小时"},{"key":"C","text":"20英里/小时"}]'::jsonb,
 '住宅区未张贴限速标志时，默认限速为25英里/小时。');

-- q18: following distance
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q18, 'en', 'The safest following distance in normal conditions is:',
 '[{"key":"A","text":"1 second for every 10 mph of speed"},{"key":"B","text":"At least 3 seconds behind the vehicle ahead"},{"key":"C","text":"One car length for every 10 mph"}]'::jsonb,
 'A minimum 3-second following distance is recommended in normal conditions to allow adequate reaction time.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q18, 'zh', '正常条件下最安全的跟车距离是：',
 '[{"key":"A","text":"每10英里/小时速度1秒"},{"key":"B","text":"与前车至少保持3秒距离"},{"key":"C","text":"每10英里/小时一辆车的长度"}]'::jsonb,
 '正常条件下建议保持至少3秒的跟车距离，以确保足够的反应时间。');

-- q19: school zone
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q19, 'en', 'The speed limit in a school zone when children are present is:',
 '[{"key":"A","text":"20 mph"},{"key":"B","text":"15 mph"},{"key":"C","text":"25 mph"}]'::jsonb,
 'In a school zone when children are present or the school zone signal is flashing, the speed limit is 25 mph unless posted otherwise.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q19, 'zh', '有儿童在场时学区限速为：',
 '[{"key":"A","text":"20英里/小时"},{"key":"B","text":"15英里/小时"},{"key":"C","text":"25英里/小时"}]'::jsonb,
 '有儿童在场或学区信号灯闪烁时，限速为25英里/小时（除非另有规定）。');

-- q20: freeway speed limit
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q20, 'en', 'The maximum speed limit on most California freeways is:',
 '[{"key":"A","text":"70 mph"},{"key":"B","text":"55 mph"},{"key":"C","text":"65 mph"}]'::jsonb,
 'The maximum speed limit on most California freeways is 65 mph, unless posted otherwise.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q20, 'zh', '加利福尼亚州大多数高速公路的最高限速是：',
 '[{"key":"A","text":"70英里/小时"},{"key":"B","text":"55英里/小时"},{"key":"C","text":"65英里/小时"}]'::jsonb,
 '加利福尼亚州大多数高速公路的最高限速为65英里/小时，除非另有标示。');

-- q21: bad weather speed
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q21, 'en', 'When driving in heavy rain, you should:',
 '[{"key":"A","text":"Maintain the posted speed limit"},{"key":"B","text":"Reduce speed and increase following distance"},{"key":"C","text":"Use high-beam headlights for better visibility"}]'::jsonb,
 'In heavy rain, reduce speed and increase following distance. Use low-beam headlights, not high-beams.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q21, 'zh', '在大雨中驾驶时，您应该：',
 '[{"key":"A","text":"保持规定限速"},{"key":"B","text":"降低速度并增加跟车距离"},{"key":"C","text":"使用远光灯以提高能见度"}]'::jsonb,
 '大雨中应降低速度并增加跟车距离。使用近光灯，而不是远光灯。');

-- q22: lane change
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q22, 'en', 'Before changing lanes, you should:',
 '[{"key":"A","text":"Signal, check mirrors, check blind spots, then change"},{"key":"B","text":"Signal and immediately change lanes"},{"key":"C","text":"Check mirrors only — blind spots are covered by mirrors"}]'::jsonb,
 'Signal first, then check all mirrors AND look over your shoulder to check blind spots before changing lanes.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q22, 'zh', '变道前，您应该：',
 '[{"key":"A","text":"打信号、检查后视镜、检查盲点，然后变道"},{"key":"B","text":"打信号后立即变道"},{"key":"C","text":"只检查后视镜——盲点已被后视镜覆盖"}]'::jsonb,
 '先打转向灯，然后检查所有后视镜并回头检查盲点，再变道。');

-- q23: motorcycle lane position
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q23, 'en', 'A motorcyclist should ride in which part of the lane for best visibility?',
 '[{"key":"A","text":"Always the right third of the lane"},{"key":"B","text":"Always the center of the lane"},{"key":"C","text":"The position that gives maximum visibility and avoids hazards"}]'::jsonb,
 'Motorcyclists should adjust lane position based on conditions to maximize visibility and avoid road hazards.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q23, 'zh', '摩托车驾驶员应在车道的哪个位置行驶以获得最佳能见度？',
 '[{"key":"A","text":"始终在车道右侧三分之一处"},{"key":"B","text":"始终在车道中央"},{"key":"C","text":"能提供最大能见度并避开危险的位置"}]'::jsonb,
 '摩托车驾驶员应根据情况调整车道位置，以最大化能见度并避开路面危险。');

-- q24: passing on right
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q24, 'en', 'You may pass on the right only when:',
 '[{"key":"A","text":"Traffic is moving slowly in the left lane"},{"key":"B","text":"The road has two or more lanes going the same direction and the lane is clear"},{"key":"C","text":"Whenever the driver in front is going below the speed limit"}]'::jsonb,
 'Passing on the right is only allowed on roads with two or more lanes in the same direction, when the lane is clear.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q24, 'zh', '只有在以下情况下才可以从右侧超车：',
 '[{"key":"A","text":"左车道交通缓慢时"},{"key":"B","text":"道路有两条或更多同向车道且车道畅通时"},{"key":"C","text":"前车行驶速度低于限速时"}]'::jsonb,
 '从右侧超车只有在同向有两条或更多车道且车道畅通时才允许。');

-- q25: HOV lane
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q25, 'en', 'A carpool (HOV) lane requires at least:',
 '[{"key":"A","text":"2 occupants including the driver, unless posted otherwise"},{"key":"B","text":"3 occupants including the driver"},{"key":"C","text":"2 passengers in addition to the driver"}]'::jsonb,
 'Most HOV lanes require at least 2 occupants (driver + 1 passenger). Check posted signs as requirements vary.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q25, 'zh', '拼车（HOV）车道至少需要：',
 '[{"key":"A","text":"包括驾驶员在内至少2人（除非另有规定）"},{"key":"B","text":"包括驾驶员在内至少3人"},{"key":"C","text":"除驾驶员外至少2名乘客"}]'::jsonb,
 '大多数HOV车道要求至少2人（驾驶员+1名乘客）。请查看张贴标志，要求可能有所不同。');

-- q26: bike lane
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q26, 'en', 'When turning right, you must merge into a bike lane:',
 '[{"key":"A","text":"No more than 200 feet before the turn"},{"key":"B","text":"At least 200 feet before the turn"},{"key":"C","text":"Only at intersections with bike signals"}]'::jsonb,
 'Before turning right, merge into the bike lane no more than 200 feet before the turn, after checking for cyclists.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q26, 'zh', '右转时，您必须在距转弯处不超过多远时并入自行车道：',
 '[{"key":"A","text":"转弯前200英尺以内"},{"key":"B","text":"转弯前至少200英尺"},{"key":"C","text":"只在有自行车信号的路口"}]'::jsonb,
 '右转前，在确认无骑车人后，在距转弯处不超过200英尺时并入自行车道。');

-- q27: right turn on red
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q27, 'en', 'When turning right on a red light, you must:',
 '[{"key":"A","text":"Proceed without stopping if no traffic is present"},{"key":"B","text":"Come to a complete stop first, then yield and turn when safe"},{"key":"C","text":"Always wait for a green light"}]'::jsonb,
 'Right turn on red requires a complete stop first, then yield to all traffic and pedestrians before turning.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q27, 'zh', '红灯右转时，您必须：',
 '[{"key":"A","text":"如果没有车辆则不停车直接通行"},{"key":"B","text":"先完全停车，然后确认安全后让行并转弯"},{"key":"C","text":"始终等绿灯"}]'::jsonb,
 '红灯右转需要先完全停车，然后让行所有车辆和行人后再转弯。');

-- q28: left turn from one-way
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q28, 'en', 'When turning left from a one-way street onto a two-way street, you must turn into:',
 '[{"key":"A","text":"The lane closest to the right curb"},{"key":"B","text":"Any available lane"},{"key":"C","text":"The lane closest to the left curb"}]'::jsonb,
 'Turning left from a one-way onto a two-way, turn into the lane closest to the left curb (the nearest lane).');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q28, 'zh', '从单行道左转进入双向街道时，您必须转入：',
 '[{"key":"A","text":"最靠近右侧路缘的车道"},{"key":"B","text":"任何可用的车道"},{"key":"C","text":"最靠近左侧路缘的车道"}]'::jsonb,
 '从单行道左转进入双向道路时，转入最靠近左侧路缘的车道（最近的车道）。');

-- q29: U-turn
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q29, 'en', 'U-turns are prohibited:',
 '[{"key":"A","text":"On any road with a median"},{"key":"B","text":"Wherever your view is blocked in either direction for 200 feet"},{"key":"C","text":"On all city streets"}]'::jsonb,
 'U-turns are prohibited where your view is obstructed within 200 feet in either direction, among other locations.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q29, 'zh', 'U形转弯被禁止的情况：',
 '[{"key":"A","text":"任何有中央隔离带的道路"},{"key":"B","text":"任何方向200英尺内视线受阻的地方"},{"key":"C","text":"所有城市街道"}]'::jsonb,
 '在任何方向200英尺内视线受阻的地方，U形转弯被禁止（以及其他情况）。');

-- q30: turn signal timing
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q30, 'en', 'When turning on a road with a speed limit of 45 mph, you should signal at least:',
 '[{"key":"A","text":"100 feet before turning"},{"key":"B","text":"200 feet before turning"},{"key":"C","text":"50 feet before turning"}]'::jsonb,
 'On roads with speed limits above 35 mph, signal at least 100 feet before turning.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q30, 'zh', '在限速45英里/小时的道路转弯时，您应该至少提前多远打信号：',
 '[{"key":"A","text":"转弯前100英尺"},{"key":"B","text":"转弯前200英尺"},{"key":"C","text":"转弯前50英尺"}]'::jsonb,
 '在限速超过35英里/小时的道路上，转弯前至少100英尺时打信号。');

-- q31: backing up
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q31, 'en', 'When backing up, you should:',
 '[{"key":"A","text":"Rely on your mirrors only"},{"key":"B","text":"Turn your body and look through the rear window"},{"key":"C","text":"Use your hazard lights and proceed quickly"}]'::jsonb,
 'When backing, turn and look through the rear window. Mirrors alone have blind spots. Move slowly.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q31, 'zh', '倒车时，您应该：',
 '[{"key":"A","text":"只依靠后视镜"},{"key":"B","text":"转身通过后窗观察"},{"key":"C","text":"打开危险警示灯并快速通过"}]'::jsonb,
 '倒车时，转身通过后窗观察。仅靠后视镜有盲点。缓慢移动。');

-- q32: BAC limit
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q32, 'en', 'It is illegal in California to drive with a blood alcohol concentration (BAC) of:',
 '[{"key":"A","text":"0.08% or higher for drivers 21 and over"},{"key":"B","text":"Any amount for all drivers"},{"key":"C","text":"0.10% or higher"}]'::jsonb,
 'For drivers 21+, the legal limit is 0.08% BAC. For commercial drivers it is 0.04%, and zero tolerance for under 21.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q32, 'zh', '在加利福尼亚州，以下血液酒精浓度（BAC）驾驶属于违法：',
 '[{"key":"A","text":"21岁及以上驾驶员BAC 0.08%或以上"},{"key":"B","text":"所有驾驶员任何含量"},{"key":"C","text":"BAC 0.10%或以上"}]'::jsonb,
 '21岁以上驾驶员的法定限制为0.08%。商业驾驶员为0.04%，21岁以下零容忍。');

-- q33: alcohol judgment
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q33, 'en', 'Alcohol affects driving by:',
 '[{"key":"A","text":"Improving reaction time at low doses"},{"key":"B","text":"Reducing coordination only after several drinks"},{"key":"C","text":"Impairing judgment and reaction time even at low levels"}]'::jsonb,
 'Even small amounts of alcohol impair judgment, coordination, and reaction time. There is no safe amount for driving.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q33, 'zh', '酒精对驾驶的影响是：',
 '[{"key":"A","text":"少量饮酒可改善反应时间"},{"key":"B","text":"只有饮几杯后才会影响协调性"},{"key":"C","text":"即使少量也会影响判断力和反应时间"}]'::jsonb,
 '即使少量酒精也会损害判断力、协调性和反应时间。驾驶没有安全饮酒量。');

-- q34: DUI penalties
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q34, 'en', 'A first DUI conviction in California can result in:',
 '[{"key":"A","text":"A warning and mandatory driving school only"},{"key":"B","text":"License suspension, fines, and possible jail time"},{"key":"C","text":"Only a fine of up to $500"}]'::jsonb,
 'A first DUI can result in license suspension, fines, DUI school, and possible jail time.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q34, 'zh', '在加利福尼亚州，首次DUI定罪可能导致：',
 '[{"key":"A","text":"警告和强制驾驶学校"},{"key":"B","text":"吊销驾照、罚款和可能的监禁"},{"key":"C","text":"仅最高500美元的罚款"}]'::jsonb,
 '首次DUI可能导致吊销驾照、罚款、DUI学校和可能的监禁。');

-- q35: medication and driving
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q35, 'en', 'Before taking a prescription medication and driving, you should:',
 '[{"key":"A","text":"Ask your doctor or pharmacist if it is safe to drive while taking it"},{"key":"B","text":"Take a smaller dose to reduce side effects"},{"key":"C","text":"Drive only in familiar areas to compensate"}]'::jsonb,
 'Always consult your doctor or pharmacist about whether a medication affects driving ability before getting behind the wheel.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q35, 'zh', '服用处方药后驾驶前，您应该：',
 '[{"key":"A","text":"询问您的医生或药剂师服药期间驾驶是否安全"},{"key":"B","text":"减少剂量以减少副作用"},{"key":"C","text":"只在熟悉的地区驾驶以补偿"}]'::jsonb,
 '在开车前，务必咨询医生或药剂师该药物是否影响驾驶能力。');

-- q36: open container
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q36, 'en', 'An open container of alcohol in a vehicle must be:',
 '[{"key":"A","text":"Kept in the glove compartment"},{"key":"B","text":"Held only by passengers, not the driver"},{"key":"C","text":"Stored in the trunk or an area not accessible to the driver or passengers"}]'::jsonb,
 'Open alcohol containers must be in the trunk or a locked area inaccessible to driver and passengers.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q36, 'zh', '车内的未密封酒精饮料容器必须：',
 '[{"key":"A","text":"放在手套箱中"},{"key":"B","text":"只能由乘客持有，而非驾驶员"},{"key":"C","text":"存放在行李箱或驾驶员和乘客无法触及的区域"}]'::jsonb,
 '未密封的酒精容器必须放在行李箱或驾驶员和乘客无法触及的锁定区域。');

-- q37: marijuana and driving
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q37, 'en', 'Driving after using marijuana is:',
 '[{"key":"A","text":"Legal if you have a medical marijuana card"},{"key":"B","text":"Illegal and impairs driving ability"},{"key":"C","text":"Only illegal if your THC level is above the legal limit"}]'::jsonb,
 'Driving under the influence of marijuana is illegal in California regardless of whether it is used medically.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q37, 'zh', '使用大麻后驾驶：',
 '[{"key":"A","text":"如果您有医用大麻卡则合法"},{"key":"B","text":"违法且会损害驾驶能力"},{"key":"C","text":"仅当THC含量超过法定限制时才违法"}]'::jsonb,
 '在加利福尼亚州，无论是否为医用，大麻驾车均属违法。');

-- q38: night driving
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q38, 'en', 'When driving at night, you should use low beams when within:',
 '[{"key":"A","text":"300 feet of an oncoming vehicle or 300 feet behind a vehicle ahead"},{"key":"B","text":"500 feet of any other vehicle"},{"key":"C","text":"1000 feet of an oncoming vehicle"}]'::jsonb,
 'Use low beams within 500 feet of oncoming vehicles and 300 feet when following another vehicle.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q38, 'zh', '夜间驾驶时，您应在距离以下距离内使用近光灯：',
 '[{"key":"A","text":"距对向来车300英尺或距前方车辆300英尺"},{"key":"B","text":"距任何其他车辆500英尺"},{"key":"C","text":"距对向来车1000英尺"}]'::jsonb,
 '距对向来车500英尺以内以及跟随前车300英尺以内时使用近光灯。');

-- q39: wet roads
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q39, 'en', 'Roads are most slippery:',
 '[{"key":"A","text":"During heavy, prolonged rain"},{"key":"B","text":"During the first few minutes of rain after a dry spell"},{"key":"C","text":"When the temperature drops below 40°F"}]'::jsonb,
 'Roads are most slippery during the first rain after a dry period, as oil and dust mix with light rain to create slick conditions.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q39, 'zh', '道路最滑的时候是：',
 '[{"key":"A","text":"持续大雨期间"},{"key":"B","text":"干旱期后最初几分钟下雨时"},{"key":"C","text":"气温降至40°F以下时"}]'::jsonb,
 '道路在干旱期后最初几分钟降雨时最滑，因为油脂和灰尘与小雨混合会造成湿滑状况。');

-- q40: fog driving
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q40, 'en', 'When driving in dense fog, you should use:',
 '[{"key":"A","text":"High-beam headlights to see farther ahead"},{"key":"B","text":"Low-beam headlights or fog lights"},{"key":"C","text":"Hazard lights and maintain normal speed"}]'::jsonb,
 'In dense fog, use low beams or fog lights. High beams reflect off the fog and reduce visibility further.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q40, 'zh', '在浓雾中驾驶时，您应该使用：',
 '[{"key":"A","text":"远光灯以看得更远"},{"key":"B","text":"近光灯或雾灯"},{"key":"C","text":"危险警示灯并保持正常速度"}]'::jsonb,
 '在浓雾中，使用近光灯或雾灯。远光灯会反射雾气，进一步降低能见度。');

-- q41: hydroplaning
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q41, 'en', 'If your vehicle starts to hydroplane, you should:',
 '[{"key":"A","text":"Brake hard to regain traction"},{"key":"B","text":"Accelerate to get through the water faster"},{"key":"C","text":"Ease off the gas and steer straight until you regain traction"}]'::jsonb,
 'If hydroplaning, ease off the gas and steer straight. Do not brake hard or make sudden steering inputs.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q41, 'zh', '如果您的车辆开始打滑（水上漂），您应该：',
 '[{"key":"A","text":"猛踩刹车以恢复抓地力"},{"key":"B","text":"加速以更快通过积水"},{"key":"C","text":"松开油门并保持直线行驶，直到恢复抓地力"}]'::jsonb,
 '如果发生水上漂，松开油门并保持直线行驶。不要猛踩刹车或突然转向。');

-- q42: sun glare
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q42, 'en', 'To reduce sun glare while driving, you should:',
 '[{"key":"A","text":"Close the passenger sun visor only"},{"key":"B","text":"Use sunglasses and the sun visor, and reduce speed"},{"key":"C","text":"Pull over and wait for the glare to pass"}]'::jsonb,
 'Use sunglasses and the sun visor to manage glare. Slow down as reduced visibility requires more reaction time.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q42, 'zh', '为减少驾驶时的眩光，您应该：',
 '[{"key":"A","text":"只关闭乘客侧遮阳板"},{"key":"B","text":"使用太阳镜和遮阳板，并降低速度"},{"key":"C","text":"靠边停车等待眩光消散"}]'::jsonb,
 '使用太阳镜和遮阳板处理眩光。由于能见度降低，需要更多反应时间，应放慢速度。');

-- q43: motorcycle helmet
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q43, 'en', 'In California, motorcycle riders are required to wear:',
 '[{"key":"A","text":"A helmet only on freeways"},{"key":"B","text":"A U.S. DOT-compliant helmet at all times"},{"key":"C","text":"A helmet only if under 18 years old"}]'::jsonb,
 'California law requires all motorcycle riders and passengers to wear a U.S. DOT-compliant helmet at all times.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q43, 'zh', '在加利福尼亚州，摩托车驾驶员需要佩戴：',
 '[{"key":"A","text":"仅在高速公路上佩戴头盔"},{"key":"B","text":"符合美国交通部标准的头盔，始终佩戴"},{"key":"C","text":"只有18岁以下才需要头盔"}]'::jsonb,
 '加利福尼亚州法律要求所有摩托车驾驶员和乘客始终佩戴符合美国交通部标准的头盔。');

-- q44: lane splitting
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q44, 'en', 'Lane splitting by motorcycles in California is:',
 '[{"key":"A","text":"Always illegal"},{"key":"B","text":"Illegal at speeds above 30 mph"},{"key":"C","text":"Legal when done safely per CHP guidelines"}]'::jsonb,
 'Lane splitting is legal in California when done safely. The CHP has issued guidelines on safe lane-splitting practices.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q44, 'zh', '摩托车在加利福尼亚州的车道分流：',
 '[{"key":"A","text":"始终违法"},{"key":"B","text":"速度超过30英里/小时时违法"},{"key":"C","text":"按照加州高速公路巡逻队指南安全进行时合法"}]'::jsonb,
 '在加利福尼亚州，安全进行车道分流是合法的。加州高速公路巡逻队已发布安全车道分流指南。');

-- q45: motorcycle stopping distance
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q45, 'en', 'Compared to a car, a motorcycle typically stops:',
 '[{"key":"A","text":"Faster, because it is lighter"},{"key":"B","text":"In the same distance at the same speed"},{"key":"C","text":"In a shorter distance when brakes are applied correctly"}]'::jsonb,
 'Motorcycles can stop faster than cars due to lower weight, but only when both brakes are applied correctly and smoothly.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q45, 'zh', '与汽车相比，摩托车通常停车：',
 '[{"key":"A","text":"更快，因为它更轻"},{"key":"B","text":"在相同速度下需要相同的距离"},{"key":"C","text":"正确使用刹车时在更短的距离内停下"}]'::jsonb,
 '由于重量较轻，摩托车可以比汽车停得更快，但前提是正确平稳地使用两个刹车。');

-- q46: motorcycle visibility
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q46, 'en', 'To increase your visibility as a motorcyclist, you should:',
 '[{"key":"A","text":"Ride in the blind spots of other vehicles for protection"},{"key":"B","text":"Wear bright or reflective clothing and use your headlight at all times"},{"key":"C","text":"Only ride during daylight hours"}]'::jsonb,
 'Wear bright or reflective gear and always ride with headlight on. Never ride in blind spots — be seen!');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(q46, 'zh', '作为摩托车驾驶员，要提高您的能见度，您应该：',
 '[{"key":"A","text":"在其他车辆的盲点中行驶以获得保护"},{"key":"B","text":"穿着明亮或反光的服装并始终开大灯"},{"key":"C","text":"只在白天行驶"}]'::jsonb,
 '穿着明亮或反光的装备，并始终开大灯行驶。永远不要在盲点中行驶——要让人看见你！');

-- ============================================================
-- MOCK EXAM TEMPLATE
-- ============================================================
INSERT INTO mock_exams (code, version, status, question_count)
VALUES ('M1_STANDARD_V1', 1, 'active', 46)
RETURNING id INTO exam1;

INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order) VALUES
(exam1, q1,  1),  (exam1, q2,  2),  (exam1, q3,  3),  (exam1, q4,  4),
(exam1, q5,  5),  (exam1, q6,  6),  (exam1, q7,  7),  (exam1, q8,  8),
(exam1, q9,  9),  (exam1, q10, 10), (exam1, q11, 11), (exam1, q12, 12),
(exam1, q13, 13), (exam1, q14, 14), (exam1, q15, 15), (exam1, q16, 16),
(exam1, q17, 17), (exam1, q18, 18), (exam1, q19, 19), (exam1, q20, 20),
(exam1, q21, 21), (exam1, q22, 22), (exam1, q23, 23), (exam1, q24, 24),
(exam1, q25, 25), (exam1, q26, 26), (exam1, q27, 27), (exam1, q28, 28),
(exam1, q29, 29), (exam1, q30, 30), (exam1, q31, 31), (exam1, q32, 32),
(exam1, q33, 33), (exam1, q34, 34), (exam1, q35, 35), (exam1, q36, 36),
(exam1, q37, 37), (exam1, q38, 38), (exam1, q39, 39), (exam1, q40, 40),
(exam1, q41, 41), (exam1, q42, 42), (exam1, q43, 43), (exam1, q44, 44),
(exam1, q45, 45), (exam1, q46, 46);

END $$;
