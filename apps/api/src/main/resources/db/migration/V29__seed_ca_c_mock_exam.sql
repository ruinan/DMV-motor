-- V29: CA-C mock exam template (CA_C_30Q) — a 30-question mock for the
-- California Class C (car) exam, drawn dynamically from the V28 CA-C bank.
--
-- Unlike the M1 mock (CA_M1_30Q, hardcoded question ids), CA-C questions get
-- their ids from V28 at apply time, so the mock is built by SELECTing ~2
-- questions per sub-topic across all 16 (16 x 2 = 32, trimmed to 30). The mock
-- is scoped to the CA-C exam_id; time limit defaults to 1800s (V21, 60s/question).

DO $$
DECLARE
    ca_c    BIGINT;
    mock_id BIGINT;
    n       INT;
BEGIN
    SELECT id INTO ca_c FROM exams WHERE state_code = 'CA' AND license_class = 'C';

    INSERT INTO mock_exams (code, question_count, status, exam_id)
    VALUES ('CA_C_30Q', 30, 'active', ca_c)
    RETURNING id INTO mock_id;

    -- Up to 2 questions per CA-C sub-topic, ordered for even coverage, capped at 30.
    INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order)
    WITH picked AS (
        SELECT id, sub_topic_id,
               ROW_NUMBER() OVER (PARTITION BY sub_topic_id ORDER BY id) AS rn
        FROM questions
        WHERE exam_id = ca_c AND status = 'active'
    )
    SELECT mock_id, id, ROW_NUMBER() OVER (ORDER BY sub_topic_id, id)
    FROM picked
    WHERE rn <= 2
    ORDER BY sub_topic_id, id
    LIMIT 30;

    -- Keep question_count consistent with however many we actually linked.
    SELECT COUNT(*) INTO n FROM mock_exam_questions WHERE mock_exam_id = mock_id;
    UPDATE mock_exams SET question_count = n WHERE id = mock_id;
END $$;
