package com.dmvmotor.api.authaccess.auth;

import com.dmvmotor.api.authaccess.auth.WeChatAuthService.LoginMethods;
import com.dmvmotor.api.authaccess.auth.WeChatGateway.WeChatSession;
import com.dmvmotor.api.authaccess.auth.WeChatLoginOutcome.Status;
import com.dmvmotor.api.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WeChat login logic with mocked seams — returning user, and the three new-user
 * branches (email required / email already in use / new email creates an account).
 */
class WeChatAuthServiceTest {

    private final WeChatGateway gateway = mock(WeChatGateway.class);
    private final FirebaseTokenMinter minter = mock(FirebaseTokenMinter.class);
    private final WeChatIdentityRepository repo = mock(WeChatIdentityRepository.class);
    private final WeChatAuthService service = new WeChatAuthService(gateway, minter, repo);

    @Test
    void returningUser_mintsTokenForExistingAccount() {
        when(gateway.codeToSession("code")).thenReturn(new WeChatSession("openid-1", null));
        when(repo.findUserIdByOpenid("openid-1")).thenReturn(42L);
        when(repo.findFirebaseUidByUserId(42L)).thenReturn("fb-42");
        when(minter.mintCustomToken("fb-42")).thenReturn("token-42");

        WeChatLoginOutcome r = service.login("code", null);

        assertThat(r.status()).isEqualTo(Status.AUTHENTICATED);
        assertThat(r.firebaseToken()).isEqualTo("token-42");
        verify(repo, never()).createAccount(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void newUser_withoutEmail_isEmailRequired() {
        when(gateway.codeToSession("code")).thenReturn(new WeChatSession("openid-2", null));
        when(repo.findUserIdByOpenid("openid-2")).thenReturn(null);

        WeChatLoginOutcome r = service.login("code", "   ");

        assertThat(r.status()).isEqualTo(Status.EMAIL_REQUIRED);
        verify(repo, never()).createAccount(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void newUser_emailAlreadyHasAccount_isLoginRequired() {
        when(gateway.codeToSession("code")).thenReturn(new WeChatSession("openid-3", null));
        when(repo.findUserIdByOpenid("openid-3")).thenReturn(null);
        when(repo.findUserIdByEmail("taken@example.com")).thenReturn(7L);

        WeChatLoginOutcome r = service.login("code", "Taken@Example.com");   // case-insensitive

        assertThat(r.status()).isEqualTo(Status.LOGIN_REQUIRED);
        verify(repo, never()).createAccount(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void newUser_newEmail_createsAccountAndLinksIdentity() {
        when(gateway.codeToSession("code")).thenReturn(new WeChatSession("openid-4", "union-4"));
        when(repo.findUserIdByOpenid("openid-4")).thenReturn(null);
        when(repo.findUserIdByEmail("new@example.com")).thenReturn(null);
        when(repo.createAccount("wx_openid-4", "new@example.com")).thenReturn(99L);
        when(minter.mintCustomToken("wx_openid-4")).thenReturn("token-99");

        WeChatLoginOutcome r = service.login("code", "New@Example.com");

        assertThat(r.status()).isEqualTo(Status.AUTHENTICATED);
        assertThat(r.firebaseToken()).isEqualTo("token-99");
        verify(repo).insertIdentity("openid-4", "union-4", 99L);
    }

    @Test
    void methods_reportsPasswordAndWechatFlags() {
        when(repo.passwordAccountExists("a@example.com")).thenReturn(true);
        when(repo.wechatAccountExists("a@example.com")).thenReturn(false);

        LoginMethods m = service.methods("a@example.com");

        assertThat(m.password()).isTrue();
        assertThat(m.wechat()).isFalse();
    }

    @Test
    void link_newOpenid_insertsIdentityForUser() {
        when(gateway.codeToSession("code")).thenReturn(new WeChatSession("openid-L", "union-L"));
        when(repo.findUserIdByOpenid("openid-L")).thenReturn(null);

        service.link(10L, "code");

        verify(repo).insertIdentity("openid-L", "union-L", 10L);
    }

    @Test
    void link_alreadyLinkedToSameUser_isIdempotent() {
        when(gateway.codeToSession("code")).thenReturn(new WeChatSession("openid-S", null));
        when(repo.findUserIdByOpenid("openid-S")).thenReturn(10L);

        service.link(10L, "code");   // no throw

        verify(repo, never()).insertIdentity(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void link_openidOwnedByAnotherUser_conflicts() {
        when(gateway.codeToSession("code")).thenReturn(new WeChatSession("openid-X", null));
        when(repo.findUserIdByOpenid("openid-X")).thenReturn(99L);

        assertThatThrownBy(() -> service.link(10L, "code"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("WECHAT_ALREADY_LINKED");
    }

    @Test
    void unlink_delegatesToRepo() {
        when(repo.deleteByUserId(10L)).thenReturn(1);
        assertThat(service.unlink(10L)).isEqualTo(1);
    }
}
