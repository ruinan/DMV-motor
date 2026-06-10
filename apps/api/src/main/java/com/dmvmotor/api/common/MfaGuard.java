package com.dmvmotor.api.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Server-side two-factor (MFA) gate for sensitive actions. The {@link UserIdResolver}
 * stashes the verified token's {@code sign_in_second_factor} on the request; this
 * guard requires it to be present — i.e. the session actually completed 2FA — so a
 * crafted client that skips the frontend enrollment gate still can't perform a
 * guarded action.
 *
 * <p>Flag-gated by {@code app.auth.mfa-required} (default {@code false}) so it's a
 * no-op until 2FA is rolled out (mirrors the reCAPTCHA / billing seams). When off,
 * nothing changes; when on, a non-2FA session is rejected with {@code MFA_REQUIRED}.
 */
@Component
public class MfaGuard {

    /** Request attribute holding the verified token's second factor (or null). */
    public static final String SECOND_FACTOR_ATTR = "dmv.auth.secondFactor";

    private final boolean required;

    public MfaGuard(@Value("${app.auth.mfa-required:false}") boolean required) {
        this.required = required;
    }

    public void requireMfa() {
        if (!required) return;
        String sf = currentSecondFactor();
        if (sf == null || sf.isBlank()) {
            throw new BusinessException("MFA_REQUIRED",
                    "Two-factor authentication is required for this action",
                    HttpStatus.FORBIDDEN);
        }
    }

    private static String currentSecondFactor() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra == null) return null;
        Object v = ra.getAttribute(SECOND_FACTOR_ATTR, RequestAttributes.SCOPE_REQUEST);
        return v instanceof String s ? s : null;
    }
}
