package com.dmvmotor.api.authaccess.auth;

/**
 * Seam over Firebase custom-token minting. WeChat users authenticate by openid
 * (no Firebase email/password); the backend mints a Firebase **custom token** for
 * the resolved account so the client can sign in and obtain a normal Firebase ID
 * token — which the existing {@link FirebaseAuthVerifier} validates unchanged.
 *
 * <p>The real impl ({@code FirebaseTokenMinterImpl}) is prod-only and excluded
 * from coverage; dev/test use {@code StubFirebaseTokenMinter}.
 */
public interface FirebaseTokenMinter {

    /** Mints a Firebase custom token for {@code uid} (the account's firebase_uid). */
    String mintCustomToken(String uid);
}
