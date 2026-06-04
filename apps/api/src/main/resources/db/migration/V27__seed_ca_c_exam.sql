-- V27: Seed the CA-C (California Class C / car) exam + its topic & sub-topic
-- taxonomy. Pure content scoped to a NEW exam_id — no schema changes. The
-- multi-exam foundation (V26: exams table + exam_id on content tables +
-- ExamContext) already routes everything by exam, so a new exam is data-only.
--
-- Mirrors the M1 catalog 1:1 (8 parent topics × 2 sub-topics, same
-- is_key_topic / risk_level / sort_order slots) per docs/ca-c-sub-topics.md.
-- Exam parameters reused from M1 (pass threshold 85%, mock 30 questions — the
-- mock template + question bank land in later migrations).
--
-- Codes use the CAC_ prefix because topics.code / sub_topics.code are globally
-- UNIQUE (the M1 codes cannot be reused). sub_topics inherit the exam via their
-- parent topic (sub_topics has no exam_id column).

DO $$
DECLARE
    ca_c       BIGINT;
    t_traffic  BIGINT;
    t_row      BIGINT;
    t_speed    BIGINT;
    t_lane     BIGINT;
    t_turn     BIGINT;
    t_alcohol  BIGINT;
    t_special  BIGINT;
    t_safety   BIGINT;
