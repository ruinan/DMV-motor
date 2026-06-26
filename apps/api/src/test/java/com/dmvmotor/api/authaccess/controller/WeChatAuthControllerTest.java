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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void link_thenUnlink_togglesWeChatForAccount() throws Exception {
        Long userId = fixtures.insertUser("binder@example.com");

        mockMvc.perform(post("/api/v1/auth/wechat/link").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userId)
                        .content(body("{\"code\":\"bindcode\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linked").value(true));

        mockMvc.perform(get("/api/v1/auth/methods").param("email", "binder@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").value(true))   // email account
                .andExpect(jsonPath("$.data.wechat").value(true));    // now linked

        mockMvc.perform(delete("/api/v1/auth/wechat/link")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unlinked").value(1));

        mockMvc.perform(get("/api/v1/auth/methods").param("email", "binder@example.com"))
                .andExpect(jsonPath("$.data.wechat").value(false));
    }

    @Test
    void link_openidAlreadyLinkedToAnother_returns409() throws Exception {
        Long userA = fixtures.insertUser("a@example.com");
        Long userB = fixtures.insertUser("b@example.com");

        mockMvc.perform(post("/api/v1/auth/wechat/link").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userA)
                        .content(body("{\"code\":\"shared\"}")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/wechat/link").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userB)
                        .content(body("{\"code\":\"shared\"}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("WECHAT_ALREADY_LINKED"));
    }

    @Test
    void link_authedWithoutCode_returns400() throws Exception {
        Long userId = fixtures.insertUser("nocode@example.com");
        mockMvc.perform(post("/api/v1/auth/wechat/link").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userId)
                        .content(body("{\"code\":\"  \"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CODE"));
    }

    @Test
    void link_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/wechat/link").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"code\":\"x\"}")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void link_staleSession_requiresReauth() throws Exception {
        Long userId = fixtures.insertUser("stale@example.com");
        mockMvc.perform(post("/api/v1/auth/wechat/link").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userId + "~1000000000")
                        .content(body("{\"code\":\"x\"}")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("REAUTH_REQUIRED"));
    }

    @Test
    void methods_wechatOnlyAccount_reportsWechatNotPassword() throws Exception {
        // A WeChat-created account (firebase_uid wx_...) → wechat true, password false.
        mockMvc.perform(post("/api/v1/auth/wechat").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"code\":\"solo\",\"email\":\"solo@example.com\"}")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/methods").param("email", "solo@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").value(false))
                .andExpect(jsonPath("$.data.wechat").value(true));
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
