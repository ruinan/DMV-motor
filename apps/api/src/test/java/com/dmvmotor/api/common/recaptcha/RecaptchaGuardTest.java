package com.dmvmotor.api.common.recaptcha;

import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.recaptcha.RecaptchaVerifier.Assessment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pure unit test for the bot-verification gate — no Spring context. Exercises
 * every branch: disabled no-op, missing token, low score / invalid token,
 * good score, and the misconfigured (enabled-but-no-verifier) case.
 */
class RecaptchaGuardTest {

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static RecaptchaProperties enabled() {
        return new RecaptchaProperties(true, "proj", "site", 0.5);
    }

    private static RecaptchaProperties disabled() {
        return new RecaptchaProperties(false, null, null, 0.5);
    }

    private static void setToken(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (token != null) req.addHeader(RecaptchaGuard.TOKEN_HEADER, token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @Test
    void disabled_isNoOp_evenWithoutRequestOrVerifier() {
        RecaptchaGuard guard = new RecaptchaGuard(disabled(), Optional.empty());
        assertDoesNotThrow(() -> guard.requireHuman("login"));
    }

    @Test
    void enabled_missingToken_throwsRequired() {
        setToken(null);
        RecaptchaGuard guard = new RecaptchaGuard(enabled(),
                Optional.of((t, a) -> new Assessment(true, 0.9, "ok")));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.requireHuman("login"));
        assertEquals("RECAPTCHA_REQUIRED", ex.getErrorCode());
    }

    @Test
    void enabled_lowScore_throwsFailed() {
        setToken("tok");
        RecaptchaGuard guard = new RecaptchaGuard(enabled(),
                Optional.of((t, a) -> new Assessment(true, 0.1, "ok")));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.requireHuman("login"));
        assertEquals("RECAPTCHA_FAILED", ex.getErrorCode());
    }

    @Test
    void enabled_invalidToken_throwsFailed() {
        setToken("tok");
        RecaptchaGuard guard = new RecaptchaGuard(enabled(),
                Optional.of((t, a) -> new Assessment(false, 0.9, "bad")));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.requireHuman("login"));
        assertEquals("RECAPTCHA_FAILED", ex.getErrorCode());
    }

    @Test
    void enabled_goodAssessment_passes() {
        setToken("tok");
        RecaptchaGuard guard = new RecaptchaGuard(enabled(),
                Optional.of((t, a) -> new Assessment(true, 0.9, "ok")));
        assertDoesNotThrow(() -> guard.requireHuman("login"));
    }

    @Test
    void enabled_noVerifierBean_throwsMisconfigured() {
        setToken("tok");
        RecaptchaGuard guard = new RecaptchaGuard(enabled(), Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> guard.requireHuman("login"));
        assertEquals("RECAPTCHA_MISCONFIGURED", ex.getErrorCode());
    }
}
