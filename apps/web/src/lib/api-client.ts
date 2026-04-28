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

type ApiResponse<T> =
  | { success: true; data: T; meta?: unknown }
  | { success: false; error: { code: string; message: string; details?: unknown } };

export async function apiFetch<T = unknown>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const token = await getIdToken();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, { ...init, headers });
  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;

  if (!response.ok || !payload || payload.success === false) {
    const code = payload && payload.success === false ? payload.error.code : undefined;
    const message =
      payload && payload.success === false
        ? payload.error.message
        : response.statusText || "Request failed";
    throw new ApiError(response.status, code, message);
  }

  return payload.data;
}
