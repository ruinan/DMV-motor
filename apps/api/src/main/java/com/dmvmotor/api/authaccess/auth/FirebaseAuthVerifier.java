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

    record VerifiedUser(String firebaseUid, String email) {}
}
