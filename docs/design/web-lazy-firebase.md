# Web: Lazy-load the Firebase Auth SDK

> Status: PLANNED 2026-07-02 (session 12 web-optimization follow-up, user
> approved "开始"). Companion to the bundle audit in commit `5fa951a`.

## Problem

`src/lib/firebase.ts` runs `initializeApp()` + `getAuth()` at module scope and
is statically imported by `auth-context.tsx` (root provider) and
`api-client.ts` (every hook). Result: the ~113 KB raw firebase/auth chunk is
eager JS on **every** route — including the anonymous marketing landing,
/login, /privacy, /terms — and sits on the critical path before hydration.

## What changes (and what deliberately does not)

- The SDK moves to an **async chunk** loaded from the AuthProvider mount
  effect (post-hydration). It still downloads on every page eventually — we
  cannot know "is this visitor signed in?" without the SDK, since session
  persistence lives in SDK-owned IndexedDB. The win is taking it **off the
  critical path** (eager JS −~113 KB raw per route, hydration/TTI earlier),
  not eliminating the download.
- No auth flow changes: email/password, email-verification gate, TOTP enroll +
  challenge, reauth, change password/email, delete account all keep identical
  behavior. (Standing rule: don't break existing auth.)

## Mechanical plan

Static value-imports of firebase exist in exactly 3 files (everything else is
type-only, which erases):

1. **`lib/firebase.ts`** → export `loadFirebaseAuth(): Promise<{ app, auth,
   mod }>` where `mod = typeof import("firebase/auth")`. Memoized promise
   (HMR-safe: keeps the `getApps()` guard + `emulatorConfig` guard for the
   emulator connect). No module-scope SDK work left.
2. **`lib/auth-context.tsx`** → listener effect awaits `loadFirebaseAuth()`,
   then wires `setPersistence` + `onIdTokenChanged` (unsubscribe still works
   via closure; effect cleanup guards the not-yet-loaded case). Every method
   (`signIn`, `signUp`, `reauth`, TOTP, …) starts with
   `const { auth, mod } = await loadFirebaseAuth()` — they are all async event
   handlers already, so no render-path await.
   - `hasMfaEnrolled(user)` is **synchronous** (calls SDK `multiFactor`), used
     by require-auth (MFA gate) + MeView (security section). It becomes a
     context field: the provider computes `mfaEnrolled` inside the listener
     callback (where `mod` is in scope) and republishes on every token
     refresh — finishTotpEnrollment already forces a refresh, so the gates
     stay reactive. Both call sites switch to `useAuth().mfaEnrolled`; the
     sync export is deleted.
3. **`lib/api-client.ts`** → `fetchWithToken` awaits `loadFirebaseAuth()` for
   `auth.currentUser` (was sync `firebaseAuth.currentUser`; callers are gated
   on `enabled: !!user`, so by the time any query runs the SDK is loaded and
   rehydrated — same semantics). The 401 teardown path uses `mod.signOut`.

## Verification

- Baseline the playwright e2e suite BEFORE the change (some failures may
  pre-exist), re-run after — the suite self-starts the Auth emulator + a dev
  server on :3100 and covers persistence, email verification, console errors.
- `next build` green; re-measure eager JS per route from the prerendered HTML
  script lists (Turbopack prints no size table): expect ~−113 KB on every
  route and the firebase chunk absent from eager `<script>` tags.

## Rollback

Single revert commit; no data, no API, no backend involvement.
