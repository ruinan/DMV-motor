# WeChat Login + Account Linking — Audit & Feature Breakdown

> Status: PLAN v2 (2026-06-26). WeChat mini-program login with **account linking**,
> **email as the universal account key**, packaged as a **modular auth component**
> in the monolith (designed for cheap later extraction). Companion to
> `wechat-miniapp.md`. **No code yet** — audited breakdown to review before building.

## 1. Audit — current auth/identity surface

| Area | Finding | Implication |
|---|---|---|
| `users` table | `id`, `email VARCHAR(255)` **nullable**, `firebase_uid VARCHAR(128)` UNIQUE (`uq_users_firebase_uid`, V11), `language_preference`. **No phone, email not unique.** | Email can become the key, but uniqueness must be enforced at the registration boundary (see §3). |
| Auth path | token → `FirebaseAuthVerifier.verify()` → `VerifiedUser(firebaseUid, email, authTime, secondFactor)` → `UserProvisioner.provisionUserId()` JIT-inserts keyed on `firebase_uid`. | Account key = `firebase_uid`. Any valid Firebase token reuses the whole path. |
| Verifier seam | `FirebaseAuthVerifier` iface; `FirebaseIdTokenVerifier` (prod) / `StubFirebaseVerifier` (dev/test). | Mirror this (real + fake) for new seams. |
| Firebase Admin | `FirebaseConfig` inits `FirebaseApp` (ADC + projectId), gated `app.auth.firebase.enabled`. | `createCustomToken` available in prod; dev/test need a **fake minter**. ⚠️ see §6 IAM risk. |

**Bottom line:** WeChat login is a Firebase **custom-token** bridge. Core verifier /
provisioner / `firebase_uid` keying / protected endpoints / web email-password-2FA
stay **untouched**; everything new is additive.

## 2. Architecture — modular auth component (chosen)

Auth lives as a **self-contained module** in `com.dmvmotor.api.authaccess.auth`
(already the home of the verifier/provisioner). It owns identity, providers,
linking, and login-method detection behind seams so a second project can lift it,
and a future extraction to a library/service is cheap. **Not** a separate service
(Firebase already is the token authority; a service would duplicate it + add ops).

Seams (interface + real impl excluded from coverage + fake for tests, like the
existing verifier): `FirebaseAuthVerifier` (exists), `WeChatGateway`
(`code2session`), `FirebaseTokenMinter` (`createCustomToken`). Identity persistence
behind `WeChatIdentityRepository`.

## 3. Identity model — email is the universal key

**Invariants:** one account per **email**; one `firebase_uid` per account (the token
key, unchanged). A `wechat_identities` side table maps WeChat → account:

```
wechat_identities(
  openid     VARCHAR PRIMARY KEY,
  unionid    VARCHAR NULL,
  user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ )
```

### Registration entry points
- **Email registration** (existing Firebase email/password) — unchanged. Afterward,
  **guide**: "Have WeChat? Link it → use the mini-program free."
