-- V30: enable free-trial practice for CA-C (California Class C / car).
--
-- The generated CA-C bank (V28) left allow_in_free_trial = false on every
-- question, so anonymous / no-pass free-trial practice for CA-C returned
-- "No practice questions available" in BOTH languages (en + zh variants both
-- exist; the gate is the free-trial flag, not language). CA-M1 ships 34
-- free-trial questions, so only the second exam was affected.
--
-- Flag 2 questions per sub-topic (16 sub-topics -> ~32) so the free taster
-- (cap 15) draws from a balanced, varied pool. Both language variants come
-- along automatically since the flag lives on questions, not variants.
WITH ca_c AS (
    SELECT id FROM exams WHERE state_code = 'CA' AND license_class = 'C'
),
ranked AS (
    SELECT q.id,
           row_number() OVER (PARTITION BY q.sub_topic_id ORDER BY q.id) AS rn
    FROM questions q
    WHERE q.exam_id = (SELECT id FROM ca_c)
      AND q.status = 'active'
)
UPDATE questions
   SET allow_in_free_trial = true
 WHERE id IN (SELECT id FROM ranked WHERE rn <= 2);
