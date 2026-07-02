import { describe, it, expect, beforeEach } from 'vitest'
import { stubRequest, resetStubState } from './devStub'

// Mock-exam state machine behind the dev bypass. Mirrors the backend contract
// used by apps/web MockLanding/MockExam: start consumes quota and returns the
// full paper; answers grade immediately with a wrong-answer cap; submit / exit
// finalize; history + stats feed the landing page.

const start = (): any => stubRequest('/api/v1/mock-exams/attempts', 'POST', { language: 'zh' })

beforeEach(() => resetStubState())

describe('mock exam stub', () => {
  it('start consumes quota and returns the full question paper', () => {
    const before = stubRequest('/api/v1/me', 'GET').access.mock_remaining
    const res = start()
    expect(res.mock_attempt_id).toBeTruthy()
    expect(res.status).toBe('in_progress')
    expect(res.questions.length).toBeGreaterThanOrEqual(5)
    expect(res.mock_remaining_after_start).toBe(before - 1)
    expect(stubRequest('/api/v1/me', 'GET').access.mock_remaining).toBe(before - 1)
  })

  it('detail returns timer fields and hides explanations while in progress', () => {
    const res = start()
    const q = res.questions[0]
    stubRequest(`/api/v1/mock-exams/attempts/${res.mock_attempt_id}/answers`, 'POST', {
      question_id: q.question_id, variant_id: q.variant_id, selected_choice_key: q.choices[0].key
    })
    const detail = stubRequest(`/api/v1/mock-exams/attempts/${res.mock_attempt_id}?language=zh`, 'GET')
    expect(detail.status).toBe('in_progress')
    expect(detail.time_limit_seconds).toBeGreaterThan(0)
    expect(detail.started_at).toBeTruthy()
    expect(detail.score_percent).toBe(-1)
    expect(detail.saved_answers.length).toBe(1)
    expect(detail.saved_answers[0].explanation).toBe('')
  })

  it('answers grade immediately and terminate past the wrong cap', () => {
    const res = start()
    let terminated = false
    let cap = 0
    for (const q of res.questions) {
      const r = stubRequest(`/api/v1/mock-exams/attempts/${res.mock_attempt_id}/answers`, 'POST', {
        question_id: q.question_id, variant_id: q.variant_id,
        selected_choice_key: '__wrong__'
      })
      expect(r.is_correct).toBe(false)
      cap = r.max_allowed_wrong
      if (r.should_terminate) { terminated = true; break }
    }
    expect(terminated).toBe(true)
    const detail = stubRequest(`/api/v1/mock-exams/attempts/${res.mock_attempt_id}?language=zh`, 'GET')
    expect(detail.status).toBe('ended_by_failure')
    expect(detail.wrong_count).toBe(cap + 1)
  })

  it('submit scores the paper, exposes explanations, and feeds history/stats', () => {
    const res = start()
    for (const q of res.questions) {
      // Look the correct key up via the review endpoint so the paper passes
      // (all-A answering trips the wrong-answer cap and terminates early).
      const review = stubRequest(`/api/v1/mistakes/${q.question_id}/review?language=zh`, 'GET')
      stubRequest(`/api/v1/mock-exams/attempts/${res.mock_attempt_id}/answers`, 'POST', {
        question_id: q.question_id, variant_id: q.variant_id,
        selected_choice_key: review.correct_choice_key
      })
    }
    const sub = stubRequest(`/api/v1/mock-exams/attempts/${res.mock_attempt_id}/submit`, 'POST')
    expect(sub.status).toBe('submitted')
    expect(sub.score_percent).toBeGreaterThanOrEqual(0)
    expect(sub.correct_count + sub.wrong_count).toBe(res.questions.length)
    expect(Array.isArray(sub.weak_topics)).toBe(true)

    const detail = stubRequest(`/api/v1/mock-exams/attempts/${res.mock_attempt_id}?language=zh`, 'GET')
    expect(detail.status).toBe('submitted')
    expect(detail.score_percent).toBe(sub.score_percent)
    expect(detail.saved_answers[0].explanation).not.toBe('')

    const history = stubRequest('/api/v1/mock-exams/attempts/history?limit=10', 'GET')
    expect(history.attempts.length).toBe(1)
    expect(history.attempts[0].status).toBe('submitted')

    const stats = stubRequest('/api/v1/mock-exams/attempts/stats', 'GET')
    expect(stats.total_attempts).toBe(1)
    expect(stats.submitted_count).toBe(1)
    expect(stats.latest_score_percent).toBe(sub.score_percent)
  })

  it('exit finalizes as ended_by_exit without a score', () => {
    const res = start()
    stubRequest(`/api/v1/mock-exams/attempts/${res.mock_attempt_id}/exit`, 'POST')
    const detail = stubRequest(`/api/v1/mock-exams/attempts/${res.mock_attempt_id}?language=zh`, 'GET')
    expect(detail.status).toBe('ended_by_exit')
    expect(detail.score_percent).toBe(-1)
    const stats = stubRequest('/api/v1/mock-exams/attempts/stats', 'GET')
    expect(stats.exited_count).toBe(1)
  })

  it('starting with zero quota is rejected with ACCESS_DENIED', () => {
    start(); start(); start() // stub quota = 3
    expect(() => start()).toThrowError(expect.objectContaining({ code: 'ACCESS_DENIED' }))
  })
})
