package com.dmvmotor.api.authaccess.auth;

import com.dmvmotor.api.authaccess.auth.FirebaseAuthVerifier.VerifiedUser;
import com.dmvmotor.api.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StubFirebaseVerifierTest {

    private final StubFirebaseVerifier verifier = new StubFirebaseVerifier();

    @Test
    void numericToken_mapsToTestUid() {
        VerifiedUser user = verifier.verify("42");
        assertThat(user.firebaseUid()).isEqualTo("test-42");
        assertThat(user.email()).isEqualTo("test42@local");
    }

    @Test
    void testPrefixedToken_passesThroughAsUid() {
        VerifiedUser user = verifier.verify("test-new-user-9");
        assertThat(user.firebaseUid()).isEqualTo("test-new-user-9");
        assertThat(user.email()).isEqualTo("test-new-user-9@local");
    }

    @Test
    void numericTokenWithSurroundingWhitespace_trimmedBeforeParse() {
        VerifiedUser user = verifier.verify("  7  ");
        assertThat(user.firebaseUid()).isEqualTo("test-7");
    }

    @Test
    void nullToken_rejectedWith401() {
        assertThatThrownBy(() -> verifier.verify(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Missing token");
    }

    @Test
    void blankToken_rejectedWith401() {
        assertThatThrownBy(() -> verifier.verify("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Missing token");
    }

    @Test
    void malformedToken_rejectedWith401() {
        assertThatThrownBy(() -> verifier.verify("not-a-number-and-no-test-prefix"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid dev-mode token");
    }
}
