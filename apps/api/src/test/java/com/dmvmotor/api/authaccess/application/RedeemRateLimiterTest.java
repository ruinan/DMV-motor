package com.dmvmotor.api.authaccess.application;

import com.dmvmotor.api.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sliding-window redeem throttle — caps, per-user isolation, window slide. */
class RedeemRateLimiterTest {

    private static RedeemRateLimiter limiter(int max, long windowSeconds, Instant at) {
        RedeemRateLimiter l = new RedeemRateLimiter(max, windowSeconds);
        l.setClock(Clock.fixed(at, ZoneOffset.UTC));
        return l;
    }

    @Test
    void allowsUpToMax_thenThrottles() {
        RedeemRateLimiter l = limiter(3, 300, Instant.parse("2026-06-25T00:00:00Z"));
        l.record(1L);
        l.record(1L);
        assertThatCode(() -> l.record(1L)).doesNotThrowAnyException();   // 3rd ok
        assertThatThrownBy(() -> l.record(1L))                          // 4th blocked
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("REDEEM_THROTTLED");
    }

    @Test
    void isPerUser() {
        RedeemRateLimiter l = limiter(1, 300, Instant.parse("2026-06-25T00:00:00Z"));
        l.record(1L);                                                    // user 1 maxed
        assertThatCode(() -> l.record(2L)).doesNotThrowAnyException();   // user 2 unaffected
    }

    @Test
    void windowSlides_allowsAgainAfterExpiry() {
        Instant t0 = Instant.parse("2026-06-25T00:00:00Z");
        RedeemRateLimiter l = limiter(1, 300, t0);
        l.record(1L);
        assertThatThrownBy(() -> l.record(1L)).isInstanceOf(BusinessException.class);

        l.setClock(Clock.fixed(t0.plusSeconds(301), ZoneOffset.UTC));    // window elapsed
        assertThatCode(() -> l.record(1L)).doesNotThrowAnyException();
    }

    @Test
    void clear_resetsAllWindows() {
        RedeemRateLimiter l = limiter(1, 300, Instant.parse("2026-06-25T00:00:00Z"));
        l.record(1L);
        l.clear();
        assertThatCode(() -> l.record(1L)).doesNotThrowAnyException();
    }
}
