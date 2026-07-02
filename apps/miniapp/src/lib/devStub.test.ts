import { describe, it, expect } from 'vitest'
import { stubFor } from './devStub'

// The dev stub must serve every endpoint the M1 shell reads, with shapes that
// match the web hooks' DTOs (apps/web/src/lib/hooks/*) so ported UI code works
// unchanged against it.
describe('dev stub M1 coverage', () => {
  it('strips query strings before lookup', () => {
    const a = stubFor('/api/v1/exams?language=zh')
    const b = stubFor('/api/v1/exams')
    expect(a).toEqual(b)
    expect(a.exams?.length).toBeGreaterThan(0)
  })

  it('/me matches MeResponse shape', () => {
    const me = stubFor('/api/v1/me')
    expect(typeof me.user_id).toBe('string')
    expect(typeof me.email).toBe('string')
    expect(['en', 'zh']).toContain(me.language)
    expect(typeof me.access.has_active_pass).toBe('boolean')
    expect(typeof me.access.mock_remaining).toBe('number')
    expect(me.learning).toHaveProperty('has_in_progress_practice')
    expect(me.current_exam).toMatchObject({
      id: expect.any(String),
      state_code: expect.any(String),
      license_class: expect.any(String),
      name_en: expect.any(String),
      name_zh: expect.any(String)
    })
  })

  it('/exams returns the catalog with localized names', () => {
    const { exams } = stubFor('/api/v1/exams')
    expect(exams.length).toBeGreaterThanOrEqual(2)
    for (const e of exams) {
      expect(e).toMatchObject({
        id: expect.any(String),
        state_code: expect.any(String),
        license_class: expect.any(String),
        name: expect.any(String)
      })
    }
  })

  it('/topics returns items[]', () => {
    const { items } = stubFor('/api/v1/topics')
    expect(items.length).toBeGreaterThan(0)
    expect(items[0]).toMatchObject({
      id: expect.any(String),
      code: expect.any(String),
      name_en: expect.any(String),
      name_zh: expect.any(String),
      is_key_topic: expect.any(Boolean)
    })
  })

  it('/topics/mastery returns topics + summary', () => {
    const m = stubFor('/api/v1/topics/mastery')
    expect(m.summary.total_sub_topics).toBeGreaterThan(0)
    expect(m.topics[0].mastery_progress).toMatchObject({
      attempted: expect.any(Number),
      progress_percent: expect.any(Number)
    })
    expect(Array.isArray(m.topics[0].sub_topics)).toBe(true)
  })

  it('/summary matches SummaryResponse shape', () => {
    const s = stubFor('/api/v1/summary')
    expect(typeof s.completion_score).toBe('number')
    expect(Array.isArray(s.weak_topics)).toBe(true)
    expect(s.next_action).toHaveProperty('type')
  })

  it('/readiness matches ReadinessResponse shape', () => {
    const r = stubFor('/api/v1/readiness')
    expect(typeof r.readiness_score).toBe('number')
    expect(typeof r.is_ready_candidate).toBe('boolean')
    expect(Array.isArray(r.missing_gates)).toBe(true)
  })

  it('/engagement matches Engagement shape', () => {
    const e = stubFor('/api/v1/engagement?tz_offset_minutes=480')
    expect(typeof e.current_streak_days).toBe('number')
    expect(typeof e.answered_today).toBe('number')
    expect(typeof e.daily_goal).toBe('number')
  })
})
