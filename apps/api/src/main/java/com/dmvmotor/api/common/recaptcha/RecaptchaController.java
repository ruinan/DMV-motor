package com.dmvmotor.api.common.recaptcha;

import com.dmvmotor.api.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public reCAPTCHA precheck for register / login. Those flows go client → Firebase
 * directly (not through our backend), so the strongest we can do server-side is a
 * precheck: the client verifies its token here BEFORE calling Firebase, and only
 * proceeds on success. Backend-enforced sensitive actions (subscription) verify
 * reCAPTCHA at the action itself via {@link RecaptchaGuard}.
 *
 * <p>No-op (always ok) when reCAPTCHA is disabled, so local dev / tests work.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class RecaptchaController {

    private final RecaptchaGuard guard;

    public RecaptchaController(RecaptchaGuard guard) {
        this.guard = guard;
    }

    @PostMapping("/recaptcha-verify")
    public ApiResponse<?> verify(@RequestParam(required = false, defaultValue = "submit") String action) {
        guard.requireHuman(action); // reads X-Recaptcha-Token; throws RECAPTCHA_* on failure
        return ApiResponse.ok(Map.of("ok", true));
    }
}
