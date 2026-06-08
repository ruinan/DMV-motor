package com.dmvmotor.api.authaccess.auth;

import com.dmvmotor.api.common.BusinessException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Production verifier. Decodes and validates Firebase ID tokens using the Admin
 * SDK (offline public-key verification). Activated only when
 * {@code app.auth.firebase.enabled=true}, which is set in
 * {@code application-prod.yml}.
 */
@Component
@ConditionalOnProperty(name = "app.auth.firebase.enabled", havingValue = "true")
public class FirebaseIdTokenVerifier implements FirebaseAuthVerifier {

    @Override
    public VerifiedUser verify(String idToken) {
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            return new VerifiedUser(decoded.getUid(), decoded.getEmail(), authTime(decoded));
        } catch (FirebaseAuthException e) {
            throw new BusinessException("UNAUTHORIZED",
                    "Invalid or expired Firebase ID token",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    /** The token's {@code auth_time} (seconds) — when the user last proved their
     *  password; updated by reauthenticateWithCredential. 0 if absent. */
    private static long authTime(FirebaseToken decoded) {
        Object v = decoded.getClaims().get("auth_time");
        return v instanceof Number n ? n.longValue() : 0L;
    }
}
