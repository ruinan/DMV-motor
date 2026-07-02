import { loadFirebaseAuth } from "@/lib/firebase";
import { emitSessionExpired } from "@/lib/session-expired-bus";

/**
 * All API requests go through Next.js rewrites (see next.config.ts), so the
 * client always uses relative paths like "/api/v1/me". This avoids CORS in
 * dev and prod alike — the rewrite proxies to NEXT_PUBLIC_API_BASE_URL on
 * the server side, where same-origin policy doesn't apply.
 */

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string | undefined,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type SuccessEnvelope<T> = { success: true; data: T; meta?: unknown };
type ErrorEnvelope = {
  success: false;
  error: { code: string; message: string; details?: unknown };
};
type ApiResponse<T> = SuccessEnvelope<T> | ErrorEnvelope;

export type Envelope<T> = { data: T; meta: unknown };

async function fetchWithToken<T>(
  path: string,
  init: RequestInit,
  forceRefresh: boolean,
): Promise<{
  response: Response;
  payload: ApiResponse<T> | null;
  token: string | null;
}> {
  // Lazy SDK: by the time any query runs, the auth listener has loaded it and
  // resolved the user (queries are enabled: !!user), so this await is a no-op
  // in practice — it just avoids a static import in the eager bundle.
  const { auth } = await loadFirebaseAuth();
  const user = auth.currentUser;
  // forceRefresh=true bypasses Firebase's internal token cache and goes
  // straight to the refresh endpoint. We only pass true on the retry path
  // so the happy case still benefits from the SDK's silent refresh.
  const token = user ? await user.getIdToken(forceRefresh).catch(() => null) : null;

  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, { ...init, headers });
  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;
  return { response, payload, token };
}

async function rawFetch<T>(
  path: string,
  init: RequestInit,
): Promise<SuccessEnvelope<T>> {
  const first = await fetchWithToken<T>(path, init, false);
  const { token } = first;
  let response = first.response;
  let payload = first.payload;

  // 401 with a token sent could mean: (a) the SDK's silent refresh hasn't
  // fired yet and the token has crossed its 1-hour TTL, or (b) the user is
  // genuinely revoked. Try once with a force-refreshed token to disambiguate.
  // Only when the *refreshed* token is also rejected do we surface the
  // session-expired toast and tear down the session.
  if (response.status === 401 && token) {
    const retry = await fetchWithToken<T>(path, init, true);
    if (retry.response.status !== 401) {
      response = retry.response;
      payload = retry.payload;
    } else {
      emitSessionExpired();
      const { auth, mod } = await loadFirebaseAuth();
      await mod.signOut(auth).catch(() => {
        // Storage locked — the auth listener will eventually pick up the
        // cleared state. Toast is already on screen so the user knows.
      });
    }
  }

  if (!response.ok || !payload || payload.success === false) {
    const code = payload && payload.success === false ? payload.error.code : undefined;
    const message =
      payload && payload.success === false
        ? payload.error.message
        : response.statusText || "Request failed";
    throw new ApiError(response.status, code, message);
  }

  return payload;
}

/** Returns just the `data` field of the envelope. Most callers use this. */
export async function apiFetch<T = unknown>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const env = await rawFetch<T>(path, init);
  return env.data;
}

/**
 * Returns the full success envelope (`data` + `meta`). Use this when the
 * caller needs `meta.total` / `meta.page` etc. for pagination — apiFetch
 * alone discards `meta`.
 */
export async function apiFetchEnvelope<T = unknown>(
  path: string,
  init: RequestInit = {},
): Promise<Envelope<T>> {
  const env = await rawFetch<T>(path, init);
  return { data: env.data, meta: env.meta ?? {} };
}
