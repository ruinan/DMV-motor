# DMV Prep — WeChat Mini-Program (Taro)

Taro 4 + React + TypeScript client for the WeChat mini-program. Talks to the same
Spring Boot backend as the web app. See `docs/design/wechat-miniapp-frontend.md`.

> ⚠️ **This is an initial scaffold, not yet verified by a build.** Taro builds and
> WeChat behavior can only be validated in **WeChat DevTools**. Run the steps
> below; expect to nudge toolchain versions on first `npm install`.

## What's here (Phase 3, T1–T2 + a working login flow)
- Project scaffold (Taro config, app entry, theming token).
- `src/lib/auth.ts` — WeChat login via Firebase Auth **REST** (no Firebase JS SDK):
  `Taro.login()` → `POST /api/v1/auth/wechat` → custom token → `signInWithCustomToken`
  REST → ID token (Bearer); transparent refresh.
- `src/lib/request.ts` — authed API wrapper over `Taro.request` (same envelope/contract as web).
- `src/lib/i18n.ts` + `src/messages.ts` — bilingual (zh/en).
- Pages: `login` (WeChat one-tap + email step) and `index` (home; proves `GET /me` works).

## Not yet (next tasks)
Practice / mock pages (T5), `/me` bind-unbind + activation-code redeem (T6), and the
full `email_in_use` → sign-in-then-link flow. WeChat Pay is later (needs the business entity).

## Setup
1. `cd apps/miniapp && npm install`
2. Copy env: `cp .env.example .env.development` and set
   - `TARO_APP_API_BASE` — local Spring Boot `http://localhost:8080` (dev) or Cloud Run URL.
   - `TARO_APP_FIREBASE_API_KEY` — the Firebase **Web API key** (public; same one the web app uses).
3. Put your **real appid** in `project.private.config.json` (gitignored):
   ```json
   { "appid": "wx_your_personal_test_appid" }
   ```
   (Use the **personal** mini-program appid for testing; the business one for prod.)
4. `npm run dev:weapp` → open `apps/miniapp/` (the project root) in **WeChat DevTools**.
5. In DevTools, enable **"Do not verify request domains/TLS"** so it can reach
   `localhost:8080` during dev. (Production needs the API domain whitelisted in the
   mini-program admin console.)

## Auth note
The backend issues a Firebase **custom token**; the mini-program exchanges it for a
Firebase ID token via REST and sends it as `Bearer`. The backend verifies it
exactly like a web login — no backend changes. Real `wx.login` needs the real appid;
DevTools' tourist appid can't complete a live `code2session`.
