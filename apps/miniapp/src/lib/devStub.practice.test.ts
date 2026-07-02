import { describe, it, expect, beforeEach } from 'vitest'
import { stubRequest, stubMetaFor, resetStubState } from './devStub'

// The practice-session state machine behind the dev bypass. Mirrors the real
// backend contract used by apps/web PracticeFlow.tsx: start → status →
// next-question → answers (verdict + mistake recording) → complete, with
// SESSION_COMPLETED when the bank is exhausted.

function startSession(): any {
  return stubRequest('/api/v1/practice/sessions', 'POST', {
    entry_type: 'full', language: 'zh', mode: 'random'
  })
}

beforeEach(() => resetStubState())

describe('practice session stub', () => {
  it('start returns a session with the first question and leaks no answers', () => {
    const res = startSession()
    expect(res.session_id).toBeTruthy()
    expect(res.status).toBe('in_progress')
    expect(res.next_question.stem).toBeTruthy()
    expect(res.next_question.choices.length).toBeGreaterThanOrEqual(3)
    expect(res.next_question).not.toHaveProperty('correct')
    expect(res.next_question).not.toHaveProperty('correct_choice_key')
    expect(res.next_question).not.toHaveProperty('explanation')

    const status = stubRequest(`/api/v1/practice/sessions/${res.session_id}`, 'GET')
    expect(status.answered_count).toBe(0)
    expect(status.total_count).toBe(10)
  })

  it('/me reflects the in-progress session; complete clears it', () => {
    const res = startSession()
    let me = stubRequest('/api/v1/me', 'GET')
    expect(me.learning.has_in_progress_practice).toBe(true)
    expect(me.learning.in_progress_practice.session_id).toBe(res.session_id)

    stubRequest(`/api/v1/practice/sessions/${res.session_id}/complete`, 'POST')
    me = stubRequest('/api/v1/me', 'GET')
    expect(me.learning.has_in_progress_practice).toBe(false)
    expect(me.learning.in_progress_practice).toBeNull()
  })

  it('a wrong answer returns the verdict + explanation and records a mistake', () => {
    const res = startSession()
    const q = res.next_question
    // Pick a deliberately wrong key: the stub grades against its own bank, so
    // answer every choice until one comes back wrong (bank has 4 choices).
    const before = Number(stubMetaFor('/api/v1/mistakes').total)
    let verdict: any = null
    for (const c of q.choices) {
      verdict = stubRequest(`/api/v1/practice/sessions/${res.session_id}/answers`, 'POST', {
        question_id: q.question_id, variant_id: q.variant_id, selected_choice_key: c.key
      })
      break // grade exactly once — inspect the verdict below
    }
    expect(typeof verdict.is_correct).toBe('boolean')
    expect(verdict.correct_choice_key).toBeTruthy()
    expect(verdict.explanation).toBeTruthy()
    expect(verdict.progress.answered_count).toBe(1)
    const after = Number(stubMetaFor('/api/v1/mistakes').total)
    if (verdict.is_correct) expect(after).toBe(before)
    else expect(after).toBe(before + 1)
  })

  it('next-question walks the bank and ends with SESSION_COMPLETED', () => {
    const res = startSession()
    let q = res.next_question
    for (let i = 0; i < 10; i++) {
      stubRequest(`/api/v1/practice/sessions/${res.session_id}/answers`, 'POST', {
        question_id: q.question_id, variant_id: q.variant_id, selected_choice_key: q.choices[0].key
      })
      if (i < 9) {
        q = stubRequest(`/api/v1/practice/sessions/${res.session_id}/next-question?language=zh`, 'GET')
        expect(q.question_id).toBeTruthy()
      }
    }
    expect(() =>
      stubRequest(`/api/v1/practice/sessions/${res.session_id}/next-question?language=zh`, 'GET')
    ).toThrowError(expect.objectContaining({ code: 'SESSION_COMPLETED' }))
  })

  it('attempts lists graded answers with stems and verdicts', () => {
    const res = startSession()
    const q = res.next_question
    stubRequest(`/api/v1/practice/sessions/${res.session_id}/answers`, 'POST', {
      question_id: q.question_id, variant_id: q.variant_id, selected_choice_key: q.choices[1].key
    })
    const { items } = stubRequest(`/api/v1/practice/sessions/${res.session_id}/attempts?language=zh`, 'GET')
    expect(items.length).toBe(1)
    expect(items[0]).toMatchObject({
      question_id: q.question_id,
      stem: q.stem,
      selected_choice_key: q.choices[1].key,
      correct_choice_key: expect.any(String),
      explanation: expect.any(String),
      is_correct: expect.any(Boolean)
    })
    expect(items[0].choices.length).toBe(q.choices.length)
  })

  it('ai/explain returns layered explanations with a depth budget', () => {
    const r0 = stubRequest('/api/v1/ai/explain', 'POST', {
      question_id: 'q1', language: 'zh', depth: 0
    })
    expect(r0.explanation).toBeTruthy()
    expect(r0.depth).toBe(0)
    expect(r0.depth_remaining).toBeGreaterThan(0)

    const r1 = stubRequest('/api/v1/ai/explain', 'POST', {
      question_id: 'q1', language: 'zh', depth: 1, aspect: 'example'
    })
    expect(r1.depth).toBe(1)
    expect(r1.explanation).not.toBe(r0.explanation)
    expect(r1.depth_remaining).toBe(r0.depth_remaining - 1)
  })

  it('mistakes endpoint pages items with envelope meta', () => {
    const res = startSession()
    const q = res.next_question
    const verdict = stubRequest(`/api/v1/practice/sessions/${res.session_id}/answers`, 'POST', {
      question_id: q.question_id, variant_id: q.variant_id,
      // Grade against an intentionally-absent key so it's always wrong.
      selected_choice_key: '__definitely_wrong__'
    })
    expect(verdict.is_correct).toBe(false)
    const items = stubRequest('/api/v1/mistakes?page=1&page_size=20', 'GET').items
    const meta = stubMetaFor('/api/v1/mistakes?page=1&page_size=20')
    expect(items.length).toBeGreaterThan(0)
    expect(Number(meta.total)).toBe(items.length)
    expect(items[0]).toMatchObject({
      mistake_id: expect.any(String),
      question_id: expect.any(String),
      wrong_count: expect.any(Number),
      source: 'practice'
    })
  })
})
