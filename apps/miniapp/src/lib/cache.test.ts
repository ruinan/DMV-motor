import { describe, it, expect, beforeEach } from 'vitest'
import {
  ttlFor,
  cacheGet,
  cacheSet,
  isFresh,
  cacheEvict,
  cachedFetch,
  resetCache
} from '@/lib/cache'

beforeEach(() => resetCache())

describe('ttlFor — per-route TTLs mirror the web staleTimes', () => {
  it('entitlements (30s) wins over the broader exams catalog rule (5min)', () => {
    expect(ttlFor('/api/v1/exams/entitlements')).toBe(30_000)
    expect(ttlFor('/api/v1/exams?language=zh')).toBe(300_000)
  })

  it('ignores query strings when matching', () => {
    expect(ttlFor('/api/v1/me?x=1')).toBe(30_000)
  })

  it('falls back to the 60s default for unlisted routes', () => {
    expect(ttlFor('/api/v1/summary')).toBe(60_000)
    expect(ttlFor('/api/v1/topics/mastery')).toBe(60_000)
  })

  it('practice sessions are never cached (per-answer freshness)', () => {
    expect(ttlFor('/api/v1/practice/sessions/s1/attempts')).toBe(0)
  })
})

describe('freshness', () => {
  it('an entry is fresh strictly within its TTL', () => {
    const t0 = 1_000_000
    cacheSet('/api/v1/me', { uid: 'u1' }, {}, t0)
    expect(isFresh('/api/v1/me', t0 + 29_999)).toBe(true)
    expect(isFresh('/api/v1/me', t0 + 30_000)).toBe(false)
  })

  it('a TTL-0 route is stale immediately after being set', () => {
    const t0 = 1_000_000
    cacheSet('/api/v1/practice/sessions/s1/attempts', { items: [] }, {}, t0)
    expect(isFresh('/api/v1/practice/sessions/s1/attempts', t0)).toBe(false)
  })

  it('a missing entry is not fresh', () => {
    expect(isFresh('/api/v1/me')).toBe(false)
  })

  it('cacheGet returns the stored entry regardless of freshness', () => {
    cacheSet('/api/v1/me', { uid: 'u1' }, { page: 1 }, 0)
    const entry = cacheGet('/api/v1/me')
    expect(entry?.data).toEqual({ uid: 'u1' })
    expect(entry?.meta).toEqual({ page: 1 })
  })
})

describe('cacheEvict — prefix matching, query strings ignored (bus semantics)', () => {
  it('evicts entries whose route starts with the prefix, keeps the rest', () => {
    cacheSet('/api/v1/mistakes?page=1', { items: [1] })
    cacheSet('/api/v1/mistakes?page=2', { items: [2] })
    cacheSet('/api/v1/summary', { score: 10 })
    cacheEvict('/api/v1/mistakes')
    expect(cacheGet('/api/v1/mistakes?page=1')).toBeUndefined()
    expect(cacheGet('/api/v1/mistakes?page=2')).toBeUndefined()
    expect(cacheGet('/api/v1/summary')).toBeDefined()
  })

  it('a broad prefix clears everything under it', () => {
    cacheSet('/api/v1/me', {})
    cacheSet('/api/v1/exams?language=zh', {})
    cacheEvict('/api/v1/')
    expect(cacheGet('/api/v1/me')).toBeUndefined()
    expect(cacheGet('/api/v1/exams?language=zh')).toBeUndefined()
  })
})

describe('cachedFetch — in-flight dedupe + write-through', () => {
  it('concurrent calls for the same path share one fetch', async () => {
    let calls = 0
    const fetcher = async () => {
      calls++
      return { data: { n: calls }, meta: {} }
    }
    const [a, b] = await Promise.all([
      cachedFetch('/api/v1/me', fetcher),
      cachedFetch('/api/v1/me', fetcher)
    ])
    expect(calls).toBe(1)
    expect(a.data).toEqual({ n: 1 })
    expect(b.data).toEqual({ n: 1 })
  })

  it('stores the result so cacheGet serves it afterwards', async () => {
    await cachedFetch('/api/v1/me', async () => ({ data: { uid: 'u1' }, meta: { m: 1 } }))
    const entry = cacheGet('/api/v1/me')
    expect(entry?.data).toEqual({ uid: 'u1' })
    expect(entry?.meta).toEqual({ m: 1 })
  })

  it('a later call after settling fetches again (inflight cleared)', async () => {
    let calls = 0
    const fetcher = async () => {
      calls++
      return { data: calls, meta: {} }
    }
    await cachedFetch('/api/v1/me', fetcher)
    await cachedFetch('/api/v1/me', fetcher)
    expect(calls).toBe(2)
  })

  it('failures propagate, are not cached, and clear the in-flight slot', async () => {
    let calls = 0
    const failing = async () => {
      calls++
      throw new Error('boom')
    }
    await expect(cachedFetch('/api/v1/me', failing)).rejects.toThrow('boom')
    expect(cacheGet('/api/v1/me')).toBeUndefined()
    await expect(cachedFetch('/api/v1/me', failing)).rejects.toThrow('boom')
    expect(calls).toBe(2)
  })
})
