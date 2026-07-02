import { View, Text, Button } from '@tarojs/components'
import Taro, { useLoad, useRouter } from '@tarojs/taro'
import { useEffect, useRef, useState } from 'react'
import { ensureAuthed } from '@/lib/auth'
import { api } from '@/lib/request'
import { invalidate } from '@/lib/bus'
import { AiExplain } from '@/components/AiExplain'
import { M, fmt } from '@/messages'
import { LANG } from '@/lib/i18n'
import './index.scss'

/** Mock-exam runner — port of web MockExam.tsx. Linear flow (no revisiting),
 * per-question verdict after save, server-anchored countdown with auto-submit,
 * wrong-answer cap with a grace countdown before the failure view, and a
 * read-only review for finished attempts. Reached via
 * /pages/mock-exam/index?id=<attemptId>. */

type Choice = { key: string; text: string }
type Question = { question_id: string; variant_id: string; stem: string; choices: Choice[] }

type SavedAnswer = {
  question_id: string
  selected_choice_key: string
  correct_choice_key: string
  is_correct: boolean
  explanation: string
}

type AttemptDetail = {
  mock_attempt_id: string
  status: string
  language: string
  questions: Question[]
  saved_answers: SavedAnswer[]
  score_percent: number
  correct_count: number
  wrong_count: number
  time_limit_seconds: number
  started_at: string
  time_used_seconds: number
}

type SubmitResponse = {
  mock_attempt_id: string
  status: string
  score_percent: number
  correct_count: number
  wrong_count: number
  weak_topics: { topic_id: string; label: string }[]
  next_action: { type: string; label: string }
}

type SaveAnswerResponse = {
  saved: boolean
  answered_count: number
  is_correct: boolean
  correct_choice_key: string
  wrong_count: number
  max_allowed_wrong: number
  should_terminate: boolean
}

type Feedback = { isCorrect: boolean; correctKey: string }

const PASS_THRESHOLD = 85

function invalidateStudyHub() {
  for (const p of [
    '/api/v1/me',
    '/api/v1/summary',
    '/api/v1/readiness',
    '/api/v1/topics/mastery',
    '/api/v1/mistakes',
    '/api/v1/engagement',
    '/api/v1/ai/recommendations',
    '/api/v1/mock-exams'
  ]) invalidate(p)
}

