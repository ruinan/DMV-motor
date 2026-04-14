-- V10: Seed 53 real California M1 motorcycle written test questions
-- Topics already exist from V2. This migration adds new questions with
-- both English and Chinese variants, plus mock exam CA_M1_30Q (30 questions).

DO $$
DECLARE
    -- topic IDs (looked up by code, not inserted)
    t_traffic   BIGINT;
    t_row       BIGINT;
    t_speed     BIGINT;
    t_lane      BIGINT;
    t_turn      BIGINT;
    t_alcohol   BIGINT;
    t_special   BIGINT;
    t_moto      BIGINT;

    -- TRAFFIC_CONTROLS (7 questions: r1–r7)
    r1  BIGINT; r2  BIGINT; r3  BIGINT; r4  BIGINT; r5  BIGINT;
    r6  BIGINT; r7  BIGINT;

    -- RIGHT_OF_WAY (6 questions: r8–r13)
    r8  BIGINT; r9  BIGINT; r10 BIGINT; r11 BIGINT; r12 BIGINT;
    r13 BIGINT;

    -- SPEED_DISTANCE (5 questions: r14–r18)
    r14 BIGINT; r15 BIGINT; r16 BIGINT; r17 BIGINT; r18 BIGINT;

    -- LANE_POSITION (5 questions: r19–r23)
    r19 BIGINT; r20 BIGINT; r21 BIGINT; r22 BIGINT; r23 BIGINT;

    -- TURNING_MANEUVERS (4 questions: r24–r27)
    r24 BIGINT; r25 BIGINT; r26 BIGINT; r27 BIGINT;

    -- ALCOHOL_DRUGS (5 questions: r28–r32)
    r28 BIGINT; r29 BIGINT; r30 BIGINT; r31 BIGINT; r32 BIGINT;

    -- SPECIAL_CONDITIONS (5 questions: r33–r37)
    r33 BIGINT; r34 BIGINT; r35 BIGINT; r36 BIGINT; r37 BIGINT;

    -- MOTORCYCLE_BASICS (16 questions: r38–r53)
    r38 BIGINT; r39 BIGINT; r40 BIGINT; r41 BIGINT; r42 BIGINT;
    r43 BIGINT; r44 BIGINT; r45 BIGINT; r46 BIGINT; r47 BIGINT;
    r48 BIGINT; r49 BIGINT; r50 BIGINT; r51 BIGINT; r52 BIGINT;
    r53 BIGINT;

    -- mock exam
    exam_30q BIGINT;

BEGIN

-- ============================================================
-- LOOK UP TOPIC IDs
-- ============================================================
SELECT id INTO t_traffic FROM topics WHERE code = 'TRAFFIC_CONTROLS';
SELECT id INTO t_row     FROM topics WHERE code = 'RIGHT_OF_WAY';
SELECT id INTO t_speed   FROM topics WHERE code = 'SPEED_DISTANCE';
SELECT id INTO t_lane    FROM topics WHERE code = 'LANE_POSITION';
SELECT id INTO t_turn    FROM topics WHERE code = 'TURNING_MANEUVERS';
SELECT id INTO t_alcohol FROM topics WHERE code = 'ALCOHOL_DRUGS';
SELECT id INTO t_special FROM topics WHERE code = 'SPECIAL_CONDITIONS';
SELECT id INTO t_moto    FROM topics WHERE code = 'MOTORCYCLE_BASICS';

-- ============================================================
-- QUESTIONS — TRAFFIC_CONTROLS (r1–r7)
-- ============================================================
-- r1: arrow signals
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag, allow_in_free_trial)
VALUES (t_traffic, 'C', TRUE, FALSE, TRUE) RETURNING id INTO r1;

-- r2: lane control signals (green arrow down = use lane)
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_traffic, 'A', TRUE, FALSE) RETURNING id INTO r2;

-- r3: school zone speed sign
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_traffic, 'B', TRUE, TRUE) RETURNING id INTO r3;

-- r4: no U-turn sign
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_traffic, 'A', FALSE, FALSE) RETURNING id INTO r4;

-- r5: flashing yellow arrow
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_traffic, 'B', TRUE, FALSE) RETURNING id INTO r5;

-- r6: HOV lane diamond sign
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_traffic, 'C', FALSE, FALSE) RETURNING id INTO r6;

-- r7: solid double yellow center lines (motorcycles)
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_traffic, 'A', TRUE, TRUE) RETURNING id INTO r7;

-- ============================================================
-- QUESTIONS — RIGHT_OF_WAY (r8–r13)
-- ============================================================
-- r8: T-intersection (terminating road yields)
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_row, 'B', TRUE, FALSE) RETURNING id INTO r8;

-- r9: roundabout
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_row, 'A', TRUE, FALSE) RETURNING id INTO r9;

-- r10: stopped school bus
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_row, 'C', TRUE, TRUE) RETURNING id INTO r10;

-- r11: blind pedestrian (white cane)
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_row, 'A', TRUE, TRUE) RETURNING id INTO r11;

-- r12: entering freeway on-ramp
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_row, 'C', TRUE, FALSE) RETURNING id INTO r12;

-- r13: bicyclist in lane
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_row, 'B', FALSE, FALSE) RETURNING id INTO r13;

-- ============================================================
-- QUESTIONS — SPEED_DISTANCE (r14–r18)
-- ============================================================
-- r14: adverse weather following distance
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_speed, 'C', TRUE, TRUE) RETURNING id INTO r14;

-- r15: construction zone speed
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_speed, 'A', TRUE, FALSE) RETURNING id INTO r15;

-- r16: school zone speed when children present
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_speed, 'B', TRUE, TRUE) RETURNING id INTO r16;

-- r17: residential area maximum speed
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_speed, 'A', FALSE, FALSE) RETURNING id INTO r17;

-- r18: basic speed law
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_speed, 'C', TRUE, FALSE) RETURNING id INTO r18;

-- ============================================================
-- QUESTIONS — LANE_POSITION (r19–r23)
-- ============================================================
-- r19: lane splitting legal in California
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag, allow_in_free_trial)
VALUES (t_lane, 'B', TRUE, FALSE, TRUE) RETURNING id INTO r19;

-- r20: best lane position for visibility
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_lane, 'A', TRUE, FALSE) RETURNING id INTO r20;

-- r21: avoiding road hazards (left tire track)
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_lane, 'C', TRUE, TRUE) RETURNING id INTO r21;

-- r22: freeway lane changes
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_lane, 'B', FALSE, FALSE) RETURNING id INTO r22;

-- r23: left vs right tire track position
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_lane, 'A', TRUE, FALSE) RETURNING id INTO r23;

-- ============================================================
-- QUESTIONS — TURNING_MANEUVERS (r24–r27)
-- ============================================================
-- r24: U-turns where permitted
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_turn, 'C', FALSE, FALSE) RETURNING id INTO r24;

-- r25: turning into correct lane
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_turn, 'A', TRUE, FALSE) RETURNING id INTO r25;

-- r26: countersteering at speed
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_turn, 'B', TRUE, TRUE) RETURNING id INTO r26;

-- r27: swerving to avoid obstacle
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_turn, 'C', TRUE, TRUE) RETURNING id INTO r27;

-- ============================================================
-- QUESTIONS — ALCOHOL_DRUGS (r28–r32)
-- ============================================================
-- r28: BAC 0.08% for adults 21+
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag, allow_in_free_trial)
VALUES (t_alcohol, 'B', TRUE, TRUE, TRUE) RETURNING id INTO r28;

-- r29: BAC 0.04% commercial drivers
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_alcohol, 'A', TRUE, TRUE) RETURNING id INTO r29;

-- r30: BAC 0.01% under 21 zero tolerance
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_alcohol, 'C', TRUE, TRUE) RETURNING id INTO r30;

-- r31: implied consent law
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_alcohol, 'B', TRUE, TRUE) RETURNING id INTO r31;

-- r32: prescription drugs and riding
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_alcohol, 'A', TRUE, TRUE) RETURNING id INTO r32;

-- ============================================================
-- QUESTIONS — SPECIAL_CONDITIONS (r33–r37)
-- ============================================================
-- r33: riding in fog
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_special, 'C', TRUE, TRUE) RETURNING id INTO r33;

