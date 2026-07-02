import { describe, it, expect, beforeEach } from 'vitest'
import { stubRequest, resetStubState } from './devStub'

// Settings-surface routes behind the dev bypass: exam switching, the
// three-state entitlement catalog (paid/free/locked), open-free, activation
// codes, and backup status.

beforeEach(() => resetStubState())

describe('me / exam catalog stub', () => {
  it('entitlements expose per-exam subscribed/opened flags', () => {
    const { entitlements } = stubRequest('/api/v1/exams/entitlements', 'GET')
    const byId = new Map(entitlements.map((e: any) => [e.exam_id, e]))
    expect((byId.get('CA-C') as any).subscribed).toBe(true)
    expect((byId.get('CA-M1') as any).subscribed).toBe(false)
    expect((byId.get('CA-M1') as any).opened).toBe(false)
  })

  it('PUT /me/exam switches the current exam (and the theme source)', () => {
    stubRequest('/api/v1/me/exam', 'PUT', { exam_id: 'CA-M1' })
    const me = stubRequest('/api/v1/me', 'GET')
    expect(me.current_exam.id).toBe('CA-M1')
    expect(me.current_exam.license_class).toBe('M1')
  })

  it('open-free marks the exam opened', () => {
    stubRequest('/api/v1/exams/CA-M1/open-free', 'POST')
    const { entitlements } = stubRequest('/api/v1/exams/entitlements', 'GET')
    const m1 = entitlements.find((e: any) => e.exam_id === 'CA-M1')
    expect(m1.opened).toBe(true)
    expect(m1.subscribed).toBe(false)
  })

  it('redeem accepts the dev code and rejects garbage', () => {
    const ok = stubRequest('/api/v1/access/redeem?code=DEV-2026', 'POST')
    expect(ok.redeemed).toBe(true)
    expect(() => stubRequest('/api/v1/access/redeem?code=NOPE', 'POST'))
      .toThrowError(expect.objectContaining({ code: 'CODE_INVALID' }))
  })

  it('backup sync updates the latest-backup timestamp', () => {
    const before = stubRequest('/api/v1/backup/latest', 'GET')
    expect(before.backed_up_at).toBeNull()
    stubRequest('/api/v1/backup/sync', 'POST')
    const after = stubRequest('/api/v1/backup/latest', 'GET')
    expect(typeof after.backed_up_at).toBe('string')
  })
})
