package com.dmvmotor.api.authaccess.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

/**
 * WeChat mini-program credentials ({@code app.wechat.*}) for {@code code2session}.
 * Sourced from env / Secret Manager; absent in dev/test by default, so WeChat
 * login is {@link #isConfigured()} only once a real mini-program's appid+secret
 * are wired. Never commit these — they belong in Secret Manager / a gitignored
 * local config.
 */
@ConfigurationProperties(prefix = "app.wechat")
public record WeChatProperties(
        @DefaultValue("") String appid,
        @DefaultValue("") String secret
) {
    public boolean isConfigured() {
        return StringUtils.hasText(appid) && StringUtils.hasText(secret);
    }
}
