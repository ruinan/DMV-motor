package com.dmvmotor.api.authaccess.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** WeChat is "configured" only when both appid and secret are present. */
class WeChatPropertiesTest {

    @Test
    void configured_onlyWhenBothPresent() {
        assertThat(new WeChatProperties("appid", "secret").isConfigured()).isTrue();
        assertThat(new WeChatProperties("appid", " ").isConfigured()).isFalse();
        assertThat(new WeChatProperties("", "secret").isConfigured()).isFalse();
        assertThat(new WeChatProperties("", "").isConfigured()).isFalse();
    }
}
