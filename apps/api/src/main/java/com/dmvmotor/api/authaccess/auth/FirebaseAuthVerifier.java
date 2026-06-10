package com.dmvmotor.api.authaccess.auth;

/**
 * Verifies a bearer token from an HTTP request and returns the authenticated
 * user's identity. Two implementations:
 * <ul>
 *   <li>{@code FirebaseIdTokenVerifier} — production, verifies Firebase ID tokens
 *       offline via cached Google public keys
 *   <li>{@code StubFirebaseVerifier} — dev/test, accepts either a numeric user id
 *       (legacy {@code Bearer <userId>} shim) or a {@code test-<uid>} literal
 * </ul>
 * Selection is via {@code app.auth.firebase.enabled}; prod sets it true.
 *
 * <p>Implementations throw {@code BusinessException("UNAUTHORIZED", ..., 401)}
 * on invalid or expired tokens. Missing header is handled upstream in the
 * resolver before this method is reached.
 */
public interface FirebaseAuthVerifier {

    VerifiedUser verify(String idToken);

    /**
     * The authenticated identity plus {@code authTimeEpochSeconds} (when the user
     * last proved their password — Firebase {@code auth_time}; used by reauth
     * gates) and {@code secondFactor} (the Firebase {@code sign_in_second_factor}
     * claim, e.g. {@code "totp"}; null when the sign-in did NOT complete 2FA —
     * used by the MFA gate).
     */
    record VerifiedUser(String firebaseUid, String email, long authTimeEpochSeconds,
                        String secondFactor) {
        public VerifiedUser(String firebaseUid, String email, long authTimeEpochSeconds) {
            this(firebaseUid, email, authTimeEpochSeconds, null);
        }
        /** Identity without a known auth time (e.g. provisioning tests) — treated
         *  as epoch 0, i.e. never "recently" authenticated, and no second factor. */
        public VerifiedUser(String firebaseUid, String email) {
            this(firebaseUid, email, 0L, null);
        }
    }
}
