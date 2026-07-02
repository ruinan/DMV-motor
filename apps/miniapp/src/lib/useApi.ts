import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from './request'
import { subscribe } from './bus'
import { cacheGet, cacheEvict, cachedFetch, isFresh } from './cache'
import { useMinLoading } from './useMinLoading'

export type ApiState<T> = {
  data: T | undefined
  error: any
  loading: boolean
  refresh: () => void
}

/**
 * Minimal data hook — the miniapp's stand-in for the web's TanStack Query
 * hooks, now with the same throttling: a fresh TTL-cache entry renders
 * synchronously with no request at all; a stale entry renders immediately
 * while a deduped refetch runs behind it (stale-while-revalidate). Refetches
 * when the path changes or someone calls invalidate() with a matching prefix
 * (which evicts the cache first — see bus.ts). Pass null to disable.
 *
 * `loading` means "fetching with nothing to show yet", smoothed so it stays
 * visible ≥300ms once shown (web use-min-loading parity).
 */
export function useApi<T = any>(path: string | null): ApiState<T> {
  const [data, setData] = useState<T | undefined>(() =>
    path ? (cacheGet(path)?.data as T | undefined) : undefined
  )
  const [error, setError] = useState<any>(null)
  const [fetching, setFetching] = useState(() => !!path && !isFresh(path))
  // Bumps on invalidate/refresh so the fetch effect re-runs.
  const [tick, setTick] = useState(0)
  const alive = useRef(true)

  // Manual refresh must bypass a still-fresh entry (e.g. 我的 page re-reads
  // entitlements right after a redeem), so evict before refetching.
  const refresh = useCallback(() => {
    if (path) cacheEvict(path)
    setTick(n => n + 1)
  }, [path])

  useEffect(() => {
    alive.current = true
    return () => { alive.current = false }
  }, [])

  useEffect(() => {
    if (!path) return
    // bus.invalidate already evicted the cache; just re-run the fetch effect.
    return subscribe(path, () => setTick(n => n + 1))
  }, [path])

  useEffect(() => {
    if (!path) { setFetching(false); return }
    const entry = cacheGet(path)
    if (entry) {
      // Serve immediately; a fresh entry ends here, a stale one revalidates.
      setData(entry.data as T)
      setError(null)
      if (isFresh(path)) { setFetching(false); return }
    }
    let stale = false
    setFetching(true)
    cachedFetch(path, async () => ({ data: await api<T>(path), meta: {} }))
      .then(e => { if (!stale && alive.current) { setData(e.data as T); setError(null) } })
      .catch(e => { if (!stale && alive.current) setError(e) })
      .finally(() => { if (!stale && alive.current) setFetching(false) })
    return () => { stale = true }
  }, [path, tick])

  const loading = useMinLoading(fetching && data === undefined)

  return { data, error, loading, refresh }
}
