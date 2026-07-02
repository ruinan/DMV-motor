// Canned responses + a small in-memory state machine for the dev-bypass
// front-end preview (no backend, no Firebase). Only ever reached behind
// DEV_BYPASS/isDevSession(), which is false in a real prod build, so none of
// this ships to production users.
//
// Static GET shapes mirror the web hooks' DTOs (apps/web/src/lib/hooks/*);
// the practice-session flow mirrors the backend contract PracticeFlow.tsx
// uses. devStub.test.ts / devStub.practice.test.ts pin both contracts.
//
// The question bank is original placeholder content written for the stub —
// NOT handbook text and NOT the production question bank.

const topics = [
  { id: 't-signs', parent_topic_id: null, code: 'CAC_SIGNS', name_en: 'Traffic Signs & Signals', name_zh: '交通标志与信号', is_key_topic: true, risk_level: 'high', sort_order: 1 },
  { id: 't-row', parent_topic_id: null, code: 'CAC_ROW', name_en: 'Right of Way', name_zh: '路权', is_key_topic: true, risk_level: 'high', sort_order: 2 },
  { id: 't-speed', parent_topic_id: null, code: 'CAC_SPEED', name_en: 'Speed Limits', name_zh: '限速规定', is_key_topic: false, risk_level: 'medium', sort_order: 3 },
  { id: 't-park', parent_topic_id: null, code: 'CAC_PARK', name_en: 'Parking Rules', name_zh: '停车规则', is_key_topic: false, risk_level: 'low', sort_order: 4 }
]

export type StubQuestion = {
  question_id: string
  variant_id: string
  topic_id: string
  stem: string
  choices: { key: string; text: string }[]
  correct: string
  explanation: string
}

