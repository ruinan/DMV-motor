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

    public Long insertQuestion(Long topicId, String correctChoiceKey) {
        return jdbc.queryForObject("""
                INSERT INTO questions (primary_topic_id, correct_choice_key, status)
                VALUES (?, ?, 'active')
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

    public Long insertUserWithoutEmail() {
        return jdbc.queryForObject("""
                INSERT INTO users (language_preference)
                VALUES ('en')
                RETURNING id
                """, Long.class);
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
    // Access Passes
    // ---------------------------------------------------------------

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
