package com.dmvmotor.api.billing.controller;

import com.dmvmotor.api.billing.application.BillingService;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.common.ReauthGuard;
import com.dmvmotor.api.common.recaptcha.RecaptchaGuard;
import com.dmvmotor.api.content.application.ExamContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Stripe billing endpoints. Subscribe / unsubscribe are authed and operate on
 * the caller's chosen exam; the webhook is public (Stripe-authenticated by its
 * signature, verified in the gateway).
 */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService service;
    private final ExamContext    examContext;
    private final ReauthGuard    reauthGuard;
    private final RecaptchaGuard recaptchaGuard;

    public BillingController(BillingService service, ExamContext examContext,
                            ReauthGuard reauthGuard, RecaptchaGuard recaptchaGuard) {
        this.service        = service;
        this.examContext    = examContext;
        this.reauthGuard    = reauthGuard;
        this.recaptchaGuard = recaptchaGuard;
    }

    /** Whether Stripe is wired (a secret key is configured). Public — the catalog
     *  uses it to choose real checkout vs the dev grant fallback. */
    @GetMapping("/config")
    public ApiResponse<?> config() {
        return ApiResponse.ok(Map.of("enabled", service.enabled()));
    }

    /** Subscribe: returns the hosted Checkout URL to redirect to. */
    @PostMapping("/checkout-session")
    public ApiResponse<?> checkout(@CurrentUser Long userId,
                                   @RequestParam(name = "exam_id", required = false) Long examId) {
        requireAuth(userId);
        recaptchaGuard.requireHuman("subscribe");   // bot check
        reauthGuard.requireRecentReauth();          // billing change → recent password proof
        Long resolved = examId != null ? examId : examContext.resolveExamId(userId);
        return ApiResponse.ok(Map.of("url", service.createCheckoutSession(userId, resolved)));
    }

    /** Unsubscribe: cancels the Stripe subscription for the exam. */
    @PostMapping("/cancel")
    public ApiResponse<?> cancel(@CurrentUser Long userId,
                                 @RequestParam(name = "exam_id", required = false) Long examId) {
        requireAuth(userId);
        recaptchaGuard.requireHuman("unsubscribe"); // bot check
        reauthGuard.requireRecentReauth();          // billing change → recent password proof
        Long resolved = examId != null ? examId : examContext.resolveExamId(userId);
        service.cancelSubscription(userId, resolved);
        return ApiResponse.ok(Map.of("cancelled", true));
    }

    /**
     * Stripe webhook (public). The raw body + {@code Stripe-Signature} header are
     * verified in the gateway; an invalid signature throws and returns non-2xx so
     * Stripe retries. Returns a plain 200 — Stripe doesn't read our envelope.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(HttpServletRequest request,
                                          @RequestHeader(name = "Stripe-Signature", required = false) String signature)
            throws IOException {
        // Read the EXACT raw bytes — Stripe's signature is over the unmodified body.
        String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        service.handleWebhook(payload, signature);
        return ResponseEntity.ok("ok");
    }

    private static void requireAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
    }
}
