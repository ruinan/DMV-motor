package com.dmvmotor.api.authaccess.controller;

import com.dmvmotor.api.authaccess.auth.WeChatAuthService;
import com.dmvmotor.api.authaccess.auth.WeChatAuthService.LoginMethods;
import com.dmvmotor.api.authaccess.auth.WeChatLoginOutcome;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import com.dmvmotor.api.common.CurrentUser;
import com.dmvmotor.api.common.MfaGuard;
import com.dmvmotor.api.common.ReauthGuard;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * WeChat mini-program login. Public (the WeChat {@code code} is itself the bot
 * barrier). Returns a Firebase custom token the client exchanges for a normal
 * Firebase ID token, after which every other endpoint authenticates it unchanged.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class WeChatAuthController {

    private final WeChatAuthService service;
    private final MfaGuard    mfaGuard;
    private final ReauthGuard reauthGuard;

    public WeChatAuthController(WeChatAuthService service, MfaGuard mfaGuard, ReauthGuard reauthGuard) {
        this.service     = service;
        this.mfaGuard    = mfaGuard;
        this.reauthGuard = reauthGuard;
    }

    /** Body: {@code {"code": "<wx.login code>", "email": "<required for new users>"}}. */
    public record WeChatLoginRequest(String code, String email) {}

    /** Body: {@code {"code": "<wx.login code>"}} — the WeChat account to bind. */
    public record LinkRequest(String code) {}

    @PostMapping("/wechat")
    public ApiResponse<?> wechat(@RequestBody(required = false) WeChatLoginRequest req) {
        if (req == null || req.code() == null || req.code().isBlank()) {
            throw new BusinessException("INVALID_CODE",
                    "A WeChat login code is required", HttpStatus.BAD_REQUEST);
        }
        WeChatLoginOutcome outcome = service.login(req.code(), req.email());
        return switch (outcome.status()) {
            case AUTHENTICATED -> ApiResponse.ok(Map.of("firebase_token", outcome.firebaseToken()));
            case EMAIL_REQUIRED -> throw new BusinessException("EMAIL_REQUIRED",
                    "An email is required to create a WeChat account",
                    HttpStatus.UNPROCESSABLE_ENTITY);
            case LOGIN_REQUIRED -> throw new BusinessException("EMAIL_IN_USE",
                    "This email already has an account — sign in to link WeChat",
                    HttpStatus.CONFLICT);
        };
    }

    /** Which login methods exist for an email (UX hint). Public. */
    @GetMapping("/methods")
    public ApiResponse<?> methods(@RequestParam String email) {
        LoginMethods m = service.methods(email);
        return ApiResponse.ok(Map.of("password", m.password(), "wechat", m.wechat()));
    }

    /** Bind a WeChat account to the signed-in account. Credential change → guarded
     *  like billing / change-password (MFA-verified + recently re-authed session). */
    @PostMapping("/wechat/link")
    public ApiResponse<?> link(@CurrentUser Long userId, @RequestBody(required = false) LinkRequest req) {
        requireAuth(userId);
        mfaGuard.requireMfa();
        reauthGuard.requireRecentReauth();
        if (req == null || req.code() == null || req.code().isBlank()) {
            throw new BusinessException("INVALID_CODE",
                    "A WeChat login code is required", HttpStatus.BAD_REQUEST);
        }
        service.link(userId, req.code());
        return ApiResponse.ok(Map.of("linked", true));
    }

    /** Unbind WeChat from the signed-in account. Same guards as linking. */
    @DeleteMapping("/wechat/link")
    public ApiResponse<?> unlink(@CurrentUser Long userId) {
        requireAuth(userId);
        mfaGuard.requireMfa();
        reauthGuard.requireRecentReauth();
        return ApiResponse.ok(Map.of("unlinked", service.unlink(userId)));
    }

    private static void requireAuth(Long userId) {
        if (userId == null) {
            throw new BusinessException("UNAUTHORIZED",
                    "Authentication required", HttpStatus.UNAUTHORIZED);
        }
    }
}
