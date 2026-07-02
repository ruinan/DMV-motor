// Canned responses for the dev-bypass front-end preview (no backend, no
// Firebase). Keyed by API path — extend as pages are ported. These are only
// ever read behind DEV_BYPASS/isDevSession(), which is false in a real prod
// build, so this data never reaches production users.
//
// Shapes mirror the web hooks' DTOs (apps/web/src/lib/hooks/*) so ported UI
// code runs unchanged against the stub — devStub.test.ts pins that contract.

const topics = [
  { id: 't-signs', parent_topic_id: null, code: 'CAC_SIGNS', name_en: 'Traffic Signs & Signals', name_zh: '交通标志与信号', is_key_topic: true, risk_level: 'high', sort_order: 1 },
  { id: 't-row', parent_topic_id: null, code: 'CAC_ROW', name_en: 'Right of Way', name_zh: '路权', is_key_topic: true, risk_level: 'high', sort_order: 2 },
  { id: 't-speed', parent_topic_id: null, code: 'CAC_SPEED', name_en: 'Speed Limits', name_zh: '限速规定', is_key_topic: false, risk_level: 'medium', sort_order: 3 },
  { id: 't-park', parent_topic_id: null, code: 'CAC_PARK', name_en: 'Parking Rules', name_zh: '停车规则', is_key_topic: false, risk_level: 'low', sort_order: 4 }
]

const masteryTopic = (t: (typeof topics)[number], mastered: boolean, progress: number) => ({
  topic_id: t.id,
  code: t.code,
  name_en: t.name_en,
  name_zh: t.name_zh,
  is_mastered: mastered,
  mastery_progress: {
    attempted: mastered ? 24 : 9,
    accuracy_percent: mastered ? 92 : 67,
    recent_correct: mastered ? 10 : 4,
    recent_window: 10,
    accuracy_threshold: 85,
    recent_correct_threshold: 8,
    progress_percent: progress
  },
  sub_topics: [
    { sub_topic_id: `${t.id}-a`, code: `${t.code}_A`, name_en: `${t.name_en} basics`, name_zh: `${t.name_zh}基础`, is_mastered: mastered, attempted_count: 12, correct_count: mastered ? 11 : 7, bank_size: 8 },
    { sub_topic_id: `${t.id}-b`, code: `${t.code}_B`, name_en: `${t.name_en} advanced`, name_zh: `${t.name_zh}进阶`, is_mastered: false, attempted_count: 6, correct_count: 4, bank_size: 8 }
  ]
})

export const devStub: Record<string, any> = {
  '/api/v1/me': {
    user_id: 'dev-user-1',
    email: 'dev@local.test',
    language: 'zh',
    access: { state: 'active', has_active_pass: true, expires_at: '2026-12-31T00:00:00Z', mock_remaining: 3 },
    learning: { has_in_progress_practice: false, in_progress_practice: null, has_in_progress_review: false },
    current_exam: { id: 'CA-C', state_code: 'CA', license_class: 'C', name_en: 'California Class C', name_zh: '加州 C 类驾照' }
  },
  '/api/v1/exams': {
    exams: [
      { id: 'CA-C', state_code: 'CA', license_class: 'C', name: '加州 C 类驾照' },
      { id: 'CA-M1', state_code: 'CA', license_class: 'M1', name: '加州 M1 摩托车' }
    ]
  },
  '/api/v1/topics': { items: topics },
  '/api/v1/topics/mastery': {
    topics: [
      masteryTopic(topics[0], true, 100),
      masteryTopic(topics[1], false, 60),
      masteryTopic(topics[2], false, 35),
      masteryTopic(topics[3], false, 10)
    ],
    summary: { total_sub_topics: 8, mastered_sub_topics: 2 }
  },
  '/api/v1/summary': {
    access_state: 'active',
    completion_score: 42,
    weak_topics: [
      { topic_id: 't-row', label: '路权' },
      { topic_id: 't-speed', label: '限速规定' }
    ],
    next_action: { type: 'practice', label: '继续练习薄弱知识点' },
    readiness_score: 58,
    is_ready_candidate: false
  },
  '/api/v1/readiness': {
    readiness_score: 58,
    is_ready_candidate: false,
    missing_gates: ['MOCK_SCORE_NOT_STABLE', 'PERSISTENT_WEAK_POINT']
  },
  '/api/v1/engagement': {
    current_streak_days: 4,
    answered_today: 6,
    daily_goal: 10
  }
}

/** Stub payload for a path (query string ignored; empty object if none yet). */
export function stubFor(path: string): any {
  const i = path.indexOf('?')
  const key = i === -1 ? path : path.slice(0, i)
  return devStub[key] ?? {}
}
