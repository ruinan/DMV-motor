# WeChat Mini-Program Frontend (Phase 3) — Plan

> Status: PLAN (2026-06-26). The Taro mini-program client. Backend WeChat login +
> linking (Phase 1/2) is done. Companion to `wechat-miniapp.md` /
> `wechat-auth-linking.md`. **No code yet** — audited breakdown to review.

## 1. Stack & location
- **Taro 4 + React + TypeScript**, new workspace `apps/miniapp` (sibling to
  `apps/web` / `apps/api`). Compiles to the WeChat mini-program (and H5 later).
- Verified in **WeChat DevTools** — needs the mini-program's **appid** (DevTools
  has a test/no-appid mode for most UI work; real `wx.login` + preview + submit
  need the real appid).

## 2. Reuse from `apps/web`
- **i18n**: copy/share the `src/messages/{en,zh}.json` strings (same keys); a small
  Taro i18n helper (no next-intl). Bilingual from day one.
- **API client**: re-implement the thin fetch wrapper over `Taro.request` (not
  `fetch`); same base URL + `Authorization: Bearer <idToken>` convention + error
  envelope handling.
- **Types/DTOs**: copy the response shapes (exams, topics, practice, mock).
- **NOT reusable**: Next.js app-router pages, React DOM components, Firebase JS SDK.

## 3. Auth flow (⚠️ no Firebase JS SDK in mini-programs)
The Firebase **Web SDK doesn't run** in a WeChat mini-program (no browser env), so
token handling uses **Firebase Auth REST** directly over `Taro.request`:
1. `Taro.login()` → `code`.
2. `POST /api/v1/auth/wechat {code, email?}` → our backend → `{firebase_token}`
   (custom token) — or `EMAIL_REQUIRED` (422) / `EMAIL_IN_USE` (409).
3. Exchange the custom token via Firebase REST
   `identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=<API_KEY>`
   → `{idToken, refreshToken}`.
4. Store tokens (`Taro.setStorageSync`); send `idToken` as Bearer on every API call.
5. Refresh via `securetoken.googleapis.com/v1/token` REST when expired.

This keeps the backend (`FirebaseIdTokenVerifier`) unchanged — it just receives a
normal Firebase ID token.

## 4. Registration / linking UX (matches the backend contract)
- New user: `Taro.login` → if `EMAIL_REQUIRED`, collect email → resubmit
  `{code, email}`. If `EMAIL_IN_USE` (409) → "this email has an account, sign in to
  link" (email/password via Firebase REST, then `POST /auth/wechat/link`).
- Returning user: `Taro.login` → straight to `signInWithCustomToken`.
- `/me`: "bind / unbind WeChat", show login methods via `GET /auth/methods`.

## 5. Pages (MVP)
`login` (WeChat one-tap + email step) · `dashboard` (exam picker + engagement) ·
`practice` · `mock` · `me` (account / binding / activation code redeem). Themed per
exam like web.

## 6. Migration-safety note (personal → business mini-program)
个人→企业 means a **new appid → openids change**. Email-as-key makes this a
one-time re-link, not data loss. For seamless migration, bind both mini-programs to
a WeChat **Open Platform** account and resolve identity by **unionid** (already
stored; backend resolution would prefer unionid when present — a small follow-up).

## 7. Task breakdown
- **T1** `apps/miniapp` Taro scaffold (config, app entry, tabbar, theming tokens).
- **T2** API client over `Taro.request` + token store + Firebase REST auth helper.
- **T3** i18n strings port + helper.
- **T4** Auth pages: WeChat login + email step + `EMAIL_IN_USE` link flow.
- **T5** Practice + mock flows against existing read APIs.
- **T6** `/me`: bind/unbind WeChat, methods, activation-code redeem (reuse A1).
- **T7** DevTools verification against the real appid; real-device preview.

## 8. Verification reality
Taro builds + WeChat behavior can only be truly verified in **WeChat DevTools**
(your machine, with the appid). I can hand-write the scaffold + code; you load it
into DevTools to run/preview. Backend stays fake/unit-tested as today.

## 9. Open decisions
1. Hand-write the scaffold now (you verify in DevTools) vs wait for the appid and
   build against the real mini-program?
2. Personal mini-program now (accept one-time re-link on migration) vs wait for the
   business entity and register once?
3. Monetize MVP via activation codes (A1, works now) — confirm.
