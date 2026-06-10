package com.dmvmotor.api.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** MFA gate logic — no Spring context needed. */
class MfaGuardTest {

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void disabled_isNoOp_evenWithoutSecondFactor() {
        RequestContextHolder.resetRequestAttributes();
        assertDoesNotThrow(() -> new MfaGuard(false).requireMfa());
    }

    @Test
    void enabled_withSecondFactor_passes() {
        bindSecondFactor("totp");
        assertDoesNotThrow(() -> new MfaGuard(true).requireMfa());
    }

    @Test
    void enabled_withoutSecondFactor_requiresMfa() {
        bindSecondFactor(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> new MfaGuard(true).requireMfa());
        assertEquals("MFA_REQUIRED", ex.getErrorCode());
    }

    @Test
    void enabled_noRequestContext_requiresMfa() {
        RequestContextHolder.resetRequestAttributes();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> new MfaGuard(true).requireMfa());
        assertEquals("MFA_REQUIRED", ex.getErrorCode());
    }

    private static void bindSecondFactor(String secondFactor) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(req);
        if (secondFactor != null) {
            attrs.setAttribute(MfaGuard.SECOND_FACTOR_ATTR, secondFactor,
                    RequestAttributes.SCOPE_REQUEST);
        }
        RequestContextHolder.setRequestAttributes(attrs);
    }
}
