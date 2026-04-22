package com.dmvmotor.api.common;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdResolverTest extends IntegrationTestBase {

    @Autowired UserIdResolver resolver;
    @Autowired TestFixtures fixtures;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
    }

    @Test
    void missingHeader_resolvesToNull() {
        assertThat(resolve(null)).isNull();
    }

    @Test
    void headerWithoutBearerScheme_resolvesToNull() {
        assertThat(resolve("Basic Zm9vOmJhcg==")).isNull();
    }

    @Test
    void invalidDevToken_resolvesToNull() {
        assertThat(resolve("Bearer not-numeric-and-no-test-prefix")).isNull();
    }

    @Test
    void blankBearerToken_resolvesToNull() {
        assertThat(resolve("Bearer    ")).isNull();
    }

    @Test
    void numericToken_resolvesToPreExistingTestFixturesUser() {
        Long userId = fixtures.insertUser("existing@example.com");

        Object resolved = resolve("Bearer " + userId);

        assertThat(resolved).isEqualTo(userId);
        int rowCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users", Integer.class);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void numericTokenForUnknownId_jitProvisionsNewUser() {
        Object resolved = resolve("Bearer 9999");

        assertThat(resolved).isNotNull();
        String firebaseUid = jdbc.queryForObject(
                "SELECT firebase_uid FROM users WHERE id = ?",
                String.class, ((Long) resolved));
        assertThat(firebaseUid).isEqualTo("test-9999");
    }

    @Test
    void testPrefixedToken_jitProvisionsAndIsIdempotent() {
        Object first  = resolve("Bearer test-brand-new");
        Object second = resolve("Bearer test-brand-new");

        assertThat(first).isNotNull().isEqualTo(second);
        int rowCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE firebase_uid = ?",
                Integer.class, "test-brand-new");
        assertThat(rowCount).isEqualTo(1);
    }

    private Object resolve(String authHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authHeader != null) request.addHeader("Authorization", authHeader);
        return resolver.resolveArgument(null, null,
                new ServletWebRequest(request), null);
    }
}