-- r34: riding in rain (slippery first minutes)
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_special, 'A', TRUE, TRUE) RETURNING id INTO r34;

-- r35: night riding visibility
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_special, 'B', TRUE, TRUE) RETURNING id INTO r35;

-- r36: fatigue while riding
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_special, 'C', TRUE, TRUE) RETURNING id INTO r36;

-- r37: railroad tracks crossing angle
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_special, 'B', TRUE, TRUE) RETURNING id INTO r37;

-- ============================================================
-- QUESTIONS — MOTORCYCLE_BASICS (r38–r53)
-- ============================================================
-- r38: helmet required by law in California
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag, allow_in_free_trial)
VALUES (t_moto, 'A', TRUE, TRUE, TRUE) RETURNING id INTO r38;

-- r39: eye protection requirement
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'C', TRUE, FALSE) RETURNING id INTO r39;

-- r40: protective gear (jacket, gloves, boots)
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'B', TRUE, FALSE) RETURNING id INTO r40;

-- r41: T-CLOCS pre-ride inspection
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'A', TRUE, FALSE) RETURNING id INTO r41;

-- r42: body position in turns
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'C', TRUE, TRUE) RETURNING id INTO r42;

-- r43: front brake vs rear brake stopping power
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'B', TRUE, TRUE) RETURNING id INTO r43;

-- r44: emergency braking technique
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'A', TRUE, TRUE) RETURNING id INTO r44;

-- r45: carrying a passenger — requirements
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'C', TRUE, FALSE) RETURNING id INTO r45;

-- r46: cargo loading — balance
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'B', FALSE, FALSE) RETURNING id INTO r46;

-- r47: group riding formation
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'A', TRUE, FALSE) RETURNING id INTO r47;

-- r48: target fixation
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'C', TRUE, TRUE) RETURNING id INTO r48;

-- r49: handlebar wobble / speed wobble
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'B', TRUE, TRUE) RETURNING id INTO r49;

-- r50: following distance for motorcycles
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'A', TRUE, FALSE) RETURNING id INTO r50;

-- r51: making yourself visible to drivers
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'C', TRUE, FALSE) RETURNING id INTO r51;

-- r52: night riding checklist
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'B', TRUE, TRUE) RETURNING id INTO r52;

-- r53: new rider risk
INSERT INTO questions (primary_topic_id, correct_choice_key, is_key_coverage, risk_flag)
VALUES (t_moto, 'A', TRUE, TRUE) RETURNING id INTO r53;


-- ============================================================
-- QUESTION VARIANTS — TRAFFIC_CONTROLS
-- ============================================================

-- r1: protected green arrow signal
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r1, 'en', 'A green arrow signal pointing left means:',
 '[{"key":"A","text":"Yield to oncoming traffic and turn left if safe"},{"key":"B","text":"Do not turn left under any circumstances"},{"key":"C","text":"You have a protected turn — oncoming traffic is stopped"}]'::jsonb,
 'A green arrow is a protected signal. Oncoming traffic faces a red light, so you may turn in the arrow''s direction without yielding.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r1, 'zh', '指向左侧的绿色箭头信号灯表示：',
 '[{"key":"A","text":"让行对向来车后可安全左转"},{"key":"B","text":"任何情况下不得左转"},{"key":"C","text":"受保护转向——对向来车已停车"}]'::jsonb,
 '绿色箭头是受保护信号。对向来车面对红灯，您可以按箭头方向转弯，无需让行。');

-- r2: lane control signal — green arrow pointing down
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r2, 'en', 'A green downward-pointing arrow above a lane means:',
 '[{"key":"A","text":"You may use this lane"},{"key":"B","text":"The lane is closed ahead — merge now"},{"key":"C","text":"Slower traffic must use this lane"}]'::jsonb,
 'A green downward arrow is a lane-control signal indicating the lane is open for your direction of travel.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r2, 'zh', '车道上方向下的绿色箭头表示：',
 '[{"key":"A","text":"您可以使用此车道"},{"key":"B","text":"前方车道关闭——立即合流"},{"key":"C","text":"慢速车辆须使用此车道"}]'::jsonb,
 '向下的绿色箭头是车道控制信号，表示该车道对您的行驶方向开放。');

-- r3: school zone speed sign
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r3, 'en', 'When passing a school with a posted "School Speed Limit 25" sign and children are present, you must:',
 '[{"key":"A","text":"Slow to 25 mph only when the school flasher is active"},{"key":"B","text":"Slow to 25 mph whenever children are present near the roadway"},{"key":"C","text":"Slow to 15 mph at all times near a school"}]'::jsonb,
 'In California, the 25 mph school zone speed limit applies whenever children are present near the road, regardless of whether a flasher is active.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r3, 'zh', '经过张贴"学校限速25英里"标志且有儿童在场时，您必须：',
 '[{"key":"A","text":"仅在学校闪烁灯亮起时减速至25英里/小时"},{"key":"B","text":"只要路旁有儿童，就减速至25英里/小时"},{"key":"C","text":"在学校附近始终减速至15英里/小时"}]'::jsonb,
 '在加州，只要儿童在路旁出现，25英里/小时的学区限速就适用，无论闪烁灯是否亮起。');

-- r4: no U-turn sign
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r4, 'en', 'A circular sign with a U-turn symbol and a red line through it means:',
 '[{"key":"A","text":"U-turns are prohibited at this location"},{"key":"B","text":"U-turns are allowed only when no traffic is present"},{"key":"C","text":"U-turns are allowed for motorcycles only"}]'::jsonb,
 'A no U-turn sign prohibits U-turns at that location for all vehicles. Violating it is a moving traffic violation.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r4, 'zh', '带有U形转弯符号并有红线穿过的圆形标志表示：',
 '[{"key":"A","text":"此处禁止U形转弯"},{"key":"B","text":"无来车时允许U形转弯"},{"key":"C","text":"仅摩托车允许U形转弯"}]'::jsonb,
 '禁止U形转弯标志禁止所有车辆在该地点掉头。违反此标志构成交通违规。');

-- r5: flashing yellow arrow (left turn)
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r5, 'en', 'A flashing yellow left-turn arrow at a traffic signal means:',
 '[{"key":"A","text":"Left turns are prohibited until the arrow turns green"},{"key":"B","text":"You may turn left but must first yield to oncoming traffic and pedestrians"},{"key":"C","text":"Oncoming traffic is stopped — proceed with your left turn"}]'::jsonb,
 'A flashing yellow arrow means a permissive left turn is allowed, but you must yield to oncoming traffic and crossing pedestrians before turning.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r5, 'zh', '交通信号灯上的黄色左转闪烁箭头表示：',
 '[{"key":"A","text":"直到箭头变绿前禁止左转"},{"key":"B","text":"可以左转，但须先让行对向来车和行人"},{"key":"C","text":"对向来车已停——可以左转"}]'::jsonb,
 '黄色闪烁箭头表示允许非受保护左转，但转弯前须让行对向来车和穿越行人。');

-- r6: HOV lane diamond sign
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r6, 'en', 'A lane marked with a diamond symbol on the pavement is:',
 '[{"key":"A","text":"A passing lane open to all vehicles"},{"key":"B","text":"A left-turn-only lane"},{"key":"C","text":"A High-Occupancy Vehicle (HOV) lane with occupancy requirements"}]'::jsonb,
 'Diamond markings designate an HOV (carpool) lane. In California, motorcycles with a single rider are permitted to use HOV lanes at any time.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r6, 'zh', '路面上标有菱形标志的车道是：',
 '[{"key":"A","text":"对所有车辆开放的超车道"},{"key":"B","text":"仅限左转的车道"},{"key":"C","text":"有人数要求的高载客率（HOV）车道"}]'::jsonb,
 '菱形标记表示HOV（拼车）车道。在加州，单人驾驶的摩托车可随时使用HOV车道。');

