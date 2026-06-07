-- V31: mock exams match the REAL DMV written-test question counts + pass marks (B14).
--
-- California Class C (car): 46 questions, pass 38 (~83%) — the under-18 tier, our
-- primary audience. California Motorcycle (M1): 30 questions, pass 24 (80%).
--
-- Per-exam pass thresholds also drive the auto-terminate cap, which is
-- ceil(total × (1 - threshold)) in MockScoringPolicy — pass-aligned, so it adjusts
-- automatically (CA-C: ceil(46×0.17)=8 wrong tolerated; M1: ceil(30×0.20)=6).
-- Rebuild the CA-C mock 30 -> 46 (3 per sub-topic, capped 46) from the V28 bank.

DO $$
DECLARE
    ca_c    BIGINT;
    mock_id BIGINT;
    n       INT;
BEGIN
    SELECT id INTO ca_c FROM exams WHERE state_code = 'CA' AND license_class = 'C';

    -- Real pass marks.
    UPDATE exams SET pass_threshold_percent = 83 WHERE id = ca_c;
    UPDATE exams SET pass_threshold_percent = 80
     WHERE state_code = 'CA' AND license_class = 'M1';

    -- Rebuild the CA-C mock at 46 questions (was CA_C_30Q / 30).
    SELECT id INTO mock_id FROM mock_exams WHERE code = 'CA_C_30Q' AND exam_id = ca_c;
    IF mock_id IS NOT NULL THEN
        DELETE FROM mock_exam_questions WHERE mock_exam_id = mock_id;

        INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order)
        WITH picked AS (
            SELECT id, sub_topic_id,
                   ROW_NUMBER() OVER (PARTITION BY sub_topic_id ORDER BY id) AS rn
            FROM questions
            WHERE exam_id = ca_c AND status = 'active'
        )
        SELECT mock_id, id, ROW_NUMBER() OVER (ORDER BY sub_topic_id, id)
        FROM picked
        WHERE rn <= 3
        ORDER BY sub_topic_id, id
        LIMIT 46;

        SELECT COUNT(*) INTO n FROM mock_exam_questions WHERE mock_exam_id = mock_id;
        UPDATE mock_exams
           SET code = 'CA_C_46Q', question_count = n, time_limit_seconds = n * 60
         WHERE id = mock_id;
    END IF;
END $$;
