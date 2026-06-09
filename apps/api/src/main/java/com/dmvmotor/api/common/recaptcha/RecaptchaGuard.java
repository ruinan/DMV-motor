package com.dmvmotor.api.common.recaptcha;

import com.dmvmotor.api.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Backend-enforced human (bot) verification for sensitive actions — register,
 * login (precheck), and subscription add/remove. The client attaches a reCAPTCHA
 * token in the {@code X-Recaptcha-Token} header; this guard verifies it
 * server-side so it can't be bypassed by a crafted client.
 *
 * <p>No-op when reCAPTCHA is disabled (dev / test), so those environments don't
 * need a key. When enabled, a missing token is {@code RECAPTCHA_REQUIRED} and a
 * failed/low-score assessment is {@code RECAPTCHA_FAILED} (both 403).
 */
@Component
public class RecaptchaGuard {

    public static final String TOKEN_HEADER = "X-Recaptcha-Token";

    private final RecaptchaProperties      props;
    private final Optional<RecaptchaVerifier> verifier;

    public RecaptchaGuard(RecaptchaProperties props, Optional<RecaptchaVerifier> verifier) {
        this.props    = props;
        this.verifier = verifier;
    }

    /** Verifies the request's reCAPTCHA token for {@code action}; no-op if disabled. */
    public void requireHuman(String action) {
        if (!props.enabled()) return;

        String token = tokenHeader();
        if (token == null || token.isBlank()) {
            throw new BusinessException("RECAPTCHA_REQUIRED",
                    "Human verification required", HttpStatus.FORBIDDEN);
        }
        RecaptchaVerifier v = verifier.orElseThrow(() -> new BusinessException(
                "RECAPTCHA_MISCONFIGURED", "Verification is unavailable",
                HttpStatus.INTERNAL_SERVER_ERROR));

        RecaptchaVerifier.Assessment a = v.verify(token, action);
        if (!a.valid() || a.score() < props.minScore()) {
            throw new BusinessException("RECAPTCHA_FAILED",
                    "Human verification failed", HttpStatus.FORBIDDEN);
        }
    }

    private static String tokenHeader() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra instanceof ServletRequestAttributes sra) {
            return sra.getRequest().getHeader(TOKEN_HEADER);
        }
        return null;
    }
}
