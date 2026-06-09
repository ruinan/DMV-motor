package com.dmvmotor.api.common.recaptcha;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import com.dmvmotor.api.common.recaptcha.RecaptchaVerifier.Assessment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * reCAPTCHA wired into endpoints, with reCAPTCHA ENABLED (it's disabled in every
 * other test). A fake verifier stands in for GCP: token "good" = human, anything
 * else = bot. Covers the public login/register precheck and that the gate is
 * actually applied to a sensitive action (billing checkout).
 */
@TestPropertySource(properties = {
        "app.recaptcha.enabled=true",
        "app.recaptcha.project-id=test-proj",
        "app.recaptcha.site-key=test-site",
        "app.recaptcha.min-score=0.5",
})
class RecaptchaControllerTest extends IntegrationTestBase {

    @TestConfiguration
    static class FakeVerifierConfig {
        @Bean
        @Primary
        RecaptchaVerifier fakeVerifier() {
            return (token, action) -> "good".equals(token)
                    ? new Assessment(true, 0.9, "ok")
                    : new Assessment(false, 0.1, "bad");
        }
    }

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
    }

    @Test
    void precheck_missingToken_returns403Required() throws Exception {
        mockMvc.perform(post("/api/v1/auth/recaptcha-verify").param("action", "login"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("RECAPTCHA_REQUIRED"));
    }

    @Test
    void precheck_goodToken_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/recaptcha-verify")
                        .param("action", "login")
                        .header(RecaptchaGuard.TOKEN_HEADER, "good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ok").value(true));
    }

    @Test
    void precheck_botToken_returns403Failed() throws Exception {
        mockMvc.perform(post("/api/v1/auth/recaptcha-verify")
                        .param("action", "register")
                        .header(RecaptchaGuard.TOKEN_HEADER, "bot"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("RECAPTCHA_FAILED"));
    }

    @Test
    void billingCheckout_missingToken_isGatedByRecaptcha() throws Exception {
        // The reCAPTCHA gate runs before reauth/billing, so an authed user with
        // no token is stopped at RECAPTCHA_REQUIRED — proving subscription
        // changes are bot-gated.
        Long uid = fixtures.insertUser("recaptcha_billing@example.com");
        mockMvc.perform(post("/api/v1/billing/checkout-session")
                        .header("Authorization", "Bearer " + uid))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("RECAPTCHA_REQUIRED"));
    }
}
