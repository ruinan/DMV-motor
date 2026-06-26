package com.dmvmotor.api.authaccess;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import com.dmvmotor.api.authaccess.application.RedeemRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Activation-code redemption (V37): a valid code grants a per-exam access pass
 * (the gift / promo / offline-activation alternative to paid checkout). Covers
 * the happy path + the four rejection cases + the per-user redeem throttle.
 */
@TestPropertySource(properties = "app.redeem.max-attempts=3")
class RedeemControllerTest extends IntegrationTestBase {

    @Autowired MockMvc           mockMvc;
    @Autowired TestFixtures      fixtures;
    @Autowired RedeemRateLimiter rateLimiter;

    private Long userId;
    private Long examId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        rateLimiter.clear();   // in-memory limiter is a singleton — reset between tests
        userId = fixtures.insertUser("redeem@example.com");
        examId = fixtures.defaultExamId();
        fixtures.setUserCurrentExam(userId, examId);
    }

    @Test
    void validCode_grantsActivePassAndEntitlement() throws Exception {
        fixtures.insertRedemptionCode("M1-TEST-CODE", examId, 1);

        mockMvc.perform(post("/api/v1/access/redeem")
                        .param("code", "m1-test-code")               // case-insensitive
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam_id").value(String.valueOf(examId)))
                .andExpect(jsonPath("$.data.mock_quota").value(5));

        mockMvc.perform(get("/api/v1/access").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.has_active_pass").value(true));
        mockMvc.perform(get("/api/v1/exams/entitlements").header("Authorization", "Bearer " + userId))
                .andExpect(jsonPath("$.data.entitlements[?(@.exam_id=='%d')].subscribed"
                        .formatted(examId)).value(hasItem(true)));
    }

    @Test
    void sameCodeTwice_sameUser_isRejected() throws Exception {
        fixtures.insertRedemptionCode("ONCE-PER-USER", examId, 100);

        mockMvc.perform(post("/api/v1/access/redeem")
                        .param("code", "ONCE-PER-USER")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/access/redeem")
                        .param("code", "ONCE-PER-USER")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_REDEEMED"));
    }

    @Test
    void unknownCode_isNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/access/redeem")
                        .param("code", "NO-SUCH-CODE")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INVALID_CODE"));
    }

    @Test
    void exhaustedCode_isRejected() throws Exception {
        // Cap of 1, already redeemed by another user → no slots left.
        Long codeId = fixtures.insertRedemptionCode("ONE-SHOT", examId, 1);
        Long otherUser = fixtures.insertUser("other@example.com");
        mockMvc.perform(post("/api/v1/access/redeem")
                        .param("code", "ONE-SHOT")
                        .header("Authorization", "Bearer " + otherUser))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/access/redeem")
                        .param("code", "ONE-SHOT")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code").value("CODE_EXHAUSTED"));
        assert codeId != null;
    }

    @Test
    void anonymous_isUnauthorized() throws Exception {
        fixtures.insertRedemptionCode("ANON-CODE", examId, 1);
        mockMvc.perform(post("/api/v1/access/redeem").param("code", "ANON-CODE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tooManyAttempts_areThrottled() throws Exception {
        // max-attempts=3 (property above): the first three attempts are allowed
        // (here all unknown → 404), the fourth is throttled before any lookup.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/access/redeem")
                            .param("code", "GUESS-" + i)
                            .header("Authorization", "Bearer " + userId))
                    .andExpect(status().isNotFound());
        }
        mockMvc.perform(post("/api/v1/access/redeem")
                        .param("code", "GUESS-4")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("REDEEM_THROTTLED"));
    }
}
