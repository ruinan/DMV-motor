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
}
