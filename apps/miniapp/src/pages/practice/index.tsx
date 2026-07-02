import { View, Text, Button } from '@tarojs/components'
import Taro, { useLoad } from '@tarojs/taro'
import { useEffect, useRef, useState } from 'react'
import { ensureAuthed } from '@/lib/auth'
import { hideHomeCapsule } from '@/lib/nav'
import { api } from '@/lib/request'
import { invalidate } from '@/lib/bus'
import { useExamTheme } from '@/lib/useExamTheme'
import { TabBar } from '@/components/TabBar'
import { AiExplain } from '@/components/AiExplain'
import { M, fmt } from '@/messages'
import { LANG } from '@/lib/i18n'
import { AttemptHistory } from './AttemptHistory'
import './index.scss'

/** Practice (tab 2) — full session flow, ported from web PracticeFlow.tsx.
 * Signed-in only (the miniapp has no anonymous mode; login/bypass is a page
 * guard), Chinese only. Phases: idle → starting → answering ↔ feedback →
 * completed, with exit-confirm and a read-only attempt-history overlay. */

type Choice = { key: string; text: string }
type Question = { question_id: string; variant_id: string; stem: string; choices: Choice[] }

type StartResponse = {
  session_id: string
  entry_type: 'free_trial' | 'full'
  status: string
  language: string
  next_question: Question
}
type SessionStatus = { session_id: string; status: string; answered_count: number; total_count: number }
type AnswerResponse = {
  question_id: string
  is_correct: boolean
  correct_choice_key: string
  explanation: string
  progress: { answered_count: number }
}

type Phase =
  | { kind: 'idle' }
  | { kind: 'starting' }
  | { kind: 'answering'; sessionId: string; question: Question; answeredCount: number; totalCount: number; picked: string | null; submitting: boolean }
  | { kind: 'feedback'; sessionId: string; question: Question; picked: string; result: AnswerResponse; totalCount: number }
  | { kind: 'completed'; sessionId: string; reason: 'finished' | 'exited' }
  | { kind: 'error'; message: string }

type PracticeMode = 'random' | 'weak_points' | 'review_learned'

const MODES: { value: PracticeMode; label: string; desc: string }[] = [
  { value: 'random', label: M.practice.modeRandom, desc: M.practice.modeRandomDesc },
  { value: 'weak_points', label: M.practice.modeWeakPoints, desc: M.practice.modeWeakPointsDesc },
  { value: 'review_learned', label: M.practice.modeReviewLearned, desc: M.practice.modeReviewLearnedDesc }
]

const MIN_STARTING_MS = 1000

async function withMinDuration<T>(work: Promise<T>, minMs: number): Promise<T> {
  const startedAt = Date.now()
  const result = await work
  const elapsed = Date.now() - startedAt
  if (elapsed < minMs) await new Promise(r => setTimeout(r, minMs - elapsed))
  return result
}

/** Refresh every Study-Hub surface practice activity touches (web parity). */
function invalidateStudyHub() {
  for (const p of [
    '/api/v1/me',
    '/api/v1/summary',
    '/api/v1/readiness',
    '/api/v1/topics/mastery',
    '/api/v1/mistakes',
    '/api/v1/engagement',
    '/api/v1/ai/recommendations',
    '/api/v1/practice',
    '/api/v1/exams'
  ]) invalidate(p)
}

