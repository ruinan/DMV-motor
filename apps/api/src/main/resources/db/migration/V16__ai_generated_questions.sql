-- V16: AI-generated questions filling thin sub-topics from B3 retag.
-- Source: aiqgen pipeline (see apps/api/.../aiqgen/), DeepSeek-chat.
-- Generation control loop: 4 gates (format / coverage / difficulty / runbook fact-check).


-- ============================================================
-- Sub-topic: LANE_SPLITTING_SHARING  (7 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'LANE_SPLITTING_SHARING';

    -- Q1: What is a potential hazard of lane splitting mentioned in the handbook excerpt?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is a potential hazard of lane splitting mentioned in the handbook excerpt?', '[{"key":"A","text":"A driver may suddenly open a car door."},{"key":"B","text":"The motorcycle may stall due to low speed."},{"key":"C","text":"Other motorcyclists may try to race you."},{"key":"D","text":"The road surface may be slippery from oil."}]'::jsonb, 'The handbook warns that during lane splitting, a door could open, a hand could come out a window, or a vehicle could turn or change lanes suddenly.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '手册摘录中提到的车道分流的一个潜在危险是什么？', '[{"key":"A","text":"驾驶员可能突然打开车门。"},{"key":"B","text":"摩托车可能因低速而熄火。"},{"key":"C","text":"其他摩托车手可能试图与你竞速。"},{"key":"D","text":"路面可能因油污而湿滑。"}]'::jsonb, '手册警告，在车道分流时，车门可能突然打开，手可能伸出车窗，或车辆可能突然转弯或变道。', 'active');

    -- Q2: Why should a motorcyclist avoid riding next to passenger vehicles or trucks in other lanes?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Why should a motorcyclist avoid riding next to passenger vehicles or trucks in other lanes?', '[{"key":"A","text":"It reduces fuel efficiency due to wind resistance."},{"key":"B","text":"The driver may not see you in their blind spot and could change lanes without warning."},{"key":"C","text":"It is illegal to ride next to any vehicle in California."},{"key":"D","text":"The motorcycle''s engine may overheat from the proximity."}]'::jsonb, 'The handbook states that riding next to vehicles in other lanes puts you in the driver''s blind spot, and they could change lanes without warning, also blocking your escape route.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '为什么摩托车手应避免与其他车道上的乘用车或卡车并排行驶？', '[{"key":"A","text":"由于风阻会降低燃油效率。"},{"key":"B","text":"驾驶员可能看不到你在盲区中，并可能在没有警告的情况下变道。"},{"key":"C","text":"在加州，与任何车辆并排行驶都是违法的。"},{"key":"D","text":"摩托车的发动机可能因靠近而过热。"}]'::jsonb, '手册指出，与其他车道上的车辆并排行驶会使你处于驾驶员的盲区中，他们可能在没有警告的情况下变道，同时也会阻挡你的逃生路线。', 'active');

    -- Q3: What is the primary risk of lane splitting between rows of stopped or slow-moving vehicles?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the primary risk of lane splitting between rows of stopped or slow-moving vehicles?', '[{"key":"A","text":"You may be struck by a vehicle that suddenly turns or changes lanes."},{"key":"B","text":"You are legally required to maintain a speed differential of no more than 10 mph."},{"key":"C","text":"You must always ride in the leftmost lane while splitting."},{"key":"D","text":"Lane splitting is only permitted on highways with a speed limit over 45 mph."}]'::jsonb, 'The handbook states that riding between rows of stopped or moving vehicles can leave you vulnerable, as a vehicle could turn suddenly or change lanes, a door could open, or a hand could come out a window.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在停止或缓慢行驶的车流中穿行（车道分流）的主要风险是什么？', '[{"key":"A","text":"你可能被突然转弯或变道的车辆撞到。"},{"key":"B","text":"法律要求你保持不超过10英里的速度差。"},{"key":"C","text":"分流时必须始终在最左侧车道行驶。"},{"key":"D","text":"只有在限速超过45英里的高速公路上才允许车道分流。"}]'::jsonb, '手册指出，在停止或行驶的车辆之间骑行可能使你处于危险之中，因为车辆可能突然转弯或变道，车门可能打开，或手可能伸出窗外。', 'active');

    -- Q4: What is the primary risk of lane splitting according to the California Motorcycle Handbook?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the primary risk of lane splitting according to the California Motorcycle Handbook?', '[{"key":"A","text":"It is illegal in most situations and can result in a traffic citation."},{"key":"B","text":"A vehicle could turn suddenly, change lanes, or a door could open."},{"key":"C","text":"It increases fuel efficiency but reduces safety."},{"key":"D","text":"It is only permitted on highways with a speed limit above 65 mph."}]'::jsonb, 'The handbook states that riding between rows of stopped or moving vehicles can leave you vulnerable, and a vehicle could turn suddenly or change lanes, a door could open, or a hand could come out a window.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据加州摩托车手册，车道分流的主要风险是什么？', '[{"key":"A","text":"在大多数情况下是违法的，可能导致交通罚单。"},{"key":"B","text":"车辆可能突然转弯、变道，或车门可能打开。"},{"key":"C","text":"它提高燃油效率但降低安全性。"},{"key":"D","text":"仅允许在限速超过65英里的高速公路上进行。"}]'::jsonb, '手册指出，在停止或移动的车辆之间骑行可能使您处于危险之中，车辆可能突然转弯或变道，车门可能打开，或手可能伸出窗外。', 'active');

    -- Q5: When lane splitting, what is a key risk mentioned in the handbook that a motorcyclist should be awar...
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When lane splitting, what is a key risk mentioned in the handbook that a motorcyclist should be aware of?', '[{"key":"A","text":"A vehicle could turn suddenly or change lanes, a door could open, or a hand could come out a window."},{"key":"B","text":"The motorcyclist might be cited for reckless driving."},{"key":"C","text":"The motorcyclist could lose balance due to wind from other vehicles."},{"key":"D","text":"The motorcyclist might accidentally hit a pedestrian crossing the street."}]'::jsonb, 'The handbook explicitly warns that when lane splitting, a vehicle could turn suddenly or change lanes, a door could open, or a hand could come out a window, making it a vulnerable position.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在进行车道分流时，手册中提到摩托车手应注意的一个关键风险是什么？', '[{"key":"A","text":"车辆可能突然转弯或变道，车门可能打开，或手可能伸出窗外。"},{"key":"B","text":"摩托车手可能因鲁莽驾驶被开罚单。"},{"key":"C","text":"摩托车手可能因其他车辆的风力而失去平衡。"},{"key":"D","text":"摩托车手可能意外撞到过马路的行人。"}]'::jsonb, '手册明确警告，在车道分流时，车辆可能突然转弯或变道，车门可能打开，或手可能伸出窗外，这使摩托车手处于易受攻击的位置。', 'active');

    -- Q6: According to the California DMV handbook, what is the primary risk when lane splitting between rows ...
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'According to the California DMV handbook, what is the primary risk when lane splitting between rows of stopped or moving vehicles?', '[{"key":"A","text":"You may be cited for an illegal lane change."},{"key":"B","text":"A vehicle could turn suddenly, change lanes, or a door could open."},{"key":"C","text":"Your motorcycle may overheat due to reduced airflow."},{"key":"D","text":"You will lose your right-of-way at intersections."}]'::jsonb, 'The handbook states that riding between rows of stopped or moving vehicles can leave you vulnerable, as a vehicle could turn suddenly, change lanes, a door could open, or a hand could come out a window.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据加州DMV手册，在停止或移动的车流之间进行车道分流时，主要风险是什么？', '[{"key":"A","text":"你可能会因非法变道而被开罚单。"},{"key":"B","text":"车辆可能突然转弯、变道，或车门可能打开。"},{"key":"C","text":"你的摩托车可能因气流减少而过热。"},{"key":"D","text":"你将在交叉路口失去路权。"}]'::jsonb, '手册指出，在停止或移动的车流之间骑行可能使你处于危险之中，因为车辆可能突然转弯、变道，车门可能打开，或者可能有手伸出窗外。', 'active');

    -- Q7: Why does the California DMV handbook advise against riding next to passenger vehicles or trucks in o...
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Why does the California DMV handbook advise against riding next to passenger vehicles or trucks in other lanes?', '[{"key":"A","text":"It is illegal to ride alongside another vehicle in California."},{"key":"B","text":"You might be in the driver’s blind spot, and the driver could change lanes without warning."},{"key":"C","text":"It reduces your fuel efficiency due to increased drag."},{"key":"D","text":"The other vehicle may be forced to slow down for you."}]'::jsonb, 'The handbook warns: ''Do not ride next to passenger vehicles or trucks in other lanes if you do not have to because you might be in the driver’s blind spot. The driver could change lanes without warning.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '为什么加州DMV手册建议不要在其他车道上与乘用车或卡车并排骑行？', '[{"key":"A","text":"在加州与其他车辆并排骑行是违法的。"},{"key":"B","text":"你可能处于驾驶员的盲点中，驾驶员可能在没有警告的情况下变道。"},{"key":"C","text":"由于增加的空气阻力，这会降低你的燃油效率。"},{"key":"D","text":"其他车辆可能被迫为你减速。"}]'::jsonb, '手册警告：''如果没必要，不要在其他车道上与乘用车或卡车并排骑行，因为你可能处于驾驶员的盲点中。驾驶员可能在没有警告的情况下变道。''', 'active');

