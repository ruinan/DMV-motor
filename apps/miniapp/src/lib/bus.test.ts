import { describe, it, expect, vi } from 'vitest'
import { subscribe, invalidate } from './bus'

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
})
