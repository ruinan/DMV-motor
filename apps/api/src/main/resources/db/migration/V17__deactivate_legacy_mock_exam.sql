-- V2 seeded an early M1_STANDARD_V1 template (46 questions). V10 later added
-- CA_M1_30Q (the real 30-question mock spec), but the V2 row was left active.
-- MockExamRepository.findActiveMockExamId picks by id ASC and was therefore
-- always returning the 46-question template, breaking the mock-attempt UX
-- (the test caps at 30, the screen showed "of 46").
--
-- Mark the legacy template inactive so CA_M1_30Q is the only active row.
-- Existing in-flight attempts against the legacy id stay valid because
-- attempts join by mock_exam_id (not by status), so historical data is
-- preserved.

UPDATE mock_exams SET status = 'inactive', updated_at = CURRENT_TIMESTAMP
 WHERE code = 'M1_STANDARD_V1';
