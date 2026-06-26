package com.dmvmotor.api.authaccess.auth;

import com.dmvmotor.api.common.BusinessException;
import org.springframework.http.HttpStatus;
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

    /** Firebase uid prefix for accounts first created via WeChat (no password
     *  unless one is later linked). Used to tell WeChat-first from password
     *  accounts in {@link #methods}. */
    static final String WECHAT_UID_PREFIX = "wx_";

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

        String firebaseUid = WECHAT_UID_PREFIX + openid;
        Long userId = repo.createAccount(firebaseUid, normalizedEmail);
        repo.insertIdentity(openid, session.unionid(), userId);
        return WeChatLoginOutcome.authenticated(minter.mintCustomToken(firebaseUid));
    }

    /** Which login methods exist for {@code email} — drives the web "this account
     *  uses WeChat / set a password" UX. A heuristic: WeChat-first accounts carry a
     *  {@code wx_} firebase_uid (see {@link #WECHAT_UID_PREFIX}); a later-linked
     *  password isn't reflected (acceptable for a UX hint). */
    public LoginMethods methods(String email) {
        String e = email == null ? "" : email.trim();
        return new LoginMethods(repo.passwordAccountExists(e), repo.wechatAccountExists(e));
    }

    public record LoginMethods(boolean password, boolean wechat) {}

    /**
     * Links the WeChat account behind {@code code} to {@code userId} (an authed
     * "bind WeChat" from an already-signed-in session). Idempotent if it's already
     * this user's; conflicts if the openid belongs to a different account.
     */
    @Transactional
    public void link(Long userId, String code) {
        WeChatGateway.WeChatSession session = gateway.codeToSession(code);
        Long owner = repo.findUserIdByOpenid(session.openid());
        if (owner != null) {
            if (owner.equals(userId)) return;   // already linked to this account
            throw new BusinessException("WECHAT_ALREADY_LINKED",
                    "This WeChat account is already linked to another account",
                    HttpStatus.CONFLICT);
        }
        repo.insertIdentity(session.openid(), session.unionid(), userId);
    }

    /** Unbinds WeChat from {@code userId}. Returns how many links were removed. */
    @Transactional
    public int unlink(Long userId) {
        return repo.deleteByUserId(userId);
    }
}