const QUESTIONS: StubQuestion[] = [
  {
    question_id: 'q-red-right', variant_id: 'v1', topic_id: 't-signs',
    stem: '红灯亮起时，你想右转。正确的做法是？',
    choices: [
      { key: 'A', text: '直接减速右转，红灯不管右转' },
      { key: 'B', text: '完全停稳，确认安全并让行后再右转（除非有禁止右转标志）' },
      { key: 'C', text: '等绿灯亮起才能右转' },
      { key: 'D', text: '鸣笛提醒后快速右转' }
    ],
    correct: 'B',
    explanation: '加州允许红灯右转，但必须先完全停稳、让行人和直行车辆，且路口没有"红灯禁止右转"标志。'
  },
  {
    question_id: 'q-stop-sign', variant_id: 'v1', topic_id: 't-signs',
    stem: '遇到 STOP（停车）标志时，你应该：',
    choices: [
      { key: 'A', text: '减速观察，无车即可通过' },
      { key: 'B', text: '在限制线前完全停稳后再继续行驶' },
      { key: 'C', text: '只有看到其他车辆时才需要停车' },
      { key: 'D', text: '闪灯后缓慢通过' }
    ],
    correct: 'B',
    explanation: 'STOP 标志要求完全停稳（车轮不再滚动），位置在限制线或人行横道前，观察让行后方可通过。'
  },
  {
    question_id: 'q-double-yellow', variant_id: 'v1', topic_id: 't-row',
    stem: '道路中央是双实黄线时，你可以：',
    choices: [
      { key: 'A', text: '跨越黄线超越慢车' },
      { key: 'B', text: '不得跨越双实黄线超车' },
      { key: 'C', text: '夜间可以跨越' },
      { key: 'D', text: '只要没有对向车就能跨越' }
    ],
    correct: 'B',
    explanation: '双实黄线表示两个方向都禁止跨线超车；仅在进出车道、按指示左转等法定情形下才可穿越。'
  },
  {
    question_id: 'q-school-speed', variant_id: 'v1', topic_id: 't-speed',
    stem: '学校区域内有儿童活动时，除非另有标示，限速为：',
    choices: [
      { key: 'A', text: '每小时 15 英里' },
      { key: 'B', text: '每小时 20 英里' },
      { key: 'C', text: '每小时 25 英里' },
      { key: 'D', text: '每小时 35 英里' }
    ],
    correct: 'C',
    explanation: '学校附近有儿童在场时，默认限速为 25 mph（部分路段可能标示更低）。'
  },
  {
    question_id: 'q-crosswalk', variant_id: 'v1', topic_id: 't-row',
    stem: '行人正在人行横道内通过，你应该：',
    choices: [
      { key: 'A', text: '减速绕过行人' },
      { key: 'B', text: '鸣笛提醒行人加快速度' },
      { key: 'C', text: '停车让行，等行人安全通过' },
      { key: 'D', text: '只需给老人和儿童让行' }
    ],
    correct: 'C',
    explanation: '行人在人行横道内享有路权，必须停车让行；催促或绕行都属于违法且危险的行为。'
  },
  {
    question_id: 'q-follow-3s', variant_id: 'v1', topic_id: 't-speed',
    stem: '正常路况下，与前车保持的安全跟车距离建议为：',
    choices: [
      { key: 'A', text: '1 秒' },
      { key: 'B', text: '2 秒' },
      { key: 'C', text: '3 秒' },
      { key: 'D', text: '10 秒' }
    ],
    correct: 'C',
    explanation: '普遍建议使用"3 秒法则"：前车通过固定参照物后，你至少 3 秒后才到达；恶劣天气应加大距离。'
  },
  {
    question_id: 'q-fog-light', variant_id: 'v1', topic_id: 't-signs',
    stem: '浓雾中行驶时，应使用：',
    choices: [
      { key: 'A', text: '远光灯' },
      { key: 'B', text: '近光灯' },
      { key: 'C', text: '双闪警示灯并停在车道内' },
      { key: 'D', text: '只开示宽灯' }
    ],
    correct: 'B',
    explanation: '雾天用近光灯：远光会被雾反射造成眩目，能见度反而更差。'
  },
  {
    question_id: 'q-siren', variant_id: 'v1', topic_id: 't-row',
    stem: '听到救护车鸣笛并看到闪灯驶近时，你应该：',
    choices: [
      { key: 'A', text: '加速让出路口' },
      { key: 'B', text: '在原车道立即刹停' },
      { key: 'C', text: '靠道路右侧停车，让其通过' },
      { key: 'D', text: '保持车速正常行驶' }
    ],
    correct: 'C',
    explanation: '遇执勤的紧急车辆必须靠右侧路边停车让行，路口内则应先通过路口再靠边。'
  },
  {
    question_id: 'q-park-downhill', variant_id: 'v1', topic_id: 't-park',
    stem: '车头朝下坡方向靠路缘停车时，前轮应：',
    choices: [
      { key: 'A', text: '转向路缘（右打方向）' },
      { key: 'B', text: '转离路缘（左打方向）' },
      { key: 'C', text: '保持直行方向' },
      { key: 'D', text: '任意方向均可' }
    ],
    correct: 'A',
    explanation: '下坡停车时前轮转向路缘，一旦溜车车轮会被路缘挡住；上坡（有路缘）则转离路缘。'
  },
  {
    question_id: 'q-round-sign', variant_id: 'v1', topic_id: 't-signs',
    stem: '圆形交通标志表示的是：',
    choices: [
      { key: 'A', text: '前方施工' },
      { key: 'B', text: '铁路道口预告' },
      { key: 'C', text: '禁止通行' },
      { key: 'D', text: '学校区域' }
    ],
    correct: 'B',
    explanation: '圆形标志专用于铁路道口预告（Railroad Crossing），提示减速观察并做好停车准备。'
  }
]

// ---------------------------------------------------------------------------
// Mutable state (module-level; reset per app launch / per test)
// ---------------------------------------------------------------------------

type StubAnswer = { question_id: string; selected: string; is_correct: boolean; submitted_at: string }
type StubSession = {
  id: string
  entry_type: string
  mode: string
  status: 'in_progress' | 'completed'
  questions: StubQuestion[]
  answers: StubAnswer[]
}
type StubMistake = {
  mistake_id: string
  question_id: string
  topic_id: string
  wrong_count: number
  last_wrong_at: string
  source: string
}

type StubMockAnswer = { question_id: string; selected: string; is_correct: boolean }
type StubMockAttempt = {
  id: string
  status: 'in_progress' | 'submitted' | 'ended_by_failure' | 'ended_by_exit'
  questions: StubQuestion[]
  answers: StubMockAnswer[]
  started_at: string
  submitted_at: string
  score_percent: number
  time_limit_seconds: number
}

const MOCK_QUOTA = 3
const MOCK_MAX_WRONG = 2 // terminate on the 3rd wrong (mirrors the 85% cap idea)
const MOCK_TIME_LIMIT_S = 600

