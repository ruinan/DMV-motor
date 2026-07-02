import { describe, it, expect, vi, beforeEach } from 'vitest'
import { subscribe, invalidate } from './bus'
import { cacheGet, cacheSet, resetCache } from './cache'

beforeEach(() => resetCache())

describe('invalidation bus', () => {
  it('notifies a subscriber whose path starts with the invalidated prefix', () => {
    const fn = vi.fn()
    const off = subscribe('/api/v1/practice/sessions/stats', fn)
    invalidate('/api/v1/practice')
    expect(fn).toHaveBeenCalledTimes(1)
    off()
  })

  it('does not notify unrelated paths', () => {
    const fn = vi.fn()
    const off = subscribe('/api/v1/topics', fn)
    invalidate('/api/v1/practice')
    expect(fn).not.toHaveBeenCalled()
    off()
  })

  it('exact path invalidation notifies the exact subscriber', () => {
    const fn = vi.fn()
    const off = subscribe('/api/v1/me', fn)
    invalidate('/api/v1/me')
    expect(fn).toHaveBeenCalledTimes(1)
    off()
  })

  it('unsubscribe stops notifications', () => {
    const fn = vi.fn()
    const off = subscribe('/api/v1/me', fn)
    off()
    invalidate('/api/v1/me')
    expect(fn).not.toHaveBeenCalled()
  })

  it('ignores query strings on the subscribed path', () => {
    const fn = vi.fn()
    const off = subscribe('/api/v1/exams?language=zh', fn)
    invalidate('/api/v1/exams')
    expect(fn).toHaveBeenCalledTimes(1)
    off()
  })

  it('evicts matching cache entries so the refetch hits the network', () => {
    cacheSet('/api/v1/me', { uid: 'u1' })
    cacheSet('/api/v1/summary', { score: 1 })
    invalidate('/api/v1/me')
    expect(cacheGet('/api/v1/me')).toBeUndefined()
    expect(cacheGet('/api/v1/summary')).toBeDefined()
  })
})
