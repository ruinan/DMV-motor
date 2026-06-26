# WeChat Login + Account Linking — Audit & Feature Breakdown

> Status: PLAN (2026-06-25). Decomposition for the WeChat mini-program login,
> with **account linking** (a person's WeChat login and email/password login
> resolve to the **same** account). Companion to `wechat-miniapp.md`.
> **No code yet** — this is the audited breakdown to review before building.

## 1. Audit — current auth/identity surface

| Area | Finding | Implication |
|---|---|---|
| `users` table | `id`, `email VARCHAR(255)` **nullable**, `firebase_uid VARCHAR(128)` UNIQUE (`uq_users_firebase_uid`, V11), `language_preference`. **No phone column.** | WeChat users (no email) provision fine. No phone → can't auto-match WeChat↔email by phone. |
| Auth path | token → `FirebaseAuthVerifier.verify()` → `VerifiedUser(firebaseUid, email, authTime, secondFactor)` → `UserProvisioner.provisionUserId()` JIT-inserts a `users` row keyed on `firebase_uid`. | The account is keyed on `firebase_uid`. Anything that yields a valid Firebase token with a uid reuses the **entire** path. |
| Verifier seam | `FirebaseAuthVerifier` interface; `FirebaseIdTokenVerifier` (prod, `app.auth.firebase.enabled=true`) / `StubFirebaseVerifier` (dev/test). | Mirror this pattern for the new pieces (real impl + fake). |
| Firebase Admin | `FirebaseConfig` initializes `FirebaseApp` (ADC + projectId), gated on `app.auth.firebase.enabled`. | `FirebaseAuth.getInstance().createCustomToken(uid)` is available in prod; dev/test need a **fake minter**. |

**Bottom line:** WeChat login is a Firebase **custom-token** bridge. We only ADD code;
`FirebaseIdTokenVerifier`, `UserProvisioner`, the `firebase_uid` keying, every
protected endpoint, and the web email/password/2FA flow stay **untouched**.

## 2. Identity & linking model (chosen: side-table, Option A)

Keep `users.firebase_uid` as the single account key. Add a side table mapping
WeChat identities to accounts:

```
wechat_identities(
  openid      VARCHAR PRIMARY KEY,   -- WeChat user id within our mini-program
  unionid     VARCHAR NULL,          -- cross-property id (only if Open Platform bound)
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at  TIMESTAMPTZ
)
```

**WeChat login** (`code` → `code2session` → `openid`):
- `openid` **found** in `wechat_identities` → load `user_id` → fetch that user's
  `firebase_uid` → mint a custom token **with that uid** → the client lands on the
  existing account (which may be an email account). ✅ linked, via the existing key.
- `openid` **not found** → new WeChat user → create a standalone account
  (`firebase_uid = "wx_<openid>"`, email null) + insert `wechat_identities`.

**Linking** is always an **explicit "bind" from a logged-in session**, never an
auto-merge of two pre-existing accounts (WeChat hands us no email → no reliable
auto-match):
- Email user (logged in) binds WeChat → attach `openid → my user_id`.
- WeChat-first user (logged in) binds email/password → Firebase `linkWithCredential`
  adds an email credential to the **same** `firebase_uid` (same `users` row).

So "打通" = the two login methods point at one `firebase_uid`. No two-account merge,
no progress-merge complexity.

## 3. Feature breakdown (tasks)

### Phase 1 — Backend WeChat login (no linking yet)
- **T1 `WeChatGateway` seam** — interface + `WeChatGatewayImpl` (real `sns/jscode2session`, JaCoCo-excluded like `StripeGatewayImpl`) + `FakeWeChatGateway` (tests). Config `app.wechat.appid` / `app.wechat.secret` (env/Secret Manager; absent in dev → gateway disabled or fake).
- **T2 `FirebaseTokenMinter` seam** — interface + impl (`createCustomToken`) + fake (tests), because `FirebaseAuth` is prod-only.
- **T3 migration** — `Vxx__wechat_identities.sql` (table above).
- **T4 `WeChatAuthService`** — `code` → gateway → `openid` → resolve-or-create user → mint custom token. New-openid → standalone account.
- **T5 `POST /api/v1/auth/wechat`** — public; body `{code}` → `{firebaseToken}`.
- **Tests** — service (known openid → existing user; new openid → new account + identity row); controller (token returned; bad/expired code → 401/400); fake gateway.

### Phase 2 — Account linking
- **T6 `POST /api/v1/auth/wechat/link`** — authed (existing token) → `code` → `openid` → insert `openid → current user_id`. If `openid` already maps to a **different** user → 409 `WECHAT_ALREADY_LINKED`.
- **T7 `DELETE /api/v1/auth/wechat/link`** (optional) — unbind WeChat from the account.
- **T8 email-on-WeChat-account** — binding email/password is client-side Firebase `linkWithCredential`; backend just sees `email` populate on the next token. (Note: §49 intentionally does NOT sync `users.email` from the token to avoid breaking fixtures — revisit whether linked email should persist.)
- **Tests** — link happy; link openid already linked elsewhere → 409; anonymous → 401.

### Phase 3 — Frontend (separate, later)
- Mini-program (Taro): `wx.login()` → `POST /auth/wechat` → `signInWithCustomToken`.
- Settings "bind WeChat" / "bind email" (mini-program and/or web).

## 4. "不动" invariants (must NOT change)
`FirebaseIdTokenVerifier`, `UserProvisioner`, `users.firebase_uid` keying, every
protected endpoint, and the web email/password/email-verify/2FA flow.

## 5. Open decisions (confirm before Phase 1)
1. **New WeChat user (unknown openid)** → auto-create standalone account
   (recommended, lowest friction; link later from settings) vs. force link first.
2. **Linking is explicit-bind** (no automatic email matching, since WeChat gives no
   email) — acceptable?
3. **reCAPTCHA on `/auth/wechat`?** Likely unnecessary (the WeChat `code` is itself
   a bot barrier + ties to a real WeChat account).
4. **unionid** — capture it now (future cross-property identity) or defer? Cheap to
   store now; only meaningful if a WeChat Open Platform account is set up later.

## 6. Migrations
One new migration (`wechat_identities`). No change to `users`.