END $$;

-- ============================================================
-- Sub-topic: MANEUVERS_SWERVE_BRAKE  (7 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'MANEUVERS_SWERVE_BRAKE';

    -- Q1: When you need to avoid a collision by swerving and braking, what is the correct sequence according t...
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'D', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When you need to avoid a collision by swerving and braking, what is the correct sequence according to the handbook?', '[{"key":"A","text":"Brake and swerve at the same time."},{"key":"B","text":"Swerve first, then brake."},{"key":"C","text":"Brake first, then swerve."},{"key":"D","text":"Either swerve then brake or brake then swerve, but never both at once."}]'::jsonb, 'The handbook states: ''If braking is required, separate it from swerving. Brake before or after, never while swerving.'' This means you can either swerve then brake or brake then swerve, but never both at the same time.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当需要通过紧急规避和制动来避免碰撞时，根据手册，正确的顺序是什么？', '[{"key":"A","text":"同时进行制动和紧急规避。"},{"key":"B","text":"先紧急规避，再制动。"},{"key":"C","text":"先制动，再紧急规避。"},{"key":"D","text":"可以先紧急规避再制动，或先制动再紧急规避，但绝不能同时进行。"}]'::jsonb, '手册指出：“如果需要制动，请将其与紧急规避分开。在紧急规避之前或之后制动，切勿在紧急规避过程中制动。”这意味着可以先紧急规避再制动，或先制动再紧急规避，但绝不能同时进行。', 'active');

    -- Q2: If you must stop quickly while leaning in a curve, what is the recommended technique?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'If you must stop quickly while leaning in a curve, what is the recommended technique?', '[{"key":"A","text":"Apply maximum front brake immediately to stop as fast as possible."},{"key":"B","text":"Straighten the motorcycle first, then brake."},{"key":"C","text":"Keep leaning and apply both brakes firmly."},{"key":"D","text":"Downshift aggressively and use only the rear brake."}]'::jsonb, 'The handbook says: ''If you must stop quickly while turning or riding a curve, the best technique is to straighten the motorcycle first and then brake.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果在弯道中倾斜车身时需要快速停车，推荐的技巧是什么？', '[{"key":"A","text":"立即施加最大前制动以尽快停车。"},{"key":"B","text":"先扶正摩托车，再制动。"},{"key":"C","text":"保持倾斜并用力施加前后制动。"},{"key":"D","text":"猛烈降档并仅使用后制动。"}]'::jsonb, '手册指出：“如果在转弯或骑行弯道时必须快速停车，最佳技巧是先扶正摩托车，然后再制动。”', 'active');

    -- Q3: When swerving to avoid an obstacle, how should you initiate the turn?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When swerving to avoid an obstacle, how should you initiate the turn?', '[{"key":"A","text":"Turn the handlebars sharply in the direction you want to go."},{"key":"B","text":"Lean your body in the direction of the turn."},{"key":"C","text":"Apply a small amount of hand pressure to the handlegrip on the side of your intended escape direction."},{"key":"D","text":"Press down on the footrest on the side you want to turn."}]'::jsonb, 'The handbook explains: ''To swerve, apply a small amount of hand pressure to the handlegrip on the side of your intended direction of escape.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当紧急规避障碍物时，应如何启动转向？', '[{"key":"A","text":"猛地把车把转向你想要去的方向。"},{"key":"B","text":"身体向转弯方向倾斜。"},{"key":"C","text":"在预期逃逸方向一侧的手把上施加少量手部压力。"},{"key":"D","text":"向下踩你想要转弯一侧的脚踏。"}]'::jsonb, '手册解释：“要紧急规避，在预期逃逸方向一侧的手把上施加少量手部压力。”', 'active');

    -- Q4: If you must stop quickly while riding a curve, what is the best technique according to the handbook?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'If you must stop quickly while riding a curve, what is the best technique according to the handbook?', '[{"key":"A","text":"Apply both brakes firmly while leaning through the curve."},{"key":"B","text":"Straighten the motorcycle first, then brake."},{"key":"C","text":"Swerve to the inside of the curve and then brake."},{"key":"D","text":"Use only the rear brake to maintain stability."}]'::jsonb, 'The handbook states: ''If you must stop quickly while turning or riding a curve, the best technique is to straighten the motorcycle first and then brake.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果在弯道行驶时需要快速停车，根据手册，最佳技巧是什么？', '[{"key":"A","text":"在弯道中倾斜时用力同时使用前后刹车。"},{"key":"B","text":"先扶正摩托车，然后再刹车。"},{"key":"C","text":"先向弯道内侧急转，然后再刹车。"},{"key":"D","text":"仅使用后刹车以保持稳定。"}]'::jsonb, '手册指出：''如果在转弯或弯道行驶时必须快速停车，最佳技巧是先扶正摩托车，然后再刹车。''', 'active');

    -- Q5: According to the handbook, what is the correct action if you need to brake while swerving?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'According to the handbook, what is the correct action if you need to brake while swerving?', '[{"key":"A","text":"Apply the front brake smoothly while continuing the swerve."},{"key":"B","text":"Brake and swerve simultaneously to maximize control."},{"key":"C","text":"Separate braking from swerving; brake before or after, never during."},{"key":"D","text":"Use only the rear brake while swerving to avoid a fall."}]'::jsonb, 'The handbook states: ''If braking is required, separate it from swerving. Brake before or after, never while swerving, especially the front brake as this may cause the motorcycle to fall over.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据手册，如果在急转时需要刹车，正确的做法是什么？', '[{"key":"A","text":"在继续急转时平稳地施加前刹车。"},{"key":"B","text":"同时刹车和急转以最大化控制。"},{"key":"C","text":"将刹车与急转分开；在急转之前或之后刹车，绝不在急转过程中刹车。"},{"key":"D","text":"在急转时仅使用后刹车以避免摔倒。"}]'::jsonb, '手册指出：''如果需要刹车，将其与急转分开。在急转之前或之后刹车，绝不在急转过程中刹车，尤其是前刹车，因为这可能导致摩托车翻倒。''', 'active');

    -- Q6: What is the recommended technique for swerving to avoid an obstacle when braking is not possible?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the recommended technique for swerving to avoid an obstacle when braking is not possible?', '[{"key":"A","text":"Press the handlegrip on the opposite side of your intended direction, then press the other to recover."},{"key":"B","text":"Press the handlegrip on the side of your intended direction, then press the opposite to recover."},{"key":"C","text":"Turn the handlebars sharply in the direction you want to go without leaning the motorcycle."},{"key":"D","text":"Lean your body in the direction of the swerve while keeping the motorcycle upright."}]'::jsonb, 'The handbook states: ''To swerve to the left, press the left handlegrip, then press the right to recover. To swerve to the right, press right, then left.'' This means press the grip on the side of your intended direction first.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当无法刹车时，为避开障碍物，推荐的急转技巧是什么？', '[{"key":"A","text":"按压你意图方向相反一侧的车把，然后按压另一侧以恢复。"},{"key":"B","text":"按压你意图方向一侧的车把，然后按压相反一侧以恢复。"},{"key":"C","text":"在不倾斜摩托车的情况下，将车把猛转向你想去的方向。"},{"key":"D","text":"身体向急转方向倾斜，同时保持摩托车直立。"}]'::jsonb, '手册指出：''要向左急转，按压左车把，然后按压右车把以恢复。要向右急转，按压右车把，然后按压左车把。'' 这意味着首先按压你意图方向一侧的车把。', 'active');

    -- Q7: When you need to avoid an obstacle and do not have enough room to stop, what is the correct sequence...
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When you need to avoid an obstacle and do not have enough room to stop, what is the correct sequence according to the handbook?', '[{"key":"A","text":"Brake and swerve at the same time to maximize control."},{"key":"B","text":"Swerve first, then brake if needed, but never brake while swerving."},{"key":"C","text":"Brake first to reduce speed, then swerve around the obstacle."},{"key":"D","text":"Apply the front brake firmly while swerving to stop quickly."}]'::jsonb, 'The handbook states: ''If braking is required, separate it from swerving. Brake before or after, never while swerving, especially the front brake.'' It also emphasizes that when you cannot stop, the only way may be to swerve. The correct sequence is to swerve first, then brake if needed, not the other way around.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当需要避开障碍物且没有足够空间停车时，根据手册的正确操作顺序是什么？', '[{"key":"A","text":"同时刹车和转向以最大化控制。"},{"key":"B","text":"先转向，必要时再刹车，但绝不在转向时刹车。"},{"key":"C","text":"先刹车减速，再转向绕过障碍物。"},{"key":"D","text":"转向时用力使用前刹车以快速停下。"}]'::jsonb, '手册指出：''如果需要刹车，应将其与转向分开。在转向之前或之后刹车，绝不要在转向时刹车，尤其是前刹车。'' 同时强调当无法停车时，唯一方法可能是转向。正确顺序是先转向，必要时再刹车，而不是相反。', 'active');

