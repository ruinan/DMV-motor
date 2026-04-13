package com.dmvmotor.api.practice.infrastructure;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class MistakeRepositoryTest extends IntegrationTestBase {

    @Autowired MistakeRepository mistakeRepository;
    @Autowired TestFixtures      fixtures;
    @Autowired JdbcTemplate      jdbc;

    private Long userId;
    private Long topicId;
    private Long questionId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId     = fixtures.insertUser("mistake-test@example.com");
        topicId    = fixtures.insertTopic("MISTAKE_TOPIC");
        questionId = fixtures.insertQuestion(topicId, "A");
    }

    @Test
    void upsertMistake_newRecord_insertsRowWithWrongCountOne() {
        mistakeRepository.upsertMistake(userId, questionId, topicId, "practice", 0);

        Integer count = jdbc.queryForObject(
                "SELECT wrong_count FROM mistake_records WHERE user_id=? AND question_id=? AND learning_cycle=0",
                Integer.class, userId, questionId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void upsertMistake_existingRecord_incrementsWrongCount() {
        mistakeRepository.upsertMistake(userId, questionId, topicId, "practice", 0);
        mistakeRepository.upsertMistake(userId, questionId, topicId, "review", 0);

        Integer count = jdbc.queryForObject(
                "SELECT wrong_count FROM mistake_records WHERE user_id=? AND question_id=? AND learning_cycle=0",
                Integer.class, userId, questionId);
        assertThat(count).isEqualTo(2);
    }
}
