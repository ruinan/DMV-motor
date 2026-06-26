package com.dmvmotor.api.authaccess.auth;

import com.dmvmotor.api.common.BusinessException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Production custom-token minter (Firebase Admin SDK). Active only when
 * {@code app.auth.firebase.enabled=true} (prod) — dev/test use
 * {@link StubFirebaseTokenMinter}. Excluded from coverage like
 * {@code FirebaseIdTokenVerifier}.
 *
 * <p>⚠️ On Cloud Run (ADC), {@code createCustomToken} requires the runtime
 * service account to hold {@code roles/iam.serviceAccountTokenCreator} (signBlob);
 * {@code verifyIdToken} does not. Grant it before relying on WeChat login in prod.
 */
@Component
@ConditionalOnProperty(name = "app.auth.firebase.enabled", havingValue = "true")
public class FirebaseTokenMinterImpl implements FirebaseTokenMinter {

    @Override
    public String mintCustomToken(String uid) {
        try {
            return FirebaseAuth.getInstance().createCustomToken(uid);
        } catch (FirebaseAuthException e) {
            throw new BusinessException("TOKEN_MINT_FAILED",
                    "Could not issue an authentication token", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
