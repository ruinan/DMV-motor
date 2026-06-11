package com.dmvmotor.api.authaccess.controller;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.authaccess.application.AccessService.AccessInfo;
import com.dmvmotor.api.authaccess.application.RedemptionService;
import com.dmvmotor.api.authaccess.application.RedemptionService.RedeemResult;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/access")
public class AccessController {

    private final AccessService accessService;
    private final RedemptionService redemptionService;

    public AccessController(AccessService accessService,
                           RedemptionService redemptionService) {
        this.accessService = accessService;
        this.redemptionService = redemptionService;
    }

    @GetMapping
    public ApiResponse<?> getAccess(@CurrentUser Long userId) {
        AccessInfo info = accessService.getAccess(userId);
        return ApiResponse.ok(Map.of(
                "state",            info.state(),
                "has_active_pass",  info.hasActivePass(),
                "mock_remaining",   info.mockRemaining(),
                "can_use_review",   info.canUseReview(),
                "can_use_mock_exam", info.canUseMockExam()
        ));
    }

    /**
     * Redeem an activation code → grants an access pass for the code's exam (or
     * the user's current exam for an exam-agnostic code). One redemption per
     * (code, user). An alternative to paid checkout.
     */
    @PostMapping("/redeem")
    public ApiResponse<?> redeem(@CurrentUser Long userId,
                                 @RequestParam String code,
                                 @RequestParam(required = false) Long exam_id) {
        RedeemResult r = redemptionService.redeem(userId, code, exam_id);
        return ApiResponse.ok(Map.of(
                "exam_id",    String.valueOf(r.examId()),
                "expires_at", r.expiresAt().toString(),
                "mock_quota", r.mockQuota()
        ));
    }
}
