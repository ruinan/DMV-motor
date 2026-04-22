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
        return jdbc.queryForObject("""
                INSERT INTO topics (code, name_en, name_zh, is_key_topic, sort_order)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, code, nameEn, nameZh, isKeyTopic, sortOrder);
    }

    public Long insertChildTopic(String code, Long parentTopicId) {
        return jdbc.queryForObject("""
                INSERT INTO topics (code, name_en, name_zh, is_key_topic, sort_order, parent_topic_id)
                VALUES (?, ?, ?, false, 0, ?)
                RETURNING id
                """, Long.class, code, code + " EN", code + " ZH", parentTopicId);
    }

    // ---------------------------------------------------------------
    // Questions
    // ---------------------------------------------------------------

    public Long insertKeyCoverageQuestion(Long topicId, String correctChoiceKey) {
        return jdbc.queryForObject("""
                INSERT INTO questions
                    (primary_topic_id, correct_choice_key, status, allow_in_free_trial, is_key_coverage)
                VALUES (?, ?, 'active', true, true)
                RETURNING id
                """, Long.class, topicId, correctChoiceKey);
    }

    public Long insertQuestion(Long topicId, String correctChoiceKey) {
        return jdbc.queryForObject("""
                INSERT INTO questions (primary_topic_id, correct_choice_key, status, allow_in_free_trial)
                VALUES (?, ?, 'active', true)
                RETURNING id
                """, Long.class, topicId, correctChoiceKey);
    }

    public void insertEnVariant(Long questionId, String stem, String explanation) {
        insertVariant(questionId, "en", stem,
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

    public Long insertUser(String email) {
        return jdbc.queryForObject("""
                INSERT INTO users (email, language_preference)
                VALUES (?, 'en')
                RETURNING id
                """, Long.class, email);
    }

    public void incrementUserResetCount(Long userId) {
        jdbc.update("UPDATE users SET reset_count = reset_count + 1 WHERE id = ?", userId);
    }

    public Long insertUserWithoutEmail() {
        return jdbc.queryForObject("""
                INSERT INTO users (language_preference)
                VALUES ('en')
                RETURNING id
                """, Long.class);
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
                     wrong_count, answered_count, quota_consumed, learning_cycle)
                VALUES (?, ?, 'submitted', ?, ?, 0, ?, true, ?)
                RETURNING id
                """, Long.class, userId, mockExamId, scorePercent,
                scorePercent / 10, scorePercent / 10, learningCycle);
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
}