-- r7: solid double yellow — motorcycle passing
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r7, 'en', 'You are riding a motorcycle on a two-lane road marked with double solid yellow center lines. May you cross them to pass a slow vehicle?',
 '[{"key":"A","text":"No — double solid yellow lines prohibit passing in either direction for all vehicles including motorcycles"},{"key":"B","text":"Yes — motorcycles are exempt from the no-passing rule"},{"key":"C","text":"Yes — if no oncoming traffic is visible"}]'::jsonb,
 'Double solid yellow lines are a no-passing zone for all vehicles. Motorcycles receive no special exemption from this rule.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r7, 'zh', '您在双向单车道公路上骑摩托车，道路中央标有双实黄线。可以越线超越慢速车辆吗？',
 '[{"key":"A","text":"不可以——双实黄线对所有车辆（包括摩托车）均禁止超车"},{"key":"B","text":"可以——摩托车豁免于禁止超车规定"},{"key":"C","text":"可以——只要前方看不到对向来车"}]'::jsonb,
 '双实黄线对所有车辆均为禁止超车区，摩托车没有特殊豁免。');


-- ============================================================
-- QUESTION VARIANTS — RIGHT_OF_WAY
-- ============================================================

-- r8: T-intersection
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r8, 'en', 'At a T-intersection with no signs or signals, which vehicle must yield?',
 '[{"key":"A","text":"The vehicle on the through road must yield to vehicles entering from the terminating road"},{"key":"B","text":"The vehicle on the terminating road must yield to traffic on the through road"},{"key":"C","text":"Both vehicles must stop and the first to arrive goes first"}]'::jsonb,
 'At a T-intersection, the driver on the road that ends (the stem of the T) must yield to traffic on the through road.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r8, 'zh', '在没有标志或信号的T形路口，哪辆车必须让行？',
 '[{"key":"A","text":"主干道上的车辆须让行于从终止道驶入的车辆"},{"key":"B","text":"在终止道路上的车辆须让行于主干道上的车辆"},{"key":"C","text":"两辆车都要停车，先到者先行"}]'::jsonb,
 '在T形路口，行驶在末端道路（T字茎部）的驾驶员须让行于主干道上的车辆。');

-- r9: roundabout
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r9, 'en', 'When entering a roundabout, you must yield to:',
 '[{"key":"A","text":"Vehicles already circulating inside the roundabout"},{"key":"B","text":"Vehicles waiting to enter the roundabout from other approaches"},{"key":"C","text":"No one — the first to enter has right of way"}]'::jsonb,
 'Vehicles already inside a roundabout have the right of way. Entering drivers must yield until a safe gap appears.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r9, 'zh', '进入环形交叉路口时，您必须让行于：',
 '[{"key":"A","text":"已在环岛内行驶的车辆"},{"key":"B","text":"在其他入口等待进入的车辆"},{"key":"C","text":"无需让行——先进者享有通行权"}]'::jsonb,
 '已在环岛内行驶的车辆享有优先通行权。进入的驾驶员须等待安全间隙。');

-- r10: stopped school bus (flashing red lights)
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r10, 'en', 'A school bus has stopped with its red lights flashing and the stop arm extended on an undivided road. You are approaching from the opposite direction. You must:',
 '[{"key":"A","text":"Slow to 25 mph and proceed carefully"},{"key":"B","text":"Stop only if children are visible on your side of the road"},{"key":"C","text":"Stop — on an undivided road, traffic from both directions must stop"}]'::jsonb,
 'On an undivided road, all traffic in both directions must stop for a school bus with flashing red lights and an extended stop arm.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r10, 'zh', '在不分隔的道路上，一辆校车停车并亮起红色闪烁灯，伸出停车臂。您从对面方向驶来，您必须：',
 '[{"key":"A","text":"减速至25英里/小时，小心通行"},{"key":"B","text":"仅当您侧有儿童时才停车"},{"key":"C","text":"停车——在不分隔的道路上，双向车辆均须停车"}]'::jsonb,
 '在不分隔的道路上，亮起红色闪烁灯并伸出停车臂的校车要求双向所有车辆停车。');

-- r11: blind pedestrian with white cane
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r11, 'en', 'A pedestrian at a crosswalk is using a white cane. You should:',
 '[{"key":"A","text":"Stop and remain stopped until the pedestrian has fully crossed"},{"key":"B","text":"Slow down and proceed once the pedestrian moves to the sidewalk"},{"key":"C","text":"Sound your horn to let the pedestrian know you are there"}]'::jsonb,
 'A white cane indicates a blind or visually impaired pedestrian. California law requires all vehicles to stop and remain stopped until such a pedestrian has completely crossed the roadway.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r11, 'zh', '一名行人在人行横道上使用白色手杖。您应该：',
 '[{"key":"A","text":"停车并保持停车，直到行人完全穿过"},{"key":"B","text":"减速，待行人走上人行道后继续前进"},{"key":"C","text":"鸣笛让行人知道您在这里"}]'::jsonb,
 '白色手杖表示行人是盲人或视障者。加州法律要求所有车辆停车，并保持停车直到此类行人完全穿过道路。');

-- r12: entering freeway from on-ramp
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r12, 'en', 'When merging onto a freeway from an on-ramp, you must:',
 '[{"key":"A","text":"Stop at the end of the ramp and wait for a large gap"},{"key":"B","text":"Maintain your current speed and let freeway traffic adjust"},{"key":"C","text":"Yield to freeway traffic and merge at highway speed when safe"}]'::jsonb,
 'Drivers entering a freeway must yield to existing freeway traffic and adjust their speed to merge safely at highway speed — not stop at the end of the ramp.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r12, 'zh', '从匝道并入高速公路时，您必须：',
 '[{"key":"A","text":"在匝道末端停车，等待较大的车距间隙"},{"key":"B","text":"保持当前速度，让高速公路上的车辆调整"},{"key":"C","text":"让行高速公路上的车辆，在安全时以高速公路速度并道"}]'::jsonb,
 '进入高速公路的驾驶员须让行于已在高速公路上的车辆，并调整速度以高速公路速度安全并道，不得在匝道末端停车。');

-- r13: bicyclist occupying travel lane
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r13, 'en', 'A bicyclist is riding in the center of a narrow traffic lane ahead of you. You should:',
 '[{"key":"A","text":"Sound your horn and pass immediately"},{"key":"B","text":"Follow at a safe distance until it is safe to pass with at least 3 feet of clearance"},{"key":"C","text":"Flash your headlight to signal the cyclist to move over"}]'::jsonb,
 'California law requires drivers to give cyclists at least 3 feet of clearance when passing. Follow safely until you can pass with adequate space.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r13, 'zh', '一名骑行者骑在您前方狭窄行车道的中央。您应该：',
 '[{"key":"A","text":"鸣笛并立即超越"},{"key":"B","text":"保持安全距离跟随，直到可以保持至少3英尺间距安全超越"},{"key":"C","text":"闪烁前灯示意骑行者靠边"}]'::jsonb,
 '加州法律要求超越骑行者时至少保留3英尺间距。保持安全跟随距离，直到有足够空间再超越。');


-- ============================================================
-- QUESTION VARIANTS — SPEED_DISTANCE
-- ============================================================

-- r14: adverse weather following distance
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r14, 'en', 'Under normal conditions, motorcyclists should maintain a following distance of at least 3 seconds. In adverse weather or poor road conditions, you should:',
 '[{"key":"A","text":"Maintain the same 3-second gap — it is always sufficient"},{"key":"B","text":"Reduce speed but keep the same 3-second gap"},{"key":"C","text":"Increase your following distance to 4 seconds or more"}]'::jsonb,
 'Bad weather reduces traction and visibility. Increasing your following distance gives more time and space to react and stop safely.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r14, 'zh', '在正常条件下，摩托车手应保持至少3秒的跟车距离。在恶劣天气或路况不佳时，您应该：',
 '[{"key":"A","text":"保持相同的3秒间距——始终足够"},{"key":"B","text":"降低速度但保持相同的3秒间距"},{"key":"C","text":"将跟车距离增加到4秒或更多"}]'::jsonb,
 '恶劣天气会降低抓地力和能见度。增加跟车距离可以争取更多时间和空间以安全停车。');

