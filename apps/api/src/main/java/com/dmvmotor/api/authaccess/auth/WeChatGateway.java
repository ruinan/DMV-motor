package com.dmvmotor.api.authaccess.auth;

/**
 * Thin seam over the WeChat {@code code2session} API so the WeChat login LOGIC
 * stays unit-testable with a fake. The real implementation
 * ({@code WeChatGatewayImpl}) is the only place that talks to WeChat over HTTP
 * and is excluded from coverage — like {@code StripeGatewayImpl} /
 * {@code FirebaseIdTokenVerifier}.
 */
public interface WeChatGateway {

    /**
     * Exchanges a mini-program {@code wx.login()} code for the user's identity.
     * Throws a {@code BusinessException} (401) on an invalid/expired code.
     */
    WeChatSession codeToSession(String code);

    /** Minimal projection of a {@code code2session} result. {@code unionid} is
     *  null unless the mini-program is bound to a WeChat Open Platform account. */
    record WeChatSession(String openid, String unionid) {}
}
