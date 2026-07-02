import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from './request'
import { subscribe } from './bus'

export type ApiState<T> = {
  data: T | undefined
  error: any
  loading: boolean
  refresh: () => void
}

/**
 * Minimal data hook — the miniapp's stand-in for the web's TanStack Query
 * hooks. Fetches on mount, refetches when the path changes or someone calls
 * invalidate() with a matching prefix (see bus.ts). Pass null to disable.
 */
export function useApi<T = any>(path: string | null): ApiState<T> {
  const [data, setData] = useState<T | undefined>(undefined)
  const [error, setError] = useState<any>(null)
  const [loading, setLoading] = useState(!!path)
  // Bumps on invalidate/refresh so the fetch effect re-runs.
  const [tick, setTick] = useState(0)
  const alive = useRef(true)

  const refresh = useCallback(() => setTick(n => n + 1), [])

  useEffect(() => {
    alive.current = true
    return () => { alive.current = false }
  }, [])

  useEffect(() => {
    if (!path) return
    return subscribe(path, refresh)
  }, [path, refresh])

  useEffect(() => {
    if (!path) { setLoading(false); return }
    let stale = false
    setLoading(true)
    api<T>(path)
      .then(d => { if (!stale && alive.current) { setData(d); setError(null) } })
      .catch(e => { if (!stale && alive.current) setError(e) })
      .finally(() => { if (!stale && alive.current) setLoading(false) })
    return () => { stale = true }
  }, [path, tick])

  return { data, error, loading, refresh }
}
