package com.dmvmotor.api.dev;

import com.dmvmotor.api.authaccess.infrastructure.AccessRepository;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Dev-only backdoor endpoints. Gated by {@code app.dev.endpoints=true} so the
 * controller bean is not registered at all when the property is absent (which
 * is the default). PROD MUST NEVER set this property — Terraform / Cloud Run
 * env wiring deliberately omits it, and a Postgres-side WARN log surfaces if
 * someone enables it by mistake.
 *
 * <p>Use case today: grant the authenticated user a 30-day access pass so they
 * can exercise the paid surfaces (full practice + mock exam) before the real
 * checkout / billing flow exists.
 *
 * <p>Defense in depth: {@code @Profile("!prod")} means that even if the
 * {@code app.dev.endpoints} flag were ever set in production by mistake, the
 * bean still would not register under the prod profile.
 */
@RestController
@RequestMapping("/api/v1/dev")
@Profile("!prod")
@ConditionalOnProperty(name = "app.dev.endpoints", havingValue = "true")
public class DevController {

    private static final Logger LOG = LoggerFactory.getLogger(DevController.class);

    private final AccessRepository accessRepo;
    private final com.dmvmotor.api.content.application.ExamContext examContext;
    private final com.dmvmotor.api.common.ReauthGuard reauthGuard;
    private final com.dmvmotor.api.common.MfaGuard mfaGuard;
    private final com.dmvmotor.api.authaccess.application.RedemptionService redemptionService;

    public DevController(AccessRepository accessRepo,
                         com.dmvmotor.api.content.application.ExamContext examContext,
                         com.dmvmotor.api.common.ReauthGuard reauthGuard,
                         com.dmvmotor.api.common.MfaGuard mfaGuard,
                         com.dmvmotor.api.authaccess.application.RedemptionService redemptionService) {
        this.accessRepo  = accessRepo;
        this.examContext = examContext;
        this.reauthGuard = reauthGuard;
        this.mfaGuard    = mfaGuard;
        this.redemptionService = redemptionService;
    }

    @PostConstruct
    void warnOnStartup() {
        LOG.warn("⚠️  Dev backdoor endpoints are ENABLED — /api/v1/dev/*. "
                + "DO NOT enable in production.");
    }

    /**
     * Grants the authenticated user a 30-day active pass with 5 mock-exam
     * attempts. Per-exam now (V32 subscription model): scoped to {@code exam_id}
     * if given, else the user's current exam — so you can test per-exam unlocking
     * (grant on CA-C, switch to M1 → still locked). Calling twice creates two
     * passes; the selection logic picks the active one with the latest expires_at.
     */
    @PostMapping("/grant-pass")
    public ApiResponse<?> grantPass(@CurrentUser Long userId,
                                    @RequestParam(required = false) Long exam_id) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED",
                    "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        mfaGuard.requireMfa();             // subscription change → 2FA-verified session
        reauthGuard.requireRecentReauth(); // subscription change → recent password proof
        Long examId = exam_id != null ? exam_id : examContext.resolveExamId(userId);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expires = now.plusDays(30);
        Long passId = accessRepo.insertActivePass(userId, examId, now, expires, 5);
        LOG.info("[dev] granted access pass {} to user {} for exam {}", passId, userId, examId);
        return ApiResponse.ok(Map.of(
                "pass_id",    String.valueOf(passId),
                "user_id",    String.valueOf(userId),
                "exam_id",    String.valueOf(examId),
                "expires_at", expires.toString(),
                "mock_quota", 5
        ));
    }

    /**
     * Unsubscribe (dev): cancels the user's active pass(es) for {@code exam_id}
     * (or the current exam when omitted) so the per-exam paywall re-engages.
     * Mirrors {@link #grantPass} — lets us exercise the subscribe → unsubscribe
     * → free-trial loop before the real billing flow exists.
     */
    @PostMapping("/revoke-pass")
    public ApiResponse<?> revokePass(@CurrentUser Long userId,
                                     @RequestParam(required = false) Long exam_id) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED",
                    "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        mfaGuard.requireMfa();             // subscription change → 2FA-verified session
        reauthGuard.requireRecentReauth(); // subscription change → recent password proof
        Long examId = exam_id != null ? exam_id : examContext.resolveExamId(userId);
        int cancelled = accessRepo.cancelActivePasses(userId, examId);
        LOG.info("[dev] revoked {} pass(es) for user {} exam {}", cancelled, userId, examId);
        return ApiResponse.ok(Map.of(
                "user_id",   String.valueOf(userId),
                "exam_id",   String.valueOf(examId),
                "cancelled", cancelled
        ));
    }

    private static final int MAX_MINT_COUNT = 100;

    /**
     * Mints one or more unguessable activation codes (SecureRandom) — the issuing
     * tool for the redeem flow, replacing hand-typed founder codes. Dev-only;
     * prod issues codes via a migration since its DB is private-IP. {@code exam_id}
     * omitted = a code that redeems against the redeemer's current exam.
     */
    @PostMapping("/redemption-codes")
    public ApiResponse<?> mintCodes(@CurrentUser Long userId,
                                    @RequestParam(required = false) Long exam_id,
                                    @RequestParam(defaultValue = "30") int duration_days,
                                    @RequestParam(defaultValue = "5")  int mock_quota,
                                    @RequestParam(defaultValue = "1")  int max_redemptions,
                                    @RequestParam(defaultValue = "1")  int count) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED",
                    "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        if (count < 1 || count > MAX_MINT_COUNT) {
            throw new BusinessException("INVALID_COUNT",
                    "count must be between 1 and " + MAX_MINT_COUNT, HttpStatus.BAD_REQUEST);
        }
        java.util.List<String> codes = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            codes.add(redemptionService.createCode(exam_id, duration_days, mock_quota, max_redemptions));
        }
        LOG.info("[dev] minted {} activation code(s) for exam {}", count, exam_id);
        return ApiResponse.ok(Map.of("codes", codes));
    }
}
