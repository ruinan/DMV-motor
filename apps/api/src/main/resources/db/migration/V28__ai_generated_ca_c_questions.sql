-- AI-generated questions (aiqgen pipeline, DeepSeek-chat).
-- Generation control loop: 4 gates (format / coverage / difficulty / runbook fact-check).
-- Grounding excerpts fetched transiently from the official handbook (not vendored).


-- ============================================================
-- Sub-topic: CAC_TRAFFIC_SIGNALS  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_TRAFFIC_SIGNALS';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: What must a driver do when approaching a traffic signal showing a flashing red light?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What must a driver do when approaching a traffic signal showing a flashing red light?', '[{"key":"A","text":"Slow down and proceed with caution without stopping."},{"key":"B","text":"Treat it as a yield sign and give way to traffic."},{"key":"C","text":"Make a full stop and proceed only when safe."},{"key":"D","text":"Stop only if pedestrians are present, otherwise continue."}]'::jsonb, 'According to the handbook, a flashing red light must be treated exactly like a STOP sign: make a full stop, then proceed when safe.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当驾驶员接近显示闪烁红灯的交通信号灯时，应该怎么做？', '[{"key":"A","text":"减速并谨慎通过，无需停车。"},{"key":"B","text":"将其视为让行标志，给其他交通让行。"},{"key":"C","text":"完全停下，只有在安全时才继续行驶。"},{"key":"D","text":"只有在有行人时才停车，否则继续行驶。"}]'::jsonb, '根据手册，闪烁的红灯必须完全当作停车标志处理：完全停下，然后在安全时继续行驶。', 'active');

    -- Q2: What does a solid yellow arrow indicate to a driver?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a solid yellow arrow indicate to a driver?', '[{"key":"A","text":"The protected turn is ending; finish the turn cautiously or stop if safe."},{"key":"B","text":"You may turn but it is not protected; yield to oncoming traffic."},{"key":"C","text":"Stop and remain stopped until a green arrow appears."},{"key":"D","text":"Proceed with caution; the light will soon turn green."}]'::jsonb, 'The handbook states that a yellow arrow means the protected turn is ending; you should finish the turn cautiously or stop if you can do so safely.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '黄色实心箭头灯对驾驶员意味着什么？', '[{"key":"A","text":"受保护的转弯即将结束；谨慎完成转弯或在安全时停下。"},{"key":"B","text":"你可以转弯，但不受保护；需让行迎面而来的车辆。"},{"key":"C","text":"停下并保持停止，直到出现绿色箭头。"},{"key":"D","text":"谨慎通行；信号灯即将变绿。"}]'::jsonb, '手册指出，黄色箭头表示受保护的转弯即将结束；应谨慎完成转弯或在安全时停下。', 'active');

    -- Q3: What should a driver do when a traffic signal is completely dark (not working)?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What should a driver do when a traffic signal is completely dark (not working)?', '[{"key":"A","text":"Proceed through the intersection without stopping."},{"key":"B","text":"Treat the intersection as an all-way stop."},{"key":"C","text":"Yield to traffic on the right only."},{"key":"D","text":"Stop and wait for a police officer to direct traffic."}]'::jsonb, 'The handbook explicitly says that when a traffic signal is not working (dark), treat the intersection as an all-way STOP.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当交通信号灯完全不亮（不工作）时，驾驶员应该怎么做？', '[{"key":"A","text":"无需停车直接通过交叉路口。"},{"key":"B","text":"将交叉路口视为四面停车。"},{"key":"C","text":"只让行右侧的车辆。"},{"key":"D","text":"停下并等待警察指挥交通。"}]'::jsonb, '手册明确说明，当交通信号灯不亮（不工作）时，应将交叉路口视为四面停车。', 'active');

    -- Q4: What does a flashing DON'T WALK signal mean for a pedestrian?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a flashing DON''T WALK signal mean for a pedestrian?', '[{"key":"A","text":"Pedestrians may start crossing if no vehicles are coming."},{"key":"B","text":"Pedestrians must not start crossing; those already crossing should finish."},{"key":"C","text":"Pedestrians must stop and wait for a green light."},{"key":"D","text":"Pedestrians can cross but must yield to vehicles."}]'::jsonb, 'The handbook states that a flashing DON''T WALK (or flashing hand) means pedestrians must not start crossing; if already crossing, they should finish. Drivers must still yield to pedestrians in the crosswalk.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '闪烁的“禁止行走”信号对行人意味着什么？', '[{"key":"A","text":"如果没有车辆，行人可以开始过马路。"},{"key":"B","text":"行人不得开始过马路；已经在过马路的应完成过街。"},{"key":"C","text":"行人必须停下并等待绿灯。"},{"key":"D","text":"行人可以过马路，但必须让行车辆。"}]'::jsonb, '手册指出，闪烁的“禁止行走”（或闪烁的手形）意味着行人不得开始过马路；如果已经在过马路，应完成过街。驾驶员仍需让行在斑马线上的行人。', 'active');

    -- Q5: What must a driver do when approaching a traffic signal that is completely dark (not working)?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What must a driver do when approaching a traffic signal that is completely dark (not working)?', '[{"key":"A","text":"Proceed with caution without stopping."},{"key":"B","text":"Treat the intersection as an all-way stop."},{"key":"C","text":"Stop only if there is cross traffic."},{"key":"D","text":"Treat it as a yield sign and slow down."}]'::jsonb, 'According to the handbook, when a traffic signal is not working (dark), you must treat the intersection as an all-way STOP.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当驾驶者接近一个完全不亮的交通信号灯时，应该怎么做？', '[{"key":"A","text":"小心通行，无需停车。"},{"key":"B","text":"将路口视为四面停车标志。"},{"key":"C","text":"只有在有横向交通时才停车。"},{"key":"D","text":"将其视为让行标志并减速。"}]'::jsonb, '根据手册，当交通信号灯不亮时，你必须将路口视为四面停车标志。', 'active');

    -- Q6: What does a flashing yellow arrow signal mean for a driver?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a flashing yellow arrow signal mean for a driver?', '[{"key":"A","text":"You must stop and wait for a green arrow."},{"key":"B","text":"You have a protected turn and oncoming traffic is stopped."},{"key":"C","text":"You may turn but it is not protected; yield to oncoming traffic and pedestrians."},{"key":"D","text":"You may turn only after coming to a full stop."}]'::jsonb, 'The handbook states that a flashing yellow arrow means you may turn but it is NOT protected; you must yield to oncoming traffic and pedestrians.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '闪烁的黄色箭头信号对驾驶者意味着什么？', '[{"key":"A","text":"你必须停车等待绿色箭头。"},{"key":"B","text":"你有受保护的转弯权，对面来车已停止。"},{"key":"C","text":"你可以转弯，但不受保护；需让行对面来车和行人。"},{"key":"D","text":"你只能在完全停车后才能转弯。"}]'::jsonb, '手册指出，闪烁的黄色箭头表示你可以转弯，但不受保护；你必须让行对面来车和行人。', 'active');

    -- Q7: When a pedestrian signal shows a flashing DON'T WALK (flashing hand), what must a driver do?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When a pedestrian signal shows a flashing DON''T WALK (flashing hand), what must a driver do?', '[{"key":"A","text":"Stop and wait for the next WALK signal before proceeding."},{"key":"B","text":"Ignore the signal and continue driving normally."},{"key":"C","text":"Yield to any pedestrians already in the crosswalk."},{"key":"D","text":"Honk to warn pedestrians to hurry across."}]'::jsonb, 'The handbook says that during a flashing DON''T WALK, drivers must still yield to pedestrians who are already in the crosswalk.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当行人信号灯显示闪烁的“禁止行走”（闪烁的手掌）时，驾驶者必须怎么做？', '[{"key":"A","text":"停车等待下一个“行走”信号再通行。"},{"key":"B","text":"忽略该信号并正常行驶。"},{"key":"C","text":"让行已在人行横道上的行人。"},{"key":"D","text":"鸣喇叭警告行人快速通过。"}]'::jsonb, '手册指出，在闪烁的“禁止行走”信号期间，驾驶者仍必须让行已在人行横道上的行人。', 'active');

    -- Q8: What is the correct action for a driver facing a solid red traffic light with no sign posted?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the correct action for a driver facing a solid red traffic light with no sign posted?', '[{"key":"A","text":"Stop, then proceed without yielding if no traffic is visible."},{"key":"B","text":"Stop, then turn right after yielding to traffic and pedestrians."},{"key":"C","text":"Slow down and turn right if the intersection is clear."},{"key":"D","text":"Stop and wait for a green light before making any turn."}]'::jsonb, 'The handbook states that at a solid red light, you must stop, and a right turn on red is allowed after a full stop and yielding, unless a ''NO TURN ON RED'' sign is posted.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '面对没有标志的红色圆形交通灯时，驾驶者的正确做法是什么？', '[{"key":"A","text":"停车，如果看不到车辆则无需让行直接通行。"},{"key":"B","text":"停车，在让行车辆和行人后右转。"},{"key":"C","text":"减速，如果路口畅通则右转。"},{"key":"D","text":"停车并等待绿灯才能转弯。"}]'::jsonb, '手册指出，在红色圆形灯前必须停车，除非有“禁止红灯右转”标志，否则在完全停车并让行后允许右转。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_TRAFFIC_SIGNS  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_TRAFFIC_SIGNS';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: What must a driver do when approaching a red octagonal STOP sign according to the handbook?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What must a driver do when approaching a red octagonal STOP sign according to the handbook?', '[{"key":"A","text":"Slow down and proceed without stopping if no traffic is present."},{"key":"B","text":"Make a full stop before the crosswalk or at the limit line, then yield and proceed when safe."},{"key":"C","text":"Stop only if a vehicle, bicyclist, or pedestrian is approaching."},{"key":"D","text":"Reduce speed to 15 mph and look both ways before crossing."}]'::jsonb, 'The handbook states that a STOP sign requires a full stop before the crosswalk or at the limit line, then yield and proceed when safe.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据手册，驾驶员在接近红色八角形停车标志时应如何操作？', '[{"key":"A","text":"减速，如果没有交通则无需停车直接通过。"},{"key":"B","text":"在人行横道或停车线前完全停下，然后让行并在安全时继续行驶。"},{"key":"C","text":"只有在车辆、自行车或行人接近时才停车。"},{"key":"D","text":"将速度降至15英里/小时，并左右观察后再通过。"}]'::jsonb, '手册规定，停车标志要求在人行横道或停车线前完全停下，然后让行并在安全时继续行驶。', 'active');

    -- Q2: What does a red circle with a red slash through a symbol indicate?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a red circle with a red slash through a symbol indicate?', '[{"key":"A","text":"The action shown is permitted only during certain hours."},{"key":"B","text":"The action shown is prohibited."},{"key":"C","text":"The action shown is a warning of a hazard ahead."},{"key":"D","text":"The action shown is required by law."}]'::jsonb, 'The handbook explains that a red circle with a red slash through a symbol means the action shown is prohibited, such as no U-turn or no right turn.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '带有红色斜杠的红色圆圈符号表示什么？', '[{"key":"A","text":"所示动作仅在特定时段允许。"},{"key":"B","text":"所示动作被禁止。"},{"key":"C","text":"所示动作是对前方危险的警告。"},{"key":"D","text":"所示动作是法律要求的。"}]'::jsonb, '手册解释，带有红色斜杠的红色圆圈表示所示动作被禁止，例如禁止掉头或禁止右转。', 'active');

    -- Q3: What does a pentagon-shaped (5-sided) sign indicate?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a pentagon-shaped (5-sided) sign indicate?', '[{"key":"A","text":"A railroad crossing ahead."},{"key":"B","text":"A general warning of a hazard or condition ahead."},{"key":"C","text":"A school zone or school crossing ahead."},{"key":"D","text":"A regulatory rule such as a speed limit."}]'::jsonb, 'The handbook states that a 5-sided (pentagon) sign indicates a school zone or school crossing ahead, requiring drivers to slow down and watch for children.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '五边形（五面）标志表示什么？', '[{"key":"A","text":"前方有铁路道口。"},{"key":"B","text":"前方有危险或状况的一般警告。"},{"key":"C","text":"前方有学校区域或学校人行横道。"},{"key":"D","text":"一项法规规则，如限速。"}]'::jsonb, '手册说明，五边形标志表示前方有学校区域或学校人行横道，驾驶员应减速并注意儿童。', 'active');

    -- Q4: What color is typically used for warning signs according to the handbook?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What color is typically used for warning signs according to the handbook?', '[{"key":"A","text":"Red"},{"key":"B","text":"Yellow"},{"key":"C","text":"Orange"},{"key":"D","text":"Green"}]'::jsonb, 'The handbook specifies that yellow is the color for general warning signs, while red indicates stop, yield, or prohibition, orange is for construction, and green for guide directions.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据手册，警告标志通常使用什么颜色？', '[{"key":"A","text":"红色"},{"key":"B","text":"黄色"},{"key":"C","text":"橙色"},{"key":"D","text":"绿色"}]'::jsonb, '手册规定，黄色是一般警告标志的颜色，而红色表示停车、让行或禁止，橙色用于施工区，绿色用于指引方向。', 'active');

    -- Q5: What action does a red octagonal sign with the word STOP require you to take?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What action does a red octagonal sign with the word STOP require you to take?', '[{"key":"A","text":"Slow down and be ready to stop to let any vehicle, bicyclist, or pedestrian pass."},{"key":"B","text":"Make a full stop before the crosswalk or at the limit line, then proceed when safe."},{"key":"C","text":"Stop only if there is cross traffic; otherwise, proceed without stopping."},{"key":"D","text":"Yield the right-of-way but you may roll through if no one is approaching."}]'::jsonb, 'The handbook states that a STOP sign (red octagon) requires you to make a full stop before the crosswalk or at the limit line, then yield and proceed when safe.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '红色八角形且写有STOP的标志要求你采取什么行动？', '[{"key":"A","text":"减速并准备停车，让任何车辆、自行车或行人先行。"},{"key":"B","text":"在人行横道或停车线前完全停下，确认安全后再通行。"},{"key":"C","text":"仅在有横向来车时停车，否则无需停车直接通过。"},{"key":"D","text":"让行，但如果无人接近可以缓慢滑行通过。"}]'::jsonb, '手册规定，STOP标志（红色八角形）要求你在人行横道或停车线前完全停下，让行后在安全时再通行。', 'active');

    -- Q6: What does a red circle with a red slash through a symbol indicate?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a red circle with a red slash through a symbol indicate?', '[{"key":"A","text":"The action shown is prohibited."},{"key":"B","text":"The action shown is recommended but not required."},{"key":"C","text":"The action shown is allowed only during certain hours."},{"key":"D","text":"The action shown is a warning of a potential hazard."}]'::jsonb, 'The handbook says a red circle with a red slash through a symbol means the action shown is prohibited (e.g., no U-turn, no right turn).', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '红色圆圈内有一条红色斜线穿过某个符号，表示什么？', '[{"key":"A","text":"所表示的行为被禁止。"},{"key":"B","text":"所表示的行为是建议性的，并非强制要求。"},{"key":"C","text":"所表示的行为仅在特定时段允许。"},{"key":"D","text":"所表示的行为是对潜在危险的警告。"}]'::jsonb, '手册指出，红色圆圈内有一条红色斜线穿过某个符号，表示所显示的行为被禁止（例如禁止掉头、禁止右转）。', 'active');

    -- Q7: What does a pentagon-shaped (5-sided) sign warn you about?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a pentagon-shaped (5-sided) sign warn you about?', '[{"key":"A","text":"A railroad crossing ahead."},{"key":"B","text":"A school zone or school crossing ahead."},{"key":"C","text":"A general warning of a curve or slippery road ahead."},{"key":"D","text":"A construction or work zone ahead."}]'::jsonb, 'The handbook states that a 5-sided (pentagon) sign indicates a school zone or school crossing ahead, and you should slow down and watch for children.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '五边形标志警告你什么？', '[{"key":"A","text":"前方有铁路道口。"},{"key":"B","text":"前方有学校区域或学校人行横道。"},{"key":"C","text":"前方有弯道或湿滑路面的通用警告。"},{"key":"D","text":"前方有施工或作业区域。"}]'::jsonb, '手册说明，五边形标志表示前方有学校区域或学校人行横道，应减速并注意儿童。', 'active');

    -- Q8: What does a diamond-shaped yellow sign typically indicate?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a diamond-shaped yellow sign typically indicate?', '[{"key":"A","text":"A regulatory rule you must obey, such as a speed limit."},{"key":"B","text":"A warning of a specific hazard or condition ahead."},{"key":"C","text":"A direction or guide to a nearby service or attraction."},{"key":"D","text":"A prohibition, such as no parking or no turns."}]'::jsonb, 'The handbook says diamond-shaped (yellow) signs are warning signs that alert you to a specific hazard or condition ahead, such as a curve, merge, or slippery road.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '黄色菱形标志通常表示什么？', '[{"key":"A","text":"你必须遵守的法规，例如限速。"},{"key":"B","text":"警告前方有特定危险或状况。"},{"key":"C","text":"指引附近服务设施或景点的方向。"},{"key":"D","text":"禁止行为，例如禁止停车或禁止转弯。"}]'::jsonb, '手册说明，黄色菱形标志是警告标志，提醒你前方有特定危险或状况，例如弯道、合流或湿滑路面。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_ROW_INTERSECTIONS  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_ROW_INTERSECTIONS';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: At an intersection without STOP or YIELD signs, two vehicles arrive at the same time. Which vehicle ...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'At an intersection without STOP or YIELD signs, two vehicles arrive at the same time. Which vehicle has the right-of-way?', '[{"key":"A","text":"The vehicle on the left"},{"key":"B","text":"The vehicle on the right"},{"key":"C","text":"The larger vehicle"},{"key":"D","text":"The vehicle going straight"}]'::jsonb, 'According to the handbook, at intersections without STOP or YIELD signs, if two vehicles arrive at the same time, you must yield to the vehicle on your right.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在一个没有停车标志或让行标志的路口，两辆车同时到达。哪辆车拥有先行权？', '[{"key":"A","text":"左侧的车辆"},{"key":"B","text":"右侧的车辆"},{"key":"C","text":"较大的车辆"},{"key":"D","text":"直行的车辆"}]'::jsonb, '根据手册，在没有停车或让行标志的路口，如果两辆车同时到达，你必须让行右侧的车辆。', 'active');

    -- Q2: At a T-intersection without any signs, who has the right-of-way?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'At a T-intersection without any signs, who has the right-of-way?', '[{"key":"A","text":"The vehicle on the ending road"},{"key":"B","text":"The vehicle turning left"},{"key":"C","text":"Vehicles, bicyclists, and pedestrians on the through road"},{"key":"D","text":"The vehicle that arrived last"}]'::jsonb, 'The handbook states that at T-intersections without signs, vehicles, bicyclists, and pedestrians on the through road (going straight) have the right-of-way.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在一个没有任何标志的T形路口，谁拥有先行权？', '[{"key":"A","text":"在尽头道路上的车辆"},{"key":"B","text":"左转的车辆"},{"key":"C","text":"在直行道路上的车辆、自行车和行人"},{"key":"D","text":"最后到达的车辆"}]'::jsonb, '手册指出，在没有标志的T形路口，直行道路上的车辆、自行车和行人拥有先行权。', 'active');

    -- Q3: When turning left at an intersection, you must yield the right-of-way to which of the following?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When turning left at an intersection, you must yield the right-of-way to which of the following?', '[{"key":"A","text":"Only pedestrians in the crosswalk"},{"key":"B","text":"Only oncoming vehicles"},{"key":"C","text":"Oncoming vehicles, bicyclists, and pedestrians that are close enough to be a hazard"},{"key":"D","text":"All traffic behind you"}]'::jsonb, 'The handbook says when turning left, yield to oncoming vehicles, bicyclists, and pedestrians that are close enough to be a hazard.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在路口左转时，你必须将先行权让给以下哪一类？', '[{"key":"A","text":"仅限人行横道上的行人"},{"key":"B","text":"仅限对向车辆"},{"key":"C","text":"对向车辆、自行车和行人，只要他们足够近构成危险"},{"key":"D","text":"你后方所有交通"}]'::jsonb, '手册指出，左转时，必须让行对向车辆、自行车和行人，只要他们足够近构成危险。', 'active');

    -- Q4: On a steep, narrow mountain road where two vehicles meet and neither can pass, which vehicle must yi...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'On a steep, narrow mountain road where two vehicles meet and neither can pass, which vehicle must yield?', '[{"key":"A","text":"The vehicle facing uphill"},{"key":"B","text":"The vehicle facing downhill"},{"key":"C","text":"The larger vehicle"},{"key":"D","text":"The vehicle that arrived first"}]'::jsonb, 'According to the handbook, on a steep narrow road where neither can pass, the vehicle facing downhill must yield because the uphill vehicle has the right-of-way and better control.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在陡峭狭窄的山路上，两车相遇且无法通行，哪辆车必须让行？', '[{"key":"A","text":"上坡的车辆"},{"key":"B","text":"下坡的车辆"},{"key":"C","text":"较大的车辆"},{"key":"D","text":"先到达的车辆"}]'::jsonb, '根据手册，在陡峭狭窄的道路上无法通行时，下坡车辆必须让行，因为上坡车辆拥有先行权且更容易控制。', 'active');

    -- Q5: At an intersection without STOP or YIELD signs, two vehicles arrive at the same time; which one has ...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'At an intersection without STOP or YIELD signs, two vehicles arrive at the same time; which one has the right-of-way?', '[{"key":"A","text":"The vehicle on the left"},{"key":"B","text":"The vehicle on the right"},{"key":"C","text":"The larger vehicle"},{"key":"D","text":"The faster vehicle"}]'::jsonb, 'According to the handbook, if two vehicles arrive at an intersection without STOP or YIELD signs at the same time, yield to the vehicle on your right.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在沒有停車或讓路標誌的交叉路口，兩輛車同時到達，哪一輛有先行權？', '[{"key":"A","text":"左側的車輛"},{"key":"B","text":"右側的車輛"},{"key":"C","text":"較大的車輛"},{"key":"D","text":"速度較快的車輛"}]'::jsonb, '根據手冊，如果兩輛車同時到達沒有停車或讓路標誌的交叉路口，應讓行右側的車輛。', 'active');

    -- Q6: At a T-intersection without signs, who has the right-of-way?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'At a T-intersection without signs, who has the right-of-way?', '[{"key":"A","text":"The vehicle on the terminating road"},{"key":"B","text":"The vehicle turning left from the through road"},{"key":"C","text":"Vehicles, bicyclists, and pedestrians on the through road going straight"},{"key":"D","text":"The vehicle that arrived first, regardless of road type"}]'::jsonb, 'The handbook states that at T-intersections without signs, vehicles, bicyclists, and pedestrians on the through road (going straight) have the right-of-way.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在沒有標誌的T型路口，誰有先行權？', '[{"key":"A","text":"在終止道路上的車輛"},{"key":"B","text":"從直行道路左轉的車輛"},{"key":"C","text":"在直行道路上直行的車輛、自行車和行人"},{"key":"D","text":"先到達的車輛，無論道路類型"}]'::jsonb, '手冊指出，在沒有標誌的T型路口，直行道路上的車輛、自行車和行人擁有先行權。', 'active');

    -- Q7: When turning left at an intersection, you must yield to which of the following?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When turning left at an intersection, you must yield to which of the following?', '[{"key":"A","text":"Only pedestrians in the crosswalk"},{"key":"B","text":"Only oncoming vehicles"},{"key":"C","text":"Oncoming vehicles, bicyclists, and pedestrians that are close enough to be a hazard"},{"key":"D","text":"All traffic behind you"}]'::jsonb, 'The handbook says when turning left, yield to oncoming vehicles, bicyclists, and pedestrians that are close enough to be a hazard.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在交叉路口左轉時，你必須讓行哪些對象？', '[{"key":"A","text":"僅限人行橫道上的行人"},{"key":"B","text":"僅限對向車輛"},{"key":"C","text":"對向車輛、自行車和行人，且距離近到構成危險"},{"key":"D","text":"你後方的所有交通"}]'::jsonb, '手冊指出，左轉時應讓行對向車輛、自行車和行人，只要他們距離近到構成危險。', 'active');

    -- Q8: On a steep, narrow mountain road where two vehicles meet and neither can pass, which vehicle must yi...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'On a steep, narrow mountain road where two vehicles meet and neither can pass, which vehicle must yield?', '[{"key":"A","text":"The vehicle facing uphill"},{"key":"B","text":"The vehicle facing downhill"},{"key":"C","text":"The larger vehicle"},{"key":"D","text":"The vehicle that arrived first"}]'::jsonb, 'The handbook states that on a steep, narrow road where neither can pass, the vehicle facing downhill must yield because the uphill vehicle has the right-of-way and better control.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在陡峭狹窄的山路上，兩車相遇且無法通行時，哪輛車必須讓行？', '[{"key":"A","text":"上坡的車輛"},{"key":"B","text":"下坡的車輛"},{"key":"C","text":"較大的車輛"},{"key":"D","text":"先到達的車輛"}]'::jsonb, '手冊指出，在陡峭狹窄的道路上無法通行時，下坡車輛必須讓行，因為上坡車輛有先行權且操控性更好。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_ROW_VULNERABLE_USERS  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_ROW_VULNERABLE_USERS';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: What must a driver do when approaching a stopped vehicle at a crosswalk?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'D', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What must a driver do when approaching a stopped vehicle at a crosswalk?', '[{"key":"A","text":"Slow down and proceed if the crosswalk appears clear."},{"key":"B","text":"Stop and then honk to alert any hidden pedestrians."},{"key":"C","text":"Pass the stopped vehicle only if you can see the entire crosswalk."},{"key":"D","text":"Stop at the limit line and do not pass the stopped vehicle."}]'::jsonb, 'The handbook states: ''Do not pass a vehicle stopped at a crosswalk — a pedestrian you cannot see may be crossing.'' You must stop at the limit line and not pass.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当接近停在人行横道前的车辆时，驾驶员必须怎么做？', '[{"key":"A","text":"减速，如果人行横道看起来空旷就继续行驶。"},{"key":"B","text":"停车并按喇叭提醒可能隐藏的行人。"},{"key":"C","text":"只有在能看到整个人行横道时才能超越停下的车辆。"},{"key":"D","text":"在停车线前停下，不要超越停下的车辆。"}]'::jsonb, '手册指出：''不要超越停在人行横道前的车辆——你可能看不到正在过马路的行人。'' 你必须在停车线前停下，不要超越。', 'active');

    -- Q2: What is the minimum distance a driver must leave when passing a bicyclist?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the minimum distance a driver must leave when passing a bicyclist?', '[{"key":"A","text":"Two feet."},{"key":"B","text":"Three feet."},{"key":"C","text":"Four feet."},{"key":"D","text":"Five feet."}]'::jsonb, 'The handbook specifies: ''When passing a bicyclist, leave at least THREE FEET between your vehicle and the bicyclist.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '驾驶员超越自行车时，必须保持的最小距离是多少？', '[{"key":"A","text":"两英尺。"},{"key":"B","text":"三英尺。"},{"key":"C","text":"四英尺。"},{"key":"D","text":"五英尺。"}]'::jsonb, '手册明确规定：''超越自行车时，车辆与自行车之间至少保持三英尺的距离。''', 'active');

    -- Q3: When is it legal to drive in a bike lane?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When is it legal to drive in a bike lane?', '[{"key":"A","text":"When traffic is heavy and you need to make better time."},{"key":"B","text":"When you are turning within 200 feet of an intersection."},{"key":"C","text":"When the bike lane is empty and you are driving slowly."},{"key":"D","text":"When you are following a bicyclist to ensure their safety."}]'::jsonb, 'The handbook says it is illegal to drive in a bike lane except to park (where allowed), enter/leave the road, or turn within 200 feet of an intersection.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在什么情况下在自行车道内驾驶是合法的？', '[{"key":"A","text":"当交通拥堵且你需要节省时间时。"},{"key":"B","text":"当你在距离交叉路口200英尺内转弯时。"},{"key":"C","text":"当自行车道空着且你缓慢行驶时。"},{"key":"D","text":"当你跟随自行车以确保其安全时。"}]'::jsonb, '手册指出，除非在允许的地方停车、进出道路或在距离交叉路口200英尺内转弯，否则在自行车道内驾驶是违法的。', 'active');

    -- Q4: What is the correct action when an emergency vehicle with flashing red lights approaches from behind...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the correct action when an emergency vehicle with flashing red lights approaches from behind?', '[{"key":"A","text":"Speed up to clear the lane quickly."},{"key":"B","text":"Continue at the same speed and let the emergency vehicle maneuver around you."},{"key":"C","text":"Pull to the right edge of the road and stop until it passes."},{"key":"D","text":"Slow down and move to the left lane."}]'::jsonb, 'The handbook states: ''Yield to any emergency vehicle using a siren and/or red lights: pull to the right edge of the road and stop until it passes.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当一辆闪着红色警示灯的应急车辆从后方驶来时，正确的做法是什么？', '[{"key":"A","text":"加速以快速让出车道。"},{"key":"B","text":"保持原速行驶，让应急车辆自行绕行。"},{"key":"C","text":"靠右行驶至路边并停车，直到它通过。"},{"key":"D","text":"减速并驶入左侧车道。"}]'::jsonb, '手册指出：''避让使用警笛和/或红色警示灯的应急车辆：靠右行驶至路边并停车，直到它通过。''', 'active');

    -- Q5: What must a driver do when approaching a stopped vehicle at a crosswalk?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What must a driver do when approaching a stopped vehicle at a crosswalk?', '[{"key":"A","text":"Slow down and proceed with caution."},{"key":"B","text":"Honk the horn to alert any hidden pedestrians."},{"key":"C","text":"Stop and do not pass the stopped vehicle."},{"key":"D","text":"Pass the stopped vehicle only if the crosswalk is unmarked."}]'::jsonb, 'The handbook states: ''Do not pass a vehicle stopped at a crosswalk — a pedestrian you cannot see may be crossing.'' Therefore, you must stop and not pass.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当接近人行横道前停下的车辆时，驾驶员必须怎么做？', '[{"key":"A","text":"减速并谨慎通过。"},{"key":"B","text":"鸣喇叭以警示可能隐藏的行人。"},{"key":"C","text":"停车，不要超越已停下的车辆。"},{"key":"D","text":"仅当人行横道未标线时才能超越停下的车辆。"}]'::jsonb, '手册规定：''不要超越在人行横道前停下的车辆——你可能看不到正在过马路的行人。''因此，你必须停车且不得超越。', 'active');

    -- Q6: When passing a bicyclist on a roadway, what is the minimum distance a driver must leave between the ...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When passing a bicyclist on a roadway, what is the minimum distance a driver must leave between the vehicle and the bicyclist?', '[{"key":"A","text":"Two feet."},{"key":"B","text":"Three feet."},{"key":"C","text":"Four feet."},{"key":"D","text":"Five feet."}]'::jsonb, 'The handbook explicitly says: ''When passing a bicyclist, leave at least THREE FEET between your vehicle and the bicyclist.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在道路上超越自行车时，驾驶员必须在车辆与自行车之间保持的最小距离是多少？', '[{"key":"A","text":"两英尺。"},{"key":"B","text":"三英尺。"},{"key":"C","text":"四英尺。"},{"key":"D","text":"五英尺。"}]'::jsonb, '手册明确说明：''超越自行车时，车辆与自行车之间至少保持三英尺的距离。''', 'active');

    -- Q7: What is the legal minimum distance a driver must maintain behind an emergency vehicle with its siren...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the legal minimum distance a driver must maintain behind an emergency vehicle with its siren or lights activated?', '[{"key":"A","text":"100 feet."},{"key":"B","text":"200 feet."},{"key":"C","text":"300 feet."},{"key":"D","text":"500 feet."}]'::jsonb, 'The handbook states: ''It is illegal to follow within 300 feet of an emergency vehicle that has its siren or lights on.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '驾驶员必须与开启警笛或警灯的应急车辆保持的法定最小距离是多少？', '[{"key":"A","text":"100英尺。"},{"key":"B","text":"200英尺。"},{"key":"C","text":"300英尺。"},{"key":"D","text":"500英尺。"}]'::jsonb, '手册规定：''跟随开启警笛或警灯的应急车辆在300英尺以内是违法的。''', 'active');

    -- Q8: What is the minimum distance you must leave between your vehicle and a bicyclist when passing?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the minimum distance you must leave between your vehicle and a bicyclist when passing?', '[{"key":"A","text":"Two feet"},{"key":"B","text":"Three feet"},{"key":"C","text":"Four feet"},{"key":"D","text":"Five feet"}]'::jsonb, 'According to the handbook, when passing a bicyclist, you must leave at least three feet between your vehicle and the bicyclist.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '超车时，您与骑自行车者之间必须保持的最小距离是多少？', '[{"key":"A","text":"两英尺"},{"key":"B","text":"三英尺"},{"key":"C","text":"四英尺"},{"key":"D","text":"五英尺"}]'::jsonb, '根据手册，超车时，您与骑自行车者之间必须至少保持三英尺的距离。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_SPEED_LIMITS  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_SPEED_LIMITS';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: According to the Basic Speed Law, when must you reduce your speed below the posted limit?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'According to the Basic Speed Law, when must you reduce your speed below the posted limit?', '[{"key":"A","text":"Only when a police officer is present."},{"key":"B","text":"Only when driving in a school zone."},{"key":"C","text":"Whenever current conditions make the posted limit unsafe."},{"key":"D","text":"Only when driving on a two-lane undivided highway."}]'::jsonb, 'The Basic Speed Law states you may never drive faster than is safe for current conditions, regardless of the posted limit. You must adjust speed for factors like weather, traffic, and road surface.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据基本速度法，何时必须将车速降至限速标志以下？', '[{"key":"A","text":"仅在有警察在场时。"},{"key":"B","text":"仅在学校区域行驶时。"},{"key":"C","text":"只要当前路况使限速标志不安全时。"},{"key":"D","text":"仅在双车道未分隔公路上行驶时。"}]'::jsonb, '基本速度法规定，无论限速标志如何，你绝不能以超过当前条件安全的速度行驶。必须根据天气、交通和路面等因素调整车速。', 'active');

    -- Q2: What is the maximum speed limit on most California freeways unless a higher limit is posted?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the maximum speed limit on most California freeways unless a higher limit is posted?', '[{"key":"A","text":"55 mph"},{"key":"B","text":"65 mph"},{"key":"C","text":"70 mph"},{"key":"D","text":"60 mph"}]'::jsonb, 'The handbook states 65 mph on most California freeways, with up to 70 mph where posted. 55 mph applies to two-lane undivided highways and when towing.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '除非另有标示，加州大多数高速公路的最高限速是多少？', '[{"key":"A","text":"55 英里/小时"},{"key":"B","text":"65 英里/小时"},{"key":"C","text":"70 英里/小时"},{"key":"D","text":"60 英里/小时"}]'::jsonb, '手册指出，加州大多数高速公路限速65英里/小时，在有标示的路段可达70英里/小时。55英里/小时适用于双车道未分隔公路和拖车时。', 'active');

    -- Q3: In a business or residential district without posted speed signs, what is the default speed limit?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'In a business or residential district without posted speed signs, what is the default speed limit?', '[{"key":"A","text":"30 mph"},{"key":"B","text":"25 mph"},{"key":"C","text":"20 mph"},{"key":"D","text":"15 mph"}]'::jsonb, 'The handbook specifies a 25 mph limit in a business or residential district unless otherwise posted. 15 mph applies to blind intersections and alleys.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在没有限速标志的商业或住宅区，默认限速是多少？', '[{"key":"A","text":"30 英里/小时"},{"key":"B","text":"25 英里/小时"},{"key":"C","text":"20 英里/小时"},{"key":"D","text":"15 英里/小时"}]'::jsonb, '手册规定，除非另有标示，商业或住宅区限速25英里/小时。15英里/小时适用于盲区和小巷。', 'active');

    -- Q4: What is the fine consequence for a speeding violation in a posted double-fine work zone when workers...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the fine consequence for a speeding violation in a posted double-fine work zone when workers are present?', '[{"key":"A","text":"The fine is doubled compared to a normal work-zone violation."},{"key":"B","text":"The fine is the same as any other speeding ticket."},{"key":"C","text":"The fine is reduced by half."},{"key":"D","text":"The fine is tripled."}]'::jsonb, 'The handbook states that work-zone fines are $1,000 or more and are DOUBLED in posted double-fine zones, especially when workers are present.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在有标示的双倍罚款施工区且工人正在作业时，超速罚款的后果是什么？', '[{"key":"A","text":"罚款是普通施工区违规的两倍。"},{"key":"B","text":"罚款与其他超速罚单相同。"},{"key":"C","text":"罚款减半。"},{"key":"D","text":"罚款增至三倍。"}]'::jsonb, '手册指出，施工区罚款为1000美元或更多，在有标示的双倍罚款区（尤其是工人作业时）罚款加倍。', 'active');

    -- Q5: According to the Basic Speed Law, when must you adjust your speed below the posted limit?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'According to the Basic Speed Law, when must you adjust your speed below the posted limit?', '[{"key":"A","text":"Only when driving in a school zone with children present."},{"key":"B","text":"Only when towing a trailer on a two-lane undivided highway."},{"key":"C","text":"Whenever current conditions such as weather, visibility, or traffic make the posted limit unsafe."},{"key":"D","text":"Only when approaching a blind intersection or railroad crossing."}]'::jsonb, 'The Basic Speed Law states you may never drive faster than is safe for current conditions, regardless of the posted limit, and requires adjusting speed for factors like weather, visibility, and traffic.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据基本速度法，何时必须将车速调整至低于标示限速？', '[{"key":"A","text":"仅在有儿童在场的学区行驶时。"},{"key":"B","text":"仅在双车道未分隔公路上拖挂拖车时。"},{"key":"C","text":"每当当前条件（如天气、能见度或交通状况）使标示限速不安全时。"},{"key":"D","text":"仅在接近盲区交叉口或铁路道口时。"}]'::jsonb, '基本速度法规定，无论标示限速如何，你绝不能以超过当前条件安全的速度行驶，并需根据天气、能见度和交通等因素调整车速。', 'active');

    -- Q6: What is the maximum posted speed limit on a two-lane undivided highway in California unless otherwis...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the maximum posted speed limit on a two-lane undivided highway in California unless otherwise posted?', '[{"key":"A","text":"65 mph"},{"key":"B","text":"55 mph"},{"key":"C","text":"70 mph"},{"key":"D","text":"60 mph"}]'::jsonb, 'The handbook specifies a maximum posted speed limit of 55 mph on two-lane undivided highways and when towing a trailer.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '除非另有标示，加利福尼亚州双车道未分隔公路的最高标示限速是多少？', '[{"key":"A","text":"65 英里/小时"},{"key":"B","text":"55 英里/小时"},{"key":"C","text":"70 英里/小时"},{"key":"D","text":"60 英里/小时"}]'::jsonb, '手册规定，在双车道未分隔公路和拖挂拖车时，最高标示限速为 55 英里/小时。', 'active');

    -- Q7: In a business or residential district with no posted speed limit, what is the default maximum speed?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'In a business or residential district with no posted speed limit, what is the default maximum speed?', '[{"key":"A","text":"30 mph"},{"key":"B","text":"20 mph"},{"key":"C","text":"25 mph"},{"key":"D","text":"35 mph"}]'::jsonb, 'The handbook states the limit is 25 mph in a business or residential district unless otherwise posted.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在未标示限速的商业区或住宅区，默认最高速度是多少？', '[{"key":"A","text":"30 英里/小时"},{"key":"B","text":"20 英里/小时"},{"key":"C","text":"25 英里/小时"},{"key":"D","text":"35 英里/小时"}]'::jsonb, '手册规定，除非另有标示，商业区或住宅区的限速为 25 英里/小时。', 'active');

    -- Q8: What is the maximum speed limit on a two-lane undivided highway in California unless otherwise poste...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the maximum speed limit on a two-lane undivided highway in California unless otherwise posted?', '[{"key":"A","text":"65 mph"},{"key":"B","text":"55 mph"},{"key":"C","text":"70 mph"},{"key":"D","text":"45 mph"}]'::jsonb, 'According to the handbook, the maximum speed limit on two-lane undivided highways is 55 mph. 65 mph applies to most freeways, and 70 mph is only where posted on certain freeways.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在加州，除非另有标示，双车道未分隔公路的最高限速是多少？', '[{"key":"A","text":"65 英里/小时"},{"key":"B","text":"55 英里/小时"},{"key":"C","text":"70 英里/小时"},{"key":"D","text":"45 英里/小时"}]'::jsonb, '根据手册，双车道未分隔公路的最高限速为 55 英里/小时。65 英里/小时适用于大多数高速公路，70 英里/小时仅适用于部分高速公路的标示路段。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_FOLLOWING_DISTANCE  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_FOLLOWING_DISTANCE';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: According to the handbook, what is the minimum following distance you should maintain under normal c...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'According to the handbook, what is the minimum following distance you should maintain under normal conditions?', '[{"key":"A","text":"One second"},{"key":"B","text":"Two seconds"},{"key":"C","text":"Three seconds"},{"key":"D","text":"Four seconds"}]'::jsonb, 'The handbook states to keep at least a three-second following distance under normal conditions. One or two seconds are too short, and four seconds is recommended only in poor conditions.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据手册，在正常条件下你应保持的最小跟车距离是多少？', '[{"key":"A","text":"一秒"},{"key":"B","text":"两秒"},{"key":"C","text":"三秒"},{"key":"D","text":"四秒"}]'::jsonb, '手册规定在正常条件下应至少保持三秒跟车距离。一秒或两秒太短，而四秒仅在恶劣条件下推荐。', 'active');

    -- Q2: When should you increase your following distance beyond the standard three-second rule?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When should you increase your following distance beyond the standard three-second rule?', '[{"key":"A","text":"Only when driving on a freeway"},{"key":"B","text":"When being tailgated, following a motorcyclist, in bad weather, at night, or on slick surfaces"},{"key":"C","text":"Only when driving in a residential area"},{"key":"D","text":"When you are driving faster than the speed limit"}]'::jsonb, 'The handbook explicitly lists these conditions: being tailgated, following a motorcyclist, bad weather, night, or slick surfaces. The other options are not mentioned as reasons to increase following distance.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在哪些情况下你应该增加跟车距离，超过标准的三秒规则？', '[{"key":"A","text":"仅在高速公路上行驶时"},{"key":"B","text":"当被尾随、跟随摩托车、恶劣天气、夜间或湿滑路面时"},{"key":"C","text":"仅在住宅区行驶时"},{"key":"D","text":"当你超速行驶时"}]'::jsonb, '手册明确列出了这些情况：被尾随、跟随摩托车、恶劣天气、夜间或湿滑路面。其他选项未提及为增加跟车距离的原因。', 'active');

    -- Q3: What should you do if another vehicle merges too closely in front of you?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What should you do if another vehicle merges too closely in front of you?', '[{"key":"A","text":"Brake suddenly to create space"},{"key":"B","text":"Ease off the accelerator to open up space again"},{"key":"C","text":"Speed up to maintain your position"},{"key":"D","text":"Honk your horn and flash your lights"}]'::jsonb, 'The handbook advises to ease off the accelerator to open up space again when another vehicle merges too closely. Braking suddenly could cause a collision, and speeding up or honking are not recommended.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果另一辆车在你前面太近地并入，你应该怎么做？', '[{"key":"A","text":"突然刹车以创造空间"},{"key":"B","text":"松开油门以重新拉开空间"},{"key":"C","text":"加速以保持你的位置"},{"key":"D","text":"按喇叭并闪灯"}]'::jsonb, '手册建议当另一辆车太近并入时，松开油门以重新拉开空间。突然刹车可能导致碰撞，加速或按喇叭不被推荐。', 'active');

    -- Q4: How should you check your blind spots before changing lanes?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'How should you check your blind spots before changing lanes?', '[{"key":"A","text":"Rely solely on your side mirrors"},{"key":"B","text":"Turn your whole body to look over your shoulder"},{"key":"C","text":"Turn your head (not your whole body) to look over your right and left shoulders"},{"key":"D","text":"Use your rearview mirror only"}]'::jsonb, 'The handbook states to check blind spots by turning your head (not your whole body) to look over your right and left shoulders. Mirrors alone are insufficient, and turning your whole body is not recommended.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在变道前，你应该如何检查盲区？', '[{"key":"A","text":"仅依赖侧视镜"},{"key":"B","text":"转动整个身体回头看"},{"key":"C","text":"转头（而非整个身体）从左右肩膀上方查看"},{"key":"D","text":"仅使用后视镜"}]'::jsonb, '手册规定通过转头（而非整个身体）从左右肩膀上方查看盲区。仅靠镜子不够，转动整个身体也不被推荐。', 'active');

    -- Q5: When should you increase your following distance beyond the standard three-second rule?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When should you increase your following distance beyond the standard three-second rule?', '[{"key":"A","text":"Only when driving on a freeway"},{"key":"B","text":"When being tailgated, following a motorcyclist, in bad weather, at night, or on slick surfaces"},{"key":"C","text":"Only when driving in heavy traffic"},{"key":"D","text":"When you are in a hurry"}]'::jsonb, 'The handbook lists several situations requiring increased following distance: being tailgated, following a motorcyclist, bad weather, night, or slick surfaces.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在哪些情况下你应该增加跟车距离，超过标准的三秒规则？', '[{"key":"A","text":"仅在高速公路上行驶时"},{"key":"B","text":"当被尾随、跟随摩托车、恶劣天气、夜间或湿滑路面时"},{"key":"C","text":"仅在交通拥堵时"},{"key":"D","text":"当你赶时间时"}]'::jsonb, '手册列出了几种需要增加跟车距离的情况：被尾随、跟随摩托车、恶劣天气、夜间或湿滑路面。', 'active');

    -- Q6: What should you do if another vehicle merges too closely in front of you?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What should you do if another vehicle merges too closely in front of you?', '[{"key":"A","text":"Brake hard to avoid a collision"},{"key":"B","text":"Ease off the accelerator to open up space again"},{"key":"C","text":"Speed up to maintain your position"},{"key":"D","text":"Honk your horn and flash your lights"}]'::jsonb, 'The handbook advises to ease off the accelerator to open up space again if another vehicle merges too closely in front of you.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果另一辆车在你前面并线太近，你应该怎么做？', '[{"key":"A","text":"紧急刹车以避免碰撞"},{"key":"B","text":"松开油门以重新拉开距离"},{"key":"C","text":"加速以保持你的位置"},{"key":"D","text":"鸣喇叭并闪灯"}]'::jsonb, '手册建议，如果另一辆车在你前面并线太近，应松开油门以重新拉开距离。', 'active');

    -- Q7: How should you check your blind spots before changing lanes?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'How should you check your blind spots before changing lanes?', '[{"key":"A","text":"Rely solely on your side mirrors"},{"key":"B","text":"Turn your whole body to look over your shoulder"},{"key":"C","text":"Turn your head (not your whole body) to look over your right and left shoulders"},{"key":"D","text":"Use your rearview mirror only"}]'::jsonb, 'The handbook states to check blind spots by turning your head (not your whole body) to look over your right and left shoulders before changing lanes.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在变道前，你应该如何检查盲区？', '[{"key":"A","text":"仅依靠侧视镜"},{"key":"B","text":"转动整个身体回头看"},{"key":"C","text":"转动头部（而非整个身体）从左右肩膀上方查看"},{"key":"D","text":"仅使用后视镜"}]'::jsonb, '手册指出，变道前应转动头部（而非整个身体）从左右肩膀上方查看以检查盲区。', 'active');

    -- Q8: According to the handbook, what is the minimum following distance you should maintain under normal d...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'According to the handbook, what is the minimum following distance you should maintain under normal driving conditions?', '[{"key":"A","text":"One second"},{"key":"B","text":"Two seconds"},{"key":"C","text":"Three seconds"},{"key":"D","text":"Four seconds"}]'::jsonb, 'The handbook states to keep at least a three-second following distance under normal conditions, measured by when the vehicle ahead passes a fixed point.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据手册，在正常驾驶条件下，您应保持的最小跟车距离是多少？', '[{"key":"A","text":"一秒"},{"key":"B","text":"两秒"},{"key":"C","text":"三秒"},{"key":"D","text":"四秒"}]'::jsonb, '手册指出，在正常条件下应保持至少三秒的跟车距离，以前车通过一个固定点的时间来测量。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_LANE_MARKINGS  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_LANE_MARKINGS';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: Under what condition may you cross double solid yellow lines?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Under what condition may you cross double solid yellow lines?', '[{"key":"A","text":"To pass a slow-moving vehicle."},{"key":"B","text":"To make a left turn into a driveway or a legal U-turn."},{"key":"C","text":"To avoid an obstacle in your lane."},{"key":"D","text":"To enter a carpool lane."}]'::jsonb, 'Double solid yellow lines may only be crossed to turn left into or out of a driveway or make a legal U-turn, per the handbook.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在什么情况下可以跨越双实黄线？', '[{"key":"A","text":"为了超越慢速车辆。"},{"key":"B","text":"为了左转进入车道或合法掉头。"},{"key":"C","text":"为了避开车道上的障碍物。"},{"key":"D","text":"为了进入拼车车道。"}]'::jsonb, '双实黄线仅可为了左转进入或驶出车道或合法掉头而跨越，手册中如此规定。', 'active');

    -- Q2: What is the purpose of a two-way left-turn center lane?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the purpose of a two-way left-turn center lane?', '[{"key":"A","text":"To use as a through travel lane during heavy traffic."},{"key":"B","text":"To prepare for and make left turns or U-turns only."},{"key":"C","text":"To pass other vehicles when the road is clear."},{"key":"D","text":"To merge into traffic from a driveway."}]'::jsonb, 'The two-way left-turn lane is used only to prepare for and make left turns or U-turns, not for through travel or passing, as stated in the handbook.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '双向左转中央车道的用途是什么？', '[{"key":"A","text":"在交通繁忙时用作直行车道。"},{"key":"B","text":"仅用于准备和进行左转或掉头。"},{"key":"C","text":"在道路畅通时超车。"},{"key":"D","text":"从车道汇入车流。"}]'::jsonb, '双向左转车道仅用于准备和进行左转或掉头，不得用于直行或超车，手册中明确说明。', 'active');

    -- Q3: What does a single solid yellow line on your side of the road indicate?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a single solid yellow line on your side of the road indicate?', '[{"key":"A","text":"You may pass when it is safe."},{"key":"B","text":"You may cross the line only to turn left into a driveway."},{"key":"C","text":"You must not pass."},{"key":"D","text":"You may change lanes into oncoming traffic if no cars are approaching."}]'::jsonb, 'A single solid yellow line next to your lane means do not pass. Passing is allowed only when a broken yellow line is on your side.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '你所在车道一侧的单条实黄线表示什么？', '[{"key":"A","text":"安全时可以超车。"},{"key":"B","text":"仅可越过该线左转进入车道。"},{"key":"C","text":"不得超车。"},{"key":"D","text":"若无来车，可驶入对向车道。"}]'::jsonb, '当实黄线位于你所在车道一侧时，禁止超车。只有虚线黄线在你一侧时才允许超车。', 'active');

    -- Q4: What do double solid white lines indicate?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What do double solid white lines indicate?', '[{"key":"A","text":"You may change lanes when safe."},{"key":"B","text":"They separate traffic moving in opposite directions."},{"key":"C","text":"They act as a barrier between a regular lane and a preferential lane; do not cross."},{"key":"D","text":"They mark the edge of the road."}]'::jsonb, 'Double solid white lines separate a regular lane from a preferential lane (e.g., carpool lane) and must not be crossed; wait for a broken line.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '双实白线表示什么？', '[{"key":"A","text":"安全时可以变道。"},{"key":"B","text":"分隔对向行驶的交通。"},{"key":"C","text":"作为普通车道与优先车道之间的屏障，禁止跨越。"},{"key":"D","text":"标示道路边缘。"}]'::jsonb, '双实白线分隔普通车道与优先车道（如拼车车道），禁止跨越，需等待虚线处。', 'active');

    -- Q5: What is the purpose of a two-way left-turn lane?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the purpose of a two-way left-turn lane?', '[{"key":"A","text":"To allow through travel in either direction."},{"key":"B","text":"To prepare for and make left turns or U-turns only."},{"key":"C","text":"To pass other vehicles when traffic is heavy."},{"key":"D","text":"To merge into oncoming traffic."}]'::jsonb, 'A two-way left-turn lane is bordered by broken and solid yellow lines and is used only to prepare for and make left turns or U-turns, not for through travel or passing.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '双向左转车道的用途是什么？', '[{"key":"A","text":"供双向直行使用。"},{"key":"B","text":"仅用于准备和进行左转或掉头。"},{"key":"C","text":"交通拥堵时超车使用。"},{"key":"D","text":"汇入对向车流。"}]'::jsonb, '双向左转车道由虚黄线和实黄线标出，仅用于准备和进行左转或掉头，不得用于直行或超车。', 'active');

    -- Q6: What does a single solid yellow line on your side of the road indicate?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a single solid yellow line on your side of the road indicate?', '[{"key":"A","text":"You may pass if the road ahead is clear."},{"key":"B","text":"You may not pass when the solid line is on your side."},{"key":"C","text":"You may cross the line to turn into a driveway."},{"key":"D","text":"You may use the lane for through travel."}]'::jsonb, 'A single solid yellow line on your side means you cannot pass. The handbook states: ''Single solid yellow line: do not pass when the solid line is on your side.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '您所在车道一侧的单实黄线表示什么？', '[{"key":"A","text":"如果前方道路畅通，您可以超车。"},{"key":"B","text":"当实线在您一侧时，您不得超车。"},{"key":"C","text":"您可以越过该线转入车道。"},{"key":"D","text":"您可以使用该车道进行直行。"}]'::jsonb, '您一侧的单实黄线表示禁止超车。手册指出：“单实黄线：当实线在您一侧时，不得超车。”', 'active');

    -- Q7: Under what condition may you cross double solid yellow lines?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Under what condition may you cross double solid yellow lines?', '[{"key":"A","text":"To pass a slow-moving vehicle."},{"key":"B","text":"To turn left into or out of a driveway or make a legal U-turn."},{"key":"C","text":"To avoid an obstacle in your lane."},{"key":"D","text":"To merge into a carpool lane."}]'::jsonb, 'Double solid yellow lines may only be crossed to turn left into or out of a driveway or make a legal U-turn, per the handbook.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在什么情况下可以越过双实黄线？', '[{"key":"A","text":"为了超越慢速行驶的车辆。"},{"key":"B","text":"为了左转进出车道或进行合法掉头。"},{"key":"C","text":"为了避开您车道上的障碍物。"},{"key":"D","text":"为了并入拼车车道。"}]'::jsonb, '根据手册，只有在左转进出车道或进行合法掉头时，才能越过双实黄线。', 'active');

    -- Q8: What is the purpose of a two-way left-turn center lane?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the purpose of a two-way left-turn center lane?', '[{"key":"A","text":"To allow through travel in both directions."},{"key":"B","text":"To prepare for and make left turns or U-turns only."},{"key":"C","text":"To pass other vehicles when safe."},{"key":"D","text":"To merge into traffic from a driveway."}]'::jsonb, 'The handbook states the two-way left-turn lane is used only to prepare for and make left turns or U-turns, not for through travel or passing.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '双向左转中央车道的用途是什么？', '[{"key":"A","text":"允许双向直行。"},{"key":"B","text":"仅用于准备和进行左转或掉头。"},{"key":"C","text":"在安全时超车。"},{"key":"D","text":"从车道并入车流。"}]'::jsonb, '手册指出，双向左转车道仅用于准备和进行左转或掉头，不得用于直行或超车。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_LANE_USE_PARKING  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_LANE_USE_PARKING';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: When parking on a downhill slope with a curb, which direction should you turn your front wheels?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When parking on a downhill slope with a curb, which direction should you turn your front wheels?', '[{"key":"A","text":"Away from the curb"},{"key":"B","text":"Toward the curb"},{"key":"C","text":"Parallel to the curb"},{"key":"D","text":"Toward the edge of the road"}]'::jsonb, 'According to the handbook, when facing downhill, turn your front wheels toward the curb so the vehicle will roll into the curb if the brakes fail.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在下坡且有路缘的路边停车时，前轮应朝向哪个方向？', '[{"key":"A","text":"远离路缘"},{"key":"B","text":"朝向路缘"},{"key":"C","text":"与路缘平行"},{"key":"D","text":"朝向道路边缘"}]'::jsonb, '根据手册，下坡停车时应将前轮转向路缘，这样如果刹车失灵，车辆会滚向路缘。', 'active');

    -- Q2: What does a red painted curb indicate?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a red painted curb indicate?', '[{"key":"A","text":"Limited-time parking with posted signs"},{"key":"B","text":"Loading zone, you must stay with the vehicle"},{"key":"C","text":"No stopping, standing, or parking"},{"key":"D","text":"Parking for persons with disabilities"}]'::jsonb, 'The handbook states red curb means no stopping, standing, or parking at any time.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '红色路缘石表示什么？', '[{"key":"A","text":"限时停车，需查看标志"},{"key":"B","text":"装卸区，驾驶员必须留在车内"},{"key":"C","text":"禁止停车、临时停车或长时间停车"},{"key":"D","text":"残疾人专用停车位"}]'::jsonb, '手册规定红色路缘石表示任何时候都禁止停车、临时停车或长时间停车。', 'active');

    -- Q3: In a center left-turn lane, how far before making a turn may you legally drive in it?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'In a center left-turn lane, how far before making a turn may you legally drive in it?', '[{"key":"A","text":"No more than 100 feet"},{"key":"B","text":"No more than 200 feet"},{"key":"C","text":"No more than 300 feet"},{"key":"D","text":"No more than 500 feet"}]'::jsonb, 'The handbook says you may use the center left-turn lane no more than 200 feet before the turn.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在中央左转车道中，最多可以提前多远的距离驶入该车道？', '[{"key":"A","text":"不超过100英尺"},{"key":"B","text":"不超过200英尺"},{"key":"C","text":"不超过300英尺"},{"key":"D","text":"不超过500英尺"}]'::jsonb, '手册规定，在转弯前使用中央左转车道的距离不得超过200英尺。', 'active');

    -- Q4: On a two-lane road, when must a slow driver use a turnout lane?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'On a two-lane road, when must a slow driver use a turnout lane?', '[{"key":"A","text":"When three or more vehicles are following and passing is unsafe"},{"key":"B","text":"When five or more vehicles are following and passing is unsafe"},{"key":"C","text":"When any vehicle is behind and the speed limit is 25 mph"},{"key":"D","text":"Only when a sign specifically requires it"}]'::jsonb, 'The handbook states a slow driver must use a turnout when five or more vehicles are following and passing is unsafe.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在双车道道路上，慢速驾驶员在什么情况下必须使用避让车道？', '[{"key":"A","text":"当后方有三辆或以上车辆跟随且超车不安全时"},{"key":"B","text":"当后方有五辆或以上车辆跟随且超车不安全时"},{"key":"C","text":"当有任何车辆跟在后面且限速为25英里/小时时"},{"key":"D","text":"仅在标志明确要求时"}]'::jsonb, '手册规定，当后方有五辆或以上车辆跟随且超车不安全时，慢速驾驶员必须使用避让车道。', 'active');

    -- Q5: When parking downhill on a street with a curb, which way should you turn your front wheels?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When parking downhill on a street with a curb, which way should you turn your front wheels?', '[{"key":"A","text":"Away from the curb"},{"key":"B","text":"Toward the curb"},{"key":"C","text":"Parallel to the curb"},{"key":"D","text":"Toward the edge of the road"}]'::jsonb, 'According to the handbook, when facing downhill with a curb, turn your front wheels toward the curb so the vehicle will roll into the curb if the brakes fail.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在有路缘的下坡路段停车时，前轮应朝向哪个方向？', '[{"key":"A","text":"远离路缘"},{"key":"B","text":"朝向路缘"},{"key":"C","text":"与路缘平行"},{"key":"D","text":"朝向道路边缘"}]'::jsonb, '根据手册，在下坡且有路缘时，应将前轮转向路缘，这样如果刹车失灵，车辆会滚向路缘。', 'active');

    -- Q6: What is the maximum distance you may drive in a center left-turn lane before making a left turn?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the maximum distance you may drive in a center left-turn lane before making a left turn?', '[{"key":"A","text":"100 feet"},{"key":"B","text":"200 feet"},{"key":"C","text":"300 feet"},{"key":"D","text":"500 feet"}]'::jsonb, 'The handbook states that the center left-turn lane should be used no more than 200 feet before the turn.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '使用中央左转车道进行左转前，最多可以行驶多远？', '[{"key":"A","text":"100英尺"},{"key":"B","text":"200英尺"},{"key":"C","text":"300英尺"},{"key":"D","text":"500英尺"}]'::jsonb, '手册规定，中央左转车道应在转弯前不超过200英尺的距离内使用。', 'active');

    -- Q7: When must a slow driver on a two-lane road use a turnout lane?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When must a slow driver on a two-lane road use a turnout lane?', '[{"key":"A","text":"When two or more vehicles are following and passing is unsafe"},{"key":"B","text":"When three or more vehicles are following and passing is unsafe"},{"key":"C","text":"When five or more vehicles are following and passing is unsafe"},{"key":"D","text":"When any vehicle is following closely"}]'::jsonb, 'The handbook specifies that a slow driver must use a turnout when five or more vehicles are following and passing is unsafe.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在双车道道路上，慢速驾驶员何时必须使用避让车道？', '[{"key":"A","text":"当有两辆或以上车辆跟随且超车不安全时"},{"key":"B","text":"当有三辆或以上车辆跟随且超车不安全时"},{"key":"C","text":"当有五辆或以上车辆跟随且超车不安全时"},{"key":"D","text":"当有任何车辆紧跟时"}]'::jsonb, '手册规定，当有五辆或以上车辆跟随且超车不安全时，慢速驾驶员必须使用避让车道。', 'active');

    -- Q8: When parking on a hill facing uphill with a curb, which way should you turn your front wheels?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When parking on a hill facing uphill with a curb, which way should you turn your front wheels?', '[{"key":"A","text":"Toward the curb"},{"key":"B","text":"Away from the curb"},{"key":"C","text":"Straight ahead"},{"key":"D","text":"Toward the edge of the road"}]'::jsonb, 'According to the handbook, when facing uphill with a curb, turn your wheels away from the curb so the wheel rests against it if the vehicle rolls back.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在上坡且有路缘的路边停车时，前轮应该朝哪个方向转动？', '[{"key":"A","text":"朝向路缘"},{"key":"B","text":"远离路缘"},{"key":"C","text":"保持笔直"},{"key":"D","text":"朝向道路边缘"}]'::jsonb, '根据手册，在上坡且有路缘时，应将前轮转向远离路缘的方向，这样如果车辆后溜，车轮会抵住路缘。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_TURNS_UTURNS  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_TURNS_UTURNS';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: When making a right turn on a solid red light, what must you do before proceeding?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When making a right turn on a solid red light, what must you do before proceeding?', '[{"key":"A","text":"Come to a complete stop and yield to traffic and pedestrians, unless a ''NO TURN ON RED'' sign is posted."},{"key":"B","text":"Slow down and proceed if no vehicles are approaching."},{"key":"C","text":"Stop only if a pedestrian is in the crosswalk."},{"key":"D","text":"Yield to traffic but you may proceed without stopping if the intersection is clear."}]'::jsonb, 'The handbook states that a right turn on a solid red is allowed only after a complete stop and yielding, unless a ''NO TURN ON RED'' sign is posted.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在稳定的红色信号灯下右转时，你必须在继续行驶前做什么？', '[{"key":"A","text":"完全停车并让行于交通和行人，除非有''禁止红灯右转''标志。"},{"key":"B","text":"减速并在没有车辆接近时继续行驶。"},{"key":"C","text":"仅当行人在人行横道内时才停车。"},{"key":"D","text":"让行于交通，但如果路口畅通，你可以不停车继续行驶。"}]'::jsonb, '手册指出，只有在完全停车并让行后，才允许在稳定的红色信号灯下右转，除非有''禁止红灯右转''标志。', 'active');

    -- Q2: In which of the following situations is a U-turn prohibited?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'In which of the following situations is a U-turn prohibited?', '[{"key":"A","text":"Across a double yellow line in a residential district when no vehicle is approaching within 200 feet."},{"key":"B","text":"At an intersection on a green light where no ''NO U-TURN'' sign is posted."},{"key":"C","text":"On a one-way street."},{"key":"D","text":"On a divided highway through a center-divider opening."}]'::jsonb, 'The handbook explicitly lists U-turns on a one-way street as prohibited. The other options are allowed under the conditions described.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在以下哪种情况下禁止掉头？', '[{"key":"A","text":"在住宅区内跨越双黄线，且200英尺内没有车辆接近时。"},{"key":"B","text":"在绿灯亮起的路口，且没有''禁止掉头''标志时。"},{"key":"C","text":"在单行道上。"},{"key":"D","text":"在分隔式高速公路上通过中央分隔带开口处。"}]'::jsonb, '手册明确将单行道上的掉头列为禁止行为。其他选项在所述条件下是允许的。', 'active');

    -- Q3: When may a left turn be made against a red light?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When may a left turn be made against a red light?', '[{"key":"A","text":"From a two-way street onto a one-way street, after stopping and yielding."},{"key":"B","text":"From a one-way street onto a one-way street, after stopping and yielding, where not prohibited."},{"key":"C","text":"From any street onto a two-way street, if no traffic is approaching."},{"key":"D","text":"From a one-way street onto a two-way street, after slowing down."}]'::jsonb, 'The handbook states that a left turn against a red light is allowed only from a one-way street onto a one-way street, after stopping and yielding, where not prohibited.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '何时可以在红色信号灯下左转？', '[{"key":"A","text":"从双向道转入单行道，在停车并让行后。"},{"key":"B","text":"从单行道转入单行道，在停车并让行后，且未被禁止时。"},{"key":"C","text":"从任何道路转入双向道，如果没有车辆接近。"},{"key":"D","text":"从单行道转入双向道，在减速后。"}]'::jsonb, '手册指出，只有在从单行道转入单行道时，在停车并让行后且未被禁止的情况下，才允许在红色信号灯下左转。', 'active');

    -- Q4: When making a left turn, where should you position your front wheels while waiting for a gap in onco...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When making a left turn, where should you position your front wheels while waiting for a gap in oncoming traffic?', '[{"key":"A","text":"Turned slightly to the left to prepare for the turn."},{"key":"B","text":"Pointed straight ahead until it is safe to turn."},{"key":"C","text":"Turned to the right to block traffic from behind."},{"key":"D","text":"Pointed toward the curb to avoid being pushed into traffic."}]'::jsonb, 'The handbook states: ''Keep your front wheels pointed STRAIGHT until it is safe to turn (if hit from behind with wheels turned left, you could be pushed into oncoming traffic).''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在左转等待对向车流间隙时，你的前轮应指向哪个方向？', '[{"key":"A","text":"稍微向左转，为转弯做准备。"},{"key":"B","text":"保持直行方向，直到可以安全转弯。"},{"key":"C","text":"向右转，以阻挡后方来车。"},{"key":"D","text":"指向路缘，避免被推入车流。"}]'::jsonb, '手册指出：''保持前轮指向正前方，直到可以安全转弯（如果车轮向左转时被后车撞击，你可能被推入对向车道）。''', 'active');

    -- Q5: Under which condition is a right turn against a red arrow allowed?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'D', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Under which condition is a right turn against a red arrow allowed?', '[{"key":"A","text":"After a complete stop and yielding to pedestrians and traffic."},{"key":"B","text":"When no ''NO TURN ON RED'' sign is posted."},{"key":"C","text":"When turning from a one-way street onto a one-way street."},{"key":"D","text":"It is never allowed."}]'::jsonb, 'The handbook explicitly says: ''Right turn on a RED ARROW is NOT allowed.'' No exceptions are given for red arrows.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在哪种情况下允许在红色箭头灯亮时右转？', '[{"key":"A","text":"在完全停车并让行行人和车辆后。"},{"key":"B","text":"当没有设置''禁止红灯右转''标志时。"},{"key":"C","text":"当从单行道转入单行道时。"},{"key":"D","text":"绝不允许。"}]'::jsonb, '手册明确说明：''红色箭头灯亮时不允许右转。'' 没有例外情况。', 'active');

    -- Q6: In which of the following situations is a U-turn always prohibited?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'D', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'In which of the following situations is a U-turn always prohibited?', '[{"key":"A","text":"At an intersection with a green light or green arrow."},{"key":"B","text":"On a divided highway through a center-divider opening."},{"key":"C","text":"In a residential district when no vehicle is approaching within 200 feet."},{"key":"D","text":"On a one-way street."}]'::jsonb, 'The handbook lists U-turn prohibitions including ''on a one-way street.'' The other options are allowed under specific conditions.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在以下哪种情况下，掉头始终被禁止？', '[{"key":"A","text":"在有绿灯或绿色箭头灯的交叉路口。"},{"key":"B","text":"在分隔式高速公路的中央分隔带开口处。"},{"key":"C","text":"在住宅区，且200英尺内无来车时。"},{"key":"D","text":"在单行道上。"}]'::jsonb, '手册列出禁止掉头的情况包括''在单行道上''。其他选项在特定条件下是允许的。', 'active');

    -- Q7: When making a left turn from a one-way street onto another one-way street, what is the rule regardin...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When making a left turn from a one-way street onto another one-way street, what is the rule regarding a red light?', '[{"key":"A","text":"Left turn against a red light is allowed after stopping and yielding, if not prohibited."},{"key":"B","text":"Left turn against a red light is never allowed under any circumstances."},{"key":"C","text":"Left turn against a red light is allowed only if there is a green arrow."},{"key":"D","text":"Left turn against a red light is allowed only if no traffic is approaching from any direction."}]'::jsonb, 'The handbook states: ''A left turn against a red light is allowed only from a one-way street onto a one-way street, after stopping and yielding, where not prohibited.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '从单行道左转进入另一条单行道时，关于红灯的规则是什么？', '[{"key":"A","text":"在停车让行且未被禁止的情况下，允许红灯时左转。"},{"key":"B","text":"任何情况下都不允许红灯时左转。"},{"key":"C","text":"只有在有绿色箭头灯时才允许红灯时左转。"},{"key":"D","text":"只有在任何方向都没有来车时才允许红灯时左转。"}]'::jsonb, '手册指出：''只有在从单行道左转进入另一条单行道时，在停车让行且未被禁止的情况下，才允许红灯时左转。''', 'active');

    -- Q8: When making a left turn, what must you do with your front wheels while waiting for a safe gap in onc...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When making a left turn, what must you do with your front wheels while waiting for a safe gap in oncoming traffic?', '[{"key":"A","text":"Turn them slightly left to prepare for the turn."},{"key":"B","text":"Keep them pointed straight ahead."},{"key":"C","text":"Turn them to the right to block other vehicles."},{"key":"D","text":"Angle them toward the curb to avoid rolling."}]'::jsonb, 'The handbook states: ''Keep your front wheels pointed STRAIGHT until it is safe to turn (if hit from behind with wheels turned left, you could be pushed into oncoming traffic).''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在左转等待对向车流安全间隙时，应如何放置前轮？', '[{"key":"A","text":"稍微向左转动，为转弯做准备。"},{"key":"B","text":"保持车轮指向正前方。"},{"key":"C","text":"向右转动以阻挡其他车辆。"},{"key":"D","text":"朝向路缘倾斜以防车辆滑动。"}]'::jsonb, '手册规定：''保持前轮指向正前方，直到安全才能转弯（如果车轮向左转时被后车追尾，可能会被推入对向车道）。''', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_MERGE_PASS  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_MERGE_PASS';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: When merging onto a freeway, what is the correct action regarding the on-ramp?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When merging onto a freeway, what is the correct action regarding the on-ramp?', '[{"key":"A","text":"Stop at the end of the on-ramp and wait for a large gap."},{"key":"B","text":"Use the on-ramp to get up to near traffic speed before merging."},{"key":"C","text":"Maintain a slow speed on the on-ramp to ensure safety."},{"key":"D","text":"Accelerate quickly on the on-ramp and merge immediately."}]'::jsonb, 'The handbook states: ''Use the on-ramp to get up to near traffic speed, then merge into a gap large enough for your vehicle.'' Stopping on the ramp is only allowed if necessary, not a routine action.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '汇入高速公路时，关于匝道的正确做法是什么？', '[{"key":"A","text":"在匝道末端停车，等待一个足够大的间隙。"},{"key":"B","text":"利用匝道将车速提升至接近车流速度后再汇入。"},{"key":"C","text":"在匝道上保持低速以确保安全。"},{"key":"D","text":"在匝道上快速加速并立即汇入。"}]'::jsonb, '手册指出：''利用匝道将车速提升至接近车流速度，然后汇入一个足够大的间隙。'' 仅在必要时才可在匝道上停车，这不是常规操作。', 'active');

    -- Q2: At what distance should you signal before exiting a freeway?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'At what distance should you signal before exiting a freeway?', '[{"key":"A","text":"About 100 feet before the exit."},{"key":"B","text":"About 200 feet before the exit."},{"key":"C","text":"About 400 feet before the exit."},{"key":"D","text":"About 600 feet before the exit."}]'::jsonb, 'The handbook specifies: ''Signal about five seconds (roughly 400 feet) before your exit.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在驶出高速公路前，应在多远处打转向灯？', '[{"key":"A","text":"在出口前约100英尺处。"},{"key":"B","text":"在出口前约200英尺处。"},{"key":"C","text":"在出口前约400英尺处。"},{"key":"D","text":"在出口前约600英尺处。"}]'::jsonb, '手册明确规定：''在出口前约五秒（大约400英尺）打转向灯。''', 'active');

    -- Q3: When crossing or entering traffic from a stop on a highway, how much space do you need to reach traf...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When crossing or entering traffic from a stop on a highway, how much space do you need to reach traffic speed?', '[{"key":"A","text":"About a half block (≈150 feet)."},{"key":"B","text":"About a full block (≈300 feet)."},{"key":"C","text":"About two blocks (≈600 feet)."},{"key":"D","text":"About 100 feet."}]'::jsonb, 'The handbook states: ''You need enough space to reach traffic speed: about a half block (≈150 feet) on city streets and a full block (≈300 feet) on the highway.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在高速公路上从停止状态汇入车流时，需要多少空间才能达到车流速度？', '[{"key":"A","text":"大约半个街区（约150英尺）。"},{"key":"B","text":"大约一个街区（约300英尺）。"},{"key":"C","text":"大约两个街区（约600英尺）。"},{"key":"D","text":"大约100英尺。"}]'::jsonb, '手册指出：''你需要足够的空间来达到车流速度：在城市街道上大约半个街区（约150英尺），在高速公路上大约一个街区（约300英尺）。''', 'active');

    -- Q4: What should you do when another vehicle is passing you?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What should you do when another vehicle is passing you?', '[{"key":"A","text":"Speed up to prevent the other vehicle from passing."},{"key":"B","text":"Slow down to let the other vehicle pass more quickly."},{"key":"C","text":"Hold your lane and speed, and let the other vehicle by."},{"key":"D","text":"Move to the right lane to give the other vehicle more room."}]'::jsonb, 'The handbook states: ''When being passed, hold your lane and speed; let the other vehicle by.'' Speeding up or slowing down is incorrect; moving to another lane is not required.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当其他车辆正在超你车时，你应该怎么做？', '[{"key":"A","text":"加速以防止其他车辆超车。"},{"key":"B","text":"减速让其他车辆更快通过。"},{"key":"C","text":"保持你的车道和速度，让其他车辆通过。"},{"key":"D","text":"向右变道给其他车辆更多空间。"}]'::jsonb, '手册指出：''当被超车时，保持你的车道和速度；让其他车辆通过。'' 加速或减速都是错误的；不需要变道。', 'active');

    -- Q5: When merging onto a freeway, what should you do if you cannot find a gap in traffic?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When merging onto a freeway, what should you do if you cannot find a gap in traffic?', '[{"key":"A","text":"Stop on the on-ramp and wait for a gap."},{"key":"B","text":"Continue on the on-ramp, adjust speed, and merge when a gap appears."},{"key":"C","text":"Cross the solid white line to force your way in."},{"key":"D","text":"Speed up and merge immediately before the on-ramp ends."}]'::jsonb, 'The handbook says to use the on-ramp to get up to near traffic speed and merge into a gap large enough. It advises not to stop on the ramp unless necessary, and not to cross solid lines.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在汇入高速公路时，如果找不到合适的车流间隙，你应该怎么做？', '[{"key":"A","text":"在匝道上停车等待间隙。"},{"key":"B","text":"继续在匝道上行驶，调整车速，待出现间隙时汇入。"},{"key":"C","text":"跨越实白线强行汇入。"},{"key":"D","text":"加速并在匝道结束前立即汇入。"}]'::jsonb, '手册指出应利用匝道将车速提升至接近车流速度，并汇入足够大的间隙。除非必要，不应在匝道上停车，也不应跨越实线。', 'active');

    -- Q6: When crossing or entering traffic from a stop on a highway, how much space do you need to reach traf...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When crossing or entering traffic from a stop on a highway, how much space do you need to reach traffic speed?', '[{"key":"A","text":"About 50 feet."},{"key":"B","text":"About 150 feet."},{"key":"C","text":"About 300 feet."},{"key":"D","text":"About 500 feet."}]'::jsonb, 'The handbook specifies you need about a full block (≈300 feet) on the highway to reach traffic speed.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在高速公路上从停止状态穿越或汇入车流时，你需要多少空间来达到车流速度？', '[{"key":"A","text":"大约50英尺。"},{"key":"B","text":"大约150英尺。"},{"key":"C","text":"大约300英尺。"},{"key":"D","text":"大约500英尺。"}]'::jsonb, '手册规定在高速公路上你需要大约一个街区（约300英尺）来达到车流速度。', 'active');

    -- Q7: When being passed by another vehicle, what should you do?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When being passed by another vehicle, what should you do?', '[{"key":"A","text":"Speed up to prevent the pass."},{"key":"B","text":"Slow down to let them pass more quickly."},{"key":"C","text":"Hold your lane and speed, and let the other vehicle by."},{"key":"D","text":"Move to the right to give them more room."}]'::jsonb, 'The handbook states: ''When being passed, hold your lane and speed; let the other vehicle by.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当被其他车辆超车时，你应该怎么做？', '[{"key":"A","text":"加速以防止被超车。"},{"key":"B","text":"减速让他们更快通过。"},{"key":"C","text":"保持你的车道和速度，让对方车辆通过。"},{"key":"D","text":"向右移动以给他们更多空间。"}]'::jsonb, '手册规定：''被超车时，保持你的车道和速度，让对方车辆通过。''', 'active');

    -- Q8: When merging onto a freeway, what is the correct action regarding the on-ramp and merging into traff...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When merging onto a freeway, what is the correct action regarding the on-ramp and merging into traffic?', '[{"key":"A","text":"Stop at the end of the on-ramp to wait for a large gap in traffic before accelerating."},{"key":"B","text":"Use the on-ramp to accelerate to near the speed of freeway traffic, then merge into a suitable gap."},{"key":"C","text":"Merge immediately onto the freeway at the start of the on-ramp, then accelerate to match traffic speed."},{"key":"D","text":"Signal and merge onto the freeway at a slow speed, then gradually accelerate to match traffic."}]'::jsonb, 'The handbook states to use the on-ramp to get up to near traffic speed, then merge into a gap large enough for your vehicle. Stopping on the ramp is not recommended unless necessary, and merging at a slow speed is unsafe.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在汇入高速公路时，关于使用匝道和汇入车流，以下哪项做法是正确的？', '[{"key":"A","text":"在匝道末端停车，等待车流中出现大空隙后再加速汇入。"},{"key":"B","text":"利用匝道加速至接近高速公路车流的速度，然后汇入合适的空隙。"},{"key":"C","text":"在匝道起点立即汇入高速公路，然后再加速以匹配车流速度。"},{"key":"D","text":"以慢速打灯汇入高速公路，然后逐渐加速以匹配车流。"}]'::jsonb, '手册指出应利用匝道加速至接近车流速度，然后汇入足够大的空隙。除非必要，不应在匝道上停车，以慢速汇入也不安全。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_ALCOHOL_BAC  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_ALCOHOL_BAC';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: What is the maximum legal Blood Alcohol Concentration (BAC) for a commercial driver's license (CDL) ...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the maximum legal Blood Alcohol Concentration (BAC) for a commercial driver''s license (CDL) holder in California?', '[{"key":"A","text":"0.08%"},{"key":"B","text":"0.04%"},{"key":"C","text":"0.01%"},{"key":"D","text":"0.00%"}]'::jsonb, 'The handbook states that the BAC limit for commercial driver''s license (CDL) holders is 0.04%, which is lower than the 0.08% limit for non-commercial drivers aged 21 or older.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在加州，持有商业驾驶执照（CDL）的驾驶员的法定最高血液酒精浓度（BAC）是多少？', '[{"key":"A","text":"0.08%"},{"key":"B","text":"0.04%"},{"key":"C","text":"0.01%"},{"key":"D","text":"0.00%"}]'::jsonb, '手册指出，商业驾驶执照（CDL）持有者的BAC限值为0.04%，低于21岁及以上非商业驾驶员的0.08%限值。', 'active');

    -- Q2: Under California's implied consent law, what happens if a driver lawfully arrested for DUI refuses t...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Under California''s implied consent law, what happens if a driver lawfully arrested for DUI refuses to take a chemical test?', '[{"key":"A","text":"The driver receives a warning but no penalty."},{"key":"B","text":"The driver''s license is suspended or revoked."},{"key":"C","text":"The driver is immediately jailed for six months."},{"key":"D","text":"The driver must pay a fine of $500."}]'::jsonb, 'The handbook says that refusing the chemical test after a lawful DUI arrest leads to suspension or revocation of your driving privilege.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据加州的默示同意法，如果因酒驾被合法逮捕的驾驶员拒绝接受化学检测，会有什么后果？', '[{"key":"A","text":"驾驶员会收到警告，但无处罚。"},{"key":"B","text":"驾驶员的驾照会被暂停或吊销。"},{"key":"C","text":"驾驶员会被立即监禁六个月。"},{"key":"D","text":"驾驶员必须支付500美元罚款。"}]'::jsonb, '手册指出，在合法酒驾逮捕后拒绝化学检测会导致驾驶特权被暂停或吊销。', 'active');

    -- Q3: Which of the following is NOT an exception to California's open-container rule for alcohol or cannab...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Which of the following is NOT an exception to California''s open-container rule for alcohol or cannabis in a vehicle?', '[{"key":"A","text":"Passengers in a bus."},{"key":"B","text":"Passengers in a taxi."},{"key":"C","text":"Passengers in a private sedan."},{"key":"D","text":"Passengers in a motorhome."}]'::jsonb, 'The handbook lists buses, taxis, campers, and motorhomes as exceptions to the open-container rules. A private sedan is not listed as an exception.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '以下哪项不属于加州车内酒精或大麻容器开封规则的例外情况？', '[{"key":"A","text":"公交车上的乘客。"},{"key":"B","text":"出租车上的乘客。"},{"key":"C","text":"私人轿车的乘客。"},{"key":"D","text":"房车上的乘客。"}]'::jsonb, '手册列出了公交车、出租车、露营车和房车作为开封容器规则的例外情况。私人轿车未被列为例外。', 'active');

    -- Q4: What is the legal Blood Alcohol Concentration (BAC) limit for a driver under 21 years old in Califor...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the legal Blood Alcohol Concentration (BAC) limit for a driver under 21 years old in California?', '[{"key":"A","text":"0.08%"},{"key":"B","text":"0.04%"},{"key":"C","text":"0.01%"},{"key":"D","text":"0.00%"}]'::jsonb, 'The handbook states that for drivers under 21 years old, the BAC limit is 0.01% under the zero tolerance law.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在加利福尼亚州，21岁以下驾驶员的法定血液酒精浓度（BAC）限值是多少？', '[{"key":"A","text":"0.08%"},{"key":"B","text":"0.04%"},{"key":"C","text":"0.01%"},{"key":"D","text":"0.00%"}]'::jsonb, '手册指出，根据零容忍法律，21岁以下驾驶员的BAC限值为0.01%。', 'active');

    -- Q5: Where must an open container of alcohol be stored in a vehicle according to California law?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Where must an open container of alcohol be stored in a vehicle according to California law?', '[{"key":"A","text":"In the glove box."},{"key":"B","text":"In the passenger area."},{"key":"C","text":"In the trunk."},{"key":"D","text":"Under the driver''s seat."}]'::jsonb, 'The handbook states an open container must be kept in the trunk, never in the glove box or passenger area.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据加利福尼亚州法律，车内的已开封酒精容器应存放在哪里？', '[{"key":"A","text":"手套箱内。"},{"key":"B","text":"乘客区域。"},{"key":"C","text":"后备箱内。"},{"key":"D","text":"驾驶员座位下。"}]'::jsonb, '手册规定，已开封的容器必须放在后备箱内，绝不能放在手套箱或乘客区域。', 'active');

    -- Q6: What is the maximum legal Blood Alcohol Concentration (BAC) for a commercial driver license (CDL) ho...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the maximum legal Blood Alcohol Concentration (BAC) for a commercial driver license (CDL) holder in California?', '[{"key":"A","text":"0.01%"},{"key":"B","text":"0.04%"},{"key":"C","text":"0.08%"},{"key":"D","text":"0.00%"}]'::jsonb, 'According to the handbook, the BAC limit for commercial driver license (CDL) holders is 0.04%.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在加州，商业驾驶执照（CDL）持有者的法定最高血液酒精浓度（BAC）是多少？', '[{"key":"A","text":"0.01%"},{"key":"B","text":"0.04%"},{"key":"C","text":"0.08%"},{"key":"D","text":"0.00%"}]'::jsonb, '根据手册，商业驾驶执照（CDL）持有者的BAC限值为0.04%。', 'active');

    -- Q7: In California, where must an open container of alcohol be kept in a vehicle?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'In California, where must an open container of alcohol be kept in a vehicle?', '[{"key":"A","text":"In the glove box"},{"key":"B","text":"In the passenger area"},{"key":"C","text":"In the trunk"},{"key":"D","text":"Under the driver''s seat"}]'::jsonb, 'The handbook specifies that an open container must be kept in the trunk, never in the glove box or passenger area.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在加州，车内打开的酒精容器必须放在哪里？', '[{"key":"A","text":"手套箱内"},{"key":"B","text":"乘客区"},{"key":"C","text":"后备箱内"},{"key":"D","text":"驾驶座下方"}]'::jsonb, '手册规定，打开的容器必须放在后备箱内，绝不能放在手套箱或乘客区。', 'active');

    -- Q8: What is the maximum blood alcohol concentration (BAC) allowed for a driver under 21 years old in Cal...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the maximum blood alcohol concentration (BAC) allowed for a driver under 21 years old in California?', '[{"key":"A","text":"0.00%"},{"key":"B","text":"0.01%"},{"key":"C","text":"0.04%"},{"key":"D","text":"0.08%"}]'::jsonb, 'California''s zero-tolerance law sets the BAC limit at 0.01% for drivers under 21. A limit of 0.00% is not stated; 0.04% applies to commercial drivers; 0.08% applies to drivers 21 or older.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在加州，21 岁以下驾驶员的最高法定血液酒精浓度 (BAC) 是多少？', '[{"key":"A","text":"0.00%"},{"key":"B","text":"0.01%"},{"key":"C","text":"0.04%"},{"key":"D","text":"0.08%"}]'::jsonb, '加州的零容忍法律规定，21 岁以下驾驶员的 BAC 限值为 0.01%。0.00% 并非规定值；0.04% 适用于商业驾驶员；0.08% 适用于 21 岁及以上驾驶员。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_DRUGS_IMPAIRMENT  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_DRUGS_IMPAIRMENT';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: According to the handbook, which of the following is true about driving under the influence of drugs...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'According to the handbook, which of the following is true about driving under the influence of drugs?', '[{"key":"A","text":"Only illegal drugs and alcohol can impair driving."},{"key":"B","text":"Prescription and over-the-counter medications can also impair driving."},{"key":"C","text":"Cannabis is legal, so it does not impair driving."},{"key":"D","text":"Only alcohol is considered a drug under the law."}]'::jsonb, 'The handbook states that it is illegal to drive under the influence of any drug, including prescription and over-the-counter medications, as they can impair driving.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据手册，以下哪项关于药物影响下驾驶的说法是正确的？', '[{"key":"A","text":"只有非法药物和酒精会影响驾驶能力。"},{"key":"B","text":"处方药和非处方药也可能影响驾驶能力。"},{"key":"C","text":"大麻是合法的，因此不会影响驾驶能力。"},{"key":"D","text":"根据法律，只有酒精被视为药物。"}]'::jsonb, '手册指出，在药物影响下驾驶是违法的，包括处方药和非处方药，因为它们可能影响驾驶能力。', 'active');

    -- Q2: What is the blood alcohol concentration (BAC) limit that triggers a one-year license suspension for ...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the blood alcohol concentration (BAC) limit that triggers a one-year license suspension for a driver under 21 under California''s zero-tolerance law?', '[{"key":"A","text":"0.08%"},{"key":"B","text":"0.05%"},{"key":"C","text":"0.01%"},{"key":"D","text":"0.00%"}]'::jsonb, 'The handbook specifies that a BAC of 0.01% or higher for a driver under 21 can result in a one-year suspension or revocation of the driving privilege.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据加州零容忍法律，21岁以下驾驶员的血液酒精浓度达到多少会触发一年驾照吊销？', '[{"key":"A","text":"0.08%"},{"key":"B","text":"0.05%"},{"key":"C","text":"0.01%"},{"key":"D","text":"0.00%"}]'::jsonb, '手册规定，21岁以下驾驶员血液酒精浓度达到0.01%或更高可能导致一年驾照吊销或撤销。', 'active');

    -- Q3: Under the zero-tolerance law, what may happen to a driver under 21 who is found with a BAC of 0.05% ...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Under the zero-tolerance law, what may happen to a driver under 21 who is found with a BAC of 0.05% or higher?', '[{"key":"A","text":"They will only receive a warning."},{"key":"B","text":"They may be arrested and face additional suspension."},{"key":"C","text":"Their vehicle will be impounded for 30 days."},{"key":"D","text":"They must complete a traffic school course."}]'::jsonb, 'The handbook states that a BAC of 0.05% or higher for a driver under 21 may lead to arrest and additional suspension beyond the one-year suspension for 0.01%.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据零容忍法律，21岁以下驾驶员血液酒精浓度达到0.05%或更高可能会发生什么？', '[{"key":"A","text":"他们只会收到警告。"},{"key":"B","text":"他们可能会被逮捕并面临额外吊销。"},{"key":"C","text":"他们的车辆将被扣押30天。"},{"key":"D","text":"他们必须完成交通学校课程。"}]'::jsonb, '手册指出，21岁以下驾驶员血液酒精浓度达到0.05%或更高可能导致逮捕和额外吊销，超出0.01%时的一年吊销。', 'active');

    -- Q4: Which of the following is a consequence for a driver under 21 who possesses alcohol in a vehicle wit...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Which of the following is a consequence for a driver under 21 who possesses alcohol in a vehicle without a parent or guardian present?', '[{"key":"A","text":"A fine and a 6-month license suspension."},{"key":"B","text":"Vehicle impoundment for up to 30 days and a one-year license suspension."},{"key":"C","text":"Only a warning and a small fine."},{"key":"D","text":"Immediate arrest and a 0.08% BAC test."}]'::jsonb, 'The handbook states that possession of alcohol by a driver under 21 without a parent or guardian can lead to vehicle impoundment for up to 30 days, fines, and a one-year license suspension or delay.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '21岁以下驾驶员在车内无家长或监护人陪同的情况下持有酒精，以下哪项是可能的后果？', '[{"key":"A","text":"罚款和6个月驾照吊销。"},{"key":"B","text":"车辆扣押最多30天和一年驾照吊销。"},{"key":"C","text":"仅警告和小额罚款。"},{"key":"D","text":"立即逮捕和0.08%血液酒精浓度测试。"}]'::jsonb, '手册指出，21岁以下驾驶员在无家长或监护人陪同的情况下持有酒精可能导致车辆扣押最多30天、罚款和一年驾照吊销或延迟。', 'active');

    -- Q5: Under California law, which of the following substances can lead to a DUI charge?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Under California law, which of the following substances can lead to a DUI charge?', '[{"key":"A","text":"Only alcohol"},{"key":"B","text":"Only illegal drugs and alcohol"},{"key":"C","text":"Any drug, including cannabis, prescription, and over-the-counter medications"},{"key":"D","text":"Only cannabis and alcohol"}]'::jsonb, 'The handbook states it is illegal to drive under the influence of any drug, not only alcohol, and this applies equally to illegal drugs, cannabis, prescription medications, and over-the-counter medications.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据加州法律，以下哪种物质可能导致酒驾/毒驾指控？', '[{"key":"A","text":"只有酒精"},{"key":"B","text":"只有非法药物和酒精"},{"key":"C","text":"任何药物，包括大麻、处方药和非处方药"},{"key":"D","text":"只有大麻和酒精"}]'::jsonb, '手册指出，不仅酒后驾驶违法，任何药物影响下驾驶均属违法，这同样适用于非法药物、大麻、处方药和非处方药。', 'active');

    -- Q6: For a driver under 21, what blood alcohol concentration (BAC) can trigger a one-year license suspens...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'For a driver under 21, what blood alcohol concentration (BAC) can trigger a one-year license suspension under the zero-tolerance law?', '[{"key":"A","text":"0.08% or higher"},{"key":"B","text":"0.05% or higher"},{"key":"C","text":"0.01% or higher"},{"key":"D","text":"0.00% (any detectable amount)"}]'::jsonb, 'The handbook specifies that a BAC of 0.01% or higher can result in a one-year suspension or revocation of the driving privilege for drivers under 21.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '对于 21 岁以下的驾驶员，零容忍法律规定的血液酒精浓度 (BAC) 达到多少可导致一年驾照吊销？', '[{"key":"A","text":"0.08% 或更高"},{"key":"B","text":"0.05% 或更高"},{"key":"C","text":"0.01% 或更高"},{"key":"D","text":"0.00%（任何可检测到的量）"}]'::jsonb, '手册规定，21 岁以下驾驶员血液酒精浓度达到 0.01% 或更高可能导致一年驾照暂停或吊销。', 'active');

    -- Q7: What is a potential consequence for a driver under 21 who possesses alcohol in a vehicle without a p...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is a potential consequence for a driver under 21 who possesses alcohol in a vehicle without a parent or guardian present?', '[{"key":"A","text":"A warning and a fine of up to $100"},{"key":"B","text":"Vehicle impoundment for up to 30 days, fines, and a one-year license suspension or delay"},{"key":"C","text":"Immediate arrest and a mandatory jail sentence"},{"key":"D","text":"Only a one-year license suspension with no vehicle impoundment"}]'::jsonb, 'The handbook states that possession violations can lead to vehicle impoundment for up to 30 days, fines, and a one-year license suspension or delay.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '21 岁以下驾驶员在没有父母或监护人陪同的情况下在车内携带酒精，可能面临什么后果？', '[{"key":"A","text":"警告并处以最高 100 美元罚款"},{"key":"B","text":"车辆扣押最多 30 天、罚款以及一年驾照暂停或延迟"},{"key":"C","text":"立即逮捕并强制监禁"},{"key":"D","text":"仅一年驾照暂停，不扣押车辆"}]'::jsonb, '手册指出，违规携带酒精可能导致车辆扣押最多 30 天、罚款以及一年驾照暂停或延迟。', 'active');

    -- Q8: Under California's zero-tolerance law, what is the minimum blood alcohol concentration (BAC) that ca...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Under California''s zero-tolerance law, what is the minimum blood alcohol concentration (BAC) that can result in a one-year license suspension for a driver under 21?', '[{"key":"A","text":"0.08%"},{"key":"B","text":"0.05%"},{"key":"C","text":"0.01%"},{"key":"D","text":"0.00%"}]'::jsonb, 'The handbook states that for drivers under 21, a BAC of 0.01% or higher can result in a one-year suspension or revocation of the driving privilege, plus required completion of a DUI program. This is the zero-tolerance limit, far below the adult limit of 0.08%.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据加州零容忍法律，21 岁以下驾驶员的血液酒精浓度 (BAC) 达到多少时，可能导致驾驶执照被吊销一年？', '[{"key":"A","text":"0.08%"},{"key":"B","text":"0.05%"},{"key":"C","text":"0.01%"},{"key":"D","text":"0.00%"}]'::jsonb, '手册指出，对于 21 岁以下的驾驶员，BAC 达到 0.01% 或更高可能导致驾驶特权被暂停或吊销一年，并需完成 DUI 课程。这是零容忍限值，远低于成人的 0.08% 限值。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_CONDITIONS_ADVERSE  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_CONDITIONS_ADVERSE';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: What is the maximum speed you should drive in heavy rain or snow when visibility is under 100 feet?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the maximum speed you should drive in heavy rain or snow when visibility is under 100 feet?', '[{"key":"A","text":"25 mph"},{"key":"B","text":"30 mph"},{"key":"C","text":"35 mph"},{"key":"D","text":"20 mph"}]'::jsonb, 'The handbook states: ''In heavy rain or snow with visibility under 100 feet, don''t drive faster than 30 mph.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在能见度低于100英尺的大雨或大雪中，你应该以多快的速度行驶？', '[{"key":"A","text":"25英里/小时"},{"key":"B","text":"30英里/小时"},{"key":"C","text":"35英里/小时"},{"key":"D","text":"20英里/小时"}]'::jsonb, '手册指出：''在大雨或大雪中，能见度低于100英尺时，行驶速度不得超过30英里/小时。''', 'active');

    -- Q2: When your brakes get wet, what is the correct way to dry them?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When your brakes get wet, what is the correct way to dry them?', '[{"key":"A","text":"Pump the brakes rapidly while driving at normal speed"},{"key":"B","text":"Lightly press the brake while driving slowly"},{"key":"C","text":"Apply firm, steady pressure to the brake pedal"},{"key":"D","text":"Turn off the engine and coast to a stop"}]'::jsonb, 'The handbook says: ''If your brakes get wet, dry them by lightly pressing the brake while driving slowly.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当刹车变湿时，正确的干燥方法是什么？', '[{"key":"A","text":"以正常速度行驶时快速踩刹车"},{"key":"B","text":"缓慢行驶时轻踩刹车"},{"key":"C","text":"用力稳定地踩下刹车踏板"},{"key":"D","text":"关闭发动机并滑行至停车"}]'::jsonb, '手册指出：''如果刹车变湿，可以通过缓慢行驶时轻踩刹车来干燥它们。''', 'active');

    -- Q3: What is the maximum speed you should drive in heavy rain or snow when visibility is under 100 feet?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the maximum speed you should drive in heavy rain or snow when visibility is under 100 feet?', '[{"key":"A","text":"25 mph"},{"key":"B","text":"30 mph"},{"key":"C","text":"35 mph"},{"key":"D","text":"20 mph"}]'::jsonb, 'The handbook states that in heavy rain or snow with visibility under 100 feet, you should not drive faster than 30 mph.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在大雨或大雪中能见度低于100英尺时，最高行驶速度不应超过多少？', '[{"key":"A","text":"25英里/小时"},{"key":"B","text":"30英里/小时"},{"key":"C","text":"35英里/小时"},{"key":"D","text":"20英里/小时"}]'::jsonb, '手册规定，在大雨或大雪中能见度低于100英尺时，行驶速度不得超过30英里/小时。', 'active');

    -- Q4: When driving in fog or heavy smoke, which headlight setting should you use?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When driving in fog or heavy smoke, which headlight setting should you use?', '[{"key":"A","text":"High beams"},{"key":"B","text":"Low beams"},{"key":"C","text":"Parking lights only"},{"key":"D","text":"Hazard flashers only"}]'::jsonb, 'The handbook specifies to use low beams in fog or heavy smoke, as high beams can reflect and reduce visibility.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在雾天或浓烟中行驶时，应使用哪种车灯设置？', '[{"key":"A","text":"远光灯"},{"key":"B","text":"近光灯"},{"key":"C","text":"仅使用停车灯"},{"key":"D","text":"仅使用危险警示灯"}]'::jsonb, '手册规定在雾天或浓烟中使用近光灯，因为远光灯会反射并降低能见度。', 'active');

    -- Q5: What should you do if you experience a locked-wheel skid in a vehicle without ABS?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What should you do if you experience a locked-wheel skid in a vehicle without ABS?', '[{"key":"A","text":"Apply firm, steady pressure on the brakes and steer"},{"key":"B","text":"Ease off the brakes and pump them gently while steering"},{"key":"C","text":"Brake hard and turn the steering wheel sharply"},{"key":"D","text":"Release the brakes completely and steer straight"}]'::jsonb, 'For a locked-wheel skid without ABS, the handbook instructs to ease off the brakes and pump them gently while steering.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在没有防抱死制动系统（ABS）的车辆中遇到车轮抱死打滑时，应如何操作？', '[{"key":"A","text":"用力稳定地踩住刹车并转向"},{"key":"B","text":"松开刹车并轻踩点刹，同时转向"},{"key":"C","text":"猛踩刹车并急打方向盘"},{"key":"D","text":"完全松开刹车并直行"}]'::jsonb, '对于没有ABS的车辆车轮抱死打滑，手册指示松开刹车并轻踩点刹，同时转向。', 'active');

    -- Q6: What is the maximum speed you should drive in heavy rain or snow when visibility is under 100 feet?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the maximum speed you should drive in heavy rain or snow when visibility is under 100 feet?', '[{"key":"A","text":"25 mph"},{"key":"B","text":"30 mph"},{"key":"C","text":"35 mph"},{"key":"D","text":"40 mph"}]'::jsonb, 'The handbook states that in heavy rain or snow with visibility under 100 feet, you should not drive faster than 30 mph.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在能见度低于100英尺的大雨或大雪中，你应该以多快的速度行驶？', '[{"key":"A","text":"25英里/小时"},{"key":"B","text":"30英里/小时"},{"key":"C","text":"35英里/小时"},{"key":"D","text":"40英里/小时"}]'::jsonb, '手册规定，在能见度低于100英尺的大雨或大雪中，行驶速度不应超过30英里/小时。', 'active');

    -- Q7: When recovering from a locked-wheel skid in a vehicle without ABS, what should you do?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When recovering from a locked-wheel skid in a vehicle without ABS, what should you do?', '[{"key":"A","text":"Apply firm, steady pressure to the brakes and steer"},{"key":"B","text":"Ease off the accelerator and steer in the direction you want the front to go"},{"key":"C","text":"Ease off the brake and pump the brakes gently while steering"},{"key":"D","text":"Brake hard and steer away from the skid"}]'::jsonb, 'For a locked-wheel skid without ABS, the handbook says to ease off and pump the brakes gently while steering.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在没有ABS的车辆中，从车轮抱死打滑中恢复时，你应该怎么做？', '[{"key":"A","text":"用力稳定地踩刹车并转向"},{"key":"B","text":"松开油门，朝车头想要的方向转向"},{"key":"C","text":"松开刹车，轻踩刹车并同时转向"},{"key":"D","text":"猛踩刹车并朝打滑的反方向转向"}]'::jsonb, '对于没有ABS的车轮抱死打滑，手册建议松开刹车，轻踩刹车并同时转向。', 'active');

    -- Q8: When driving in fog or heavy smoke, which lights should you use?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When driving in fog or heavy smoke, which lights should you use?', '[{"key":"A","text":"High beams"},{"key":"B","text":"Low beams"},{"key":"C","text":"Parking lights only"},{"key":"D","text":"Hazard lights only"}]'::jsonb, 'The handbook specifies to use low beams in fog or heavy smoke, not high beams, and never parking lights alone.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在雾天或浓烟中行驶时，你应该使用哪种车灯？', '[{"key":"A","text":"远光灯"},{"key":"B","text":"近光灯"},{"key":"C","text":"仅使用停车灯"},{"key":"D","text":"仅使用危险警示灯"}]'::jsonb, '手册规定在雾天或浓烟中使用近光灯，而非远光灯，且绝不能单独使用停车灯。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_HAZARDS_RAILROAD_LARGE  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_HAZARDS_RAILROAD_LARGE';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: When approaching a railroad crossing with flashing red lights and lowered gates, how far must you st...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When approaching a railroad crossing with flashing red lights and lowered gates, how far must you stop from the nearest track?', '[{"key":"A","text":"At least 10 feet"},{"key":"B","text":"At least 15 feet"},{"key":"C","text":"At least 20 feet"},{"key":"D","text":"At least 25 feet"}]'::jsonb, 'The handbook states: ''Flashing red lights or lowered gates: STOP at least 15 feet from the nearest track; do not cross until the lights stop and gates rise.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当接近有红灯闪烁且栏杆放下的铁路道口时，你应在距离最近铁轨多远的地方停车？', '[{"key":"A","text":"至少10英尺"},{"key":"B","text":"至少15英尺"},{"key":"C","text":"至少20英尺"},{"key":"D","text":"至少25英尺"}]'::jsonb, '手册规定：''红灯闪烁或栏杆放下时：在距离最近铁轨至少15英尺处停车；在灯停止闪烁且栏杆升起前不得穿越。''', 'active');

    -- Q2: What is the fine for a work-zone violation in a posted double-fine construction zone when workers ar...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the fine for a work-zone violation in a posted double-fine construction zone when workers are present?', '[{"key":"A","text":"At least $500"},{"key":"B","text":"At least $1,000 and fines are doubled"},{"key":"C","text":"Exactly $1,000 with no doubling"},{"key":"D","text":"Up to $2,500"}]'::jsonb, 'The handbook says: ''Fines for work-zone violations are $1,000 or more, and fines are DOUBLED in posted double-fine / construction zones when workers are present.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在有标示双倍罚款的施工区且工人正在作业时，施工区违规的罚款是多少？', '[{"key":"A","text":"至少500美元"},{"key":"B","text":"至少1,000美元，且罚款加倍"},{"key":"C","text":"恰好1,000美元，不加倍"},{"key":"D","text":"最高2,500美元"}]'::jsonb, '手册规定：''施工区违规的罚款为1,000美元或以上，且在标示双倍罚款/施工区且工人正在作业时，罚款加倍。''', 'active');

    -- Q3: When approaching a railroad crossing with flashing red lights and lowered gates, how far from the ne...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When approaching a railroad crossing with flashing red lights and lowered gates, how far from the nearest track must you stop?', '[{"key":"A","text":"At least 15 feet"},{"key":"B","text":"At least 10 feet"},{"key":"C","text":"At least 20 feet"},{"key":"D","text":"At least 5 feet"}]'::jsonb, 'The handbook says when flashing red lights or lowered gates are present, you must stop at least 15 feet from the nearest track.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '当接近有红色闪光灯和降下的栏杆的铁路道口时，你必须在距离最近轨道多远的地方停车？', '[{"key":"A","text":"至少 15 英尺"},{"key":"B","text":"至少 10 英尺"},{"key":"C","text":"至少 20 英尺"},{"key":"D","text":"至少 5 英尺"}]'::jsonb, '手册规定，当有红色闪光灯或降下的栏杆时，你必须在距离最近轨道至少 15 英尺处停车。', 'active');

    -- Q4: What does a reflective orange-red triangle on the back of a vehicle indicate?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What does a reflective orange-red triangle on the back of a vehicle indicate?', '[{"key":"A","text":"The vehicle is a slow-moving vehicle traveling at about 25 mph or less"},{"key":"B","text":"The vehicle is a school bus preparing to stop"},{"key":"C","text":"The vehicle is a large truck with wide blind spots"},{"key":"D","text":"The vehicle is a work zone vehicle with doubled fines"}]'::jsonb, 'The handbook states that a reflective orange-red triangle marks a slow-moving vehicle, typically traveling at about 25 mph or less.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '车辆后部的反光橙红色三角形表示什么？', '[{"key":"A","text":"该车辆是慢速车辆，行驶速度约为 25 英里/小时或更低"},{"key":"B","text":"该车辆是准备停车的校车"},{"key":"C","text":"该车辆是具有大盲区的大型卡车"},{"key":"D","text":"该车辆是施工区车辆，罚款加倍"}]'::jsonb, '手册指出，反光橙红色三角形表示慢速车辆，通常行驶速度约为 25 英里/小时或更低。', 'active');

    -- Q5: If you cannot see 400 feet in both directions when approaching a railroad crossing, what is the spee...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'If you cannot see 400 feet in both directions when approaching a railroad crossing, what is the speed limit within 100 feet of the crossing?', '[{"key":"A","text":"15 mph"},{"key":"B","text":"10 mph"},{"key":"C","text":"25 mph"},{"key":"D","text":"20 mph"}]'::jsonb, 'The handbook says if you can''t see 400 feet in both directions, the speed limit near the crossing is 15 mph within 100 feet.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果接近铁路道口时无法看到两侧 400 英尺的距离，那么在道口 100 英尺内的限速是多少？', '[{"key":"A","text":"15 英里/小时"},{"key":"B","text":"10 英里/小时"},{"key":"C","text":"25 英里/小时"},{"key":"D","text":"20 英里/小时"}]'::jsonb, '手册规定，如果无法看到两侧 400 英尺的距离，道口附近 100 英尺内的限速为 15 英里/小时。', 'active');

    -- Q6: When approaching a railroad crossing with flashing red lights but no gates, how far from the nearest...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When approaching a railroad crossing with flashing red lights but no gates, how far from the nearest track must you stop?', '[{"key":"A","text":"At least 10 feet"},{"key":"B","text":"At least 15 feet"},{"key":"C","text":"At least 20 feet"},{"key":"D","text":"At least 25 feet"}]'::jsonb, 'The handbook says: ''Flashing red lights or lowered gates: STOP at least 15 feet from the nearest track.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '接近有红色闪光灯但没有栏杆的铁路道口时，你必须在距离最近轨道多远的地方停车？', '[{"key":"A","text":"至少 10 英尺"},{"key":"B","text":"至少 15 英尺"},{"key":"C","text":"至少 20 英尺"},{"key":"D","text":"至少 25 英尺"}]'::jsonb, '手册规定：''红色闪光灯或放下的栏杆：在距离最近轨道至少 15 英尺处停车。''', 'active');

    -- Q7: What is the maximum fine for failing to stop for a school bus with flashing red lights?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the maximum fine for failing to stop for a school bus with flashing red lights?', '[{"key":"A","text":"$500 and license suspension up to 6 months"},{"key":"B","text":"$1,000 and license suspension up to 1 year"},{"key":"C","text":"$750 and license suspension up to 1 year"},{"key":"D","text":"$1,500 and license suspension up to 2 years"}]'::jsonb, 'The handbook states: ''Failure to stop: fine up to $1,000 and license suspension up to 1 year.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '对于未在闪烁红灯的校车前停车的违规行为，最高罚款是多少？', '[{"key":"A","text":"500 美元罚款，驾照吊销最多 6 个月"},{"key":"B","text":"1,000 美元罚款，驾照吊销最多 1 年"},{"key":"C","text":"750 美元罚款，驾照吊销最多 1 年"},{"key":"D","text":"1,500 美元罚款，驾照吊销最多 2 年"}]'::jsonb, '手册规定：''未停车：最高罚款 1,000 美元，驾照吊销最多 1 年。''', 'active');

    -- Q8: If you cannot see 400 feet in both directions when approaching a railroad crossing, what is the spee...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'If you cannot see 400 feet in both directions when approaching a railroad crossing, what is the speed limit within 100 feet of the crossing?', '[{"key":"A","text":"10 mph"},{"key":"B","text":"15 mph"},{"key":"C","text":"20 mph"},{"key":"D","text":"25 mph"}]'::jsonb, 'The handbook says: ''If you can''t see 400 feet in both directions, the speed limit near the crossing is 15 mph within 100 feet.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果在接近铁路道口时无法看清两侧 400 英尺的距离，那么在道口 100 英尺范围内的限速是多少？', '[{"key":"A","text":"10 英里/小时"},{"key":"B","text":"15 英里/小时"},{"key":"C","text":"20 英里/小时"},{"key":"D","text":"25 英里/小时"}]'::jsonb, '手册规定：''如果无法看清两侧 400 英尺的距离，道口附近 100 英尺范围内的限速是 15 英里/小时。''', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_OCCUPANT_PROTECTION  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_OCCUPANT_PROTECTION';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: Under California law, when may a child under 2 years old legally ride in a forward-facing child rest...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Under California law, when may a child under 2 years old legally ride in a forward-facing child restraint?', '[{"key":"A","text":"When the child weighs at least 40 pounds or is at least 40 inches tall."},{"key":"B","text":"When the child is at least 1 year old and weighs at least 20 pounds."},{"key":"C","text":"When the vehicle has no rear seat and the front passenger airbag is deactivated."},{"key":"D","text":"When the child is seated in the rear seat and the seat belt fits properly."}]'::jsonb, 'The handbook states a child under 2 must ride in a rear-facing restraint unless the child weighs 40 pounds or more or is 40 inches or taller, which allows forward-facing.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据加州法律，未满2岁的儿童在什么情况下可以合法地乘坐前向式儿童约束装置？', '[{"key":"A","text":"当儿童体重至少40磅或身高至少40英寸时。"},{"key":"B","text":"当儿童至少1岁且体重至少20磅时。"},{"key":"C","text":"当车辆没有后座且前排乘客气囊已关闭时。"},{"key":"D","text":"当儿童坐在后座且安全带合适时。"}]'::jsonb, '手册规定，未满2岁的儿童必须使用后向式约束装置，除非儿童体重达到40磅或以上，或身高达到40英寸或以上，此时允许使用前向式。', 'active');

    -- Q2: Who receives the ticket if a 15-year-old passenger in the front seat is not wearing a seat belt?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Who receives the ticket if a 15-year-old passenger in the front seat is not wearing a seat belt?', '[{"key":"A","text":"The passenger receives the ticket."},{"key":"B","text":"The driver receives the ticket."},{"key":"C","text":"Both the driver and the passenger receive tickets."},{"key":"D","text":"No ticket is issued because the passenger is under 16."}]'::jsonb, 'The handbook says the driver gets the ticket for any passenger under 16 who is not properly buckled.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果前排座位上一名15岁的乘客没有系安全带，谁会被开罚单？', '[{"key":"A","text":"乘客会被开罚单。"},{"key":"B","text":"驾驶员会被开罚单。"},{"key":"C","text":"驾驶员和乘客都会被开罚单。"},{"key":"D","text":"不会开罚单，因为乘客未满16岁。"}]'::jsonb, '手册规定，对于任何未满16岁且未正确系好安全带的乘客，驾驶员会被开罚单。', 'active');

    -- Q3: What is the minimum distance you should sit from the steering wheel to reduce the risk of injury fro...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the minimum distance you should sit from the steering wheel to reduce the risk of injury from an air bag?', '[{"key":"A","text":"6 inches from the center of the steering wheel to your chest."},{"key":"B","text":"10 inches from the center of the steering wheel to your breastbone."},{"key":"C","text":"12 inches from the steering wheel to your nose."},{"key":"D","text":"15 inches from the steering wheel to your abdomen."}]'::jsonb, 'The handbook advises sitting at least 10 inches back from the airbag cover, measured from the center of the steering wheel to your breastbone.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '为了减少气囊造成伤害的风险，你应该坐在离方向盘至少多远的位置？', '[{"key":"A","text":"从方向盘中心到胸部6英寸。"},{"key":"B","text":"从方向盘中心到胸骨10英寸。"},{"key":"C","text":"从方向盘到鼻子12英寸。"},{"key":"D","text":"从方向盘到腹部15英寸。"}]'::jsonb, '手册建议坐在距离气囊盖至少10英寸的位置，测量方式是从方向盘中心到胸骨。', 'active');

    -- Q4: Under California law, it is illegal to leave a child 6 years old or younger alone in a vehicle unles...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Under California law, it is illegal to leave a child 6 years old or younger alone in a vehicle unless:', '[{"key":"A","text":"The child is supervised by a person at least 12 years old."},{"key":"B","text":"The vehicle is parked in a shaded area and the windows are cracked."},{"key":"C","text":"The child is secured in a child restraint system."},{"key":"D","text":"The engine is running and the air conditioning is on."}]'::jsonb, 'The handbook states it is illegal to leave a child 6 or younger alone unless supervised by someone at least 12 years old.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据加州法律，将6岁或以下的儿童单独留在车内是违法的，除非：', '[{"key":"A","text":"儿童由至少12岁的人照看。"},{"key":"B","text":"车辆停在阴凉处且窗户留有缝隙。"},{"key":"C","text":"儿童被固定在儿童约束装置中。"},{"key":"D","text":"发动机保持运转且空调开启。"}]'::jsonb, '手册规定，将6岁或以下儿童单独留下是违法的，除非有至少12岁的人照看。', 'active');

    -- Q5: Under California law, who receives the ticket if a passenger under 16 years old is not wearing a sea...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Under California law, who receives the ticket if a passenger under 16 years old is not wearing a seat belt?', '[{"key":"A","text":"The passenger under 16"},{"key":"B","text":"The driver of the vehicle"},{"key":"C","text":"The vehicle owner"},{"key":"D","text":"The passenger''s parent or guardian"}]'::jsonb, 'The handbook states: ''The driver gets the ticket for any passenger under 16 who is not properly buckled.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据加州法律，如果一名未满16岁的乘客没有系安全带，谁会被开罚单？', '[{"key":"A","text":"未满16岁的乘客"},{"key":"B","text":"车辆驾驶员"},{"key":"C","text":"车辆车主"},{"key":"D","text":"乘客的父母或监护人"}]'::jsonb, '手册指出：''如果任何未满16岁的乘客没有正确系好安全带，驾驶员将收到罚单。''', 'active');

    -- Q6: When may a child under 2 years old legally ride in a forward-facing child restraint instead of a rea...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'When may a child under 2 years old legally ride in a forward-facing child restraint instead of a rear-facing one?', '[{"key":"A","text":"When the child weighs 30 pounds or more"},{"key":"B","text":"When the child is at least 3 feet 4 inches tall or weighs 40 pounds or more"},{"key":"C","text":"When the child is at least 2 feet 9 inches tall"},{"key":"D","text":"When the vehicle has no rear seat"}]'::jsonb, 'The handbook says a child under 2 must use a rear-facing restraint unless the child weighs 40 pounds or more or is 3 feet 4 inches (40 inches) or taller.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '在什么情况下，未满2岁的儿童可以合法地使用前向式儿童约束装置而非后向式？', '[{"key":"A","text":"当儿童体重达到30磅或以上时"},{"key":"B","text":"当儿童身高至少3英尺4英寸或体重达到40磅或以上时"},{"key":"C","text":"当儿童身高至少2英尺9英寸时"},{"key":"D","text":"当车辆没有后排座椅时"}]'::jsonb, '手册规定，未满2岁的儿童必须使用后向式约束装置，除非儿童体重达到40磅或以上，或身高达到3英尺4英寸（40英寸）或以上。', 'active');

    -- Q7: Where must a rear-facing child restraint never be placed?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Where must a rear-facing child restraint never be placed?', '[{"key":"A","text":"In the rear seat behind the driver"},{"key":"B","text":"In the front seat with an active airbag"},{"key":"C","text":"In the rear seat behind the passenger"},{"key":"D","text":"In the middle of the rear seat"}]'::jsonb, 'The handbook explicitly states: ''A rear-facing seat must never be placed in front of an active airbag.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '后向式儿童约束装置绝对不可以放置在哪个位置？', '[{"key":"A","text":"驾驶员后方的后排座椅"},{"key":"B","text":"带有激活安全气囊的前排座椅"},{"key":"C","text":"乘客后方的后排座椅"},{"key":"D","text":"后排座椅中间位置"}]'::jsonb, '手册明确说明：''后向式座椅绝不能放置在激活的安全气囊前面。''', 'active');

    -- Q8: Under California law, when may a child under 2 years old legally ride in a forward-facing child rest...
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'Under California law, when may a child under 2 years old legally ride in a forward-facing child restraint instead of a rear-facing one?', '[{"key":"A","text":"When the child weighs at least 30 pounds."},{"key":"B","text":"When the child weighs 40 pounds or more or is 40 inches or taller."},{"key":"C","text":"When the child is at least 1 year old and weighs 20 pounds."},{"key":"D","text":"When the child is at least 18 months old and the rear-facing seat no longer fits."}]'::jsonb, 'The handbook states that a child under 2 must ride in a rear-facing child restraint unless the child weighs 40 pounds or more or is 3 feet 4 inches (40 inches) or taller. Only then may a forward-facing seat be used.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '根据加州法律，未满2岁的儿童在什么情况下可以合法使用前向式儿童约束装置而非后向式？', '[{"key":"A","text":"当儿童体重至少30磅时。"},{"key":"B","text":"当儿童体重达到40磅或以上，或身高达到40英寸或以上时。"},{"key":"C","text":"当儿童至少1岁且体重20磅时。"},{"key":"D","text":"当儿童至少18个月大且后向式座椅不再适用时。"}]'::jsonb, '手册规定，未满2岁的儿童必须使用后向式儿童约束装置，除非儿童体重达到40磅或以上，或身高达到40英寸或以上。只有在这种情况下才可以使用前向式座椅。', 'active');

