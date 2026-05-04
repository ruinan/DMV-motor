import { signOut } from "firebase/auth";
import { firebaseAuth } from "@/lib/firebase";

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

async function getIdToken(): Promise<string | null> {
  const user = firebaseAuth.currentUser;
  if (!user) return null;
  return user.getIdToken();
}

type SuccessEnvelope<T> = { success: true; data: T; meta?: unknown };
type ErrorEnvelope = {
  success: false;
  error: { code: string; message: string; details?: unknown };
};
type ApiResponse<T> = SuccessEnvelope<T> | ErrorEnvelope;

export type Envelope<T> = { data: T; meta: unknown };

async function rawFetch<T>(
  path: string,
  init: RequestInit,
): Promise<SuccessEnvelope<T>> {
  const token = await getIdToken();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, { ...init, headers });
  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;

  // 401 with a token sent = our token is bad (revoked, expired, malformed).
  // Sign out so the auth listener tears down the session and RequireAuth
  // redirects to /login. Firebase already auto-refreshes; if the refreshed
  // token is also rejected the user genuinely needs to re-authenticate.
  if (response.status === 401 && token) {
    await signOut(firebaseAuth).catch(() => {
      // signOut can fail if storage is locked; the throw below still fires
      // and the auth listener will eventually pick up the cleared state.
    });
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