const EXAMS = [
  { id: 'CA-C', state_code: 'CA', license_class: 'C', name_en: 'California Class C', name_zh: '加州 C 类驾照' },
  { id: 'CA-M1', state_code: 'CA', license_class: 'M1', name_en: 'California M1 Motorcycle', name_zh: '加州 M1 摩托车' }
]
const DEV_REDEEM_CODE = 'DEV-2026'

const sessions = new Map<string, StubSession>()
const mistakes = new Map<string, StubMistake>()
const mockAttempts = new Map<string, StubMockAttempt>()
let mockRemaining = MOCK_QUOTA
let currentExamId = 'CA-C'
let subscribedExams = new Set(['CA-C'])
let openedExams = new Set(['CA-C'])
let backedUpAt: string | null = null
let seq = 1

/** Test hook — wipe all mutable stub state. */
export function resetStubState(): void {
  sessions.clear()
  mistakes.clear()
  mockAttempts.clear()
  mockRemaining = MOCK_QUOTA
  currentExamId = 'CA-C'
  subscribedExams = new Set(['CA-C'])
  openedExams = new Set(['CA-C'])
  backedUpAt = null
  seq = 1
}

function stubError(code: string, status: number, message: string): Error {
  return Object.assign(new Error(message), { code, status })
}

function publicQuestion(q: StubQuestion) {
  return { question_id: q.question_id, variant_id: q.variant_id, stem: q.stem, choices: q.choices }
}

function activeSession(): StubSession | null {
  for (const s of sessions.values()) if (s.status === 'in_progress') return s
  return null
}

function mockDetail(a: StubMockAttempt) {
  const finished = a.status !== 'in_progress'
  const correct = a.answers.filter(x => x.is_correct).length
  const wrong = a.answers.length - correct
  return {
    mock_attempt_id: a.id,
    mock_exam_id: 'CA_C_STUB',
    status: a.status,
    language: 'zh',
    questions: a.questions.map(publicQuestion),
    saved_answers: a.answers.map(x => {
      const q = a.questions.find(qq => qq.question_id === x.question_id)!
      return {
        question_id: x.question_id,
        selected_choice_key: x.selected,
        correct_choice_key: q.correct,
        is_correct: x.is_correct,
        explanation: finished ? q.explanation : ''
      }
    }),
    score_percent: a.score_percent,
    correct_count: correct,
    wrong_count: wrong,
    time_limit_seconds: a.time_limit_seconds,
    started_at: a.started_at,
    time_used_seconds: finished ? Math.min(a.time_limit_seconds, 137) : -1
  }
}

function recordMistake(q: StubQuestion, source: string = 'practice'): void {
  const existing = mistakes.get(q.question_id)
  if (existing) {
    existing.wrong_count += 1
    existing.last_wrong_at = new Date().toISOString()
  } else {
    mistakes.set(q.question_id, {
      mistake_id: `m-${seq++}`,
      question_id: q.question_id,
      topic_id: q.topic_id,
      wrong_count: 1,
      last_wrong_at: new Date().toISOString(),
      source
    })
  }
}

// ---------------------------------------------------------------------------
// Static GET data
// ---------------------------------------------------------------------------

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
  },
  '/api/v1/ai/recommendations': {
    recommendations: [
      { topic_id: 't-row', label: '路权', reason_code: 'active_mistakes', mistake_count: 2, topic_filter: ['t-row'] }
    ]
  }
}

// ---------------------------------------------------------------------------
// Router
// ---------------------------------------------------------------------------

const AI_MAX_DEPTH = 3

const AI_BASE = '这道题考察的是核心规则本身：先把"必须完全停稳、确认安全、让行后才能继续"的顺序记牢。你的选项忽略了让行这一步，所以不对。（开发环境 AI 桩数据）'
const AI_ASPECTS: Record<string, string> = {
  example: '举个例子：你在放学时段接近学校路口，红灯右转前先停稳，看到人行横道上有学生，就必须等他们完全通过后再转。（开发环境 AI 桩数据）',
  mnemonic: '记忆法："一停二看三让四走"——停稳、观察、让行、再通过，四步一步都不能省。（开发环境 AI 桩数据）',
  distractors: '错项辨析：其余选项都省略了"完全停稳"或"让行"环节，看似更快，实际都是扣分点。（开发环境 AI 桩数据）',
  rule: '背后规则：路权规则的核心是"弱势方优先、先到优先、直行优先"，理解这三条就能推出大多数题的答案。（开发环境 AI 桩数据）'
}

