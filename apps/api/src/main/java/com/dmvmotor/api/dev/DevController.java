package com.dmvmotor.api.dev;

import com.dmvmotor.api.authaccess.infrastructure.AccessRepository;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
 */
@RestController
@RequestMapping("/api/v1/dev")
@ConditionalOnProperty(name = "app.dev.endpoints", havingValue = "true")
public class DevController {

    private static final Logger LOG = LoggerFactory.getLogger(DevController.class);

    private final AccessRepository accessRepo;

    public DevController(AccessRepository accessRepo) {
        this.accessRepo = accessRepo;
    }

    @PostConstruct
    void warnOnStartup() {
        LOG.warn("⚠️  Dev backdoor endpoints are ENABLED — /api/v1/dev/*. "
                + "DO NOT enable in production.");
    }

    /**
     * Grants the authenticated user a 30-day active pass with 5 mock-exam
     * attempts available. Idempotent only in the sense that calling twice
     * creates two passes; the AccessRepository selection logic picks the
     * currently-active one with the latest expires_at.
     */
    @PostMapping("/grant-pass")
    public ApiResponse<?> grantPass(@CurrentUser Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED",
                    "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expires = now.plusDays(30);
        Long passId = accessRepo.insertActivePass(userId, now, expires, 5);
        LOG.info("[dev] granted access pass {} to user {}", passId, userId);
        return ApiResponse.ok(Map.of(
                "pass_id",    String.valueOf(passId),
                "user_id",    String.valueOf(userId),
                "expires_at", expires.toString(),
                "mock_quota", 5
        ));
    }
}
