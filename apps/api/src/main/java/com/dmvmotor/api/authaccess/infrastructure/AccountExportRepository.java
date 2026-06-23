package com.dmvmotor.api.authaccess.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles a portable snapshot of everything the app stores about a single
 * user (CCPA "right to know / data portability"). Read-only; the caller is the
 * authenticated owner of the data. Operational/derived rows (AI explanation
 * cache, deep-dive log, reminders, the JSONB progress backup) are intentionally
 * omitted — they're regenerated from the learning records included here.
 */
@Repository
public class AccountExportRepository {

    private final JdbcTemplate jdbc;

    public AccountExportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> export(Long userId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exported_at", OffsetDateTime.now().toString());
        out.put("profile", jdbc.queryForMap(
                "SELECT id AS user_id, email, language_preference, reset_count, "
                        + "current_exam_id, created_at FROM users WHERE id = ?", userId));
        out.put("access_passes",     rows("SELECT * FROM access_passes     WHERE user_id = ? ORDER BY id", userId));
        out.put("practice_sessions", rows("SELECT * FROM practice_sessions WHERE user_id = ? ORDER BY id", userId));
        out.put("practice_attempts", rows("SELECT * FROM practice_attempts WHERE user_id = ? ORDER BY id", userId));
        out.put("mistakes",          rows("SELECT * FROM mistake_records   WHERE user_id = ? ORDER BY id", userId));
        out.put("mock_attempts",     rows("SELECT * FROM mock_attempts     WHERE user_id = ? ORDER BY id", userId));
        out.put("redemptions",       rows("SELECT * FROM code_redemptions  WHERE user_id = ? ORDER BY id", userId));
        out.put("free_unlocks",      rows("SELECT * FROM exam_free_unlocks WHERE user_id = ? ORDER BY id", userId));
        return out;
    }

    private List<Map<String, Object>> rows(String sql, Long userId) {
        return jdbc.queryForList(sql, userId);
    }
}
