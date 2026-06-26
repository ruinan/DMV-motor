package com.dmvmotor.api.authaccess.controller;

import com.dmvmotor.api.authaccess.auth.WeChatAuthService;
import com.dmvmotor.api.authaccess.auth.WeChatLoginOutcome;
import com.dmvmotor.api.common.ApiResponse;
import com.dmvmotor.api.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public WeChatAuthController(WeChatAuthService service) {
        this.service = service;
    }

    /** Body: {@code {"code": "<wx.login code>", "email": "<required for new users>"}}. */
    public record WeChatLoginRequest(String code, String email) {}

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
}
