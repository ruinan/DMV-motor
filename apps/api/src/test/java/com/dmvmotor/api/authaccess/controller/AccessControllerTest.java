package com.dmvmotor.api.authaccess.controller;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccessControllerTest extends IntegrationTestBase {

    @Autowired MockMvc      mockMvc;
    @Autowired TestFixtures fixtures;

    private Long userId;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
        userId = fixtures.insertUser("bob@example.com");
    }

    @Test
    void getAccess_nonBearerAuthHeader_treatsAsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/access")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("free_trial"));
    }

    @Test
    void getAccess_nonNumericBearerToken_treatsAsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/access")
                        .header("Authorization", "Bearer notanumber"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("free_trial"));
    }

    @Test
    void getAccess_anonymous_returnsFreeTrial() throws Exception {
        mockMvc.perform(get("/api/v1/access"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("free_trial"))
                .andExpect(jsonPath("$.data.has_active_pass").value(false))
                .andExpect(jsonPath("$.data.mock_remaining").value(0))
                .andExpect(jsonPath("$.data.can_use_review").value(false))
                .andExpect(jsonPath("$.data.can_use_mock_exam").value(false));
    }

    @Test
    void getAccess_userWithNoPass_returnsFreeTrial() throws Exception {
        mockMvc.perform(get("/api/v1/access")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("free_trial"))
                .andExpect(jsonPath("$.data.has_active_pass").value(false));
    }

    @Test
    void getAccess_userWithActivePass_returnsActiveWithMockRemaining() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        fixtures.insertAccessPass(userId, "active",
                now.minusDays(1), now.plusDays(30), 3, 1);

        mockMvc.perform(get("/api/v1/access")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("active"))
                .andExpect(jsonPath("$.data.has_active_pass").value(true))
                .andExpect(jsonPath("$.data.mock_remaining").value(2))
                .andExpect(jsonPath("$.data.can_use_review").value(true))
                .andExpect(jsonPath("$.data.can_use_mock_exam").value(true));
    }

    @Test
    void getAccess_userWithExpiredPass_returnsExpired() throws Exception {
        OffsetDateTime past = OffsetDateTime.now().minusDays(1);
        fixtures.insertAccessPass(userId, "expired",
                past.minusDays(30), past, 3, 0);

        mockMvc.perform(get("/api/v1/access")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("expired"))
                .andExpect(jsonPath("$.data.has_active_pass").value(false))
                .andExpect(jsonPath("$.data.mock_remaining").value(0));
    }

    @Test
    void getAccess_passStatusActiveButExpiresAtPast_returnsExpired() throws Exception {
        // E1: row carries status='active' but the time window has elapsed.
        // Without server-side clock checks (current bug), this would still grant access
        // until a background job flips the row.
        OffsetDateTime now = OffsetDateTime.now();
        fixtures.insertAccessPass(userId, "active",
                now.minusDays(30), now.minusHours(1), 3, 0);

        mockMvc.perform(get("/api/v1/access")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("expired"))
                .andExpect(jsonPath("$.data.has_active_pass").value(false))
                .andExpect(jsonPath("$.data.mock_remaining").value(0))
                .andExpect(jsonPath("$.data.can_use_review").value(false))
                .andExpect(jsonPath("$.data.can_use_mock_exam").value(false));
    }

    @Test
    void getAccess_passStatusActiveButStartsInFuture_returnsExpired() throws Exception {
        // E2: future-dated pass — status='active' but window hasn't opened yet.
        OffsetDateTime now = OffsetDateTime.now();
        fixtures.insertAccessPass(userId, "active",
                now.plusHours(1), now.plusDays(30), 3, 0);

        mockMvc.perform(get("/api/v1/access")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("expired"))
                .andExpect(jsonPath("$.data.has_active_pass").value(false))
                .andExpect(jsonPath("$.data.can_use_review").value(false));
    }

    // ---------------------------------------------------------------
    // Multi-pass selection (sec audit #3a). The buggy repo prefers the
    // newest-created pass after the status='active' tie, so a future or
    // expired-but-still-active pass could mask a currently-valid one.
    // ---------------------------------------------------------------

    @Test
    void getAccess_currentPassPlusFuturePass_picksCurrentNotFuture() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        // Insert current pass FIRST so it has the older created_at — the
        // buggy ORDER BY (status DESC, created_at DESC) would pick the
        // newer future pass and report state=expired.
        fixtures.insertAccessPass(userId, "active",
                now.minusDays(5), now.plusDays(25), 3, 0);
        fixtures.insertAccessPass(userId, "active",
                now.plusDays(30), now.plusDays(60), 3, 0);

        mockMvc.perform(get("/api/v1/access")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("active"))
                .andExpect(jsonPath("$.data.has_active_pass").value(true))
                .andExpect(jsonPath("$.data.mock_remaining").value(3));
    }

    @Test
    void getAccess_currentPassPlusExpiredPass_picksCurrentNotExpired() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        // Old expired-but-still-status-active pass inserted SECOND so its
        // created_at is newer. Buggy query picks it → state=expired.
        // Fixed query prefers in-window → state=active.
        fixtures.insertAccessPass(userId, "active",
                now.minusDays(5), now.plusDays(25), 5, 1);
        fixtures.insertAccessPass(userId, "active",
                now.minusDays(60), now.minusDays(30), 5, 0);

        mockMvc.perform(get("/api/v1/access")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("active"))
                .andExpect(jsonPath("$.data.has_active_pass").value(true))
                .andExpect(jsonPath("$.data.mock_remaining").value(4));
    }

    @Test
    void getAccess_twoOverlappingActivePasses_picksLongerExpiring() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        // Both currently in-window. The longer-expiring pass is inserted
        // FIRST so its created_at is older — the buggy `created_at DESC`
        // tier would pick the shorter-expiring one. Fixed query orders by
        // expires_at DESC within the in-window tier and picks the longer.
        fixtures.insertAccessPass(userId, "active",
                now.minusDays(1), now.plusDays(60), 3, 0);
        fixtures.insertAccessPass(userId, "active",
                now.minusDays(1), now.plusDays(7), 5, 1);

        mockMvc.perform(get("/api/v1/access")
                        .header("Authorization", "Bearer " + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("active"))
                // mock_remaining=3 confirms the longer-expiring pass was picked.
                .andExpect(jsonPath("$.data.mock_remaining").value(3));
    }
}