/**
 * Route a stubbed API call. GETs with no dynamic behaviour fall through to the
 * static map; unknown paths return {}. Throws {code,status} errors matching
 * the real client contract (request.ts).
 */
export function stubRequest(path: string, method: string = 'GET', data?: any): any {
  const qIdx = path.indexOf('?')
  const key = qIdx === -1 ? path : path.slice(0, qIdx)

  // ---- /me (dynamic learning + exam state) ----
  if (key === '/api/v1/me' && method === 'GET') {
    const base = devStub['/api/v1/me']
    const s = activeSession()
    const exam = EXAMS.find(e => e.id === currentExamId) || EXAMS[0]
    return {
      ...base,
      current_exam: exam,
      access: {
        ...base.access,
        has_active_pass: subscribedExams.has(currentExamId),
        state: subscribedExams.has(currentExamId) ? 'active' : 'free_trial',
        mock_remaining: mockRemaining
      },
      learning: {
        ...base.learning,
        has_in_progress_practice: !!s,
        in_progress_practice: s
          ? {
              session_id: s.id,
              entry_type: s.entry_type,
              language: 'zh',
              answered_count: s.answers.length,
              total_count: s.questions.length,
              last_activity_at: new Date().toISOString()
            }
          : null
      }
    }
  }

  // ---- exams / entitlements / redeem / backup ----
  if (key === '/api/v1/me/exam' && method === 'PUT') {
    const exam = EXAMS.find(e => e.id === data?.exam_id)
    if (!exam) throw stubError('NOT_FOUND', 404, '考试不存在')
    currentExamId = exam.id
    openedExams.add(exam.id)
    return { current_exam: exam }
  }
  if (key === '/api/v1/exams/entitlements' && method === 'GET') {
    return {
      entitlements: EXAMS.map(e => ({
        exam_id: e.id,
        subscribed: subscribedExams.has(e.id),
        opened: openedExams.has(e.id)
      }))
    }
  }
  const of = key.match(/^\/api\/v1\/exams\/([^/]+)\/open-free$/)
  if (of && method === 'POST') {
    if (!EXAMS.some(e => e.id === of[1])) throw stubError('NOT_FOUND', 404, '考试不存在')
    openedExams.add(of[1])
    return { opened: true }
  }
  if (key === '/api/v1/access/redeem' && method === 'POST') {
    const code = decodeURIComponent((path.split('code=')[1] || '').split('&')[0])
    if (code !== DEV_REDEEM_CODE) throw stubError('CODE_INVALID', 422, '激活码无效')
    subscribedExams.add(currentExamId)
    return { redeemed: true, exam_id: currentExamId }
  }
  if (key === '/api/v1/backup/latest' && method === 'GET') {
    return { backed_up_at: backedUpAt, exam_id: currentExamId }
  }
  if (key === '/api/v1/backup/sync' && method === 'POST') {
    backedUpAt = new Date().toISOString()
    return { synced: true, backed_up_at: backedUpAt }
  }

  // ---- practice sessions ----
  if (key === '/api/v1/practice/sessions' && method === 'POST') {
    const s: StubSession = {
      id: `ps-${seq++}`,
      entry_type: data?.entry_type || 'full',
      mode: data?.mode || 'random',
      status: 'in_progress',
      questions: QUESTIONS,
      answers: []
    }
    sessions.set(s.id, s)
    return {
      session_id: s.id,
      entry_type: s.entry_type,
      status: s.status,
      language: 'zh',
      next_question: publicQuestion(s.questions[0])
    }
  }

  const m = key.match(/^\/api\/v1\/practice\/sessions\/([^/]+)(\/[a-z-]+)?$/)
  if (m) {
    const s = sessions.get(m[1])
    if (!s) throw stubError('NOT_FOUND', 404, '会话不存在')
    const sub = m[2] || ''

    if (sub === '' && method === 'GET') {
      return { session_id: s.id, status: s.status, answered_count: s.answers.length, total_count: s.questions.length }
    }
    if (sub === '/next-question' && method === 'GET') {
      const answered = new Set(s.answers.map(a => a.question_id))
      const next = s.questions.find(q => !answered.has(q.question_id))
      if (!next || s.status !== 'in_progress') throw stubError('SESSION_COMPLETED', 404, '本轮已完成')
      return publicQuestion(next)
    }
    if (sub === '/answers' && method === 'POST') {
      const q = s.questions.find(x => x.question_id === data?.question_id)
      if (!q) throw stubError('NOT_FOUND', 404, '题目不存在')
      const isCorrect = data?.selected_choice_key === q.correct
      s.answers.push({
        question_id: q.question_id,
        selected: data?.selected_choice_key,
        is_correct: isCorrect,
        submitted_at: new Date().toISOString()
      })
      if (!isCorrect) recordMistake(q)
      return {
        question_id: q.question_id,
        is_correct: isCorrect,
        correct_choice_key: q.correct,
        explanation: q.explanation,
        progress: { answered_count: s.answers.length }
      }
    }
    if (sub === '/complete' && method === 'POST') {
      s.status = 'completed'
      return { session_id: s.id, status: s.status }
    }
    if (sub === '/attempts' && method === 'GET') {
      return {
        items: s.answers.map(a => {
          const q = s.questions.find(x => x.question_id === a.question_id)!
          return {
            question_id: q.question_id,
            variant_id: q.variant_id,
            topic_id: q.topic_id,
            language: 'zh',
            stem: q.stem,
            choices: q.choices,
            correct_choice_key: q.correct,
            selected_choice_key: a.selected,
            explanation: q.explanation,
            is_correct: a.is_correct,
            submitted_at: a.submitted_at
          }
        })
      }
    }
  }

  // ---- mock exams ----
  if (key === '/api/v1/mock-exams/attempts' && method === 'POST') {
    if (mockRemaining <= 0) throw stubError('ACCESS_DENIED', 403, '模拟考次数已用完')
    mockRemaining -= 1
    const a: StubMockAttempt = {
      id: `ma-${seq++}`,
      status: 'in_progress',
      questions: QUESTIONS,
      answers: [],
      started_at: new Date().toISOString(),
      submitted_at: '',
      score_percent: -1,
      time_limit_seconds: MOCK_TIME_LIMIT_S
    }
    mockAttempts.set(a.id, a)
    return {
      mock_attempt_id: a.id,
      status: a.status,
      mock_remaining_after_start: mockRemaining,
      questions: a.questions.map(publicQuestion)
    }
  }
  if (key === '/api/v1/mock-exams/attempts/history' && method === 'GET') {
    const attempts = Array.from(mockAttempts.values())
      .sort((a, b) => b.started_at.localeCompare(a.started_at))
      .map(a => ({
        attempt_id: a.id,
        mock_exam_id: 'CA_C_STUB',
        mock_exam_code: 'CA_C_STUB',
        status: a.status,
        score_percent: a.score_percent,
        correct_count: a.answers.filter(x => x.is_correct).length,
        answered_count: a.answers.length,
        started_at: a.started_at,
        submitted_at: a.submitted_at
      }))
    return { attempts, total_in_db: attempts.length }
  }
  if (key === '/api/v1/mock-exams/attempts/stats' && method === 'GET') {
    const all = Array.from(mockAttempts.values())
    const submitted = all
      .filter(a => a.status === 'submitted')
      .sort((a, b) => b.submitted_at.localeCompare(a.submitted_at))
    const scores = submitted.map(a => a.score_percent)
    const recent3 = scores.slice(0, 3)
    return {
      total_attempts: all.length,
      submitted_count: submitted.length,
      exited_count: all.filter(a => a.status === 'ended_by_exit').length,
      recent_3_avg_score_percent: recent3.length
        ? Math.round(recent3.reduce((x, y) => x + y, 0) / recent3.length)
        : -1,
      best_score_percent: scores.length ? Math.max(...scores) : -1,
      latest_score_percent: scores.length ? scores[0] : -1
    }
  }
  const mm = key.match(/^\/api\/v1\/mock-exams\/attempts\/([^/]+)(\/[a-z-]+)?$/)
  if (mm) {
    const a = mockAttempts.get(mm[1])
    if (!a) throw stubError('NOT_FOUND', 404, '考试不存在')
    const sub = mm[2] || ''

    if (sub === '' && method === 'GET') return mockDetail(a)

    if (sub === '/answers' && method === 'POST') {
      if (a.status !== 'in_progress') throw stubError('MOCK_EXPIRED', 409, '考试已结束')
      const q = a.questions.find(x => x.question_id === data?.question_id)
      if (!q) throw stubError('NOT_FOUND', 404, '题目不存在')
      const isCorrect = data?.selected_choice_key === q.correct
      a.answers.push({ question_id: q.question_id, selected: data?.selected_choice_key, is_correct: isCorrect })
      if (!isCorrect) recordMistake(q, 'mock')
      const wrong = a.answers.filter(x => !x.is_correct).length
      const shouldTerminate = wrong > MOCK_MAX_WRONG
      if (shouldTerminate) {
        a.status = 'ended_by_failure'
        a.submitted_at = new Date().toISOString()
      }
      return {
        saved: true,
        answered_count: a.answers.length,
        is_correct: isCorrect,
        correct_choice_key: q.correct,
        wrong_count: wrong,
        max_allowed_wrong: MOCK_MAX_WRONG,
        should_terminate: shouldTerminate
      }
    }
    if (sub === '/submit' && method === 'POST') {
      if (a.status === 'in_progress') {
        const correct = a.answers.filter(x => x.is_correct).length
        a.status = 'submitted'
        a.submitted_at = new Date().toISOString()
        a.score_percent = Math.round((correct / a.questions.length) * 100)
      }
      const correct = a.answers.filter(x => x.is_correct).length
      const wrongTopics = new Map<string, string>()
      for (const x of a.answers) {
        if (x.is_correct) continue
        const q = a.questions.find(qq => qq.question_id === x.question_id)!
        const t = topics.find(tt => tt.id === q.topic_id)
        if (t) wrongTopics.set(t.id, t.name_zh)
      }
      return {
        mock_attempt_id: a.id,
        status: a.status,
        score_percent: a.score_percent,
        correct_count: correct,
        wrong_count: a.answers.length - correct,
        weak_topics: Array.from(wrongTopics, ([topic_id, label]) => ({ topic_id, label })),
        next_action: { type: 'practice', label: '继续练习薄弱知识点' }
      }
    }
    if (sub === '/exit' && method === 'POST') {
      if (a.status === 'in_progress') {
        a.status = 'ended_by_exit'
        a.submitted_at = new Date().toISOString()
      }
      return { mock_attempt_id: a.id, status: a.status }
    }
  }

  // ---- mistakes ----
  if (key === '/api/v1/mistakes' && method === 'GET') {
    const items = Array.from(mistakes.values()).sort((a, b) => b.last_wrong_at.localeCompare(a.last_wrong_at))
    return { items }
  }
  const mr = key.match(/^\/api\/v1\/mistakes\/([^/]+)\/review$/)
  if (mr && method === 'GET') {
    const q = QUESTIONS.find(x => x.question_id === mr[1])
    if (!q) throw stubError('NOT_FOUND', 404, '错题不存在')
    return {
      question_id: q.question_id,
      variant_id: q.variant_id,
      stem: q.stem,
      choices: q.choices,
      correct_choice_key: q.correct,
      explanation: q.explanation
    }
  }

  // ---- AI explain ----
  if (key === '/api/v1/ai/explain' && method === 'POST') {
    const depth = Number(data?.depth ?? 0)
    const text = depth === 0 ? AI_BASE : AI_ASPECTS[data?.aspect] || AI_ASPECTS.rule
    return {
      explanation: text,
      cached: false,
      model: 'stub',
      language: 'zh',
      depth,
      depth_remaining: Math.max(0, AI_MAX_DEPTH - depth)
    }
  }

  // ---- static fallback ----
  return devStub[key] ?? {}
}

/** Envelope meta for endpoints that page (mistakes). */
export function stubMetaFor(path: string): Record<string, unknown> {
  const key = path.indexOf('?') === -1 ? path : path.slice(0, path.indexOf('?'))
  if (key === '/api/v1/mistakes') {
    return { page: 1, page_size: 20, total: mistakes.size }
  }
  return {}
}

/** Legacy static lookup (query string ignored) — kept for shape tests. */
export function stubFor(path: string): any {
  const i = path.indexOf('?')
  const key = i === -1 ? path : path.slice(0, i)
  return devStub[key] ?? {}
}
