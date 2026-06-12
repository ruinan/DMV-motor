package com.dmvmotor.api.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Instant;

/**
 * Server-side re-authentication gate for sensitive actions (billing changes,
 * later password change). The {@link UserIdResolver} stashes the verified token's
 * {@code auth_time} on the request; this guard requires it to be recent — within
 * {@code app.auth.reauth-window-seconds}. A stale session is rejected with
 * {@code REAUTH_REQUIRED} so the client prompts for the password, calls Firebase
 * {@code reauthenticateWithCredential} (which refreshes {@code auth_time}), and
 * retries. The check lives on the BACKEND so it can't be bypassed client-side.
 */
@Component
public class ReauthGuard {

    /** Request attribute holding the verified token's auth_time (epoch seconds). */
    public static final String AUTH_TIME_ATTR = "dmv.auth.authTime";

    private final long windowSeconds;
    /** Master switch. Off (e.g. local dev against the Auth emulator) makes the
     *  guard a no-op so sensitive actions don't need a password re-prompt that's
     *  awkward to satisfy locally. Defaults ON — prod always enforces it. */
    private final boolean enabled;

    public ReauthGuard(@Value("${app.auth.reauth-window-seconds:300}") long windowSeconds,
                       @Value("${app.auth.reauth-enabled:true}") boolean enabled) {
        this.windowSeconds = windowSeconds;
        this.enabled = enabled;
    }

    public void requireRecentReauth() {
        if (!enabled) return;
        Long authTime = currentAuthTime();
        long now = Instant.now().getEpochSecond();
        if (authTime == null || now - authTime > windowSeconds) {
            throw new BusinessException("REAUTH_REQUIRED",
                    "Please re-enter your password to continue", HttpStatus.FORBIDDEN);
        }
    }

    private static Long currentAuthTime() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra == null) return null;
        Object v = ra.getAttribute(AUTH_TIME_ATTR, RequestAttributes.SCOPE_REQUEST);
        return v instanceof Long l ? l : null;
    }
}
