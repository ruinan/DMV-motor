package com.dmvmotor.api.authaccess.auth;

/**
 * Result of a WeChat login attempt. Either the caller is authenticated (a custom
 * token is returned) or the client must take a step first:
 * <ul>
 *   <li>{@code EMAIL_REQUIRED} — a new WeChat user must supply an email
 *       (email is the universal account key).</li>
 *   <li>{@code LOGIN_REQUIRED} — the supplied email already has an account; the
 *       user must sign in to that account to link WeChat (anti-takeover).</li>
 * </ul>
 */
public record WeChatLoginOutcome(Status status, String firebaseToken) {

    public enum Status { AUTHENTICATED, EMAIL_REQUIRED, LOGIN_REQUIRED }

    public static WeChatLoginOutcome authenticated(String firebaseToken) {
        return new WeChatLoginOutcome(Status.AUTHENTICATED, firebaseToken);
    }

    public static WeChatLoginOutcome emailRequired() {
        return new WeChatLoginOutcome(Status.EMAIL_REQUIRED, null);
    }

    public static WeChatLoginOutcome loginRequired() {
        return new WeChatLoginOutcome(Status.LOGIN_REQUIRED, null);
    }
}
