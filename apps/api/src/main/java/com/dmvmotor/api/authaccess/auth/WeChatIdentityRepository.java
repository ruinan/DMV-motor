package com.dmvmotor.api.authaccess.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Persistence for WeChat login: the {@code wechat_identities} side table
 * (openid → account) plus the few {@code users} lookups the flow needs. Uses
 * {@link JdbcTemplate} (like {@link UserProvisioner}) so it owns its access
 * without a jOOQ regen and keeps the auth module self-contained.
 */
@Repository
public class WeChatIdentityRepository {

    private final JdbcTemplate jdbc;

    public WeChatIdentityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The account linked to {@code openid}, or null if this WeChat user is new. */
    public Long findUserIdByOpenid(String openid) {
        return jdbc.query(
                "SELECT user_id FROM wechat_identities WHERE openid = ?",
                rs -> rs.next() ? rs.getLong("user_id") : null,
                openid);
    }

    public void insertIdentity(String openid, String unionid, Long userId) {
        jdbc.update(
                "INSERT INTO wechat_identities (openid, unionid, user_id) VALUES (?, ?, ?)",
                openid, unionid, userId);
    }

    /** An existing account for {@code email} (case-insensitive), or null. Drives the
     *  "email already in use → sign in to link" branch. */
    public Long findUserIdByEmail(String email) {
        return jdbc.query(
                "SELECT id FROM users WHERE lower(email) = lower(?) ORDER BY id LIMIT 1",
                rs -> rs.next() ? rs.getLong("id") : null,
                email);
    }

    /** The account's Firebase uid — the custom token is minted with this so a
     *  returning WeChat login resolves to the same account via the existing key.
     *  Only called for a user that exists (resolved from openid), so a missing row
     *  is a programming error and surfaces as an exception. */
    public String findFirebaseUidByUserId(Long userId) {
        return jdbc.queryForObject(
                "SELECT firebase_uid FROM users WHERE id = ?", String.class, userId);
    }

    /** Creates a fresh account for a new WeChat user (no password; email is the key). */
    public Long createAccount(String firebaseUid, String email) {
        return jdbc.queryForObject("""
                INSERT INTO users (firebase_uid, email, language_preference)
                VALUES (?, ?, 'en')
                RETURNING id
                """, Long.class, firebaseUid, email);
    }
}
