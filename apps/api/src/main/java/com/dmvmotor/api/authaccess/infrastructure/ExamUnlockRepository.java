package com.dmvmotor.api.authaccess.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;

/**
 * Records which exams a user has "opened" at the free tier (V38). A UX marker
 * only — free-trial practice is open to everyone, so this grants nothing beyond
 * the free tier; it keeps an exam shown as "Free" (not "Locked") in the switcher
 * once the user taps Free trial, even before they practice. Paid access stays
 * separate and authoritative in {@link AccessRepository}.
 */
@Repository
public class ExamUnlockRepository {

    private final JdbcTemplate jdbc;

    public ExamUnlockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Idempotently marks the exam as free-opened for the user. */
    public void openFree(long userId, long examId) {
        jdbc.update("""
                INSERT INTO exam_free_unlocks (user_id, exam_id)
                VALUES (?, ?)
                ON CONFLICT (user_id, exam_id) DO NOTHING
                """, userId, examId);
    }

    /** The exam ids this user has free-opened. */
    public Set<Long> freeUnlockedExamIds(long userId) {
        return new HashSet<>(jdbc.queryForList(
                "SELECT exam_id FROM exam_free_unlocks WHERE user_id = ?",
                Long.class, userId));
    }
}