- **WeChat registration** (mini-program) — `wx.login` → `openid`, then **email is
  required**:
  - email is **new** → create account (`email`, `firebase_uid = "wx_<openid>"`) +
    `wechat_identities` row. (Email verification can be lazy — no existing account to
    hijack.)
  - email **matches an existing account** → **must prove ownership before linking**
    (email verification link, or the existing account's password) → then attach
    `openid → that user_id`. ⚠️ Without this, typing someone else's email hijacks
    their account.

### WeChat login (returning)
`code` → `openid` → `wechat_identities` lookup:
- **found** → `user_id` → that account's `firebase_uid` → mint custom token with
  **that uid** → lands on the existing account (email-first or wechat-first alike).
- **not found** → treat as WeChat registration (collect email, above).

### One account per email (anti-duplicate)
Firebase issues a uid per provider, but we want one account per email. Enforced at
the **registration boundary**, leaving `UserProvisioner` untouched:
- Before creating a Firebase email/password user, the client checks
  `GET /api/v1/auth/methods?email=…`. If the email already has an account, route to
  **login / link**, never a second signup.
- A wechat-first user who wants web/password access **links email-password to the
  same Firebase uid** (`linkWithCredential`), not a fresh signup → same `firebase_uid`,
  same `users` row.

### Login-method detection
`GET /api/v1/auth/methods?email=…` → `{ password: bool, wechat: bool }` (from
`users` + `wechat_identities`). Drives "this account uses WeChat — sign in with
WeChat / set a password." (Mild account-enumeration surface → rate-limit; acceptable.)

## 4. Feature breakdown (tasks)

### Phase 1 — Backend WeChat login (account creation + returning login)
- **T1 `WeChatGateway`** seam + `WeChatGatewayImpl` (`sns/jscode2session`, JaCoCo-excluded) + `FakeWeChatGateway`. Config `app.wechat.appid/secret` (Secret Manager / gitignored local).
- **T2 `FirebaseTokenMinter`** seam + impl (`createCustomToken`) + fake.
- **T3 migration** `Vxx__wechat_identities.sql`.
- **T4 `WeChatIdentityRepository`** (JdbcTemplate) — find by openid, insert, find user's firebase_uid.
- **T5 `WeChatAuthService`** — code → openid → returning(found)/register(new). Register requires email; new-email → create account; existing-email → require verification (T6).
- **T6 email-ownership verification** before linking to an existing account (verify-link or password challenge).
- **T7 `POST /api/v1/auth/wechat`** (public) → `{firebaseToken}` or a "needs email / needs verification" response.
- **Tests:** service (new email, existing-email-needs-verify, returning), controller, fakes.

### Phase 2 — Linking + detection
- **T8 `GET /api/v1/auth/methods?email=`** → `{password, wechat}` (+ rate-limit).
- **T9 `POST /api/v1/auth/wechat/link`** (authed) → attach `openid → current user`; openid already linked elsewhere → 409 `WECHAT_ALREADY_LINKED`.
- **T10 `DELETE /api/v1/auth/wechat/link`** (optional) — unbind.
- **Tests:** methods lookup, link happy / 409 / 401.

### Phase 3 — Frontend (separate, later)
- Mini-program (Taro): `wx.login` → `/auth/wechat` → `signInWithCustomToken`; mandatory-email step; "set password" optional.
- Web: post-registration "link WeChat (free mini-program)" prompt; method-aware login.

## 5. Resolved decisions
- **Linking:** email is the key; guided link from email side, mandatory email on WeChat side. ✅
- **Anti-takeover:** verify email ownership before linking to an existing account. ✅
- **Architecture:** modular component in the monolith (not a service). ✅
- **reCAPTCHA on `/auth/wechat`:** default **no** (the WeChat `code` is itself a bot barrier); revisit if abused.
- **unionid:** **store now** (nullable, cheap; future cross-property identity).

## 6. Risks / "不动" invariants
- ⚠️ **IAM:** `createCustomToken` on Cloud Run ADC needs the runtime SA to hold
  `roles/iam.serviceAccountTokenCreator` (signBlob). `verifyIdToken` doesn't — so this
  is invisible until prod. **Grant before prod relies on it.** (Fakes hide it in tests.)
- **Untouched:** `FirebaseIdTokenVerifier`, `UserProvisioner`, `firebase_uid` keying,
  every protected endpoint, web email/password/email-verify/2FA.
- **No merge** of two *already-separate* accounts (bind → 409). Email-as-key +
  registration-boundary checks make this rare; a merge tool is out of scope.
- **§49 email no-sync:** linked email may not appear in `users.email`; revisit if
  detection/display needs it.
- **MfaGuard:** custom-token sessions have no second factor; keep `mfa-required` off
  or exempt them.

## 7. Migrations
One new (`wechat_identities`). No change to `users`.