-- r15: construction zone speed
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r15, 'en', 'When riding through an active construction zone with workers present, you must:',
 '[{"key":"A","text":"Obey all posted reduced speed limits — fines are doubled in active construction zones"},{"key":"B","text":"Slow to 25 mph regardless of the posted limit"},{"key":"C","text":"Match the speed of surrounding traffic regardless of signs"}]'::jsonb,
 'In active construction zones with workers present, California law doubles fines for speeding violations. Obey all posted speed limit signs in the zone.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r15, 'zh', '在有工人作业的施工区穿行时，您必须：',
 '[{"key":"A","text":"遵守所有张贴的限速——在施工区超速罚款加倍"},{"key":"B","text":"不论张贴限速如何，均减速至25英里/小时"},{"key":"C","text":"不论标志如何，跟随周围车辆速度行驶"}]'::jsonb,
 '在有工人作业的施工区，加州法律规定超速违规罚款加倍。请遵守施工区内所有张贴的限速标志。');

-- r16: school zone 25 mph
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r16, 'en', 'What is the speed limit in a school zone when children are present near the roadway in California?',
 '[{"key":"A","text":"15 mph"},{"key":"B","text":"25 mph"},{"key":"C","text":"35 mph"}]'::jsonb,
 'California Vehicle Code sets the school zone speed limit at 25 mph when children are present near the road.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r16, 'zh', '在加州，当儿童在路旁时，学区的限速是多少？',
 '[{"key":"A","text":"15英里/小时"},{"key":"B","text":"25英里/小时"},{"key":"C","text":"35英里/小时"}]'::jsonb,
 '加州车辆法典规定，当儿童在路旁时，学区限速为25英里/小时。');

-- r17: residential street speed
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r17, 'en', 'In a residential district with no posted speed limit, the maximum speed limit is:',
 '[{"key":"A","text":"25 mph"},{"key":"B","text":"35 mph"},{"key":"C","text":"45 mph"}]'::jsonb,
 'California''s prima facie speed limit for residential districts is 25 mph unless otherwise posted.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r17, 'zh', '在没有张贴限速的住宅区，最高限速为：',
 '[{"key":"A","text":"25英里/小时"},{"key":"B","text":"35英里/小时"},{"key":"C","text":"45英里/小时"}]'::jsonb,
 '加州住宅区的法定默认限速为25英里/小时，除非另有张贴。');

-- r18: basic speed law
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r18, 'en', 'California''s Basic Speed Law states that you must:',
 '[{"key":"A","text":"Always drive at the posted speed limit regardless of conditions"},{"key":"B","text":"Never exceed 65 mph on any road"},{"key":"C","text":"Never drive faster than is safe for current road, weather, and traffic conditions"}]'::jsonb,
 'The Basic Speed Law means you can be cited for driving too fast for conditions even if you are within the posted speed limit.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r18, 'zh', '加州基本速度法规定您必须：',
 '[{"key":"A","text":"无论条件如何，始终以张贴的限速行驶"},{"key":"B","text":"在任何道路上均不超过65英里/小时"},{"key":"C","text":"行驶速度不超过当前路况、天气和交通条件所允许的安全速度"}]'::jsonb,
 '基本速度法意味着即使在限速范围内，如果速度不适合当前条件，您仍可能被罚款。');


-- ============================================================
-- QUESTION VARIANTS — LANE_POSITION
-- ============================================================

-- r19: lane splitting in California
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r19, 'en', 'Is lane splitting — riding a motorcycle between rows of stopped or slow-moving vehicles — legal in California?',
 '[{"key":"A","text":"No — lane splitting is illegal in California"},{"key":"B","text":"Yes — lane splitting is legal and regulated by California law"},{"key":"C","text":"Only on freeways with posted speeds above 65 mph"}]'::jsonb,
 'California was the first U.S. state to formally legalize lane splitting (AB 51, 2016). Riders should do so safely at reasonable speeds.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r19, 'zh', '在加州，车道分流——骑摩托车穿行于停止或缓行车辆之间——是否合法？',
 '[{"key":"A","text":"不合法——加州禁止车道分流"},{"key":"B","text":"合法——加州法律允许并规范车道分流"},{"key":"C","text":"仅在限速65英里/小时以上的高速公路上合法"}]'::jsonb,
 '加州是美国第一个正式将车道分流合法化的州（AB 51，2016年）。骑行者应以合理速度安全进行。');

-- r20: best lane position for visibility
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r20, 'en', 'Which lane position generally gives a motorcyclist the best visibility and makes the rider most visible to other drivers?',
 '[{"key":"A","text":"The left portion of the lane (left tire track position)"},{"key":"B","text":"The center of the lane directly over the oil strip"},{"key":"C","text":"The right edge of the lane"}]'::jsonb,
 'Riding in the left third of the lane gives you the best view of traffic ahead and makes you more visible in following drivers'' mirrors.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r20, 'zh', '哪个车道位置通常能给摩托车手最佳视野，并使骑手对其他驾驶员最为可见？',
 '[{"key":"A","text":"车道的左侧部分（左轮迹位置）"},{"key":"B","text":"直接在油迹上方的车道中央"},{"key":"C","text":"车道右边缘"}]'::jsonb,
 '骑行在车道左三分之一处，您能获得最佳的前方视野，并在后方驾驶员的后视镜中更加显眼。');

-- r21: avoiding road hazards — avoid the center strip
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r21, 'en', 'You are approaching a debris field in the center of your lane. The safest action is to:',
 '[{"key":"A","text":"Brake hard and stop before the debris"},{"key":"B","text":"Speed up to clear the debris quickly"},{"key":"C","text":"Change your lane position smoothly to avoid the debris, then return"}]'::jsonb,
 'Adjust your lane position early and smoothly to avoid hazards. Abrupt braking or acceleration while avoiding debris can destabilize the motorcycle.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r21, 'zh', '您正在接近车道中央的一片碎片。最安全的做法是：',
 '[{"key":"A","text":"猛踩刹车，在碎片前停下"},{"key":"B","text":"加速快速通过碎片区"},{"key":"C","text":"平稳调整车道位置避开碎片，然后复位"}]'::jsonb,
 '提前平稳调整车道位置以避开危险。在避开碎片时突然制动或加速会使摩托车失去稳定。');

-- r22: freeway lane changes
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r22, 'en', 'Before changing lanes on a freeway, a motorcyclist should:',
 '[{"key":"A","text":"Turn the handlebars sharply in the target lane direction"},{"key":"B","text":"Check mirrors, signal, check blind spots by turning your head, then move smoothly"},{"key":"C","text":"Signal and immediately move without checking mirrors"}]'::jsonb,
 'Motorcycles are small and easily hidden in blind spots. Always check mirrors, signal intent, then physically turn your head to check blind spots before changing lanes.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r22, 'zh', '在高速公路上变道前，摩托车手应该：',
 '[{"key":"A","text":"猛打车把朝目标车道方向"},{"key":"B","text":"查看后视镜，打转向灯，转头检查盲区，然后平稳移动"},{"key":"C","text":"打转向灯后立即移动，不需检查后视镜"}]'::jsonb,
 '摩托车体积小，容易隐藏在盲区中。变道前务必查看后视镜，示意转向意图，然后转头检查盲区。');

-- r23: left vs right tire track
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r23, 'en', 'When following a vehicle, which portion of your lane should you generally ride in?',
 '[{"key":"A","text":"The left tire track, to maximize visibility and avoid the center oil strip"},{"key":"B","text":"The center of the lane directly behind the vehicle ahead"},{"key":"C","text":"The right tire track, closest to the road edge"}]'::jsonb,
 'The left tire track position gives you the best view past the vehicle ahead, keeps you out of the slippery center oil strip, and makes you visible in the driver''s mirror.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r23, 'zh', '跟随前方车辆行驶时，您通常应该骑在车道的哪个位置？',
 '[{"key":"A","text":"车道左侧轮迹，以最大化视野并避开中央油迹"},{"key":"B","text":"直接跟随前车的车道中央"},{"key":"C","text":"最靠近路边的右侧轮迹"}]'::jsonb,
 '左侧轮迹位置能让您最清楚地看到前车前方情况，避开中央滑溜油迹，并使您出现在驾驶员的后视镜中。');


-- ============================================================
-- QUESTION VARIANTS — TURNING_MANEUVERS
-- ============================================================

