package com.dmvmotor.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Shared test data builder for integration tests.
 *
 * Design rules:
 * - Methods provide sensible defaults; callers only specify what the test cares about.
 * - Methods return DB-generated IDs for subsequent reference.
 * - No raw SQL in test classes — all data setup goes through here.
 */
@Component
public class TestFixtures {

    private final JdbcTemplate jdbc;

    public TestFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Subquery resolving the V26-seeded CA-M1 exam. The {@code exams} table is
     * deliberately NOT truncated by {@link #truncateAll()} (it's reference data
     * seeded by migration), so this always resolves — the default exam every
     * fixture row belongs to unless an explicit exam is given.
     */
    private static final String CA_M1_EXAM =
            "(SELECT id FROM exams WHERE state_code='CA' AND license_class='M1')";

    // ---------------------------------------------------------------
    // Cleanup
    // ---------------------------------------------------------------

    public void truncateAll() {
        jdbc.execute("""
                TRUNCATE
                    code_redemptions,
                    redemption_codes,
                    progress_backups,
                    reminder_tasks,
                    mock_review_plans,
                    ai_deep_dive_log,
                    ai_explanations,
                    mistake_records,
                    practice_attempts,
                    practice_sessions,
                    review_task_questions,
                    review_tasks,
                    review_packs,
                    mock_attempts,
                    mock_exam_questions,
                    mock_exams,
                    question_related_topics,
                    question_variants,
                    questions,
                    sub_topics,
                    topics,
                    access_passes,
                    users
                RESTART IDENTITY CASCADE
                """);
        // The exams catalog (V26) is reference data seeded by migration, not
        // truncated above (its sequence must keep the seeded CA-M1 row). Restore
        // its baseline so a test that adds an exam or flips a status can't leak
        // into the next test — all exam_id FK holders were just truncated, so
        // there are no inbound references to block this.
        jdbc.update("DELETE FROM exams WHERE NOT (state_code = 'CA' AND license_class = 'M1')");
        jdbc.update("UPDATE exams SET status = 'active' WHERE state_code = 'CA' AND license_class = 'M1'");
    }

    // ---------------------------------------------------------------
    // Exams (V26)
    // ---------------------------------------------------------------

    /** The V26-seeded CA-M1 exam id (the default every other fixture belongs to). */
    public Long defaultExamId() {
        return jdbc.queryForObject(
                "SELECT id FROM exams WHERE state_code='CA' AND license_class='M1'", Long.class);
    }

    /** Seed an additional active exam (for cross-exam isolation / threshold tests). */
    public Long insertExam(String stateCode, String licenseClass,
                           String nameEn, String nameZh, int passThresholdPercent) {
        return jdbc.queryForObject("""
                INSERT INTO exams
                    (state_code, license_class, name_en, name_zh, pass_threshold_percent, status, sort_order)
                VALUES (?, ?, ?, ?, ?, 'active', 10)
                RETURNING id
                """, Long.class, stateCode, licenseClass, nameEn, nameZh, passThresholdPercent);
    }

    public void setUserCurrentExam(Long userId, Long examId) {
        jdbc.update("UPDATE users SET current_exam_id = ? WHERE id = ?", examId, userId);
    }

    public void setExamStatus(Long examId, String status) {
        jdbc.update("UPDATE exams SET status = ? WHERE id = ?", status, examId);
    }

    // ---------------------------------------------------------------
    // Redemption codes (V37)
    // ---------------------------------------------------------------

    /** Seed an active redemption code for {@code examId} (null = current-exam)
     *  with the given redemption cap. Returns the code id. */
    public Long insertRedemptionCode(String code, Long examId, int maxRedemptions) {
        return jdbc.queryForObject("""
                INSERT INTO redemption_codes
                    (code, exam_id, duration_days, mock_quota, max_redemptions, status)
                VALUES (?, ?, 30, 5, ?, 'active')
                RETURNING id
                """, Long.class, code, examId, maxRedemptions);
    }

    // ---------------------------------------------------------------
    // Topics
    // ---------------------------------------------------------------

    public Long insertTopic(String code) {
        return insertTopic(code, code + " EN", code + " ZH", false, 0);
    }

