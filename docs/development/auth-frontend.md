# Frontend Auth — Token Injection Path

> Audit reference for Round 4 #3 — every protected backend call must carry a
> Firebase ID token, and a rejected token must terminate the session cleanly.
> Last reviewed: 2026-05-03.

## End-to-end flow

```
Firebase Web SDK ──── ID token ────▶ apiFetch (Authorization header)
       ▲                                         │
       │                                         ▼
onAuthStateChanged                       backend /api/v1/*
       ▲                                         │
       │                                  401 ───┤
       │                                         ▼
       └────── firebaseSignOut ◀──── apiFetch (auto on 401)
                          │
                          ▼
              RequireAuth detects user=null
                          │
                          ▼
                router.replace(/${lang}/login)
```

## Token acquisition

- **Source of truth**: `firebaseAuth.currentUser?.getIdToken()` (Firebase Web
  SDK v12).
- **Persistence**: `auth-context.tsx` calls
  `setPersistence(firebaseAuth, browserLocalPersistence)` on mount, so the
  user rehydrates across reloads from IndexedDB / localStorage. Restrictive
  environments (Safari ITP, blocked storage) silently fall back to in-memory.
- **Auto-refresh**: the SDK refreshes the ID token roughly 5 minutes before
  its 1-hour expiry, transparently to callers. `getIdToken()` always returns
  a fresh token unless the refresh token itself has been revoked.

## Single injection point — `apiFetch`

All protected calls go through `apps/web/src/lib/api-client.ts`. There are
**two** exported entrypoints, both built on the same `rawFetch` core:

| Function | Returns | When to use |
| --- | --- | --- |
| `apiFetch<T>(path, init?)` | `data` only | 95% of calls |
| `apiFetchEnvelope<T>(path, init?)` | `{ data, meta }` | When pagination/meta is needed (e.g. `/mistakes` `meta.total`) |

`rawFetch` is the only place that:

1. Reads the ID token via `firebaseAuth.currentUser?.getIdToken()`.
2. Sets `Authorization: Bearer <token>` if a token exists.
3. Sets `Accept: application/json` and `Content-Type: application/json` for
   bodied requests.
4. Calls `signOut(firebaseAuth)` on a 401 response **iff a token was sent**
   (anonymous 401s are surfaced as `ApiError` only — there is no session to
   tear down).
5. Throws `ApiError(status, code, message)` extracted from the snake_case
   error envelope `{ success: false, error: { code, message, details? } }`.

### Audit invariant — no naked `fetch("/api/...")`

`grep -rn "fetch\s*(" apps/web/src` and
`grep -rn "/api/v1" apps/web/src` should only find:

- `apps/web/src/lib/api-client.ts:43` (the `rawFetch` core)
- the documented public callsites that all import `apiFetch` /
  `apiFetchEnvelope`

If a new file appears in those greps with a raw `fetch("/api/v1/...")` call,
that bypasses both token injection and 401 handling — it must be refactored.

## 401 handling

`apiFetch` does the cleanup centrally. Specifically:

- **Bad token sent → 401**: `signOut(firebaseAuth)` is awaited, then
  `ApiError(401, …)` is thrown. The auth listener fires with `user=null`,
  `<RequireAuth>` (which wraps the entire `(app)` route group) sees the
  null user and `router.replace`s to `/${lang}/login`.
- **Anonymous request → 401**: nothing to sign out; the error just
  propagates.
- **TanStack Query retry policy** (`query-provider.tsx`): retries are
  globally disabled for 401/403 — those are deterministic and won't succeed
  on retry. 5xx still retries up to 2 times.
- **Mutations**: `retry: false` globally — write operations that fail
  surface immediately so the UI can show the error rather than silently
  retry an idempotent-looking POST.

## Backend verifier path

| Environment | Bean | Token format |
| --- | --- | --- |
| dev / test (default) | `StubFirebaseVerifier` | `Bearer <numericUserId>` or `Bearer test-<uid>` |
| prod (`app.auth.firebase.enabled=true`) | `FirebaseIdTokenVerifier` | Real Firebase ID token (signed JWT) |

Firebase Admin SDK verifies the JWT signature offline against the Firebase
public keys. `UserProvisioner` JIT-creates a row in `users` keyed by
`firebase_uid` on first `/api/v1/me` call after Firebase issues a token.

## Where to look when debugging

| Symptom | First place to check |
| --- | --- |
| Every API call returns 401 | `firebaseAuth.currentUser` is null (user signed out) — or the prod backend is rejecting the token (check `FirebaseConfig` projectId and Cloud Run env) |
| User stays signed in despite token rejection | Verify `apiFetch` is the call path, not a raw `fetch` |
| `RequireAuth` flashes / doesn't redirect | Check `auth-context.tsx`'s `onAuthStateChanged` listener is mounted; check `RequireAuth.tsx` `useEffect` dependencies |
| Token expired mid-session, request fails | Firebase auto-refresh failed; user must re-authenticate. Expected behaviour after the 401 → signOut path runs. |