-- r24: U-turns where permitted
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r24, 'en', 'U-turns are generally permitted in California EXCEPT:',
 '[{"key":"A","text":"On a residential street with no posted prohibition"},{"key":"B","text":"At a green light at an intersection"},{"key":"C","text":"In a business district except at intersections or openings in a divided highway"}]'::jsonb,
 'In a business district, U-turns are only legal at intersections or marked openings. They are also prohibited wherever signs forbid them or visibility is limited.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r24, 'zh', '在加州，U形转弯通常在哪种情况下不被允许？',
 '[{"key":"A","text":"在没有禁止标志的住宅街道上"},{"key":"B","text":"在路口绿灯亮时"},{"key":"C","text":"在商业区内除路口或分隔高速公路开口以外的地方"}]'::jsonb,
 '在商业区，U形转弯只在路口或有标记的开口处合法。在有禁止标志或能见度受限的地方也禁止掉头。');

-- r25: turning into correct lane
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r25, 'en', 'When completing a right turn, you should turn into:',
 '[{"key":"A","text":"The nearest (rightmost) lane of the road you are entering"},{"key":"B","text":"The center lane of the road you are entering"},{"key":"C","text":"Any lane that is convenient"}]'::jsonb,
 'When turning right, complete the turn into the closest right-hand lane. You may then change lanes after safely completing the turn.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r25, 'zh', '完成右转时，您应该转入：',
 '[{"key":"A","text":"所进入道路的最近（最右侧）车道"},{"key":"B","text":"所进入道路的中间车道"},{"key":"C","text":"任何方便的车道"}]'::jsonb,
 '右转时，完成转弯后应进入最近的右侧车道。安全完成转弯后，可再变道。');

-- r26: countersteering at highway speed
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r26, 'en', 'To steer a motorcycle to the right at speeds above approximately 12 mph, you should:',
 '[{"key":"A","text":"Turn the handlebars sharply to the right"},{"key":"B","text":"Push forward on the right handlebar (countersteer) to lean the bike right"},{"key":"C","text":"Lean your body to the right without moving the handlebars"}]'::jsonb,
 'At higher speeds, motorcycles are steered by countersteering: pressing the handlebar on the side you want to turn toward causes the bike to lean and turn that direction.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r26, 'zh', '在大约12英里/小时以上的速度向右转向时，您应该：',
 '[{"key":"A","text":"猛将车把转向右侧"},{"key":"B","text":"向前推右侧车把（反向转向），使车身向右倾斜"},{"key":"C","text":"身体向右倾而不移动车把"}]'::jsonb,
 '在较高速度下，摩托车通过反向转向来操控：向您想转向的一侧推车把，会使车身倾斜并朝该方向转弯。');

-- r27: swerving technique
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r27, 'en', 'When you must swerve to avoid a sudden obstacle on a motorcycle, you should:',
 '[{"key":"A","text":"Brake hard first, then swerve"},{"key":"B","text":"Swerve and brake at the same time"},{"key":"C","text":"Swerve first, then brake after the swerve is complete"}]'::jsonb,
 'Braking while swerving can cause a skid and loss of control. Complete the swerve first to stabilize the motorcycle, then apply brakes if needed.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r27, 'zh', '骑摩托车时需要急转闪避突发障碍物，您应该：',
 '[{"key":"A","text":"先猛踩刹车，再转向躲避"},{"key":"B","text":"同时转向躲避和制动"},{"key":"C","text":"先完成转向躲避，再在躲避完成后制动"}]'::jsonb,
 '在转向躲避的同时制动可能导致打滑并失控。先完成转向以稳定摩托车，如有必要再施加制动。');


-- ============================================================
-- QUESTION VARIANTS — ALCOHOL_DRUGS
-- ============================================================

-- r28: BAC 0.08% adults 21+
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r28, 'en', 'In California, the legal blood alcohol concentration (BAC) limit for a motorcyclist who is 21 years of age or older is:',
 '[{"key":"A","text":"0.05%"},{"key":"B","text":"0.08%"},{"key":"C","text":"0.10%"}]'::jsonb,
 'California sets the DUI BAC limit at 0.08% for drivers and motorcyclists aged 21 and over. You can still be cited for impaired riding below this level.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r28, 'zh', '在加州，21岁及以上摩托车手的法定血液酒精含量（BAC）上限是：',
 '[{"key":"A","text":"0.05%"},{"key":"B","text":"0.08%"},{"key":"C","text":"0.10%"}]'::jsonb,
 '加州规定21岁及以上驾驶员和摩托车手的DUI血液酒精含量上限为0.08%。即使低于此水平，如有驾驶障碍仍可被罚。');

-- r29: BAC 0.04% commercial
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r29, 'en', 'What is the BAC limit for a person holding a commercial driver''s license (CDL) operating any vehicle?',
 '[{"key":"A","text":"0.04%"},{"key":"B","text":"0.08%"},{"key":"C","text":"0.06%"}]'::jsonb,
 'Commercial drivers are held to a stricter BAC standard of 0.04% when operating any vehicle — not just commercial vehicles.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r29, 'zh', '持有商业驾驶执照（CDL）的人员驾驶任何车辆时的BAC上限是：',
 '[{"key":"A","text":"0.04%"},{"key":"B","text":"0.08%"},{"key":"C","text":"0.06%"}]'::jsonb,
 '商业驾驶员在驾驶任何车辆时（不仅限于商用车辆）均须遵守更严格的0.04% BAC标准。');

-- r30: BAC 0.01% under 21 zero tolerance
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r30, 'en', 'California''s Zero Tolerance Law for drivers under 21 sets the BAC limit at:',
 '[{"key":"A","text":"0.05%"},{"key":"B","text":"0.02%"},{"key":"C","text":"0.01%"}]'::jsonb,
 'California''s Zero Tolerance Law makes it illegal for anyone under 21 to drive with a BAC of 0.01% or higher — effectively any measurable amount of alcohol.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r30, 'zh', '加州针对21岁以下驾驶员的"零容忍法"设定的BAC上限为：',
 '[{"key":"A","text":"0.05%"},{"key":"B","text":"0.02%"},{"key":"C","text":"0.01%"}]'::jsonb,
 '加州零容忍法规定，任何21岁以下人员的BAC达到0.01%或以上即属违法——实际上是任何可测量的酒精量。');

-- r31: implied consent
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r31, 'en', 'Under California''s Implied Consent Law, if a law enforcement officer lawfully arrests you for DUI, you are required to:',
 '[{"key":"A","text":"Submit to a field sobriety test only — chemical tests are optional"},{"key":"B","text":"Submit to a chemical test (breath, blood, or urine) to determine BAC"},{"key":"C","text":"Answer all questions but may refuse any testing"}]'::jsonb,
 'By driving in California, you implicitly consent to chemical testing if arrested for DUI. Refusal results in automatic license suspension, separate from any DUI charge.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r31, 'zh', '根据加州的隐含同意法，如果执法人员依法因DUI逮捕您，您须：',
 '[{"key":"A","text":"仅接受现场清醒度测试——化学测试是可选的"},{"key":"B","text":"接受化学测试（呼气、血液或尿液）以检测BAC"},{"key":"C","text":"回答所有问题，但可以拒绝任何测试"}]'::jsonb,
 '在加州驾车即表示您隐性同意在因DUI被捕时接受化学测试。拒绝将导致驾照自动被暂扣，与DUI指控分开处理。');

-- r32: prescription drugs
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r32, 'en', 'You take a prescription medication that warns "may cause drowsiness." You want to ride your motorcycle. You should:',
 '[{"key":"A","text":"Not ride until you know how the medication affects you — drowsiness impairs riding just as alcohol does"},{"key":"B","text":"Ride carefully at reduced speeds, as prescription drugs are legal"},{"key":"C","text":"Drink a cup of coffee to counteract the drowsiness and then ride"}]'::jsonb,
 'Any substance — legal or not — that impairs your ability to ride safely is prohibited. Prescription drug impairment carries the same DUI penalties as alcohol.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r32, 'zh', '您服用了注明"可能导致嗜睡"的处方药，想要骑摩托车出行。您应该：',
 '[{"key":"A","text":"在了解药物对您的影响之前不要骑车——嗜睡与酒精一样会影响驾驶"},{"key":"B","text":"小心骑行并降低速度，因为处方药是合法的"},{"key":"C","text":"喝一杯咖啡来抵消嗜睡，然后骑车"}]'::jsonb,
 '任何影响安全骑行能力的物质——无论是否合法——均被禁止。处方药驾驶障碍与酒精的DUI处罚相同。');