END $$;

-- ============================================================
-- Sub-topic: EMERGENCIES_MECHANICAL  (10 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'EMERGENCIES_MECHANICAL';

    -- Q1: What is the correct immediate action if your throttle becomes stuck while riding?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the correct immediate action if your throttle becomes stuck while riding?', '[{"key":"A","text":"Apply the front brake firmly and downshift quickly."},{"key":"B","text":"Twist the throttle back and forth several times, then if still stuck, operate the engine cut-off switch and pull in the clutch simultaneously."},{"key":"C","text":"Turn off the ignition key immediately and coast to a stop."},{"key":"D","text":"Release the clutch and accelerate to break the cable free."}]'::jsonb, 'The handbook states that if the throttle is stuck, first twist it back and forth several times to free it. If it remains stuck, immediately operate the engine cut-off switch and pull in the clutch at the same time to remove power from the rear wheel.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '骑行中油门卡死时，正确的立即操作是什么？', '[{"key":"A","text":"用力捏前刹车并快速降档。"},{"key":"B","text":"反复来回拧动油门，如果仍然卡住，同时操作发动机熄火开关并捏住离合器。"},{"key":"C","text":"立即关闭点火钥匙并滑行至停车。"},{"key":"D","text":"松开离合器并加速以挣脱油门拉线。"}]'::jsonb, '手册指出，如果油门卡住，首先反复来回拧动油门以尝试松开。如果仍然卡住，应立即同时操作发动机熄火开关并捏住离合器，以切断后轮动力。', 'active');

    -- Q2: When a tire goes flat while riding, what should you do with the brakes?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When a tire goes flat while riding, what should you do with the brakes?', '[{"key":"A","text":"Apply both brakes firmly and immediately to stop as quickly as possible."},{"key":"B","text":"Gradually apply the brake of the tire that is not flat, if you are sure which one it is."},{"key":"C","text":"Use only the rear brake to maintain stability."},{"key":"D","text":"Do not use any brakes; coast to a stop using engine braking only."}]'::jsonb, 'The handbook advises that if braking is required during a tire failure, gradually apply the brake of the tire that is not flat, if you are sure which one it is. This helps maintain control.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '骑行中轮胎漏气时，应如何操作刹车？', '[{"key":"A","text":"同时用力捏前后刹车，尽快停车。"},{"key":"B","text":"如果确定哪个轮胎没气，逐渐对未漏气的轮胎施加刹车。"},{"key":"C","text":"仅使用后刹车以保持稳定。"},{"key":"D","text":"不使用任何刹车，仅靠发动机制动滑行至停车。"}]'::jsonb, '手册建议，轮胎故障时如需刹车，如果确定哪个轮胎没气，应逐渐对未漏气的轮胎施加刹车，这有助于保持控制。', 'active');

    -- Q3: What is the first sign of an engine seizure according to the handbook?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the first sign of an engine seizure according to the handbook?', '[{"key":"A","text":"A loud knocking noise from the engine."},{"key":"B","text":"A sudden loss of power or a change in the engine''s sound."},{"key":"C","text":"The rear wheel locking up immediately."},{"key":"D","text":"Smoke coming from the exhaust."}]'::jsonb, 'The handbook states that the first sign of engine seizure may be a loss of engine power or a change in the engine''s sound. The rider should then squeeze the clutch lever and pull off the road.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据手册，引擎死火的第一个迹象是什么？', '[{"key":"A","text":"引擎发出响亮的敲击声。"},{"key":"B","text":"突然失去动力或引擎声音发生变化。"},{"key":"C","text":"后轮立即锁死。"},{"key":"D","text":"排气管冒烟。"}]'::jsonb, '手册指出，引擎死火的第一个迹象可能是失去动力或引擎声音发生变化。骑手应随后捏住离合器并驶离道路。', 'active');

    -- Q4: If you experience a chain breakage while riding, what is the recommended action?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'If you experience a chain breakage while riding, what is the recommended action?', '[{"key":"A","text":"Immediately apply the front brake and downshift to slow down."},{"key":"B","text":"Roll off the throttle and brake to a stop."},{"key":"C","text":"Accelerate to regain power and steer to the shoulder."},{"key":"D","text":"Pull in the clutch and coast to a stop without braking."}]'::jsonb, 'The handbook says that if the chain breaks, you will notice an instant loss of power to the rear wheel. The recommended action is to roll off the throttle and brake to a stop.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '骑行中链条断裂时，建议采取什么行动？', '[{"key":"A","text":"立即捏前刹车并降档减速。"},{"key":"B","text":"收油门并刹车至停车。"},{"key":"C","text":"加速以恢复动力并驶向路肩。"},{"key":"D","text":"捏住离合器并滑行至停车，不使用刹车。"}]'::jsonb, '手册指出，链条断裂时你会注意到后轮瞬间失去动力。建议的行动是收油门并刹车至停车。', 'active');

    -- Q5: What is the correct immediate action if your throttle becomes stuck while riding?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the correct immediate action if your throttle becomes stuck while riding?', '[{"key":"A","text":"Apply both brakes firmly and downshift quickly."},{"key":"B","text":"Operate the engine cut-off switch and pull in the clutch at the same time."},{"key":"C","text":"Turn the handlebars sharply to one side to break the cable free."},{"key":"D","text":"Release the throttle and coast to a stop without using the clutch."}]'::jsonb, 'According to the handbook, if the throttle stays stuck, immediately operate the engine cut-off switch and pull in the clutch at the same time. This removes power from the rear wheel, allowing you to regain control.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '骑行中油门卡死时，正确的立即操作是什么？', '[{"key":"A","text":"用力同时使用前后刹车并快速降档。"},{"key":"B","text":"同时操作发动机熄火开关并捏紧离合器。"},{"key":"C","text":"将车把猛地转向一侧以扯断油门线。"},{"key":"D","text":"松开油门并滑行至停车，不使用离合器。"}]'::jsonb, '根据手册，如果油门卡死，应立即同时操作发动机熄火开关并捏紧离合器，从而切断后轮动力，使你能重新控制摩托车。', 'active');

    -- Q6: What is the recommended first action if you suspect a tire has gone flat while riding?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the recommended first action if you suspect a tire has gone flat while riding?', '[{"key":"A","text":"Immediately apply the brake on the flat tire to slow down."},{"key":"B","text":"Hold the handlegrips firmly, ease off the throttle, and keep a straight course."},{"key":"C","text":"Accelerate slightly to stabilize the motorcycle before braking."},{"key":"D","text":"Downshift aggressively to use engine braking as the primary slowdown method."}]'::jsonb, 'The handbook states: if either tire goes flat, hold the handlegrips firmly, ease off the throttle, and keep a straight course. Braking should only be applied gradually to the tire that is not flat, if you are sure which one it is.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '骑行中怀疑轮胎漏气时，建议首先采取什么行动？', '[{"key":"A","text":"立即对漏气轮胎施加刹车以减速。"},{"key":"B","text":"紧握车把，缓慢收油，并保持直线行驶。"},{"key":"C","text":"稍微加速以稳定摩托车，然后再刹车。"},{"key":"D","text":"猛烈降档，利用发动机制动作为主要减速方式。"}]'::jsonb, '手册指出：如果任何轮胎漏气，应紧握车把，缓慢收油，并保持直线行驶。只有在确定哪个轮胎没气时，才可逐渐对未漏气轮胎施加刹车。', 'active');

    -- Q7: What should you do if your motorcycle's engine seizes while you are riding?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What should you do if your motorcycle''s engine seizes while you are riding?', '[{"key":"A","text":"Keep the throttle open to try to force the engine to restart."},{"key":"B","text":"Squeeze the clutch lever to disengage the engine from the rear wheel, then pull off the road and stop."},{"key":"C","text":"Apply the rear brake hard to prevent the rear wheel from locking."},{"key":"D","text":"Shift into neutral and immediately restart the engine while moving."}]'::jsonb, 'The handbook says: if the engine seizes, squeeze the clutch lever to disengage the engine from the rear wheel, pull off the road and stop. This prevents the locked engine from causing a rear-wheel skid.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '骑行中引擎死火（抱死）时，你应该怎么做？', '[{"key":"A","text":"保持油门全开，试图强制引擎重新启动。"},{"key":"B","text":"捏紧离合器手柄，使引擎与后轮分离，然后驶离道路并停车。"},{"key":"C","text":"用力踩后刹车，防止后轮抱死。"},{"key":"D","text":"挂入空档，并在行驶中立即重新启动引擎。"}]'::jsonb, '手册指出：如果引擎抱死，应捏紧离合器手柄使引擎与后轮分离，然后驶离道路并停车。这可以防止锁死的引擎导致后轮打滑。', 'active');

    -- Q8: What is the correct response if your motorcycle's drive chain breaks while you are riding?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the correct response if your motorcycle''s drive chain breaks while you are riding?', '[{"key":"A","text":"Immediately pull in the clutch and accelerate to clear the broken chain."},{"key":"B","text":"Roll off the throttle and brake to a stop."},{"key":"C","text":"Downshift to use engine braking and steer to the shoulder."},{"key":"D","text":"Apply the front brake only and keep the throttle steady."}]'::jsonb, 'The handbook states: if the chain breaks, you will notice an instant loss of power to the rear wheel. Roll off the throttle and brake to a stop. This is the safe procedure to follow.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '骑行中链条断裂时，正确的应对方法是什么？', '[{"key":"A","text":"立即捏离合器并加速，以甩开断裂的链条。"},{"key":"B","text":"收油门并刹车至停车。"},{"key":"C","text":"降档利用发动机制动，并驶向路肩。"},{"key":"D","text":"仅使用前刹车并保持油门稳定。"}]'::jsonb, '手册指出：如果链条断裂，你会立即感到后轮失去动力。应收油门并刹车至停车。这是安全操作步骤。', 'active');

    -- Q9: What is the correct immediate action if your throttle becomes stuck while riding?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the correct immediate action if your throttle becomes stuck while riding?', '[{"key":"A","text":"Firmly apply both brakes and steer to the shoulder."},{"key":"B","text":"Twist the throttle back and forth several times, then if still stuck, operate the engine cut-off switch and pull in the clutch simultaneously."},{"key":"C","text":"Downshift quickly to slow the motorcycle using engine compression."},{"key":"D","text":"Turn the handlebars sharply to one side to break the throttle cable free."}]'::jsonb, 'The handbook states: if the throttle stays stuck, immediately operate the engine cut-off switch and pull in the clutch at the same time. Twisting the throttle back and forth first may free it, but if not, the cut-off switch and clutch are the correct immediate actions.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '骑行中油门卡死时，正确的立即操作是什么？', '[{"key":"A","text":"用力同时使用前后刹车，并转向路肩。"},{"key":"B","text":"反复扭转油门把手数次，若仍卡死，立即同时操作发动机熄火开关并捏紧离合器。"},{"key":"C","text":"快速降档，利用发动机制动减速。"},{"key":"D","text":"将车把猛转向一侧，以扯断油门拉线。"}]'::jsonb, '手册指出：如果油门持续卡死，应立即同时操作发动机熄火开关并捏紧离合器。先反复扭转油门把手可能使其复位，但若无效，熄火开关和离合器才是正确的即时操作。', 'active');

    -- Q10: When a rear tire goes flat while riding, what is the recommended braking technique?
    INSERT INTO questions (primary_topic_id, sub_topic_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When a rear tire goes flat while riding, what is the recommended braking technique?', '[{"key":"A","text":"Apply the rear brake firmly to stabilize the motorcycle."},{"key":"B","text":"Apply both brakes evenly to slow down as quickly as possible."},{"key":"C","text":"Gradually apply the front brake only, if you are sure the rear tire is flat."},{"key":"D","text":"Do not brake at all; coast to a stop using engine braking only."}]'::jsonb, 'The handbook advises: if braking is required, gradually apply the brake of the tire that is not flat, if you are sure which one it is. For a rear flat, that means using the front brake gradually.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '骑行中后轮爆胎时，推荐的刹车方法是什么？', '[{"key":"A","text":"用力使用后刹车以稳定摩托车。"},{"key":"B","text":"均匀使用前后刹车，尽快减速。"},{"key":"C","text":"如果确定是后轮爆胎，仅逐渐使用前刹车。"},{"key":"D","text":"完全不刹车，仅利用发动机制动滑行至停止。"}]'::jsonb, '手册建议：如果需要刹车，在确定哪个轮胎没气的情况下，逐渐使用未爆胎轮胎的刹车。对于后轮爆胎，这意味着逐渐使用前刹车。', 'active');

END $$;
