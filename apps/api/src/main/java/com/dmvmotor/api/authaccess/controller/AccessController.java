package com.dmvmotor.api.authaccess.controller;

import com.dmvmotor.api.authaccess.application.AccessService;
import com.dmvmotor.api.authaccess.application.AccessService.AccessInfo;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/access")
public class AccessController {

    private final AccessService accessService;

    public AccessController(AccessService accessService) {
        this.accessService = accessService;
    }

    @GetMapping
    public ApiResponse<?> getAccess(@CurrentUser Long userId) {
        AccessInfo info = accessService.getAccess(userId);
        return ApiResponse.ok(Map.of(
                "state",         info.state(),
                "hasActivePass", info.hasActivePass(),
                "mockRemaining", info.mockRemaining(),
                "canUseReview",  info.canUseReview(),
                "canUseMockExam", info.canUseMockExam()
        ));
    }
}