-- ============================================================
-- QUESTION VARIANTS — SPECIAL_CONDITIONS
-- ============================================================

-- r33: riding in fog
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r33, 'en', 'When riding in fog, which of the following is the BEST strategy?',
 '[{"key":"A","text":"Use your high-beam headlight to see further ahead"},{"key":"B","text":"Ride in the center of the lane to maximize distance from other vehicles"},{"key":"C","text":"Reduce speed significantly, increase following distance, and use low-beam headlights"}]'::jsonb,
 'Low beams penetrate fog better than high beams. Slowing down and increasing following distance gives you more time to react to obstacles you may see late.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r33, 'zh', '在雾中骑行时，以下哪种方式是最佳策略？',
 '[{"key":"A","text":"使用远光灯以看得更远"},{"key":"B","text":"骑在车道中央以最大化与其他车辆的距离"},{"key":"C","text":"大幅降低速度，增加跟车距离，并使用近光灯"}]'::jsonb,
 '近光灯比远光灯更能穿透雾气。减速并增加跟车距离，在您晚发现障碍物时有更多时间做出反应。');

-- r34: rain — most dangerous at start
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r34, 'en', 'Rain is most hazardous for motorcyclists:',
 '[{"key":"A","text":"During the first 10–15 minutes of rain, when oil and residue mix with water to create slippery conditions"},{"key":"B","text":"After rain has been falling for more than an hour"},{"key":"C","text":"Only during heavy downpours — light rain poses no significant risk"}]'::jsonb,
 'Road oil and residue accumulate between rainfalls. When light rain first starts, it mixes with this buildup to create extremely slippery conditions before the road is washed clean.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r34, 'zh', '对摩托车手来说，雨天最危险的时段是：',
 '[{"key":"A","text":"降雨开始后的10到15分钟内，此时油污和残留物与雨水混合形成湿滑路面"},{"key":"B","text":"持续降雨超过一小时后"},{"key":"C","text":"仅在暴雨时——小雨不构成明显危险"}]'::jsonb,
 '路面油污和残留物在两次降雨之间积累。细雨初降时，这些积累物与雨水混合，在道路被冲洗干净之前形成极度湿滑的路面。');

-- r35: night riding — reduce speed to headlight range
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r35, 'en', 'When riding at night, your speed should allow you to:',
 '[{"key":"A","text":"Maintain the same speed as daytime since modern headlights are powerful enough"},{"key":"B","text":"Stop within the distance illuminated by your headlight"},{"key":"C","text":"Rely on other vehicles'' headlights to see hazards"}]'::jsonb,
 'At night you must be able to stop within the distance you can see. Riding faster than your headlight range is called "overdriving your headlights" and is dangerous.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r35, 'zh', '夜间骑行时，您的速度应使您能够：',
 '[{"key":"A","text":"保持与白天相同的速度，因为现代前灯已足够强大"},{"key":"B","text":"在前灯照明范围内停车"},{"key":"C","text":"依靠其他车辆的前灯来发现危险"}]'::jsonb,
 '夜间您必须能在可视距离内停车。以超过前灯照明范围的速度骑行称为"超越前灯距离行驶"，非常危险。');

-- r36: fatigue — pull over
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r36, 'en', 'You are on a long motorcycle ride and begin to feel drowsy. What should you do?',
 '[{"key":"A","text":"Open the visor and let fresh air in to stay alert"},{"key":"B","text":"Increase your speed to reach your destination sooner"},{"key":"C","text":"Stop riding immediately, pull over safely, and rest before continuing"}]'::jsonb,
 'Fatigue significantly impairs reaction time and judgment. The only safe response is to stop and rest. No technique reliably restores alertness while riding.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r36, 'zh', '您在进行长途摩托车旅行时开始感到困倦。您应该怎么办？',
 '[{"key":"A","text":"打开面罩让新鲜空气进来以保持清醒"},{"key":"B","text":"加速尽快到达目的地"},{"key":"C","text":"立即停止骑行，安全靠边停车，休息后再继续"}]'::jsonb,
 '疲劳会严重影响反应时间和判断力。唯一安全的做法是停下来休息。骑行时没有任何技巧能可靠地恢复清醒状态。');

-- r37: railroad tracks crossing angle
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r37, 'en', 'When crossing railroad or streetcar tracks on a motorcycle, you should cross at an angle of:',
 '[{"key":"A","text":"Less than 30 degrees to minimize time on the tracks"},{"key":"B","text":"45 to 90 degrees to your direction of travel"},{"key":"C","text":"Exactly parallel to the tracks to avoid the grooves"}]'::jsonb,
 'Crossing tracks at 45–90 degrees prevents your tires from catching in the groove or rail, which can cause a sudden loss of control.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r37, 'zh', '骑摩托车穿越铁路或有轨电车轨道时，您应以什么角度穿越：',
 '[{"key":"A","text":"小于30度，以最小化在轨道上的时间"},{"key":"B","text":"与行驶方向成45至90度角"},{"key":"C","text":"完全平行于轨道以避开凹槽"}]'::jsonb,
 '以45至90度角穿越轨道可防止轮胎卡入凹槽或钢轨，否则可能导致突然失控。');


-- ============================================================
-- QUESTION VARIANTS — MOTORCYCLE_BASICS
-- ============================================================

-- r38: helmet required for all riders
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r38, 'en', 'In California, who is required to wear a helmet while riding a motorcycle?',
 '[{"key":"A","text":"All motorcycle riders and passengers, regardless of age"},{"key":"B","text":"Only riders under 18 years old"},{"key":"C","text":"Only the rider — passengers may choose"}]'::jsonb,
 'California law (Vehicle Code §27803) requires all motorcycle riders and passengers to wear a U.S. DOT-compliant helmet at all times, regardless of age.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r38, 'zh', '在加州，谁必须在骑摩托车时佩戴头盔？',
 '[{"key":"A","text":"所有摩托车驾驶员和乘客，不论年龄"},{"key":"B","text":"仅18岁以下的骑手"},{"key":"C","text":"仅驾驶员——乘客可自行选择"}]'::jsonb,
 '加州法律（车辆法典第27803条）要求所有摩托车驾驶员和乘客在任何时候均须佩戴符合美国DOT标准的头盔，不论年龄。');

-- r39: eye protection requirement
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r39, 'en', 'When is a California motorcyclist NOT required to wear eye protection?',
 '[{"key":"A","text":"Never — eye protection is always required"},{"key":"B","text":"When riding only in residential areas"},{"key":"C","text":"When the motorcycle is equipped with a windshield that meets legal requirements"}]'::jsonb,
 'Eye protection (goggles, face shield, or glasses) is required unless the motorcycle has a windshield that provides equivalent protection under California law.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r39, 'zh', '加州摩托车手在什么情况下不需要佩戴护眼装置？',
 '[{"key":"A","text":"从不——护眼装置始终必须佩戴"},{"key":"B","text":"仅在住宅区骑行时"},{"key":"C","text":"当摩托车配备符合法律要求的挡风玻璃时"}]'::jsonb,
 '除非摩托车配备了根据加州法律提供同等保护的挡风玻璃，否则必须佩戴护眼装置（护目镜、面罩或眼镜）。');

-- r40: protective gear
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r40, 'en', 'Beyond a helmet, which gear is most recommended for motorcyclist protection in a crash?',
 '[{"key":"A","text":"A brightly colored vest only"},{"key":"B","text":"A sturdy jacket, full-fingered gloves, long pants, and over-the-ankle boots"},{"key":"C","text":"Shorts and sandals are acceptable in warm weather"}]'::jsonb,
 'A motorcycle jacket (leather or textile), gloves, long pants, and boots protect against abrasion and impact in a crash. Road rash can be severe even at low speeds.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r40, 'zh', '除头盔外，摩托车事故中最推荐的防护装备是什么？',
 '[{"key":"A","text":"仅一件色彩鲜艳的背心"},{"key":"B","text":"坚固的夹克、全指手套、长裤和高于脚踝的靴子"},{"key":"C","text":"在温暖天气穿短裤和凉鞋是可以接受的"}]'::jsonb,
 '摩托车夹克（皮革或纺织）、手套、长裤和靴子在事故中可防止擦伤和撞击伤。即使在低速时，路面擦伤也可能非常严重。');

