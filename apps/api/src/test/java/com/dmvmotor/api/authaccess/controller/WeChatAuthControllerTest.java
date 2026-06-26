package com.dmvmotor.api.authaccess.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import com.dmvmotor.api.authaccess.auth.WeChatGateway;
import com.dmvmotor.api.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WeChat login endpoint with a FAKE gateway (code → openid; "BADCODE" → invalid).
 * The stub token minter (firebase disabled in tests) returns a deterministic
 * "stub-custom-token:&lt;uid&gt;" so we can assert which account was minted for.
 */
@Import(WeChatAuthControllerTest.FakeWeChatConfig.class)
class WeChatAuthControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
    }

    private static String body(String json) {
        return json;
    }

    @Test
    void newUserWithEmail_authenticates_andReturningLoginResolvesSameAccount() throws Exception {
        // New WeChat user + new email → account created (firebase_uid = wx_openid-good).
        mockMvc.perform(post("/api/v1/auth/wechat").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"code\":\"good\",\"email\":\"wxuser@example.com\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firebase_token").value("stub-custom-token:wx_openid-good"));

        // Returning login (same code → same openid, no email needed) → SAME account.
        mockMvc.perform(post("/api/v1/auth/wechat").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"code\":\"good\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firebase_token").value("stub-custom-token:wx_openid-good"));
    }

    @Test
    void newUserWithoutEmail_returns422() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"code\":\"fresh\"}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("EMAIL_REQUIRED"));
    }

    @Test
    void emailAlreadyHasAccount_returns409() throws Exception {
        fixtures.insertUser("dupe@example.com");

        mockMvc.perform(post("/api/v1/auth/wechat").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"code\":\"another\",\"email\":\"dupe@example.com\"}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_IN_USE"));
    }

    @Test
    void invalidCode_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"code\":\"BADCODE\",\"email\":\"x@example.com\"}")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("WECHAT_CODE_INVALID"));
    }

    @Test
    void missingCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"email\":\"x@example.com\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CODE"));
    }

    @Test
    void blankCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"code\":\"   \",\"email\":\"x@example.com\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CODE"));
    }

    @Test
    void noBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CODE"));
    }

    // ---------------------------------------------------------------

    @TestConfiguration
    static class FakeWeChatConfig {
        @Bean
        @Primary
        WeChatGateway fakeWeChatGateway() {
            return new FakeWeChatGateway();
        }
    }

    static class FakeWeChatGateway implements WeChatGateway {
        @Override
        public WeChatSession codeToSession(String code) {
            if ("BADCODE".equals(code)) {
                throw new BusinessException("WECHAT_CODE_INVALID",
                        "Invalid or expired WeChat login code", HttpStatus.UNAUTHORIZED);
            }
            return new WeChatSession("openid-" + code, null);
        }
    }
}