export default function Practice() {
  const { themeClass, me, loading: meLoading } = useExamTheme()
  const [phase, setPhase] = useState<Phase>({ kind: 'idle' })
  const [historyOpen, setHistoryOpen] = useState(false)
  const [practiceMode, setPracticeMode] = useState<PracticeMode>('random')
  const autoResumeFired = useRef(false)

  useLoad(() => {
    ensureAuthed()
    hideHomeCapsule()
  })

  const hasPass = me?.access?.has_active_pass ?? false
  const entryType: 'free_trial' | 'full' = hasPass ? 'full' : 'free_trial'
  const inProgress = me?.learning?.in_progress_practice ?? null

  // Auto-resume a half-finished session the moment /me reports one.
  useEffect(() => {
    if (autoResumeFired.current) return
    if (phase.kind !== 'idle') return
    if (!inProgress) return
    autoResumeFired.current = true
    void resume(inProgress.session_id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inProgress, phase.kind])

  const activeSessionId =
    phase.kind === 'answering' || phase.kind === 'feedback' || phase.kind === 'completed'
      ? phase.sessionId
      : null

  async function start() {
    setPhase({ kind: 'starting' })
    try {
      const { startRes, status } = await withMinDuration(
        (async () => {
          const startRes = await api<StartResponse>('/api/v1/practice/sessions', {
            method: 'POST',
            data: {
              entry_type: entryType,
              language: LANG,
              ...(hasPass ? { mode: practiceMode } : {})
            }
          })
          const status = await api<SessionStatus>(`/api/v1/practice/sessions/${startRes.session_id}`)
          return { startRes, status }
        })(),
        MIN_STARTING_MS
      )
      setPhase({
        kind: 'answering',
        sessionId: startRes.session_id,
        question: startRes.next_question,
        answeredCount: status.answered_count,
        totalCount: status.total_count,
        picked: null,
        submitting: false
      })
      invalidateStudyHub()
    } catch (err: any) {
      setPhase({ kind: 'error', message: errorMessage(err) })
    }
  }

  async function resume(sessionId: string) {
    setPhase({ kind: 'starting' })
    try {
      const { question, status } = await withMinDuration(
        (async () => {
          const question = await api<Question>(
            `/api/v1/practice/sessions/${sessionId}/next-question?language=${LANG}`
          )
          const status = await api<SessionStatus>(`/api/v1/practice/sessions/${sessionId}`)
          return { question, status }
        })(),
        MIN_STARTING_MS
      )
      setPhase({
        kind: 'answering',
        sessionId,
        question,
        answeredCount: status.answered_count,
        totalCount: status.total_count,
        picked: null,
        submitting: false
      })
    } catch (err: any) {
      if (err?.code === 'SESSION_COMPLETED') {
        await complete(sessionId, 'finished')
        return
      }
      setPhase({ kind: 'error', message: errorMessage(err) })
    }
  }

  async function submit() {
    if (phase.kind !== 'answering' || !phase.picked || phase.submitting) return
    setPhase({ ...phase, submitting: true })
    try {
      const res = await api<AnswerResponse>(
        `/api/v1/practice/sessions/${phase.sessionId}/answers`,
        {
          method: 'POST',
          data: {
            question_id: phase.question.question_id,
            variant_id: phase.question.variant_id,
            selected_choice_key: phase.picked
          }
        }
      )
      setPhase({
        kind: 'feedback',
        sessionId: phase.sessionId,
        question: phase.question,
        picked: phase.picked,
        result: res,
        totalCount: phase.totalCount
      })
      invalidateStudyHub()
    } catch (err: any) {
      setPhase({ kind: 'error', message: errorMessage(err) })
    }
  }

  async function next() {
    if (phase.kind !== 'feedback') return
    const { sessionId, totalCount } = phase
    const answeredCount = phase.result.progress.answered_count
    setPhase({ kind: 'starting' })
    try {
      const q = await api<Question>(
        `/api/v1/practice/sessions/${sessionId}/next-question?language=${LANG}`
      )
      setPhase({
        kind: 'answering',
        sessionId,
        question: q,
        answeredCount,
        totalCount,
        picked: null,
        submitting: false
      })
    } catch (err: any) {
      if (err?.code === 'SESSION_COMPLETED') {
        await complete(sessionId, 'finished')
        return
      }
      setPhase({ kind: 'error', message: errorMessage(err) })
    }
  }

  async function complete(sessionId: string, reason: 'finished' | 'exited') {
    try {
      await api(`/api/v1/practice/sessions/${sessionId}/complete`, { method: 'POST' })
    } catch {
      // Even if complete fails, the user has effectively ended the session.
    }
    setPhase({ kind: 'completed', sessionId, reason })
    invalidateStudyHub()
  }

  async function askExit() {
    if (phase.kind !== 'answering' && phase.kind !== 'feedback') return
    const sessionId = phase.sessionId
    const res = await Taro.showModal({
      title: M.practice.exitConfirmTitle,
      content: M.practice.exitConfirmBody,
      confirmText: M.practice.exitConfirmYes,
      cancelText: M.practice.exitConfirmCancel,
      confirmColor: '#dc2626'
    })
    if (res.confirm) await complete(sessionId, 'exited')
  }

  // -------------------------------------------------------------------------
  // Render
  // -------------------------------------------------------------------------

  if (historyOpen && activeSessionId) {
    return (
      <View className={`page practice ${themeClass}`}>
        <AttemptHistory sessionId={activeSessionId} onBack={() => setHistoryOpen(false)} />
      </View>
    )
  }

  // Idle guard: don't flash the Start UI while /me may still auto-resume.
  const willAutoResume = phase.kind === 'idle' && (meLoading || inProgress != null)

  let body: JSX.Element
  if (phase.kind === 'starting' || willAutoResume) {
    body = (
      <View className='center-box'>
        <View className='pulse-dot' />
        <Text className='muted'>{M.practice.starting}</Text>
      </View>
    )
  } else if (phase.kind === 'error') {
    body = (
      <>
        <View className='error-box'>
          <Text>{phase.message}</Text>
        </View>
        <Button className='btn-secondary' onClick={() => setPhase({ kind: 'idle' })}>
          {M.practice.backToDashboard}
        </Button>
      </>
    )
  } else if (phase.kind === 'completed') {
    const isExited = phase.reason === 'exited'
    body = (
      <View className='card done-card'>
        <Text className='done-icon'>✓</Text>
        <Text className='done-title'>{isExited ? M.practice.exitedTitle : M.practice.completedTitle}</Text>
        <Text className='done-body'>{isExited ? M.practice.exitedBody : M.practice.completedBody}</Text>
        <Text className='done-readiness'>{M.practice.readinessCounted}</Text>
        <Button className='btn-primary done-btn' onClick={() => setHistoryOpen(true)}>
          {M.practice.reviewHistoryFromCompleted}
        </Button>
        <Button
          className='btn-secondary done-btn'
          onClick={() => Taro.redirectTo({ url: '/pages/dashboard/index' })}
        >
          {M.practice.backToDashboard}
        </Button>
      </View>
    )
  } else if (phase.kind === 'idle') {
    body = (
      <>
        <View className='idle-head'>
          <Text className='idle-title'>{M.practice.title}</Text>
          <Text className='idle-sub'>
            {hasPass ? M.practice.subtitlePaid : M.practice.subtitleNoPass}
          </Text>
        </View>

        {hasPass ? (
          <View className='mode-picker'>
            <Text className='mode-title'>{M.practice.modeTitle}</Text>
            {MODES.map(o => (
              <View
                key={o.value}
                className={`mode-option ${practiceMode === o.value ? 'mode-option--active' : ''}`}
                onClick={() => setPracticeMode(o.value)}
              >
                <View className={`radio ${practiceMode === o.value ? 'radio--on' : ''}`} />
                <View className='mode-texts'>
                  <Text className='mode-label'>{o.label}</Text>
                  <Text className='mode-desc'>{o.desc}</Text>
                </View>
              </View>
            ))}
          </View>
        ) : (
          <Text className='muted free-note'>{M.practice.modeFreeNote}</Text>
        )}

        {inProgress ? (
          <>
            <Button className='btn-primary start-btn' onClick={() => resume(inProgress.session_id)}>
              {fmt(M.practice.resumeCta, {
                answered: inProgress.answered_count,
                total: inProgress.total_count
              })}
            </Button>
            <Text className='start-fresh' onClick={() => start()}>{M.practice.startFresh}</Text>
          </>
        ) : (
          <Button className='btn-primary start-btn' disabled={meLoading} onClick={() => start()}>
            {hasPass ? M.practice.startFull : M.practice.startFreeTrial}
          </Button>
        )}
        {!hasPass && <Text className='muted pass-note'>{M.practice.errorPassRequired}</Text>}
      </>
    )
  } else {
    // answering / feedback
    const question = phase.question
    const totalCount = phase.totalCount
    const isFeedback = phase.kind === 'feedback'
    const answeredCount = isFeedback ? phase.result.progress.answered_count : phase.answeredCount
    const correctKey = isFeedback ? phase.result.correct_choice_key : null
    const pickedKey = phase.picked
    const displayNumber = Math.min(isFeedback ? answeredCount : answeredCount + 1, totalCount)
    const pct = totalCount > 0 ? Math.min(100, Math.round((answeredCount / totalCount) * 100)) : 0

    body = (
      <>
        <View className='progress-row'>
          <View className='progress-meta'>
            <Text className='muted'>{M.practice.answered}</Text>
            <Text className='progress-count'>{answeredCount} / {totalCount}</Text>
          </View>
          <View className='progress-track'>
            <View className='progress-fill' style={{ width: `${pct}%` }} />
          </View>
          {answeredCount > 0 && (
            <Text className='history-link' onClick={() => setHistoryOpen(true)}>
              {M.practice.reviewHistory}
            </Text>
          )}
        </View>

        <View className='dmv-retro question-card'>
          <Text className='q-counter'>
            {fmt(M.practice.questionOf, { current: displayNumber, total: totalCount })}
          </Text>
          <Text className='q-stem'>{question.stem}</Text>

          {question.choices.map(c => {
            const selected = pickedKey === c.key
            const isCorrect = isFeedback && correctKey === c.key
            const isWrongPick = isFeedback && selected && correctKey !== c.key
            const cls = isFeedback
              ? isCorrect
                ? 'choice--correct'
                : isWrongPick
                  ? 'choice--wrong'
                  : 'choice--dim'
              : selected
                ? 'choice--selected'
                : ''
            return (
              <View
                key={c.key}
                className={`choice ${cls}`}
                onClick={() => {
                  if (phase.kind !== 'answering' || phase.submitting) return
                  setPhase({ ...phase, picked: c.key })
                }}
              >
                <Text className='choice-key'>{c.key}</Text>
                <Text className='choice-text'>{c.text}</Text>
                {isCorrect && <Text className='choice-mark ok'>✓</Text>}
                {isWrongPick && <Text className='choice-mark bad'>✕</Text>}
              </View>
            )
          })}

          {isFeedback && (
            <View className={`verdict-box ${phase.result.is_correct ? 'verdict-box--ok' : 'verdict-box--bad'}`}>
              <Text className={`verdict-title ${phase.result.is_correct ? 'ok' : 'bad'}`}>
                {phase.result.is_correct ? M.practice.correct : M.practice.incorrect}
              </Text>
              {phase.result.explanation && (
                <Text className='verdict-expl'>
                  {M.practice.explanation}：{phase.result.explanation}
                </Text>
              )}
              {!phase.result.is_correct && (
                <AiExplain
                  key={question.question_id}
                  questionId={question.question_id}
                  variantId={question.variant_id}
                  selectedChoiceKey={phase.picked}
                />
              )}
            </View>
          )}
        </View>

        <View className='action-row'>
          <Text className='exit-link' onClick={askExit}>{M.practice.exit}</Text>
          {phase.kind === 'answering' && (
            <Button
              className='btn-primary action-btn'
              disabled={!phase.picked || phase.submitting}
              onClick={submit}
            >
              {phase.submitting ? M.practice.submitting : M.practice.submitAnswer}
            </Button>
          )}
          {isFeedback && (
            <Button className='btn-primary action-btn' onClick={next}>
              {M.practice.nextQuestion}
            </Button>
          )}
        </View>
      </>
    )
  }

  // Hide the bottom TabBar mid-session (focus mode; exit is explicit) —
  // matches the web where practice runs outside the tabbed shell.
  const showTabBar = phase.kind === 'idle' || phase.kind === 'completed' || phase.kind === 'error'

  return (
    <View className={`page practice ${themeClass}`}>
      {body}
      {showTabBar && !willAutoResume && <TabBar current='practice' />}
    </View>
  )
}

function errorMessage(err: any): string {
  if (err?.code === 'ACCESS_DENIED') return M.practice.errorPassRequired
  return err?.message || M.app.error
}
