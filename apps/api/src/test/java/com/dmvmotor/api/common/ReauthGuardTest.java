package com.dmvmotor.api.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Reauth freshness logic — no Spring context needed. */
class ReauthGuardTest {

    private static final long WINDOW = 300;

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void noRequestContext_requiresReauth() {
        RequestContextHolder.resetRequestAttributes();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> new ReauthGuard(WINDOW).requireRecentReauth());
        assertEquals("REAUTH_REQUIRED", ex.getErrorCode());
    }

    @Test
    void freshAuthTime_passes() {
        bindAuthTime(Instant.now().getEpochSecond());
        assertDoesNotThrow(() -> new ReauthGuard(WINDOW).requireRecentReauth());
    }

    @Test
    void staleAuthTime_requiresReauth() {
        bindAuthTime(Instant.now().getEpochSecond() - (WINDOW + 60));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> new ReauthGuard(WINDOW).requireRecentReauth());
        assertEquals("REAUTH_REQUIRED", ex.getErrorCode());
    }

    private static void bindAuthTime(long epochSeconds) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(req);
        attrs.setAttribute(ReauthGuard.AUTH_TIME_ATTR, epochSeconds, RequestAttributes.SCOPE_REQUEST);
        RequestContextHolder.setRequestAttributes(attrs);
    }
}
