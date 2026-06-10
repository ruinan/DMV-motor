package com.dmvmotor.api.dev;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Backend MFA enforcement on a sensitive endpoint, with {@code app.auth.mfa-required=true}.
 * A session that did NOT complete a second factor (the {@code "!nomfa"} stub suffix)
 * is rejected server-side even though the frontend enrollment gate could be bypassed;
 * a normal session (stub defaults to {@code "totp"}) passes the MFA check.
 */
@TestPropertySource(properties = {
        "app.dev.endpoints=true",
        "app.auth.mfa-required=true"
})
class DevControllerMfaTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;
    private Long examId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId = fixtures.insertUser("mfa@example.com");
        examId = fixtures.defaultExamId();
    }

    @Test
    void grantPass_sessionWithoutSecondFactor_returns403() throws Exception {
        // "!nomfa" → the verified token has no sign_in_second_factor.
        mockMvc.perform(post("/api/v1/dev/grant-pass")
                        .param("exam_id", String.valueOf(examId))
                        .header("Authorization", "Bearer " + userId + "!nomfa"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MFA_REQUIRED"));
    }

    @Test
    void grantPass_twoFactorSession_passesMfaGate() throws Exception {
        // Plain token → stub supplies a "totp" second factor → MFA gate satisfied.
        mockMvc.perform(post("/api/v1/dev/grant-pass")
                        .param("exam_id", String.valueOf(examId))
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam_id").value(String.valueOf(examId)));
    }
}
