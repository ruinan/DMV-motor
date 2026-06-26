package com.dmvmotor.api.authaccess.application;

import com.dmvmotor.api.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user sliding-window rate limit on activation-code redemption attempts.
 * High-entropy codes ({@link RedemptionCodeGenerator}) already make guessing
 * infeasible; this is defense-in-depth against an authenticated user
 * machine-gunning the redeem endpoint to fish for a valid code.
 *
 * <p>In-memory and per-instance (good enough — the entropy is the real defense;
 * each Cloud Run instance independently caps a given user). Keyed by user id, so
 * one user's attempts never affect another's.
 */
@Component
public class RedeemRateLimiter {

    private final int      maxAttempts;
    private final Duration window;
    private final Map<Long, Deque<Instant>> attempts = new ConcurrentHashMap<>();
    private volatile Clock clock = Clock.systemUTC();

    public RedeemRateLimiter(
            @Value("${app.redeem.max-attempts:10}")   int  maxAttempts,
            @Value("${app.redeem.window-seconds:300}") long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.window      = Duration.ofSeconds(windowSeconds);
    }

    /**
     * Records a redemption attempt for {@code userId} and throws 429 if they have
     * exceeded the allowed attempts within the rolling window. Counts every
     * attempt (success or failure) — a legitimate user redeems a code or two,
     * well under the cap.
     */
    public void record(Long userId) {
        Instant now    = clock.instant();
        Instant cutoff = now.minus(window);
        Deque<Instant> dq = attempts.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (dq) {
            while (!dq.isEmpty() && dq.peekFirst().isBefore(cutoff)) {
                dq.pollFirst();
            }
            if (dq.size() >= maxAttempts) {
                throw new BusinessException("REDEEM_THROTTLED",
                        "Too many activation attempts — please wait a few minutes and try again.",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
            dq.addLast(now);
        }
    }

    /** Drops all tracked attempts (ops/test reset of the in-memory window). */
    public void clear() { attempts.clear(); }

    /** Test seam — override the clock to exercise window expiry deterministically. */
    void setClock(Clock clock) { this.clock = clock; }
}
