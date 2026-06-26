package com.dmvmotor.api.authaccess.application;

import com.dmvmotor.api.authaccess.infrastructure.AccessRepository;
import com.dmvmotor.api.authaccess.infrastructure.RedemptionRepository;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.content.application.ExamContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Redeems an activation code into an access pass — an alternative to paid
 * checkout (gift / launch promo / offline activation). Produces the same pass
 * the dev grant / Stripe checkout produce, so the access gate is unchanged.
 */
@Service
public class RedemptionService {

    private final RedemptionRepository codes;
    private final AccessRepository passes;
    private final ExamContext examContext;
    private final RedemptionCodeGenerator generator;
    private final RedeemRateLimiter rateLimiter;

    public RedemptionService(RedemptionRepository codes,
                             AccessRepository passes,
                             ExamContext examContext,
                             RedemptionCodeGenerator generator,
                             RedeemRateLimiter rateLimiter) {
        this.codes = codes;
        this.passes = passes;
        this.examContext = examContext;
        this.generator = generator;
        this.rateLimiter = rateLimiter;
    }

    public record RedeemResult(Long examId, OffsetDateTime expiresAt, int mockQuota) {}

    /**
     * Grants {@code userId} an access pass for the code's exam (or their current
     * exam when the code is exam-agnostic). One redemption per (code, user).
     * Transactional so a failed redemption leaves no orphan pass or claimed slot.
     */
    @Transactional
    public RedeemResult redeem(Long userId, String rawCode, Long requestedExamId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED",
                    "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        // Throttle BEFORE any lookup so a user can't machine-gun the endpoint
        // fishing for a valid code (defense-in-depth atop high-entropy codes).
        rateLimiter.record(userId);
        String code = rawCode == null ? "" : rawCode.trim();
        if (code.isEmpty()) {
            throw new BusinessException("INVALID_CODE",
                    "Enter an activation code.", HttpStatus.BAD_REQUEST);
        }

        var row = codes.findActiveByCode(code);
        if (row == null) {
            throw new BusinessException("INVALID_CODE",
                    "That activation code isn't valid.", HttpStatus.NOT_FOUND);
        }
        if (row.expired()) {
            throw new BusinessException("CODE_EXPIRED",
                    "That activation code has expired.", HttpStatus.GONE);
        }
        if (codes.alreadyRedeemed(row.id(), userId)) {
            throw new BusinessException("ALREADY_REDEEMED",
                    "You've already redeemed this code.", HttpStatus.CONFLICT);
        }

        Long examId = row.examId() != null
                ? row.examId()
                : examContext.resolveExamId(userId, requestedExamId);

        // Claim a slot before granting so a capped code can't be over-redeemed
        // under concurrency; the (code_id, user_id) unique index below is the
        // real double-redeem guard (the check above is a fast path).
        if (!codes.claimSlot(row.id())) {
            throw new BusinessException("CODE_EXHAUSTED",
                    "That activation code has been fully redeemed.", HttpStatus.GONE);
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expires = now.plusDays(row.durationDays());
        Long passId = passes.insertActivePass(userId, examId, now, expires, row.mockQuota());
        codes.insertRedemption(row.id(), userId, examId, passId);
        return new RedeemResult(examId, expires, row.mockQuota());
    }

    private static final int MAX_CODE_GEN_ATTEMPTS = 5;

    /**
     * Mints a fresh, unguessable activation code and persists it. Retries on the
     * (astronomically unlikely) collision against the unique {@code UPPER(code)}
     * index. Returns the generated code so the caller can hand it out.
     */
    public String createCode(Long examId, int durationDays, int mockQuota, int maxRedemptions) {
        for (int attempt = 0; attempt < MAX_CODE_GEN_ATTEMPTS; attempt++) {
            String code = generator.generate();
            try {
                codes.insertCode(code, examId, durationDays, mockQuota, maxRedemptions);
                return code;
            } catch (DuplicateKeyException collision) {
                // try a different code
            }
        }
        throw new BusinessException("CODE_GENERATION_FAILED",
                "Could not generate a unique activation code — please retry.",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