-- r41: T-CLOCS pre-ride inspection
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r41, 'en', 'The T-CLOCS pre-ride inspection checklist stands for:',
 '[{"key":"A","text":"Tires/Wheels, Controls, Lights/Electrics, Oil/Fluids, Chassis/Chain, Stands"},{"key":"B","text":"Tires, Clutch, Lights, Oil, Carburetor, Seat"},{"key":"C","text":"Throttle, Coolant, Levers, Other, Cables, Starter"}]'::jsonb,
 'T-CLOCS is the Motorcycle Safety Foundation''s pre-ride checklist: Tires & wheels, Controls, Lights & electrics, Oil & fluids, Chassis & chain, Stands.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r41, 'zh', 'T-CLOCS骑前检查清单代表：',
 '[{"key":"A","text":"轮胎/车轮、控制装置、灯光/电气、机油/液体、车架/链条、支架"},{"key":"B","text":"轮胎、离合器、灯光、机油、化油器、座椅"},{"key":"C","text":"油门、冷却液、操纵杆、其他、线缆、启动器"}]'::jsonb,
 'T-CLOCS是摩托车安全基金会的骑前检查清单：轮胎和车轮、控制装置、灯光和电气、机油和液体、车架和链条、支架。');

-- r42: body position in turns
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r42, 'en', 'When taking a curve on a motorcycle, you should:',
 '[{"key":"A","text":"Lean the bike but keep your body upright to reduce lean angle"},{"key":"B","text":"Brake hard at the apex of the curve for maximum control"},{"key":"C","text":"Look through the turn toward your exit point, keep smooth throttle, and lean with the motorcycle"}]'::jsonb,
 'Look where you want to go, maintain a smooth steady throttle, and lean naturally with the bike. Abrupt throttle changes or braking mid-corner can cause a skid.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r42, 'zh', '骑摩托车过弯时，您应该：',
 '[{"key":"A","text":"倾斜车身但保持身体直立以减小倾角"},{"key":"B","text":"在弯道顶点猛踩刹车以获得最大控制"},{"key":"C","text":"视线穿过弯道看向出口，保持平稳油门，随摩托车倾斜"}]'::jsonb,
 '视线看向您想去的方向，保持平稳稳定的油门，随车身自然倾斜。过弯途中突然改变油门或制动可能导致打滑。');

-- r43: front brake stopping power
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r43, 'en', 'Approximately what percentage of a motorcycle''s total stopping power comes from the front brake?',
 '[{"key":"A","text":"30%"},{"key":"B","text":"70%"},{"key":"C","text":"50%"}]'::jsonb,
 'The front brake provides approximately 70% of stopping power because weight shifts forward during braking, increasing front tire traction. Use both brakes together for shortest stops.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r43, 'zh', '摩托车前刹车提供的总制动力大约占：',
 '[{"key":"A","text":"30%"},{"key":"B","text":"70%"},{"key":"C","text":"50%"}]'::jsonb,
 '前刹车提供约70%的制动力，因为制动时重量前移，增加了前轮的附着力。同时使用前后刹车可实现最短制动距离。');

-- r44: emergency braking
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r44, 'en', 'During emergency braking on a motorcycle (no ABS), the correct technique is:',
 '[{"key":"A","text":"Apply both brakes firmly and progressively without locking either wheel; keep the motorcycle upright"},{"key":"B","text":"Apply only the rear brake to avoid front wheel lockup"},{"key":"C","text":"Squeeze the front brake as hard as possible and ignore the rear brake"}]'::jsonb,
 'Apply both brakes simultaneously with progressive pressure. A locked front wheel causes a fall; a locked rear wheel can be controlled but still reduces stopping effectiveness.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r44, 'zh', '在没有ABS的摩托车上进行紧急制动时，正确的技术是：',
 '[{"key":"A","text":"同时用力且逐渐施力于两个刹车，不使任何车轮抱死；保持车身直立"},{"key":"B","text":"只使用后刹车以避免前轮抱死"},{"key":"C","text":"尽可能用力握前刹车，不管后刹车"}]'::jsonb,
 '同时对两个刹车逐渐加压。前轮抱死会导致摔倒；后轮抱死虽然可以控制但仍会降低制动效果。');

-- r45: carrying a passenger requirements
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r45, 'en', 'To legally carry a passenger on a motorcycle in California, the motorcycle must have:',
 '[{"key":"A","text":"A sidecar or at least 1000cc engine displacement"},{"key":"B","text":"Anti-lock brakes (ABS) installed"},{"key":"C","text":"A permanent, regular seat and footrests for the passenger"}]'::jsonb,
 'California law requires that the motorcycle have a seat and footrests designed and equipped for a passenger. The rider must also hold a full (non-provisional) motorcycle license.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r45, 'zh', '在加州合法携带乘客骑摩托车，摩托车必须具备：',
 '[{"key":"A","text":"边车或至少1000cc排量发动机"},{"key":"B","text":"已安装防抱死制动系统（ABS）"},{"key":"C","text":"专为乘客设计的固定常规座位和脚踏板"}]'::jsonb,
 '加州法律要求摩托车必须配备专为乘客设计的座位和脚踏板。骑手还必须持有完整的（非临时）摩托车驾照。');

-- r46: cargo loading — low and centered
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r46, 'en', 'When loading cargo on a motorcycle, you should place heavier items:',
 '[{"key":"A","text":"High up and on the rear to maximize storage space"},{"key":"B","text":"Low and centered, close to the motorcycle''s center of gravity"},{"key":"C","text":"On one side only for easy access"}]'::jsonb,
 'Heavy loads placed high or off-center raise the center of gravity and make the motorcycle unstable. Keep weight low and evenly distributed on both sides.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r46, 'zh', '在摩托车上装载货物时，较重的物品应放置：',
 '[{"key":"A","text":"高处和后部以最大化存储空间"},{"key":"B","text":"低处和中央，靠近摩托车的重心"},{"key":"C","text":"仅放在一侧以方便取用"}]'::jsonb,
 '放置在高处或偏心位置的重物会抬高重心并使摩托车不稳定。保持重量低且均匀分布在两侧。');

-- r47: group riding — staggered formation
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r47, 'en', 'The recommended formation for a group of motorcycles riding together on a highway is:',
 '[{"key":"A","text":"Staggered formation, with each rider offset in the lane from the rider ahead"},{"key":"B","text":"Side-by-side formation to occupy a full lane as a group"},{"key":"C","text":"Single-file in a straight line with 1-second following distances"}]'::jsonb,
 'Staggered formation keeps the group compact while giving each rider space to maneuver. The lead rider takes the left track, the second rider the right track, and so on.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r47, 'zh', '一组摩托车在高速公路上一起行驶时，推荐的编队方式是：',
 '[{"key":"A","text":"交错编队，每位骑手在车道内与前方骑手错开位置"},{"key":"B","text":"并排编队，以整个群体占据一条完整车道"},{"key":"C","text":"保持1秒跟车距离的单列纵队"}]'::jsonb,
 '交错编队使队伍紧凑，同时为每位骑手提供机动空间。领队骑手占据左轮迹，第二位骑右轮迹，依此类推。');

-- r48: target fixation
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r48, 'en', 'Target fixation is a dangerous phenomenon in which:',
 '[{"key":"A","text":"You stare at your speedometer instead of the road ahead"},{"key":"B","text":"You focus on your destination sign and miss surrounding hazards"},{"key":"C","text":"You fix your eyes on a hazard and unconsciously steer toward it instead of away"}]'::jsonb,
 'Target fixation means the motorcycle follows your eyes. Always look where you WANT to go, not at what you are trying to avoid.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r48, 'zh', '目标固视是一种危险现象，表现为：',
 '[{"key":"A","text":"您盯着速度表而不是前方道路"},{"key":"B","text":"您专注于目的地标志而错过周围危险"},{"key":"C","text":"您将视线固定在危险物上，并不自觉地朝其转向而非避开"}]'::jsonb,
 '目标固视意味着摩托车跟随您的视线走。始终看向您想去的地方，而不是您试图避开的东西。');

