package com.dmvmotor.api.authaccess.auth;

import com.dmvmotor.api.IntegrationTestBase;
import com.dmvmotor.api.TestFixtures;
import com.dmvmotor.api.authaccess.auth.FirebaseAuthVerifier.VerifiedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class UserProvisionerTest extends IntegrationTestBase {

    @Autowired UserProvisioner provisioner;
    @Autowired TestFixtures    fixtures;
    @Autowired JdbcTemplate    jdbc;

    @BeforeEach
    void setUp() {
        fixtures.truncateAll();
    }

    @Test
    void firstLogin_createsNewUser() {
        Long id = provisioner.provisionUserId(
                new VerifiedUser("firebase-new-1", "new1@example.com"));

        assertThat(id).isNotNull();

        String firebaseUid = jdbc.queryForObject(
                "SELECT firebase_uid FROM users WHERE id = ?", String.class, id);
        String email = jdbc.queryForObject(
                "SELECT email FROM users WHERE id = ?", String.class, id);

        assertThat(firebaseUid).isEqualTo("firebase-new-1");
        assertThat(email).isEqualTo("new1@example.com");
    }

    @Test
    void repeatLogin_returnsSameUserId() {
        Long first  = provisioner.provisionUserId(
                new VerifiedUser("firebase-repeat", "repeat@example.com"));
        Long second = provisioner.provisionUserId(
                new VerifiedUser("firebase-repeat", "repeat@example.com"));

        assertThat(second).isEqualTo(first);

        int rowCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE firebase_uid = ?",
                Integer.class, "firebase-repeat");
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void differentUids_createDifferentUsers() {
        Long a = provisioner.provisionUserId(new VerifiedUser("firebase-a", "a@x.com"));
        Long b = provisioner.provisionUserId(new VerifiedUser("firebase-b", "b@x.com"));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void missingEmail_provisionsUserWithNullEmail() {
        Long id = provisioner.provisionUserId(
                new VerifiedUser("firebase-phone-only", null));

        String email = jdbc.queryForObject(
                "SELECT email FROM users WHERE id = ?", String.class, id);
        assertThat(email).isNull();
    }
}
