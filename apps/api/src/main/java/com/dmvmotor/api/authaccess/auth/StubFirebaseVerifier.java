package com.dmvmotor.api.authaccess.auth;

import com.dmvmotor.api.common.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Development / test fallback verifier. Active whenever
 * {@code app.auth.firebase.enabled} is {@code false} or unset (default) — i.e.
 * everywhere except prod. Accepts two token shapes:
 *
 * <ul>
 *   <li>A bare numeric id, e.g. {@code "123"} — maps to
 *       {@code VerifiedUser(firebaseUid = "test-123", email = "test123@local")}.
 *       This preserves the pre-Firebase
 *       {@code Authorization: Bearer <numericUserId>} shim used by 100+ existing
 *       test assertions and by Postman during local dev.
 *   <li>A literal {@code "test-<anyUid>"} — returned as-is
 *       ({@code firebaseUid = "test-<anyUid>"}, {@code email = "test-<anyUid>@local"}).
 *       Lets tests assert just-in-time provisioning of previously-unseen uids.
 * </ul>
 *
 * Anything else — empty, malformed, a real-looking JWT — rejected with 401.
 */
@Component
@ConditionalOnProperty(name = "app.auth.firebase.enabled",
                       havingValue = "false",
                       matchIfMissing = true)
public class StubFirebaseVerifier implements FirebaseAuthVerifier {

    @Override
    public VerifiedUser verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new BusinessException("UNAUTHORIZED",
                    "Missing token", HttpStatus.UNAUTHORIZED);
        }
        String token = idToken.trim();

        if (token.startsWith("test-")) {
            return new VerifiedUser(token, token + "@local");
        }

        try {
            long numericId = Long.parseLong(token);
            String uid = "test-" + numericId;
            return new VerifiedUser(uid, "test" + numericId + "@local");
        } catch (NumberFormatException e) {
            throw new BusinessException("UNAUTHORIZED",
                    "Invalid dev-mode token (expected numeric id or 'test-<uid>')",
                    HttpStatus.UNAUTHORIZED);
        }
    }
}
