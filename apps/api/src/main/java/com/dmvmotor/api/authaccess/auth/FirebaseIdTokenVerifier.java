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
            return new VerifiedUser(decoded.getUid(), decoded.getEmail(),
                    authTime(decoded), secondFactor(decoded));
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

    /** The token's {@code firebase.sign_in_second_factor} (e.g. {@code "totp"});
     *  present only when the sign-in completed a second factor. null otherwise —
     *  which the MFA gate treats as "2FA not satisfied". */
    @SuppressWarnings("unchecked")
    private static String secondFactor(FirebaseToken decoded) {
        Object firebase = decoded.getClaims().get("firebase");
        if (firebase instanceof java.util.Map<?, ?> m) {
            Object sf = ((java.util.Map<String, Object>) m).get("sign_in_second_factor");
            if (sf instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }
}