END $$;

-- ============================================================
-- Sub-topic: CAC_EMERGENCIES_DISTRACTION  (8 questions)
-- ============================================================
DO $$
DECLARE
    st_id      BIGINT;
    parent_id  BIGINT;
    v_exam_id  BIGINT;
    new_q_id   BIGINT;
BEGIN
    SELECT id, parent_topic_id INTO st_id, parent_id FROM sub_topics WHERE code = 'CAC_EMERGENCIES_DISTRACTION';
    SELECT t.exam_id INTO v_exam_id FROM topics t WHERE t.id = parent_id;

    -- Q1: What is the correct action to take if you experience a tire blowout while driving?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the correct action to take if you experience a tire blowout while driving?', '[{"key":"A","text":"Brake hard immediately to stop the vehicle as quickly as possible."},{"key":"B","text":"Hold the steering wheel firmly, keep the vehicle straight, gradually ease off the accelerator, and slow down before pulling off the road."},{"key":"C","text":"Steer sharply to the shoulder to get off the road as soon as possible."},{"key":"D","text":"Shift into neutral and coast to a stop without using the brakes."}]'::jsonb, 'The handbook states that during a tire blowout you should hold the steering wheel firmly, keep the vehicle going straight, gradually ease off the accelerator, steer smoothly, slow down, and pull off the road when safe. Braking hard or jerking the wheel is not recommended.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果在驾驶过程中遇到轮胎爆胎，正确的做法是什么？', '[{"key":"A","text":"立即猛踩刹车，尽快停车。"},{"key":"B","text":"紧握方向盘，保持车辆直行，逐渐松开油门，减速后安全驶离道路。"},{"key":"C","text":"急转向路肩，尽快驶离道路。"},{"key":"D","text":"挂空挡滑行至停止，不使用刹车。"}]'::jsonb, '手册指出，爆胎时应紧握方向盘，保持车辆直行，逐渐松开油门，平稳转向，减速并在安全时驶离道路。不建议猛踩刹车或急转方向盘。', 'active');

    -- Q2: What should a driver under 18 years old do regarding cell phone use while driving?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'C', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What should a driver under 18 years old do regarding cell phone use while driving?', '[{"key":"A","text":"They may use a hands-free device to make calls."},{"key":"B","text":"They may use a cell phone only if it is mounted and used with a single tap or swipe."},{"key":"C","text":"They may not use any cell phone or wireless device while driving, except for emergency calls."},{"key":"D","text":"They may text while stopped at a red light."}]'::jsonb, 'According to the handbook, drivers under 18 may not use any cell phone or wireless device while driving, even hands-free, except to call for an emergency. This is stricter than the rule for adults.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '未满18岁的驾驶员在驾驶时使用手机应遵守什么规定？', '[{"key":"A","text":"可以使用免提设备拨打电话。"},{"key":"B","text":"只有在手机固定且通过单次点击或滑动操作时才能使用。"},{"key":"C","text":"驾驶时不得使用任何手机或无线设备，紧急呼叫除外。"},{"key":"D","text":"在红灯停车时可以发短信。"}]'::jsonb, '根据手册，未满18岁的驾驶员在驾驶时不得使用任何手机或无线设备，即使是免提设备也不行，紧急呼叫除外。这比成年人的规定更严格。', 'active');

    -- Q3: If your vehicle becomes disabled on the freeway, what is the recommended action?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'If your vehicle becomes disabled on the freeway, what is the recommended action?', '[{"key":"A","text":"Stay in the vehicle with your seat belt on, turn on emergency flashers, and call 511 for help."},{"key":"B","text":"Exit the vehicle and stand behind it to warn other drivers."},{"key":"C","text":"Push the vehicle to the nearest exit before calling for help."},{"key":"D","text":"Turn off the engine and wait for a passing motorist to assist."}]'::jsonb, 'The handbook instructs that if your vehicle is disabled on the freeway, you should pull onto the right shoulder, turn on emergency flashers, stay in the vehicle with your seat belt on, and call 511 for help.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果车辆在高速公路上发生故障，建议采取什么行动？', '[{"key":"A","text":"留在车内系好安全带，打开应急闪光灯，并拨打511求助。"},{"key":"B","text":"下车站在车辆后方以警示其他驾驶员。"},{"key":"C","text":"将车辆推到最近的出口后再求助。"},{"key":"D","text":"关闭发动机，等待路过的司机帮助。"}]'::jsonb, '手册指示，如果车辆在高速公路上发生故障，应驶入右侧路肩，打开应急闪光灯，留在车内系好安全带，并拨打511求助。', 'active');

    -- Q4: What should you do if your accelerator becomes stuck while driving?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What should you do if your accelerator becomes stuck while driving?', '[{"key":"A","text":"Pump the accelerator pedal repeatedly to try to free it."},{"key":"B","text":"Shift to neutral, brake, and steer off the road."},{"key":"C","text":"Turn off the ignition immediately to stop the engine."},{"key":"D","text":"Downshift to a lower gear and apply the parking brake gradually."}]'::jsonb, 'The handbook says that if the accelerator sticks, you should shift to neutral, brake, and steer off the road. This safely disengages the engine power and allows you to stop.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果驾驶时油门卡住，你应该怎么做？', '[{"key":"A","text":"反复踩踏油门踏板试图松开它。"},{"key":"B","text":"挂空挡，刹车，然后驶离道路。"},{"key":"C","text":"立即关闭点火开关以停止发动机。"},{"key":"D","text":"降档至较低档位并逐渐使用驻车制动。"}]'::jsonb, '手册指出，如果油门卡住，应挂空挡，刹车，然后驶离道路。这样可以安全地切断发动机动力并停车。', 'active');

    -- Q5: What is the correct action to take when experiencing a tire blowout while driving?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the correct action to take when experiencing a tire blowout while driving?', '[{"key":"A","text":"Brake hard immediately to stop the vehicle as quickly as possible."},{"key":"B","text":"Hold the steering wheel firmly, keep the vehicle straight, gradually ease off the accelerator, and slow down smoothly before pulling off the road."},{"key":"C","text":"Steer sharply to the shoulder to get off the road as soon as possible."},{"key":"D","text":"Accelerate slightly to maintain speed and then steer to the shoulder."}]'::jsonb, 'The handbook states: ''Hold the steering wheel firmly with both hands. Keep the vehicle going straight and your speed steady, then gradually ease off the accelerator. Steer smoothly, slow down, and pull off the road when safe. Do not brake hard.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '驾驶时遇到轮胎爆裂，正确的做法是什么？', '[{"key":"A","text":"立即猛踩刹车，尽快停车。"},{"key":"B","text":"紧握方向盘，保持车辆直行，逐渐松开油门，平稳减速后驶离道路。"},{"key":"C","text":"急转向路肩，尽快驶离道路。"},{"key":"D","text":"稍微加速以保持速度，然后转向路肩。"}]'::jsonb, '手册指出：''用双手紧握方向盘。保持车辆直行和速度稳定，然后逐渐松开油门。平稳转向，减速，并在安全时驶离道路。不要猛踩刹车。''', 'active');

    -- Q6: If your vehicle becomes disabled on a freeway, what should you do according to the handbook?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'A', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'If your vehicle becomes disabled on a freeway, what should you do according to the handbook?', '[{"key":"A","text":"Stay in the vehicle with your seat belt on, turn on emergency flashers, and call 511 for help."},{"key":"B","text":"Exit the vehicle and stand behind it to warn other drivers."},{"key":"C","text":"Push the vehicle to the nearest exit before calling for help."},{"key":"D","text":"Turn off the engine and wait for a passing motorist to assist you."}]'::jsonb, 'The handbook says: ''Pull onto the right shoulder, away from traffic. Turn on your emergency flashers. Stay in the vehicle with your seat belt on; call 511 for help.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果车辆在高速公路上发生故障，根据手册你应该怎么做？', '[{"key":"A","text":"留在车内系好安全带，打开应急闪光灯，拨打511求助。"},{"key":"B","text":"下车站在车辆后方以警示其他驾驶员。"},{"key":"C","text":"将车辆推到最近的出口再求助。"},{"key":"D","text":"关闭发动机，等待过往司机帮助。"}]'::jsonb, '手册指出：''驶入右侧路肩，远离车流。打开应急闪光灯。留在车内系好安全带；拨打511求助。''', 'active');

    -- Q7: What is the rule for drivers under 18 regarding cell phone use while driving?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the rule for drivers under 18 regarding cell phone use while driving?', '[{"key":"A","text":"They may use a hands-free device as long as it is mounted."},{"key":"B","text":"They may not use any cell phone or wireless device while driving, even hands-free, except for emergency calls."},{"key":"C","text":"They may use a cell phone only if it is for navigation purposes."},{"key":"D","text":"They may use a cell phone if they pull over to the side of the road."}]'::jsonb, 'The handbook states: ''Drivers UNDER 18 may NOT use any cell phone or wireless device while driving, even hands-free — except to call for an emergency.''', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '对于未满18岁的驾驶员，开车时使用手机的规定是什么？', '[{"key":"A","text":"只要设备已安装，他们可以使用免提设备。"},{"key":"B","text":"他们开车时不得使用任何手机或无线设备，即使是免提的，紧急呼叫除外。"},{"key":"C","text":"他们仅可在导航时使用手机。"},{"key":"D","text":"如果他们靠边停车，可以使用手机。"}]'::jsonb, '手册指出：''未满18岁的驾驶员在开车时不得使用任何手机或无线设备，即使是免提的——紧急呼叫除外。''', 'active');

    -- Q8: What is the correct action to take if your vehicle has a tire blowout while driving?
    INSERT INTO questions (primary_topic_id, sub_topic_id, exam_id, correct_choice_key, status, allow_in_free_trial, allow_in_practice, allow_in_mock_exam, allow_in_review)
    VALUES (parent_id, st_id, v_exam_id, 'B', 'active', false, true, true, true)
    RETURNING id INTO new_q_id;
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'en', 'What is the correct action to take if your vehicle has a tire blowout while driving?', '[{"key":"A","text":"Brake hard immediately to stop the vehicle as quickly as possible."},{"key":"B","text":"Hold the steering wheel firmly, keep the vehicle straight, ease off the accelerator, and gradually slow down."},{"key":"C","text":"Turn the steering wheel sharply to the side of the blowout to stabilize the vehicle."},{"key":"D","text":"Accelerate slightly to regain control before pulling over."}]'::jsonb, 'According to the handbook, during a tire blowout you should hold the steering wheel firmly, keep the vehicle straight, ease off the accelerator, and slow down gradually without braking hard.', 'active');
    INSERT INTO question_variants (question_id, language_code, stem_text, choices_payload, explanation_text, status)
    VALUES (new_q_id, 'zh', '如果在驾驶过程中车辆爆胎，正确的做法是什么？', '[{"key":"A","text":"立即猛踩刹车，尽快停车。"},{"key":"B","text":"紧握方向盘，保持车辆直行，松开油门，逐渐减速。"},{"key":"C","text":"将方向盘猛转向爆胎一侧以稳定车辆。"},{"key":"D","text":"稍微加速以重新控制车辆，然后再靠边停车。"}]'::jsonb, '根据手册，爆胎时应紧握方向盘，保持车辆直行，松开油门，逐渐减速，切勿猛踩刹车。', 'active');

END $$;
