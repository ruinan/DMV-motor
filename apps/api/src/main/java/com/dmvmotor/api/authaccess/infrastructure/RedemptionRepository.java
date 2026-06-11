package com.dmvmotor.api.authaccess.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Data access for activation / redemption codes (V37). Uses {@link JdbcTemplate}
 * because the codes tables aren't part of the jOOQ-generated model.
 */
@Repository
public class RedemptionRepository {

    private final JdbcTemplate jdbc;

    public RedemptionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** A code's redemption-relevant fields. {@code examId} null = redeem against
     *  the user's current exam; {@code expired} reflects expires_at vs now. */
    public record CodeRow(long id, Long examId, int durationDays, int mockQuota, boolean expired) {}

    /** The active code matching {@code code} (case-insensitive), or null. */
    public CodeRow findActiveByCode(String code) {
        return jdbc.query("""
                SELECT id, exam_id, duration_days, mock_quota,
                       (expires_at IS NOT NULL AND expires_at <= now()) AS expired
                FROM redemption_codes
                WHERE UPPER(code) = UPPER(?) AND status = 'active'
                """,
                rs -> rs.next()
                        ? new CodeRow(
                                rs.getLong("id"),
                                (Long) rs.getObject("exam_id"),
                                rs.getInt("duration_days"),
                                rs.getInt("mock_quota"),
                                rs.getBoolean("expired"))
                        : null,
                code);
    }

    public boolean alreadyRedeemed(long codeId, long userId) {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM code_redemptions WHERE code_id = ? AND user_id = ?",
                Integer.class, codeId, userId);
        return n != null && n > 0;
    }

    /**
     * Atomically claims one redemption slot: increments redemption_count only
     * while the code is still active, unexpired, and under its cap. Returns true
     * if a slot was claimed — so a popular code can't be over-redeemed past
     * {@code max_redemptions} under concurrency.
     */
    public boolean claimSlot(long codeId) {
        return jdbc.update("""
                UPDATE redemption_codes
                   SET redemption_count = redemption_count + 1,
                       updated_at = now()
                 WHERE id = ?
                   AND status = 'active'
                   AND (expires_at IS NULL OR expires_at > now())
                   AND redemption_count < max_redemptions
                """, codeId) == 1;
    }

    public void insertRedemption(long codeId, long userId, long examId, long passId) {
        jdbc.update("""
                INSERT INTO code_redemptions (code_id, user_id, exam_id, pass_id)
                VALUES (?, ?, ?, ?)
                """, codeId, userId, examId, passId);
    }
}
