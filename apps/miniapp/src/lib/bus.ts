// Tiny invalidation bus — the miniapp's stand-in for TanStack Query's partial
// key invalidation on web. useApi subscribes with its request path; mutations
// call invalidate(prefix) and every subscriber whose path starts with that
// prefix refetches. Matching cache entries are evicted first so those
// refetches (and later remounts) hit the network, not the TTL cache.
// Query strings are ignored for matching.

import { cacheEvict } from './cache'

type Listener = () => void

const subscribers = new Map<Listener, string>()

function stripQuery(path: string): string {
  const i = path.indexOf('?')
  return i === -1 ? path : path.slice(0, i)
}

/** Register a refetch callback for a request path. Returns an unsubscriber. */
export function subscribe(path: string, fn: Listener): () => void {
  subscribers.set(fn, stripQuery(path))
  return () => { subscribers.delete(fn) }
}

/** Evict matching cache entries, then notify every subscriber whose path
 * starts with `prefix`. */
export function invalidate(prefix: string): void {
  cacheEvict(prefix)
  const p = stripQuery(prefix)
  for (const [fn, path] of subscribers) {
    if (path.startsWith(p)) fn()
  }
}
