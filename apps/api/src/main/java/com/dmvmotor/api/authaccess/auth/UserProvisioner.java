package com.dmvmotor.api.authaccess.auth;

import com.dmvmotor.api.authaccess.auth.FirebaseAuthVerifier.VerifiedUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps an authenticated Firebase identity to an internal {@code users.id} (bigint).
 * On first login for a given Firebase UID, inserts a fresh {@code users} row
 * (JIT provisioning) — no separate signup endpoint is needed.
 *
 * <p>Race: two simultaneous first-logins will both miss the SELECT, both attempt
 * the INSERT, and one will lose on the {@code uq_users_firebase_uid} constraint.
 * The loser catches {@link DuplicateKeyException} and re-reads, returning the
 * winner's id. Idempotent from the caller's perspective.
 */
@Component
public class UserProvisioner {

    private final JdbcTemplate jdbc;

    public UserProvisioner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Long provisionUserId(VerifiedUser user) {
        Long existing = findByFirebaseUid(user.firebaseUid());
        if (existing != null) return existing;

        try {
            return jdbc.queryForObject("""
                    INSERT INTO users (firebase_uid, email, language_preference)
                    VALUES (?, ?, 'en')
                    RETURNING id
                    """, Long.class, user.firebaseUid(), user.email());
        } catch (DuplicateKeyException race) {
            Long winner = findByFirebaseUid(user.firebaseUid());
            if (winner != null) return winner;
            throw race;
        }
    }

    private Long findByFirebaseUid(String uid) {
        return jdbc.query(
                "SELECT id FROM users WHERE firebase_uid = ?",
                rs -> rs.next() ? rs.getLong("id") : null,
                uid);
    }
}
