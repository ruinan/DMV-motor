# WeChat Mini-Program Frontend (Phase 3) — Plan

> Status: IN PROGRESS (updated 2026-07-01; original plan 2026-06-26). Backend
> WeChat login + linking (Phase 1/2) is done. Companion to `wechat-miniapp.md` /
> `wechat-auth-linking.md`.
>
> **Done so far** (commit `46734b4`): T1 scaffold builds & renders in DevTools
> (Taro 4.0.9 + Vite 4, alias `@→src`, `defineConstants` bakes env, minified +
> lazyCodeLoading), T2 API client (`lib/request.ts` over `Taro.request`, envelope
> unwrap) + token store + Firebase REST auth (`lib/auth.ts`), T3 i18n helper
> (`lib/i18n.ts` + `messages.ts`), T4 login page (WeChat one-tap + email step) +
> **dev bypass** (auto stub-login when `TARO_APP_DEV_BYPASS`, `lib/devStub.ts`
> serves canned responses; tree-shaken out of prod builds — verified).
>
> **Current task: §10 full feature port** — replicate the web app's features and
> UI style in the mini-program. Audit + breakdown below.

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
   build against the real mini-program? → **Resolved: scaffold hand-written, appid
   `wx581164f3d39b37f5` wired, renders in DevTools.**
2. Personal mini-program now (accept one-time re-link on migration) vs wait for the
   business entity and register once?
3. Monetize MVP via activation codes (A1, works now) — confirm.

---

# §10 Full feature port (web → miniapp) — audit & breakdown (2026-07-01)

Goal: every user-facing web feature reachable in the mini-program, styled like the
web app. Development runs against the **dev bypass + stub** (no backend needed in
DevTools); real-API wiring is already in place for when the backend is up.

## 10.1 Web feature inventory (audited)

Web pages are thin wrappers; the real UI lives in colocated view components:

| Web route | View component (lines) | Miniapp target |
|---|---|---|
| `/dashboard` | `Dashboard.tsx` (634) + study-hub/* (486) | **tab 1** `pages/dashboard` |
| `/practice` | `PracticeFlow.tsx` (930) + `PracticeShell` (77) + `AttemptHistory` (174) | **tab 2** `pages/practice` |
| `/mock` | `MockLanding.tsx` (144) | **tab 3** `pages/mock` |
| `/mock/[attemptId]` | `MockExam.tsx` (928) + `MockReview.tsx` (159) | `pages/mock-exam` (navigateTo, id param) |
| `/mistakes` | `MistakesView.tsx` (311) | `pages/mistakes` (navigateTo from dashboard) |
| `/progress` | `ProgressView.tsx` (326) | `pages/progress` (navigateTo from dashboard) |
| `/me` | `MeView.tsx` (1292) | **tab 4** `pages/me` |
| `/start` | `OnboardingExamChoice.tsx` (122) | `pages/start` (post-login gate, redirectTo) |
| `/login` | `LoginForm.tsx` (337) — email/password web login | **already exists** as WeChat login; email flow N/A |
| marketing / privacy / terms | static | **skip** (web link from `me` if needed) |

Navigation = web's `MobileTabBar` (4 items: 学习/练习/模考/我的) → native miniapp
`tabBar` with the same 4 items. `Taro.setTabBarStyle` re-tints selectedColor per
exam (matches web's per-exam accent).

## 10.2 Shared systems to port

1. **Theme tokens** (`apps/web/src/styles/theme.css` → `src/app.scss` CSS vars —
   miniapp WXSS supports CSS variables):
   - Mode A default: `--background #f8f9ff`, `--primary #1b5e9b` (Class C blue),
     card white, radius 24rpx (web 0.75rem×2), muted `#64748b`, border `#e2e8f0`.
   - Per-exam: page root gets `class="theme-M1"` etc. → M1/M2 amber `#b45309`,
     bg `#fffaf3` (same override set as web's `[data-exam=…]`).
   - Mode B `.dmv-retro` for answering surfaces (practice/mock questions): white
     bg, black text, navy `#003f7f` accent, radius 0, Arial — same as web.
   - Sizing rule: web px ≈ 2× rpx (750rpx design width).
2. **i18n**: extend `messages.ts` with the keys each page needs, mirroring
   `apps/web/src/messages/{en,zh}.json` key names where they overlap. Language
   switch persisted via `PUT /me/language` + local storage, like web.
3. **Data layer**: web uses TanStack Query (27 hooks). Miniapp gets a minimal
   `useApi(path)` hook (load + loading/error state + refresh) plus a tiny
   invalidation bus (`invalidate(prefix)`) — no TanStack dependency, keeps the
   package small. Hooks mirror web names: `useMe/useTopics/useReadiness/...`.
4. **Loading rule**: every wait shows loading, min 0.3s (existing project rule;
   login page already implements the pattern — reuse helper).

## 10.3 API surface (from web hooks — all already exist on the backend)

`GET /me` · `PUT /me/exam` · `PUT /me/language` · `POST /me/reset-learning` ·
`GET /me/export` · `GET /exams` · `GET /exams/entitlements` ·
`POST /exams/{id}/open-free` · `GET /topics` · `GET /topics/mastery` ·
`POST|GET|PATCH /practice/sessions[...]` · `GET /practice/sessions/stats` ·
`POST|GET /mock-exams/attempts[...]` · `GET /mock-exams/attempts/stats` ·
`GET /mistakes` · `POST /mistakes/{qid}/review` · `GET /readiness` ·
`GET /summary` · `GET /engagement` · `POST /ai/explain` ·
`GET /ai/recommendations` · `POST /ai/review-plan` · `POST /access/redeem`

**Not ported (deliberate):** Stripe billing (`/billing/*` — WeChat forbids
external web checkout in-miniapp; monetize via activation codes `/access/redeem`),
reCAPTCHA (`/auth/recaptcha-verify`, web-only), backup **restore** (needs
reauth/2FA — Firebase REST reauth is a follow-up; backup **status display** is
fine), change email/password + TOTP 2FA management (web-only for now; `me` links
out), delete account (needs reauth — defer with the same follow-up).

## 10.4 Dev stub expansion

`lib/devStub.ts` grows from `{/me}` to cover every read endpoint above with
realistic canned data, plus a tiny **in-memory state machine** for practice
sessions and mock attempts (start → answer → complete) so the full flows are
clickable in DevTools with zero backend. Stub data lives in one file; each
milestone adds only the endpoints it needs.

## 10.5 Milestones (each = build green + DevTools check before the next)

> Status 2026-07-02: **M1–M6 done** (`8ab2bba`→`69fc6a4`), pending the user's
> DevTools walkthrough. Adjustments made along the way: native tabBar replaced
> by an in-page TabBar component with 32rpx labels (user asked for a bigger
> footer font; also gives per-exam tinting for free) and the miniapp went
> **Chinese-only** by user decision (i18n reduced to `LANG='zh'`, copy taken
> verbatim from web zh.json). Entry page is now the dashboard with an
> ensureAuthed() guard instead of login-first. Remaining: **M7** onboarding
> exam-picker gate + dashboard exam switcher + polish; deferred extras: AI
> review plan block on mock review, practice/mock history on the hub.
>
> First review round (2026-07-02, user walkthrough feedback): hub header shows
> a subscription tag (已订阅/免费版); WeChat home capsule hidden on tab pages
> (side effect of redirectTo tab switching); TabBar restyled to solid
> --primary with white labels (more prominent, still exam-tinted); the Mode B
> `dmv-retro` answering frame was **dropped** — questions render frameless
> (`.question-block`) with the choice rows as the only boxes, filled key
> badges for selected/correct/wrong (mobile-first clarity, diverges from web
> on purpose).

- **M1 — Shell**: 4-tab tabBar (icons = simple PNGs), page stubs, theme tokens in
  `app.scss`, per-exam theme helper (`useExamTheme` → root class +
  `setTabBarStyle`), `useApi` + invalidation bus, stub: `/me /exams /topics
  /summary /readiness /engagement /topics/mastery`.
- **M2 — Dashboard (study hub)**: readiness display, engagement strip, coverage +
  topic progress (CSS bars; donut/sparkline simplified to bars/ring via CSS —
  visual parity, not canvas), AI recommendations (paid-gated like web), links to
  mistakes/progress pages.
- **M3 — Practice**: session flow (10 questions, answer → immediate feedback →
  complete screen with readiness attribution), `dmv-retro` answering surface,
  AI-explain block (cooldown auto-retry behavior like web), practice modes
  (random free; weak_points/review_learned paid-gated), attempt history.
- **M4 — Mock**: landing (stats + history), 30-question timed attempt, abandon/
  submit paths, result + per-topic breakdown, review screen for finished attempt.
- **M5 — Mistakes + Progress**: mistakes list + re-review flow + mastery-clears
  explanation; progress page (history, per-topic mastery bars).
- **M6 — Me**: profile + signed-in state, exam catalog with three-state
  locked/free/paid (FreeBadge equivalent), open-free flow, activation-code
  redeem, language switch, backup status, reset-learning (confirm dialog), sign
  out; links to web for password/2FA/billing.
- **M7 — Onboarding + polish**: `/start` exam picker gate after first login,
  exam switcher on dashboard (navigate + invalidate like web `useSetExam`),
  per-exam theming end-to-end, empty/error states, loading polish.

## 10.6 Testing approach

- **Unit (TDD)**: pure logic in `lib/` — `useApi` cache/invalidation, stub state
  machine, theme mapping, i18n — via vitest with a mocked `@tarojs/taro`.
  UI pages are DevTools-verified (no reliable headless WXML runtime).
- **Manual gate per milestone**: compile in DevTools, walk the flow with dev
  bypass on, screenshot for the session log.
- **Prod-safety gate**: `npm run build:weapp` (bypass off) must contain zero
  `dev-bypass` strings (same check as the login milestone).