function mmss(total: number): string {
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

export default function MockExam() {
  const router = useRouter()
  const attemptId = router.params.id || ''

  const [detail, setDetail] = useState<AttemptDetail | null>(null)
  const [loadError, setLoadError] = useState(false)
  const [index, setIndex] = useState(0)
  const [picks, setPicks] = useState<Map<string, string>>(new Map())
  const [feedback, setFeedback] = useState<Map<string, Feedback>>(new Map())
  const [saveStatus, setSaveStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle')
  const [failing, setFailing] = useState(false)
  const [terminatingSec, setTerminatingSec] = useState(15)
  const [terminated, setTerminated] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState<SubmitResponse | null>(null)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [remainingSec, setRemainingSec] = useState<number | null>(null)
  const submittingRef = useRef(false)
  const autoSubmittedRef = useRef(false)
  const savedTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useLoad(() => { ensureAuthed() })

  async function load() {
    try {
      const d = await api<AttemptDetail>(`/api/v1/mock-exams/attempts/${attemptId}?language=${LANG}`)
      // Seed picks + verdicts from persisted answers so resuming lands on the
      // first unanswered question with earlier verdicts intact.
      const seededPicks = new Map<string, string>()
      const seededFb = new Map<string, Feedback>()
      for (const a of d.saved_answers) {
        seededPicks.set(a.question_id, a.selected_choice_key)
        seededFb.set(a.question_id, { isCorrect: a.is_correct, correctKey: a.correct_choice_key })
      }
      const answeredIds = new Set(d.saved_answers.map(a => a.question_id))
      const firstUnanswered = d.questions.findIndex(q => !answeredIds.has(q.question_id))
      setPicks(seededPicks)
      setFeedback(seededFb)
      setIndex(firstUnanswered === -1 ? Math.max(0, d.questions.length - 1) : firstUnanswered)
      setDetail(d)
    } catch {
      setLoadError(true)
    }
  }

  useEffect(() => {
    if (attemptId) void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attemptId])

  useEffect(() => () => {
    if (savedTimer.current) clearTimeout(savedTimer.current)
  }, [])

  async function submitNow() {
    if (submittingRef.current) return
    submittingRef.current = true
    setSubmitting(true)
    setErrorMsg(null)
    try {
      const res = await api<SubmitResponse>(
        `/api/v1/mock-exams/attempts/${attemptId}/submit`,
        { method: 'POST' }
      )
      invalidateStudyHub()
      setResult(res)
    } catch (err: any) {
      setErrorMsg(err?.message || M.app.error)
      setSubmitting(false)
      submittingRef.current = false
    }
  }

  // Server-anchored countdown: deadline = started_at + limit; auto-submit at 0.
  const deadlineMs =
    detail && detail.status === 'in_progress'
      ? new Date(detail.started_at).getTime() + detail.time_limit_seconds * 1000
      : null
  useEffect(() => {
    if (deadlineMs == null || result || terminated) return
    const tick = () => {
      const rem = Math.max(0, Math.round((deadlineMs - Date.now()) / 1000))
      setRemainingSec(rem)
      if (rem <= 0 && !autoSubmittedRef.current) {
        autoSubmittedRef.current = true
        void submitNow()
      }
    }
    tick()
    const id = setInterval(tick, 1000)
    return () => clearInterval(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deadlineMs, result, terminated])

  // Failure grace countdown (~15s) before swapping to the failure view.
  useEffect(() => {
    if (!failing || terminated) return
    let deadline: number | null = null
    const tick = () => {
      if (deadline === null) deadline = Date.now() + 15_000
      const rem = Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
      setTerminatingSec(rem)
      if (rem <= 0) setTerminated(true)
    }
    tick()
    const id = setInterval(tick, 500)
    return () => clearInterval(id)
  }, [failing, terminated])

  async function pick(choiceKey: string) {
    if (!detail || saveStatus === 'saving') return
    const question = detail.questions[index]
    if (!question || feedback.has(question.question_id)) return

    setPicks(prev => new Map(prev).set(question.question_id, choiceKey))
    setErrorMsg(null)
    setSaveStatus('saving')
    if (savedTimer.current) {
      clearTimeout(savedTimer.current)
      savedTimer.current = null
    }
    try {
      const res = await api<SaveAnswerResponse>(
        `/api/v1/mock-exams/attempts/${attemptId}/answers`,
        {
          method: 'POST',
          data: {
            question_id: question.question_id,
            variant_id: question.variant_id,
            selected_choice_key: choiceKey
          }
        }
      )
      setFeedback(prev =>
        new Map(prev).set(question.question_id, {
          isCorrect: res.is_correct,
          correctKey: res.correct_choice_key
        })
      )
      setSaveStatus('saved')
      savedTimer.current = setTimeout(() => setSaveStatus('idle'), 1500)
      if (res.should_terminate) {
        setFailing(true)
        invalidateStudyHub()
      }
    } catch (err: any) {
      if (err?.code === 'MOCK_EXPIRED') {
        // Timer ran out server-side — reload into the read-only finished view.
        await load()
        return
      }
      setSaveStatus('error')
      setErrorMsg(err?.message || M.app.error)
      setPicks(prev => {
        const nextMap = new Map(prev)
        nextMap.delete(question.question_id)
        return nextMap
      })
    }
  }

  async function askExit() {
    const res = await Taro.showModal({
      title: M.mock.exitConfirmTitle,
      content: M.mock.exitConfirmBody,
      confirmText: M.mock.exitConfirmYes,
      cancelText: M.mock.exitConfirmCancel,
      confirmColor: '#dc2626'
    })
    if (!res.confirm) return
    try {
      await api(`/api/v1/mock-exams/attempts/${attemptId}/exit`, { method: 'POST' })
      invalidateStudyHub()
      Taro.navigateBack()
    } catch (err: any) {
      setErrorMsg(err?.message || M.app.error)
    }
  }

  // -------------------------------------------------------------------------
  // Views
  // -------------------------------------------------------------------------

  if (loadError) {
    return (
      <View className='page mock-exam'>
        <View className='error-box'><Text>{M.app.error}</Text></View>
        <Button className='btn-secondary' onClick={() => Taro.navigateBack()}>
          {M.mock.backToDashboard}
        </Button>
      </View>
    )
  }

  if (!detail) {
    return (
      <View className='page mock-exam'>
        <View className='center-box'>
          <View className='pulse-dot' />
          <Text className='muted'>{M.app.loading}</Text>
        </View>
      </View>
    )
  }

  if (result) {
    const passed = result.score_percent >= PASS_THRESHOLD
    return (
      <View className='page mock-exam'>
        <Text className='page-title'>{M.mock.resultTitle}</Text>
        <View className='card result-card'>
          <Text className='result-label'>{M.mock.scorePercent}</Text>
          <Text className='result-score'>{result.score_percent}%</Text>
          <Text className={`result-badge ${passed ? 'result-badge--pass' : 'result-badge--fail'}`}>
            {passed ? M.mock.passed : M.mock.failed}
          </Text>
          <View className='result-counts'>
            <Text className='ok'>{M.mock.correctCount} {result.correct_count}</Text>
            <Text className='bad'>{M.mock.wrongCount} {result.wrong_count}</Text>
          </View>
          <Text className='result-readiness'>{M.mock.readinessCounted}</Text>
        </View>

        <View className='card'>
          <Text className='card-title'>{M.mock.weakTopics}</Text>
          {result.weak_topics.length === 0 ? (
            <Text className='muted'>{M.mock.noWeakTopics}</Text>
          ) : (
            <View className='weak-chips'>
              {result.weak_topics.map(w => (
                <Text key={w.topic_id} className='weak-chip'>{w.label}</Text>
              ))}
            </View>
          )}
        </View>

        <Button className='btn-primary block-btn' onClick={() => Taro.navigateBack()}>
          {M.mock.tryAgain}
        </Button>
        <Button
          className='btn-secondary block-btn'
          onClick={() => Taro.reLaunch({ url: '/pages/dashboard/index' })}
        >
          {M.mock.backToDashboard}
        </Button>
      </View>
    )
  }

  if (terminated) {
    const verdicts = Array.from(feedback.values())
    const wrong = verdicts.filter(f => !f.isCorrect).length
    return (
      <View className='page mock-exam'>
        <View className='card terminated-card'>
          <Text className='terminated-icon'>⚠️</Text>
          <Text className='terminated-title'>{M.mock.terminatedTitle}</Text>
          <Text className='terminated-body'>
            {fmt(M.mock.terminatedBody, { answered: verdicts.length, wrong })}
          </Text>
        </View>
        <Button className='btn-primary block-btn' onClick={() => Taro.navigateBack()}>
          {M.mock.terminatedTryAgain}
        </Button>
        <Button
          className='btn-secondary block-btn'
          onClick={() => Taro.reLaunch({ url: '/pages/dashboard/index' })}
        >
          {M.mock.backToDashboard}
        </Button>
      </View>
    )
  }

  // Cold re-open of a finished attempt → read-only review.
  if (detail.status !== 'in_progress') {
    const answersById = new Map(detail.saved_answers.map(a => [a.question_id, a]))
    const answeredQuestions = detail.questions.filter(q => answersById.has(q.question_id))
    return (
      <View className='page mock-exam'>
        <View className='review-head'>
          <Text className='page-title'>{M.mock.reviewTitle}</Text>
          {detail.score_percent >= 0 ? (
            <Text className='muted'>
              {detail.score_percent}% · {detail.correct_count}/{detail.correct_count + detail.wrong_count}
            </Text>
          ) : (
            <Text className='muted'>{M.mock.exitedSummary}</Text>
          )}
        </View>

        {answeredQuestions.length === 0 && <Text className='muted'>{M.mock.reviewEmpty}</Text>}

        {answeredQuestions.map((q, idx) => {
          const a = answersById.get(q.question_id)!
          return (
            <View key={q.question_id} className='card review-item'>
              <View className='review-item-head'>
                <Text className='review-index'>{idx + 1}</Text>
                <Text className={a.is_correct ? 'ok review-verdict' : 'bad review-verdict'}>
                  {a.is_correct ? M.mock.correct : M.mock.incorrect}
                </Text>
              </View>
              <Text className='review-stem'>{q.stem}</Text>
              {q.choices.map(c => {
                const isCorrect = c.key === a.correct_choice_key
                const wrongPick = c.key === a.selected_choice_key && !isCorrect
                return (
                  <View
                    key={c.key}
                    className={`choice choice--small ${isCorrect ? 'choice--correct' : ''} ${wrongPick ? 'choice--wrong' : ''}`}
                  >
                    <Text className='choice-key'>{c.key}</Text>
                    <Text className='choice-text'>{c.text}</Text>
                  </View>
                )
              })}
              {a.explanation && (
                <View className='review-expl'>
                  <Text>{M.mock.explanation}：{a.explanation}</Text>
                </View>
              )}
              {!a.is_correct && (
                <AiExplain
                  questionId={q.question_id}
                  variantId={q.variant_id}
                  selectedChoiceKey={a.selected_choice_key}
                />
              )}
            </View>
          )
        })}

        <Button className='btn-primary block-btn' onClick={() => Taro.navigateBack()}>
          {M.mock.backToDashboard}
        </Button>
      </View>
    )
  }

  // ---- live answering ----
  const questions = detail.questions
  const total = questions.length
  const question = questions[index]
  const pickedKey = question ? picks.get(question.question_id) ?? null : null
  const fb = question ? feedback.get(question.question_id) : undefined
  const answeredCount = picks.size
  const isLast = index + 1 >= total
  const progressPct = total > 0 ? Math.round((answeredCount / total) * 100) : 0

  return (
    <View className='page mock-exam'>
      {remainingSec !== null && (
        <Text className={`timer ${remainingSec <= 60 ? 'timer--low' : ''}`}>
          ⏱ {mmss(remainingSec)}
        </Text>
      )}

      <View className='progress-row'>
        <View className='progress-meta'>
          <Text className='muted'>{fmt(M.mock.questionOf, { current: index + 1, total })}</Text>
          <Text className='muted'>{fmt(M.mock.answeredOf, { answered: answeredCount, total })}</Text>
        </View>
        <View className='progress-track'>
          <View className='progress-fill' style={{ width: `${progressPct}%` }} />
        </View>
        <View className='save-status'>
          {saveStatus === 'saving' && <Text className='muted'>{M.mock.saving}</Text>}
          {saveStatus === 'saved' && <Text className='ok'>{M.mock.saved}</Text>}
        </View>
      </View>

      <View className='dmv-retro question-card'>
        <Text className='q-counter'>{fmt(M.mock.questionLabel, { n: index + 1 })}</Text>
        <Text className='q-stem'>{question?.stem}</Text>

        {(question?.choices ?? []).map(c => {
          const selected = pickedKey === c.key
          const showVerdict = !!fb
          const isCorrect = showVerdict && c.key === fb!.correctKey
          const isWrongPick = showVerdict && selected && c.key !== fb!.correctKey
          const cls = showVerdict
            ? isCorrect
              ? 'choice--correct'
              : isWrongPick
                ? 'choice--wrong'
                : 'choice--dim'
            : selected
              ? 'choice--selected'
              : ''
          return (
            <View key={c.key} className={`choice ${cls}`} onClick={() => pick(c.key)}>
              <Text className='choice-key'>{c.key}</Text>
              <Text className='choice-text'>{c.text}</Text>
              {isCorrect && <Text className='choice-mark ok'>✓</Text>}
              {isWrongPick && <Text className='choice-mark bad'>✕</Text>}
            </View>
          )
        })}

        {errorMsg && <Text className='bad inline-error'>{errorMsg}</Text>}
      </View>

      {failing && !terminated && (
        <View className='error-box'><Text>{M.mock.terminatingBanner}</Text></View>
      )}

      <View className='action-row'>
        <Text className='exit-link' onClick={askExit}>{M.mock.exit}</Text>
        {failing && !terminated ? (
          <Button className='btn-danger action-btn' onClick={() => setTerminated(true)}>
            {fmt(M.mock.endExamNow, { n: terminatingSec })}
          </Button>
        ) : !isLast ? (
          <Button
            className='btn-primary action-btn'
            disabled={!fb || submitting}
            onClick={() => setIndex(i => Math.min(i + 1, total - 1))}
          >
            {M.mock.next}
          </Button>
        ) : (
          <Button
            className='btn-primary action-btn'
            disabled={!fb || submitting}
            onClick={() => void submitNow()}
          >
            {submitting ? M.mock.submitting : M.mock.submitExam}
          </Button>
        )}
      </View>
    </View>
  )
}
