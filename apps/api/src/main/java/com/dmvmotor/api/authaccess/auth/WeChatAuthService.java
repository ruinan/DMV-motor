package com.dmvmotor.api.authaccess.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WeChat mini-program login → a Firebase custom token, reusing the existing
 * auth path (the minted token is verified by {@link FirebaseAuthVerifier} and
 * mapped to a user by {@link UserProvisioner} exactly like an email login).
 *
 * <p>Identity model (see {@code docs/design/wechat-auth-linking.md}): email is the
 * universal key. A returning WeChat user (known openid) is minted a token for the
 * account the openid links to. A new WeChat user must supply an email; a new email
 * creates an account (firebase_uid {@code wx_<openid>}), while an email that
 * already has an account routes to "sign in to link" so nobody can claim another
 * user's email.
 */
@Service
public class WeChatAuthService {

    private final WeChatGateway gateway;
    private final FirebaseTokenMinter minter;
    private final WeChatIdentityRepository repo;

    public WeChatAuthService(WeChatGateway gateway, FirebaseTokenMinter minter,
                             WeChatIdentityRepository repo) {
        this.gateway = gateway;
        this.minter = minter;
        this.repo = repo;
    }

    @Transactional
    public WeChatLoginOutcome login(String code, String email) {
        WeChatGateway.WeChatSession session = gateway.codeToSession(code); // 401 on bad code
        String openid = session.openid();

        Long existingUserId = repo.findUserIdByOpenid(openid);
        if (existingUserId != null) {
            // Returning WeChat user → mint a token for the account it already links to.
            return WeChatLoginOutcome.authenticated(
                    minter.mintCustomToken(repo.findFirebaseUidByUserId(existingUserId)));
        }

        // New WeChat user → registration. Email is the universal key, so it's required.
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isEmpty()) {
            return WeChatLoginOutcome.emailRequired();
        }
        if (repo.findUserIdByEmail(normalizedEmail) != null) {
            // Existing account for this email → must sign in to link (anti-takeover).
            return WeChatLoginOutcome.loginRequired();
        }

        String firebaseUid = "wx_" + openid;
        Long userId = repo.createAccount(firebaseUid, normalizedEmail);
        repo.insertIdentity(openid, session.unionid(), userId);
        return WeChatLoginOutcome.authenticated(minter.mintCustomToken(firebaseUid));
    }
}