-- r49: speed wobble / tank slapper
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r49, 'en', 'If your motorcycle begins a sudden wobble or oscillation of the front wheel, you should:',
 '[{"key":"A","text":"Grip the handlebars tightly and brake hard to stop the wobble"},{"key":"B","text":"Grip the tank firmly with your knees, ease off the throttle gradually, and avoid braking until the wobble stops"},{"key":"C","text":"Accelerate through the wobble to stabilize the front wheel"}]'::jsonb,
 'Gripping the tank with your knees and gradually reducing throttle lets the motorcycle naturally stabilize. Hard braking or fighting the bars can worsen the wobble.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r49, 'zh', '如果您的摩托车前轮突然发生抖动或震荡，您应该：',
 '[{"key":"A","text":"紧握车把并猛踩刹车以停止抖动"},{"key":"B","text":"用膝盖紧夹油箱，逐渐松开油门，在抖动停止前避免制动"},{"key":"C","text":"加速穿越抖动以稳定前轮"}]'::jsonb,
 '用膝盖夹住油箱并逐渐减小油门，让摩托车自然稳定下来。猛踩刹车或强行控制车把会加剧抖动。');

-- r50: following distance for motorcycles
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r50, 'en', 'Compared to cars, motorcyclists should maintain a following distance that is:',
 '[{"key":"A","text":"Greater — motorcycles are less stable and require more distance to stop safely in emergencies"},{"key":"B","text":"The same — the 3-second rule applies equally to all vehicles"},{"key":"C","text":"Shorter — motorcycles can stop faster than cars"}]'::jsonb,
 'Motorcycles are less stable than cars and can be destabilized by debris or sudden hazards. A greater following distance provides more reaction time and braking room.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r50, 'zh', '与汽车相比，摩托车手应保持更____的跟车距离：',
 '[{"key":"A","text":"更大——摩托车稳定性较差，紧急情况下需要更多距离才能安全停车"},{"key":"B","text":"相同——3秒规则对所有车辆同样适用"},{"key":"C","text":"更小——摩托车比汽车停车更快"}]'::jsonb,
 '摩托车比汽车稳定性差，可能被碎片或突发危险破坏稳定性。更大的跟车距离提供更多的反应时间和制动空间。');

-- r51: visibility to other drivers
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r51, 'en', 'Which of the following best increases a motorcyclist''s visibility to other drivers?',
 '[{"key":"A","text":"Riding in the far right lane at all times"},{"key":"B","text":"Wearing dark clothing to appear more professional"},{"key":"C","text":"Wearing bright or reflective gear, using headlights at all times, and positioning in the lane for maximum visibility"}]'::jsonb,
 'Motorcycles are harder to see than cars. Wearing high-visibility gear, keeping your headlight on, and riding where you can be seen in mirrors all reduce the risk of crashes caused by other drivers failing to notice you.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r51, 'zh', '以下哪项最能提高摩托车手对其他驾驶员的可见性？',
 '[{"key":"A","text":"始终在最右侧车道骑行"},{"key":"B","text":"穿深色衣物显得更专业"},{"key":"C","text":"穿着明亮或反光装备，随时开启前灯，并在车道内选择最佳能见位置"}]'::jsonb,
 '摩托车比汽车更难被发现。穿着高能见度装备、保持前灯开启、在能被后视镜看到的位置骑行，均能降低因其他驾驶员未注意到您而引发事故的风险。');

-- r52: night riding checklist
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r52, 'en', 'When preparing for a night motorcycle ride, which precaution is MOST important?',
 '[{"key":"A","text":"Wear your darkest jacket to blend with the night environment"},{"key":"B","text":"Ensure headlight, tail light, and turn signals all work properly, and wear reflective gear"},{"key":"C","text":"Increase your tire pressure for better night traction"}]'::jsonb,
 'Working lights make you visible to other road users. Reflective gear further increases your conspicuity at night, dramatically reducing collision risk.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r52, 'zh', '为夜间摩托车骑行做准备时，最重要的预防措施是：',
 '[{"key":"A","text":"穿最深色的夹克以融入夜间环境"},{"key":"B","text":"确保前灯、尾灯和转向灯均正常工作，并穿着反光装备"},{"key":"C","text":"增加轮胎气压以获得更好的夜间牵引力"}]'::jsonb,
 '正常工作的灯光使您对其他道路使用者可见。反光装备进一步提高您在夜间的醒目性，大幅降低碰撞风险。');

-- r53: new rider crash risk
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r53, 'en', 'Statistics show that new motorcycle riders face the highest crash risk:',
 '[{"key":"A","text":"During their first months of riding, before building hazard-perception and control skills"},{"key":"B","text":"After riding for several years, due to overconfidence"},{"key":"C","text":"Only when riding on freeways — city riding is safe for new riders"}]'::jsonb,
 'New riders have not yet developed automatic hazard recognition and vehicle control. The crash rate is highest in the first year of riding; formal training and gradual experience reduce risk significantly.');
INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text) VALUES
(r53, 'zh', '统计数据显示，新摩托车骑手面临最高事故风险的时段是：',
 '[{"key":"A","text":"骑行的最初几个月内，尚未建立危险感知和控制技能"},{"key":"B","text":"骑行数年后，因过度自信"},{"key":"C","text":"仅在高速公路上骑行时——市区骑行对新手是安全的"}]'::jsonb,
 '新手骑手尚未培养出自动危险识别和车辆控制能力。事故率在骑行第一年最高；正规培训和循序渐进的经验积累能显著降低风险。');


-- ============================================================
-- MOCK EXAM: CA_M1_30Q
-- 30 questions drawn from the 53 new questions above.
-- Selection covers all 8 topics proportionally.
-- ============================================================
INSERT INTO mock_exams (code, question_count, status)
VALUES ('CA_M1_30Q', 30, 'active')
RETURNING id INTO exam_30q;

-- TRAFFIC_CONTROLS: 4 questions (r1, r3, r5, r7)
INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order) VALUES
(exam_30q, r1,  1),
(exam_30q, r3,  2),
(exam_30q, r5,  3),
(exam_30q, r7,  4);

-- RIGHT_OF_WAY: 4 questions (r8, r9, r10, r11)
INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order) VALUES
(exam_30q, r8,  5),
(exam_30q, r9,  6),
(exam_30q, r10, 7),
(exam_30q, r11, 8);

-- SPEED_DISTANCE: 3 questions (r14, r16, r18)
INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order) VALUES
(exam_30q, r14, 9),
(exam_30q, r16, 10),
(exam_30q, r18, 11);

-- LANE_POSITION: 3 questions (r19, r20, r23)
INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order) VALUES
(exam_30q, r19, 12),
(exam_30q, r20, 13),
(exam_30q, r23, 14);

-- TURNING_MANEUVERS: 3 questions (r25, r26, r27)
INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order) VALUES
(exam_30q, r25, 15),
(exam_30q, r26, 16),
(exam_30q, r27, 17);

-- ALCOHOL_DRUGS: 4 questions (r28, r29, r30, r31)
INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order) VALUES
(exam_30q, r28, 18),
(exam_30q, r29, 19),
(exam_30q, r30, 20),
(exam_30q, r31, 21);

-- SPECIAL_CONDITIONS: 4 questions (r33, r34, r35, r37)
INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order) VALUES
(exam_30q, r33, 22),
(exam_30q, r34, 23),
(exam_30q, r35, 24),
(exam_30q, r37, 25);

-- MOTORCYCLE_BASICS: 5 questions (r38, r41, r43, r44, r48)
INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order) VALUES
(exam_30q, r38, 26),
(exam_30q, r41, 27),
(exam_30q, r43, 28),
(exam_30q, r44, 29),
(exam_30q, r48, 30);

END $$;
