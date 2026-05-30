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

    // ---------------------------------------------------------------
    // Cleanup
    // ---------------------------------------------------------------

    public void truncateAll() {
        jdbc.execute("""
                TRUNCATE
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
    }

    // ---------------------------------------------------------------
    // Topics
    // ---------------------------------------------------------------

    public Long insertTopic(String code) {
        return insertTopic(code, code + " EN", code + " ZH", false, 0);
    }

    public Long insertTopic(String code, String nameEn, String nameZh,
                             boolean isKeyTopic, int sortOrder) {
        Long topicId = jdbc.queryForObject("""
                INSERT INTO topics (code, name_en, name_zh, is_key_topic, sort_order)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, code, nameEn, nameZh, isKeyTopic, sortOrder);
        // Auto-seed one default sub-topic so insertQuestion's sub_topic_id
        // resolver (added when V15 set the column NOT NULL) always finds a row.
        // Production data uses the V14 seed; tests build minimal topics ad hoc.
        jdbc.update("""
                INSERT INTO sub_topics (parent_topic_id, code, name_en, name_zh, sort_order)
                VALUES (?, ?, ?, ?, 0)
                """, topicId, code + "_ST", nameEn + " sub", nameZh + " 子");
        return topicId;
    }

    public Long insertChildTopic(String code, Long parentTopicId) {
        Long topicId = jdbc.queryForObject("""
                INSERT INTO topics (code, name_en, name_zh, is_key_topic, sort_order, parent_topic_id)
                VALUES (?, ?, ?, false, 0, ?)
                RETURNING id
                """, Long.class, code, code + " EN", code + " ZH", parentTopicId);
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
                    (primary_topic_id, correct_choice_key, status, allow_in_free_trial, is_key_coverage, sub_topic_id)
                VALUES (?, ?, 'active', true, true,
                    (SELECT id FROM sub_topics WHERE parent_topic_id = ? ORDER BY sort_order LIMIT 1))
                RETURNING id
                """, Long.class, topicId, correctChoiceKey, topicId);
    }

    public Long insertQuestion(Long topicId, String correctChoiceKey) {
        return jdbc.queryForObject("""
                INSERT INTO questions (primary_topic_id, correct_choice_key, status, allow_in_free_trial, sub_topic_id)
                VALUES (?, ?, 'active', true,
                    (SELECT id FROM sub_topics WHERE parent_topic_id = ? ORDER BY sort_order LIMIT 1))
                RETURNING id
                """, Long.class, topicId, correctChoiceKey, topicId);
    }

    /** Active question excluded from the free-trial pool (allow_in_free_trial=false). */
    public Long insertPaidOnlyQuestion(Long topicId, String correctChoiceKey) {
        return jdbc.queryForObject("""
                INSERT INTO questions (primary_topic_id, correct_choice_key, status, allow_in_free_trial, sub_topic_id)
                VALUES (?, ?, 'active', false,
                    (SELECT id FROM sub_topics WHERE parent_topic_id = ? ORDER BY sort_order LIMIT 1))
                RETURNING id
                """, Long.class, topicId, correctChoiceKey, topicId);
    }

    /** Question with status='inactive' — should never appear in any practice pool. */
    public Long insertInactiveQuestion(Long topicId, String correctChoiceKey) {
        return jdbc.queryForObject("""
                INSERT INTO questions (primary_topic_id, correct_choice_key, status, allow_in_free_trial, sub_topic_id)
                VALUES (?, ?, 'inactive', true,
                    (SELECT id FROM sub_topics WHERE parent_topic_id = ? ORDER BY sort_order LIMIT 1))
                RETURNING id
                """, Long.class, topicId, correctChoiceKey, topicId);
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
                     submitted_at)
                VALUES (?, ?, 'submitted', ?, ?, 0, ?, true, ?, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, userId, mockExamId, scorePercent,
                scorePercent / 10, scorePercent / 10, learningCycle);
    }

    public Long insertInProgressMockAttempt(Long userId, Long mockExamId) {
        return jdbc.queryForObject("""
                INSERT INTO mock_attempts
                    (user_id, mock_exam_id, status, answered_count, quota_consumed, learning_cycle)
                VALUES (?, ?, 'in_progress', 0, true, 0)
                RETURNING id
                """, Long.class, userId, mockExamId);
    }

    /** In-progress attempt whose clock started {@code ageSeconds} ago — used to
     *  exercise mock-timer expiry without real waiting. */
    public Long insertInProgressMockAttemptStartedSecondsAgo(Long userId, Long mockExamId,
                                                             int ageSeconds) {
        return jdbc.queryForObject("""
                INSERT INTO mock_attempts
                    (user_id, mock_exam_id, status, answered_count, quota_consumed,
                     learning_cycle, started_at)
                VALUES (?, ?, 'in_progress', 0, true, 0,
                        CURRENT_TIMESTAMP - make_interval(secs => ?))
                RETURNING id
                """, Long.class, userId, mockExamId, ageSeconds);
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
                INSERT INTO mock_exams (code, question_count, status)
                VALUES (?, ?, 'active')
                RETURNING id
                """, Long.class, code, questionCount);
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
                    (user_id, status, entry_type, language_code, learning_cycle)
                VALUES (?, 'completed', 'full', 'en', ?)
                RETURNING id
                """, Long.class, userId, learningCycle);
    }

    public Long insertInProgressPracticeSession(Long userId, int learningCycle,
                                                 String entryType, String language) {
        return jdbc.queryForObject("""
                INSERT INTO practice_sessions
                    (user_id, status, entry_type, language_code, learning_cycle)
                VALUES (?, 'in_progress', ?, ?, ?)
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
                    (user_id, status, entry_type, language_code, learning_cycle, topic_filter)
                VALUES (?, 'in_progress', ?, ?, ?, ?)
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
}
