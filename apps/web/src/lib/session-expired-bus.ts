// Module-level pub/sub used by api-client to tell the rest of the app that a
// 401 response survived a force-refreshed token retry — i.e. the session is
// genuinely dead. A bus avoids a circular import between api-client (which
// must not depend on React) and AuthProvider (the natural toast renderer).
//
// Lives in client-only code paths; the typeof window guard keeps SSR imports
// inert instead of throwing on `new EventTarget()` in environments without it.

const EVENT_NAME = "dmv-session-expired";
const target: EventTarget | null =
  typeof window !== "undefined" ? new EventTarget() : null;

export function emitSessionExpired(): void {
  target?.dispatchEvent(new Event(EVENT_NAME));
}

export function subscribeSessionExpired(handler: () => void): () => void {
  if (!target) return () => {};
  target.addEventListener(EVENT_NAME, handler);
  return () => target.removeEventListener(EVENT_NAME, handler);
}