    public Long insertTopic(String code, String nameEn, String nameZh,
                             boolean isKeyTopic, int sortOrder) {
        return insertTopicForExam(defaultExamId(), code, nameEn, nameZh, isKeyTopic, sortOrder);
    }

    /** Topic belonging to a specific exam (cross-exam isolation tests). */
    public Long insertTopicForExam(Long examId, String code, String nameEn, String nameZh,
                                   boolean isKeyTopic, int sortOrder) {
        Long topicId = jdbc.queryForObject("""
                INSERT INTO topics (code, name_en, name_zh, is_key_topic, sort_order, exam_id)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, code, nameEn, nameZh, isKeyTopic, sortOrder, examId);
        // Auto-seed one default sub-topic so insertQuestion's sub_topic_id
        // resolver (added when V15 set the column NOT NULL) always finds a row.
        // Production data uses the V14 seed; tests build minimal topics ad hoc.
        jdbc.update("""
                INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, sort_order)
                VALUES (?, ?, ?, ?, 0)
                """, topicId, code + "_ST", nameEn + " sub", nameZh + " 子");
        return topicId;
    }

    /** Topic with NO sub-topic seeded — a topic with no sub-topics can never be
     *  "mastered" (the donut treats childless topics as not mastered). */
    public Long insertTopicWithoutSubTopic(String code) {
        return jdbc.queryForObject("""
                INSERT INTO topics (code, name_en, name_zh, is_key_topic, sort_order, exam_id)
                VALUES (?, ?, ?, false, 99, """ + CA_M1_EXAM + """
                )
                RETURNING id
                """, Long.class, code, code + " EN", code + " ZH");
    }

    public Long insertChildTopic(String code, Long parentTopicId) {
        Long topicId = jdbc.queryForObject("""
                INSERT INTO topics (code, name_en, name_zh, is_key_topic, sort_order, parent_topic_id, exam_id)
                VALUES (?, ?, ?, false, 0, ?, (SELECT exam_id FROM topics WHERE id = ?))
                RETURNING id
                """, Long.class, code, code + " EN", code + " ZH", parentTopicId, parentTopicId);
        jdbc.update("""
                INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, sort_order)
                VALUES (?, ?, ?, ?, 0)
                """, topicId, code + "_ST", code + " EN sub", code + " ZH 子");
        return topicId;
    }

    // ---------------------------------------------------------------
    // Questions
    // ---------------------------------------------------------------

    public Long insertKeyCoverageQuestion(Long topicId, String correctChoiceKey) {
        return jdbc.queryForObject("""
                INSERT INTO questions
                    (primary_topic_id, correct_choice_key, status, allow_in_free_trial, is_key_coverage, sub_topic_id, exam_id)
                VALUES (?, ?, 'active', true, true,
                    (SELECT id FROM sub_topics WHERE parent_topic_id = ? ORDER BY sort_order LIMIT 1),
                    (SELECT exam_id FROM topics WHERE id = ?))
                RETURNING id
                """, Long.class, topicId, correctChoiceKey, topicId, topicId);
    }

    public Long insertQuestion(Long topicId, String correctChoiceKey) {
        return jdbc.queryForObject("""
                INSERT INTO questions (primary_topic_id, correct_choice_key, status, allow_in_free_trial, sub_topic_id, exam_id)
                VALUES (?, ?, 'active', true,
                    (SELECT id FROM sub_topics WHERE parent_topic_id = ? ORDER BY sort_order LIMIT 1),
                    (SELECT exam_id FROM topics WHERE id = ?))
                RETURNING id
                """, Long.class, topicId, correctChoiceKey, topicId, topicId);
    }

    /** Active question excluded from the free-trial pool (allow_in_free_trial=false). */
    public Long insertPaidOnlyQuestion(Long topicId, String correctChoiceKey) {
        return jdbc.queryForObject("""
                INSERT INTO questions (primary_topic_id, correct_choice_key, status, allow_in_free_trial, sub_topic_id, exam_id)
                VALUES (?, ?, 'active', false,
                    (SELECT id FROM sub_topics WHERE parent_topic_id = ? ORDER BY sort_order LIMIT 1),
                    (SELECT exam_id FROM topics WHERE id = ?))
                RETURNING id
                """, Long.class, topicId, correctChoiceKey, topicId, topicId);
    }

    /** Question with status='inactive' — should never appear in any practice pool. */
    public Long insertInactiveQuestion(Long topicId, String correctChoiceKey) {
        return jdbc.queryForObject("""
                INSERT INTO questions (primary_topic_id, correct_choice_key, status, allow_in_free_trial, sub_topic_id, exam_id)
                VALUES (?, ?, 'inactive', true,
                    (SELECT id FROM sub_topics WHERE parent_topic_id = ? ORDER BY sort_order LIMIT 1),
                    (SELECT exam_id FROM topics WHERE id = ?))
                RETURNING id
                """, Long.class, topicId, correctChoiceKey, topicId, topicId);
    }

    public void insertEnVariant(Long questionId, String stem, String explanation) {
        insertVariant(questionId, "en", stem,
                "[{\"key\":\"A\",\"text\":\"Option A\"},{\"key\":\"B\",\"text\":\"Option B\"},{\"key\":\"C\",\"text\":\"Option C\"}]",
                explanation);
    }

    /** Same as {@link #insertEnVariant} but returns the generated variant id for FK references. */
    public Long insertEnVariantReturningId(Long questionId, String stem, String explanation) {
        return jdbc.queryForObject("""
                INSERT INTO question_variants
                    (question_id, language_code, stem_text, choices_payload, explanation_text)
                VALUES (?, 'en', ?, ?::jsonb, ?)
                RETURNING id
                """, Long.class, questionId, stem,
                "[{\"key\":\"A\",\"text\":\"Option A\"},{\"key\":\"B\",\"text\":\"Option B\"},{\"key\":\"C\",\"text\":\"Option C\"}]",
                explanation);
    }

    public void insertZhVariant(Long questionId, String stem, String explanation) {
        insertVariant(questionId, "zh", stem,
                "[{\"key\":\"A\",\"text\":\"选项A\"},{\"key\":\"B\",\"text\":\"选项B\"},{\"key\":\"C\",\"text\":\"选项C\"}]",
                explanation);
    }

    public void insertVariant(Long questionId, String languageCode,
                               String stem, String choicesJson, String explanation) {
        jdbc.update("""
                INSERT INTO question_variants
                    (question_id, language_code, stem_text, choices_payload, explanation_text)
                VALUES (?, ?, ?, ?::jsonb, ?)
                """, questionId, languageCode, stem, choicesJson, explanation);
    }

    public Long insertVariantReturningId(Long questionId, String languageCode,
                                          String stem, String choicesJson, String explanation) {
        return jdbc.queryForObject("""
                INSERT INTO question_variants
                    (question_id, language_code, stem_text, choices_payload, explanation_text)
                VALUES (?, ?, ?, ?::jsonb, ?)
                RETURNING id
                """, Long.class, questionId, languageCode, stem, choicesJson, explanation);
    }

    // ---------------------------------------------------------------
    // Users
    // ---------------------------------------------------------------

    /**
     * Inserts a user and stamps {@code firebase_uid = "test-<id>"} so that
     * {@code Authorization: Bearer <id>} flows through {@code StubFirebaseVerifier}
     * (numeric → "test-<id>") and resolves back to the same row — preserving
     * the pre-Firebase test-helper contract where callers pass raw numeric ids.
     */
    public Long insertUser(String email) {
        Long id = jdbc.queryForObject("""
                INSERT INTO users (email, language_preference)
                VALUES (?, 'en')
                RETURNING id
                """, Long.class, email);
        stampFirebaseUid(id);
        return id;
    }

    public void incrementUserResetCount(Long userId) {
        jdbc.update("UPDATE users SET reset_count = reset_count + 1 WHERE id = ?", userId);
    }

    public Long insertUserWithoutEmail() {
        Long id = jdbc.queryForObject("""
                INSERT INTO users (language_preference)
                VALUES ('en')
                RETURNING id
                """, Long.class);
        stampFirebaseUid(id);
        return id;
    }

    private void stampFirebaseUid(Long id) {
        jdbc.update("UPDATE users SET firebase_uid = ? WHERE id = ?", "test-" + id, id);
    }

    // ---------------------------------------------------------------
    // Mock Attempts
    // ---------------------------------------------------------------

    public Long insertMockAttemptWithScore(Long userId, Long mockExamId, int scorePercent) {
        return insertMockAttemptWithScore(userId, mockExamId, scorePercent, 0);
    }

    public Long insertMockAttemptWithScore(Long userId, Long mockExamId,
                                            int scorePercent, int learningCycle) {
        return jdbc.queryForObject("""
                INSERT INTO mock_attempts
                    (user_id, mock_exam_id, status, score_percent, correct_count,
                     wrong_count, answered_count, quota_consumed, learning_cycle,
                     submitted_at, exam_id)
                VALUES (?, ?, 'submitted', ?, ?, 0, ?, true, ?, CURRENT_TIMESTAMP,
                        (SELECT exam_id FROM mock_exams WHERE id = ?))
                RETURNING id
                """, Long.class, userId, mockExamId, scorePercent,
                scorePercent / 10, scorePercent / 10, learningCycle, mockExamId);
    }

    public Long insertInProgressMockAttempt(Long userId, Long mockExamId) {
        return jdbc.queryForObject("""
                INSERT INTO mock_attempts
                    (user_id, mock_exam_id, status, answered_count, quota_consumed, learning_cycle, exam_id)
                VALUES (?, ?, 'in_progress', 0, true, 0,
                        (SELECT exam_id FROM mock_exams WHERE id = ?))
                RETURNING id
                """, Long.class, userId, mockExamId, mockExamId);
    }

    /** In-progress attempt whose clock started {@code ageSeconds} ago — used to
     *  exercise mock-timer expiry without real waiting. */
    public Long insertInProgressMockAttemptStartedSecondsAgo(Long userId, Long mockExamId,
                                                             int ageSeconds) {
        return jdbc.queryForObject("""
                INSERT INTO mock_attempts
                    (user_id, mock_exam_id, status, answered_count, quota_consumed,
                     learning_cycle, started_at, exam_id)
                VALUES (?, ?, 'in_progress', 0, true, 0,
                        CURRENT_TIMESTAMP - make_interval(secs => ?),
                        (SELECT exam_id FROM mock_exams WHERE id = ?))
                RETURNING id
                """, Long.class, userId, mockExamId, ageSeconds, mockExamId);
    }

    public void insertMockAttemptResult(Long attemptId, Long questionId, Long variantId,
                                         String selectedKey, boolean isCorrect) {
        jdbc.update("""
                INSERT INTO mock_attempt_results
                    (mock_attempt_id, question_id, question_variant_id,
                     selected_choice_key, is_correct)
                VALUES (?, ?, ?, ?, ?)
                """, attemptId, questionId, variantId, selectedKey, isCorrect);
    }

    // ---------------------------------------------------------------
    // Mistake Records
    // ---------------------------------------------------------------

    public Long insertMistakeRecord(Long userId, Long questionId, Long topicId,
                                     int wrongCount, String source) {
        return jdbc.queryForObject("""
                INSERT INTO mistake_records
                    (user_id, question_id, primary_topic_id, wrong_count, last_entry_source)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, userId, questionId, topicId, wrongCount, source);
    }

    // ---------------------------------------------------------------
    // Mock Exams
    // ---------------------------------------------------------------

    public Long insertMockExam(String code, int questionCount) {
        return jdbc.queryForObject("""
                INSERT INTO mock_exams (code, question_count, status, exam_id)
                VALUES (?, ?, 'active', """ + CA_M1_EXAM + """
                )
                RETURNING id
                """, Long.class, code, questionCount);
    }

    /** Mock-exam template belonging to a specific exam (cross-exam isolation tests). */
    public Long insertMockExamForExam(String code, int questionCount, Long examId) {
        return jdbc.queryForObject("""
                INSERT INTO mock_exams (code, question_count, status, exam_id)
                VALUES (?, ?, 'active', ?)
                RETURNING id
                """, Long.class, code, questionCount, examId);
    }

    public void insertMockExamQuestion(Long mockExamId, Long questionId, int sortOrder) {
        jdbc.update("""
                INSERT INTO mock_exam_questions (mock_exam_id, question_id, sort_order)
                VALUES (?, ?, ?)
                """, mockExamId, questionId, sortOrder);
    }

    // ---------------------------------------------------------------
    // Access Passes
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    // Review Packs / Tasks (for readiness-engine tests)
    // ---------------------------------------------------------------

    public Long insertReviewPack(Long userId, int learningCycle) {
        return jdbc.queryForObject("""
                INSERT INTO review_packs (user_id, learning_cycle, status)
                VALUES (?, ?, 'active')
                RETURNING id
                """, Long.class, userId, learningCycle);
    }

    public Long insertReviewTask(Long reviewPackId, Long userId, Long topicId,
                                  int targetCount, int completedCount) {
        return jdbc.queryForObject("""
                INSERT INTO review_tasks
                    (review_pack_id, user_id, topic_id,
                     target_question_count, completed_question_count)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, reviewPackId, userId, topicId, targetCount, completedCount);
    }

    // ---------------------------------------------------------------
    // Practice Sessions / Attempts (for stability + basic-practice tests)
    // ---------------------------------------------------------------

    public Long insertPracticeSession(Long userId, int learningCycle) {
        return jdbc.queryForObject("""
                INSERT INTO practice_sessions
                    (user_id, status, entry_type, language_code, learning_cycle, exam_id)
                VALUES (?, 'completed', 'full', 'en', ?, """ + CA_M1_EXAM + """
                )
                RETURNING id
                """, Long.class, userId, learningCycle);
    }

    /** Completed practice session belonging to a specific exam (cross-exam
     *  history isolation tests). */
    public Long insertPracticeSessionForExam(Long userId, int learningCycle, Long examId) {
        return jdbc.queryForObject("""
                INSERT INTO practice_sessions
                    (user_id, status, entry_type, language_code, learning_cycle, exam_id)
                VALUES (?, 'completed', 'full', 'en', ?, ?)
                RETURNING id
                """, Long.class, userId, learningCycle, examId);
    }

    public Long insertInProgressPracticeSession(Long userId, int learningCycle,
                                                 String entryType, String language) {
        return jdbc.queryForObject("""
                INSERT INTO practice_sessions
                    (user_id, status, entry_type, language_code, learning_cycle, exam_id)
                VALUES (?, 'in_progress', ?, ?, ?, """ + CA_M1_EXAM + """
                )
                RETURNING id
                """, Long.class, userId, entryType, language, learningCycle);
    }

    /** In-progress session scoped to a comma-joined topic_filter CSV (or null
     *  for the full pool) — mirrors how PracticeSessionRepository persists it. */
    public Long insertInProgressPracticeSession(Long userId, int learningCycle,
                                                 String entryType, String language,
                                                 String topicFilterCsv) {
        return jdbc.queryForObject("""
                INSERT INTO practice_sessions
                    (user_id, status, entry_type, language_code, learning_cycle, topic_filter, exam_id)
                VALUES (?, 'in_progress', ?, ?, ?, ?, """ + CA_M1_EXAM + """
                )
                RETURNING id
                """, Long.class, userId, entryType, language, learningCycle, topicFilterCsv);
    }

    public void insertPracticeAttempt(Long userId, Long practiceSessionId,
                                       Long questionId, Long variantId,
                                       String selectedKey, boolean isCorrect) {
        jdbc.update("""
                INSERT INTO practice_attempts
                    (user_id, practice_session_id, question_id, question_variant_id,
                     selected_choice_key, is_correct)
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId, practiceSessionId, questionId, variantId, selectedKey, isCorrect);
    }

    private int engSeq = 0;

    /** A single practice attempt for {@code userId} submitted at {@code submittedAt},
     *  building the minimal topic/question/variant/session chain. For engagement
     *  (streak / daily-goal) tests that need attempts on specific calendar days. */
    public void insertPracticeAttemptAt(Long userId, Long examId, OffsetDateTime submittedAt) {
        String code = "ENG_" + (++engSeq);
        Long topic = insertTopicForExam(examId, code, code + " EN", code + " ZH", false, 0);
        Long q = insertQuestion(topic, "A");
        Long v = insertVariantReturningId(q, "en", "stem",
                "[{\"key\":\"A\",\"text\":\"x\"}]", "expl");
        Long session = insertPracticeSessionForExam(userId, 0, examId);
        jdbc.update("""
                INSERT INTO practice_attempts
                    (user_id, practice_session_id, question_id, question_variant_id,
                     selected_choice_key, is_correct, submitted_at, created_at)
                VALUES (?, ?, ?, ?, 'A', true, ?, ?)
                """, userId, session, q, v, submittedAt, submittedAt);
    }

    public Long insertAccessPass(Long userId, String status,
                                  OffsetDateTime startsAt, OffsetDateTime expiresAt,
                                  int mockTotal, int mockUsed) {
        return jdbc.queryForObject("""
                INSERT INTO access_passes
                    (user_id, status, starts_at, expires_at,
                     mock_exam_total_count, mock_exam_used_count)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, userId, status, startsAt, expiresAt, mockTotal, mockUsed);
    }

    /** A pass scoped to one exam (V32 per-exam subscription). */
    public Long insertAccessPassForExam(Long userId, Long examId, String status,
                                        OffsetDateTime startsAt, OffsetDateTime expiresAt,
                                        int mockTotal, int mockUsed) {
        return jdbc.queryForObject("""
                INSERT INTO access_passes
                    (user_id, exam_id, status, starts_at, expires_at,
                     mock_exam_total_count, mock_exam_used_count)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, userId, examId, status, startsAt, expiresAt, mockTotal, mockUsed);
    }

    /** Sets an exam's Stripe Price id so it's purchasable (V34 billing tests). */
    public void setExamStripePrice(Long examId, String priceId) {
        jdbc.update("UPDATE exams SET stripe_price_id = ? WHERE id = ?", priceId, examId);
    }

    /** Force an existing pass to be time-expired by pulling expires_at into the past. */
    public void expireAccessPass(Long passId) {
        jdbc.update("""
                UPDATE access_passes
                SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 day'
                WHERE id = ?
                """, passId);
    }

    /** Read mock_exam_used_count for assertions about quota decrement. */
    public Integer getAccessPassMockUsed(Long passId) {
        return jdbc.queryForObject(
                "SELECT mock_exam_used_count FROM access_passes WHERE id = ?",
                Integer.class, passId);
    }

    // ---------------------------------------------------------------
    // AI Explanations (cache + rate-limit history)
    // ---------------------------------------------------------------

    /**
     * Insert a synthetic ai_explanations row whose {@code created_at} is
     * shifted into the past by {@code ageSeconds}. Used by rate-limit
     * and cache-hit tests to simulate prior calls.
     */
    public Long insertAiExplanation(Long userId, Long questionId, String language,
                                     long ageSeconds) {
        return jdbc.queryForObject("""
                INSERT INTO ai_explanations
                    (user_id, question_id, language, selected_choice_key,
                     explanation, model, tokens_in, tokens_out, created_at)
                VALUES (?, ?, ?, 'A', 'fixture-explanation', 'stub', 0, 0,
                        CURRENT_TIMESTAMP - make_interval(secs => ?))
                RETURNING id
                """, Long.class, userId, questionId, language, (double) ageSeconds);
    }

    public Integer countAiExplanationsForUser(Long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM ai_explanations WHERE user_id = ?",
                Integer.class, userId);
    }

    /**
     * Insert a synthetic ai_deep_dive_log row (metadata only, no text) aged
     * {@code ageSeconds} into the past. Used by deep-dive cap / rate-limit tests
     * to simulate prior deep-dive calls.
     */
    public void insertDeepDiveLog(Long userId, Long questionId, String language,
                                  int depth, long ageSeconds) {
        jdbc.update("""
                INSERT INTO ai_deep_dive_log (user_id, question_id, language, depth, created_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP - make_interval(secs => ?))
                """, userId, questionId, language, depth, (double) ageSeconds);
    }

    public Integer countDeepDiveLogForUser(Long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM ai_deep_dive_log WHERE user_id = ?",
                Integer.class, userId);
    }

    /**
     * Insert a synthetic reminder_tasks row aged {@code ageSeconds} into the
     * past. Used by daily-cap / pause / ownership tests.
     */
    public Long insertReminder(Long userId, String type, String status,
                               int priority, long ageSeconds) {
        return jdbc.queryForObject("""
                INSERT INTO reminder_tasks (user_id, type, status, priority, created_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP - make_interval(secs => ?))
                RETURNING id
                """, Long.class, userId, type, status, priority, (double) ageSeconds);
    }
}
