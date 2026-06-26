# WeChat Mini-Program — Architecture & Plan

> Status: PROPOSAL (2026-06-25). Adds a WeChat Mini-Program (微信小程序) client to
> DMV Prep. The Vercel web app and Spring Boot backend stay; the mini-program is
> an additional front end on top of the same backend.

## 1. Goal & audience

Reach Chinese-speaking users — Chinese immigrants in the US studying for the
California DMV written test — with native WeChat login and (eventually) WeChat
Pay. The bilingual (zh/en) content already fits this segment.

### 微信 vs WeChat (account ecosystems)

Same app, two ecosystems split by the account's registration region:

- **微信 (Weixin)** — mainland-registered accounts → full mini-program ecosystem
  + RMB wallet. **This is the target.**
- **WeChat** — internationally-registered accounts → limited mini-program support,
  WeChat Pay via international cards is restricted.

Mini-programs live on the mainland WeChat Open Platform (mp.weixin.qq.com); users
need a **微信 (mainland)** account for the full experience and RMB payment.

Target users = people with a mainland wallet / RMB who are physically in the US.
Domestic WeChat Pay keys off the wallet/card region, **not** the payer's physical
location, so they can pay. Main UX caveat: risk-control friction on overseas
device/IP (verification SMS to a Chinese number).

## 2. Payment & settlement decision

Two independent merchant axes:

| | 境内 (domestic) | 跨境 (cross-border) |
|---|---|---|
| Entity | China business license (企业/个体工商户) | Overseas company (e.g. US LLC) |
| ICP filing | Required (mainland subject) | Not required |
| Settlement | **RMB → a Chinese bank account** | Foreign currency → overseas account |
| Payers | mainland wallets | mainland wallets (foreign nationals blocked in overseas-subject mini-programs) |

**Chosen: 境内, via a family member's Chinese entity ("scenario 2").** The
relative owns the 营业执照/个体工商户 + bank account, receives the RMB revenue, and
pays Chinese tax. The DMV Prep operator only builds the software and does **not**
receive or control the funds — so it is genuinely the relative's Chinese income,
not the operator's US income.

> ⚠️ **Tax/legal note (not legal advice).** This "clean" structure holds only if
> substance matches form: the operator must not control the account or receive the
> money. Otherwise US law treats it as assignment-of-income / nominee → the income
> is US-reportable, with FBAR/FATCA and H1B exposure. Confirm with a cross-border
> CPA / tax attorney before going live. Note also: RMB settlement does **not** by
> itself avoid US tax for a US tax resident — only genuinely-someone-else's income
> does.

Because 境内主体 onboarding (mini-program 主体认证 + 商户进件, possibly ICP) is slow
and depends on the relative, **payment is decoupled from launch**: ship login +
activation codes first, add WeChat Pay later.

## 3. Reuse map

- ✅ **Reused as-is:** Spring Boot backend (content, practice/mock, AI explain,
  mastery, backup, access gate, **A1 activation codes**), Postgres + Flyway, all
  business logic.
- 🔁 **New backend:** WeChat login endpoint; later a `WechatPayGateway` + checkout/notify.
- ❌ **Not reusable:** the Next.js/React web front end (can't run in a mini-program).
  A new front end is required.

## 4. Tech decisions

1. **Frontend: Taro + React + TypeScript.** Keeps the React mental model, hooks,
   i18n messages, and API-client structure; one codebase compiles to the
   mini-program (+ H5). Lowest ramp from the current React stack. (Alternatives:
   uni-app/Vue — bigger ecosystem but new language; native WXML — no reuse.)
2. **Auth: Firebase Custom Token.** `wx.login()` code → backend
   `POST /api/v1/auth/wechat` → `code2session` (appid+secret) → openid/unionid →
   find/create user → mint a Firebase **custom token** via the Admin SDK → client
   exchanges it for a Firebase ID token. The backend's existing
   `FirebaseIdTokenVerifier`, user model, and every protected endpoint stay
   unchanged. 2FA / email-verify gates are already flag-gated → exempt for the
   mini-program (WeChat login is already strong identity).
3. **Monetization: activation codes first, WeChat Pay later.** A1 redemption codes
   already work via `/access/redeem`; the mini-program can unlock with codes on day
   one with zero merchant/entity dependency. WeChat Pay is added later as a
   `WechatPayGateway` seam mirroring `StripeGateway` (JSAPI 统一下单 + `/notify`
   验签 → access pass; access logic reused).

## 5. Phased rollout

1. **Backend WeChat login** (custom-token path) — pure increment, TDD, no change to
   existing logic. Buildable + testable now with a fake gateway (Stripe/Firebase
   stub pattern); real appid/secret wired later via Secret Manager / local config.
2. **Taro scaffold** — reuse i18n / API client; wire login → fetch topics →
   practice/mock (read-heavy existing APIs).
3. **Monetize** — activation codes (zero dependency) now; WeChat Pay once the
   relative's 境内 商户号 is ready.
4. **Submit for review** (category: education) & launch.

## 6. Prerequisites

- WeChat Mini-Program registered under the relative's 境内主体 — needs the
  relative's 营业执照/身份证 + 主体认证.
- **appid + appsecret** (for `code2session`) — needed to live-test Step 1 (the
  endpoint + logic can be built/tested before this with a fake).
- (Later) 境内微信支付商户号 (MCHID + API key) under the same entity; possibly ICP
  for certain capabilities.

## 7. Open questions

- What share of target users actually still pay with a mainland wallet (validates
  the whole payment direction)?
- Waive 2FA / email-verify entirely in the mini-program, or offer optionally?
