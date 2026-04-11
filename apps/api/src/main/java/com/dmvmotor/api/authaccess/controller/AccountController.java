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
        return Map.of(
                "userId",   String.valueOf(me.userId()),
                "email",    me.email() != null ? me.email() : "",
                "language", me.language(),
                "access", Map.of(
                        "state",        a.state(),
                        "hasActivePass", a.hasActivePass(),
                        "mockRemaining", a.mockRemaining(),
                        "expiresAt",    ""
                ),
                "learning", Map.of(
                        "hasInProgressPractice", me.hasInProgressPractice(),
                        "hasInProgressReview",   false   // TODO: implement when review module ships
                )
        );
    }
}
