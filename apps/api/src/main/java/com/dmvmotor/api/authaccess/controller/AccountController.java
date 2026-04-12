package com.dmvmotor.api.authaccess.controller;

import com.dmvmotor.api.authaccess.application.AccountService;
import com.dmvmotor.api.authaccess.application.AccountService.MeResult;
import com.dmvmotor.api.authaccess.application.AccessService.AccessInfo;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResponse<?> getMe(@CurrentUser Long userId) {
        requireAuth(userId);
        MeResult me = accountService.getMe(userId);
        return ApiResponse.ok(toDto(me));
    }

    @PutMapping("/language")
    public ApiResponse<?> updateLanguage(@CurrentUser Long userId,
                                          @Valid @RequestBody LanguageRequest req) {
        requireAuth(userId);
        String updated = accountService.updateLanguage(userId, req.language());
        return ApiResponse.ok(Map.of("language", updated));
    }

    @PostMapping("/reset-learning")
    public ApiResponse<?> resetLearning(@CurrentUser Long userId,
                                         @Valid @RequestBody ResetRequest req) {
        requireAuth(userId);
        accountService.resetLearning(userId);
        return ApiResponse.ok(Map.of("reset", true));
    }

    // ---------------------------------------------------------------
    // DTOs
    // ---------------------------------------------------------------

    record LanguageRequest(
            @NotNull(message = "must not be null")
            @Pattern(regexp = "^(en|zh)$", message = "must be en or zh")
            String language
    ) {}

    record ResetRequest(
            @NotNull(message = "must not be null")
            Boolean confirm
    ) {
        @jakarta.validation.constraints.AssertTrue(message = "must be true")
        public boolean isConfirm() { return Boolean.TRUE.equals(confirm); }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void requireAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED", "Authentication required",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    private Map<String, Object> toDto(MeResult me) {
        AccessInfo a = me.access();

        // expires_at: null for free_trial/no-pass, ISO string for active/expired passes
        Object expiresAt = a.expiresAt() != null ? a.expiresAt().toString() : null;

        Map<String, Object> access = new HashMap<>();
        access.put("state",           a.state());
        access.put("has_active_pass", a.hasActivePass());
        access.put("mock_remaining",  a.mockRemaining());
        access.put("expires_at",      expiresAt);

        Map<String, Object> learning = new HashMap<>();
        learning.put("has_in_progress_practice", me.hasInProgressPractice());
        learning.put("has_in_progress_review",   me.hasInProgressReview());

        Map<String, Object> dto = new HashMap<>();
        dto.put("user_id",  String.valueOf(me.userId()));
        dto.put("email",    me.email() != null ? me.email() : "");
        dto.put("language", me.language());
        dto.put("access",   access);
        dto.put("learning", learning);
        return dto;
    }
}