BEGIN
    -- 1. The exam. sort_order=1 keeps CA-M1 (sort_order=0) as the anonymous /
    --    pre-onboarding default (findDefaultActiveId orders by sort_order, id).
    INSERT INTO exams (state_code, license_class, name_en, name_zh, pass_threshold_percent, status, sort_order)
    VALUES ('CA', 'C', 'California Class C (Car)', '加州 C 类（小汽车）', 85, 'active', 1)
    RETURNING id INTO ca_c;

    -- 2. Parent topics (8) — same key/risk/sort layout as the M1 seed (V2).
    INSERT INTO topics (exam_id, code, name_en, name_zh, is_key_topic, risk_level, sort_order)
    VALUES (ca_c, 'CAC_TRAFFIC_CONTROLS', 'Traffic Signs & Signals', '交通标志与信号', TRUE, 'high', 10)
    RETURNING id INTO t_traffic;

    INSERT INTO topics (exam_id, code, name_en, name_zh, is_key_topic, risk_level, sort_order)
    VALUES (ca_c, 'CAC_RIGHT_OF_WAY', 'Right of Way', '通行权', TRUE, 'high', 20)
    RETURNING id INTO t_row;

    INSERT INTO topics (exam_id, code, name_en, name_zh, is_key_topic, risk_level, sort_order)
    VALUES (ca_c, 'CAC_SPEED_DISTANCE', 'Speed & Following Distance', '速度与跟车距离', TRUE, 'high', 30)
    RETURNING id INTO t_speed;

    INSERT INTO topics (exam_id, code, name_en, name_zh, is_key_topic, risk_level, sort_order)
    VALUES (ca_c, 'CAC_LANE_POSITION', 'Lane Use & Markings', '车道使用与标线', FALSE, 'medium', 40)
    RETURNING id INTO t_lane;

    INSERT INTO topics (exam_id, code, name_en, name_zh, is_key_topic, risk_level, sort_order)
    VALUES (ca_c, 'CAC_TURNING_MANEUVERS', 'Turns, Merging & Passing', '转弯、汇入与超车', FALSE, 'medium', 50)
    RETURNING id INTO t_turn;

    INSERT INTO topics (exam_id, code, name_en, name_zh, is_key_topic, risk_level, sort_order)
    VALUES (ca_c, 'CAC_ALCOHOL_DRUGS', 'Alcohol & Drugs', '酒精与药物', TRUE, 'high', 60)
    RETURNING id INTO t_alcohol;

    INSERT INTO topics (exam_id, code, name_en, name_zh, is_key_topic, risk_level, sort_order)
    VALUES (ca_c, 'CAC_SPECIAL_CONDITIONS', 'Special Driving Conditions', '特殊驾驶条件', FALSE, 'medium', 70)
    RETURNING id INTO t_special;

    INSERT INTO topics (exam_id, code, name_en, name_zh, is_key_topic, risk_level, sort_order)
    VALUES (ca_c, 'CAC_DRIVER_SAFETY', 'Driver & Occupant Safety', '驾驶人与乘员安全', TRUE, 'high', 80)
    RETURNING id INTO t_safety;

    -- 3. Sub-topics (16, 2 per parent). sort_order = parent_sort*10 + index.

    -- CAC_TRAFFIC_CONTROLS
    INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order) VALUES
    (t_traffic, 'CAC_TRAFFIC_SIGNALS', 'Traffic Signals & Light Colors', '交通信号灯与灯色',
     'Solid, arrow, and flashing red/yellow/green lights, what to do when a signal is not working (treat as all-way stop), and pedestrian signals (WALK / DON''T WALK / countdown / push button).', 101),
    (t_traffic, 'CAC_TRAFFIC_SIGNS', 'Regulatory & Warning Signs', '管制与警告标志',
     'STOP and YIELD signs, WRONG WAY, regulatory red/white and prohibitory signs, 5-sided school sign, diamond warning signs, and the meaning carried by sign shape and color.', 102);

    -- CAC_RIGHT_OF_WAY
    INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order) VALUES
    (t_row, 'CAC_ROW_INTERSECTIONS', 'Intersections, Roundabouts & Crossings', '路口、环岛与交叉口',
     'Who proceeds first at four-way stops, uncontrolled and T-intersections, left turns against oncoming traffic, roundabouts, blind intersections, and narrow-mountain-road rules.', 201),
    (t_row, 'CAC_ROW_VULNERABLE_USERS', 'Pedestrians, Bicyclists & Emergency Vehicles', '行人、自行车与应急车辆',
     'Yielding to pedestrians at crosswalks (including blind pedestrians and around children), bicyclists'' rights and the 3-foot passing rule and bike lanes, and yielding to emergency vehicles (Move Over).', 202);

    -- CAC_SPEED_DISTANCE
    INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order) VALUES
    (t_speed, 'CAC_SPEED_LIMITS', 'Speed Limits & Basic Speed Law', '限速与基本速度法',
     'The basic speed law (never faster than safe for conditions), maximum posted limits (65 mph freeways, 55 mph two-lane undivided and towing), school-zone / business / residential limits, and double-fine work zones.', 301),
    (t_speed, 'CAC_FOLLOWING_DISTANCE', 'Following Distance, Scanning & Blind Spots', '跟车距离、观察与盲区',
     'The 3-second following rule and extending it in poor conditions, tailgating risks, scanning surroundings, knowing what is at your sides and behind you, and checking blind spots before moving over.', 302);

    -- CAC_LANE_POSITION
    INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order) VALUES
    (t_lane, 'CAC_LANE_MARKINGS', 'Pavement Markings & Line Colors', '路面标线与线色',
     'Single/double solid and broken yellow lines, single/double solid and broken white lines, yield lines, end-of-lane markings, two-way left-turn center lanes, and what crossing/passing each permits.', 401),
    (t_lane, 'CAC_LANE_USE_PARKING', 'Lane Selection, Special Lanes & Parking', '车道选择、专用道与停车',
     'Choosing and changing lanes safely; carpool/HOV, center left-turn, bicycle, passing, and turnout lanes; plus parking rules — parking on hills (wheel direction), colored-curb meanings, and prohibited zones.', 402);

    -- CAC_TURNING_MANEUVERS
    INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order) VALUES
    (t_turn, 'CAC_TURNS_UTURNS', 'Turns & U-Turns', '转弯与掉头',
     'Right and left turns from/into the correct lane, right turn against a red light/arrow, left turn against a red light, hand signals, and where U-turns are legal versus prohibited.', 501),
    (t_turn, 'CAC_MERGE_PASS', 'Merging, Exiting & Passing', '汇入、驶出与超车',
     'Merging onto and with freeway traffic, using acceleration/deceleration lanes, exiting, crossing or entering traffic, passing legally and safely, and being passed (maintain speed, do not accelerate).', 502);

    -- CAC_ALCOHOL_DRUGS
    INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order) VALUES
    (t_alcohol, 'CAC_ALCOHOL_BAC', 'BAC Limits & DUI Consequences', 'BAC 限值与酒驾后果',
     'California BAC limits (0.08% adults, 0.04% commercial, 0.01%/zero for under-21), implied consent, open-container and cannabis-in-vehicle rules, and DUI arrest and conviction penalties.', 601),
    (t_alcohol, 'CAC_DRUGS_IMPAIRMENT', 'Drugs, Cannabis & Under-21 Zero Tolerance', '药物、大麻与 21 岁以下零容忍',
     'Impairment from cannabis, prescription, and OTC drugs as well as alcohol; the zero-tolerance law for drivers under 21 possessing or consuming alcohol; how small amounts of any impairing substance degrade driving.', 602);

    -- CAC_SPECIAL_CONDITIONS
    INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order) VALUES
    (t_special, 'CAC_CONDITIONS_ADVERSE', 'Night, Weather, Skids & Visibility', '夜间、天气、打滑与视线',
     'Driving in darkness and sun glare, slippery roads, hydroplaning, slippery-surface and locked-wheel skids and how to recover, fog/heavy smoke, high winds, snow/mud, and flooded roads.', 701),
    (t_special, 'CAC_HAZARDS_RAILROAD_LARGE', 'Railroad Crossings, Work Zones & Large Vehicles', '铁道口、施工区与大型车辆',
     'Railroad and light-rail crossings, road workers and work/double-fine zones, sharing the road with large trucks and buses (the No-Zone blind spots), school-bus flashing-red-light stops, and slow-moving vehicles.', 702);

    -- CAC_DRIVER_SAFETY
    INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, description, sort_order) VALUES
    (t_safety, 'CAC_OCCUPANT_PROTECTION', 'Seat Belts, Child Restraints & Air Bags', '安全带、儿童约束与气囊',
     'Seat belt law for driver and passengers, child restraint system / safety seat requirements (age, height, weight, rear-facing), how air bags work with belts and seating position, and never leaving children or pets unattended.', 801),
    (t_safety, 'CAC_EMERGENCIES_DISTRACTION', 'Vehicle Emergencies & Distracted Driving', '车辆应急与分心驾驶',
     'Handling a tire blowout, brake failure, a stuck accelerator, overheating, and a vehicle disabled on the freeway; what to do in a collision (stop, exchange info, report); and distracted driving — cell phones, texting, and the stricter rule for minors.', 802);
END $$;
