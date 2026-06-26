package com.dmvmotor.api.authaccess.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Dev / test custom-token minter. Active whenever {@code app.auth.firebase.enabled}
 * is {@code false} or unset (everywhere except prod) — mirrors
 * {@link StubFirebaseVerifier}. Returns a deterministic token so WeChat-login
 * tests can assert which uid was minted without a real Firebase project.
 */
@Component
@ConditionalOnProperty(name = "app.auth.firebase.enabled",
                       havingValue = "false",
                       matchIfMissing = true)
public class StubFirebaseTokenMinter implements FirebaseTokenMinter {

    @Override
    public String mintCustomToken(String uid) {
        return "stub-custom-token:" + uid;
    }
}
