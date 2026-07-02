// TTL read cache + in-flight dedupe — the miniapp's stand-in for TanStack
// Query's staleTime layer on web. Tab switching redirects (full page remount),
// so without this every switch refires /me, /summary, /engagement… with a
// loading flash. Entries are keyed by the full path (query included); TTL and
// eviction matching ignore query strings, same as bus.ts.

export type CacheEntry = { data: unknown; meta: unknown; at: number }

const DEFAULT_TTL_MS = 60_000

/** Most-specific prefix first — the first match wins. Values mirror the web
 * hooks' staleTimes (use-me 30s, use-exams 5min, use-attempts 0, …). */
const TTL_RULES: ReadonlyArray<readonly [string, number]> = [
  ['/api/v1/exams/entitlements', 30_000],
  ['/api/v1/exams', 300_000],
  ['/api/v1/me', 30_000],
  ['/api/v1/backup', 30_000],
  ['/api/v1/mistakes', 30_000],
  ['/api/v1/mock-exams/attempts', 30_000],
  // Practice-session reads (attempt history) change on every answer.
  ['/api/v1/practice/sessions', 0]
]

const store = new Map<string, CacheEntry>()
const inflight = new Map<string, Promise<CacheEntry>>()

function stripQuery(path: string): string {
  const i = path.indexOf('?')
  return i === -1 ? path : path.slice(0, i)
}

export function ttlFor(path: string): number {
  const route = stripQuery(path)
  for (const [prefix, ttl] of TTL_RULES) {
    if (route.startsWith(prefix)) return ttl
  }
  return DEFAULT_TTL_MS
}

/** The stored entry, fresh or stale — callers show stale data while a refetch
 * runs (stale-while-revalidate), so staleness is not a miss here. */
export function cacheGet(path: string): CacheEntry | undefined {
  return store.get(path)
}

export function isFresh(path: string, now: number = Date.now()): boolean {
  const entry = store.get(path)
  if (!entry) return false
  return now - entry.at < ttlFor(path)
}

export function cacheSet(path: string, data: unknown, meta: unknown = {}, at: number = Date.now()): void {
  store.set(path, { data, meta, at })
}

/** Drop every entry whose route starts with `prefix` (query strings ignored on
 * both sides). Wired into bus.invalidate() so an invalidation also empties the
 * cache — otherwise a remounted page would happily serve the stale entry. */
export function cacheEvict(prefix: string): void {
  const p = stripQuery(prefix)
  for (const key of store.keys()) {
    if (stripQuery(key).startsWith(p)) store.delete(key)
  }
}

/** Run `fetcher` for `path`, sharing one request across concurrent callers and
 * writing the result through to the cache. Failures are not cached. */
export function cachedFetch(
  path: string,
  fetcher: () => Promise<{ data: unknown; meta: unknown }>
): Promise<CacheEntry> {
  const existing = inflight.get(path)
  if (existing) return existing
  const p = fetcher()
    .then(({ data, meta }) => {
      const entry: CacheEntry = { data, meta, at: Date.now() }
      store.set(path, entry)
      return entry
    })
    .finally(() => {
      inflight.delete(path)
    })
  inflight.set(path, p)
  return p
}

export function resetCache(): void {
  store.clear()
  inflight.clear()
}
