import { View, Text, Button } from '@tarojs/components'
import Taro, { useLoad } from '@tarojs/taro'
import { useState } from 'react'
import { ensureAuthed } from '@/lib/auth'
import { api } from '@/lib/request'
import { useApi } from '@/lib/useApi'
import { useExamTheme } from '@/lib/useExamTheme'
import { TabBar } from '@/components/TabBar'
import { M } from '@/messages'
import { LANG } from '@/lib/i18n'
import './index.scss'

/** Mock exam landing (tab 3) — port of web MockLanding + Study-Hub history:
 * quota + stats + recent attempts, start (or resume) an attempt. */

type StartResponse = {
  mock_attempt_id: string
  status: string
  mock_remaining_after_start: number
  questions: unknown[]
}

type HistoryItem = {
  attempt_id: string
  status: string
  score_percent: number
  correct_count: number
  answered_count: number
  started_at: string
}

type Stats = {
  total_attempts: number
  submitted_count: number
  exited_count: number
  recent_3_avg_score_percent: number
  best_score_percent: number
  latest_score_percent: number
}

const STATUS_LABEL: Record<string, string> = {
  submitted: M.mock.statusSubmitted,
  in_progress: M.mock.statusInProgress,
  ended_by_exit: M.mock.statusExited,
  ended_by_failure: M.mock.statusFailed,
  expired: M.mock.statusExpired
}

const pct = (v: number) => (v >= 0 ? `${v}%` : '—')

export default function Mock() {
  const { themeClass, me, loading: meLoading } = useExamTheme()
  const { data: stats } = useApi<Stats>('/api/v1/mock-exams/attempts/stats')
  const { data: history } = useApi<{ attempts: HistoryItem[] }>(
    '/api/v1/mock-exams/attempts/history?limit=10'
  )
  const [starting, setStarting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useLoad(() => { ensureAuthed() })

  const hasPass = me?.access?.has_active_pass ?? false
  const remaining = me?.access?.mock_remaining ?? 0
  const canStart = hasPass && remaining > 0
  const inProgress = history?.attempts.find(a => a.status === 'in_progress')

  async function start() {
    if (starting) return
    setStarting(true)
    setError(null)
    const startedAt = Date.now()
    try {
      const res = await api<StartResponse>('/api/v1/mock-exams/attempts', {
        method: 'POST',
        data: { language: LANG }
      })
      Taro.navigateTo({ url: `/pages/mock-exam/index?id=${res.mock_attempt_id}` })
    } catch (err: any) {
      setError(err?.code === 'ACCESS_DENIED' ? M.mock.errorPassRequired : err?.message || M.app.error)
    } finally {
      const elapsed = Date.now() - startedAt
      if (elapsed < 300) await new Promise(r => setTimeout(r, 300 - elapsed))
      setStarting(false)
    }
  }

  const header = (
    <View className='mock-head'>
      <Text className='mock-title'>{M.mock.title}</Text>
      <Text className='mock-sub'>{M.mock.subtitle}</Text>
    </View>
  )

  // No active pass → guide to subscribe (web parity: no raw "0 attempts").
  if (!meLoading && !hasPass) {
    return (
      <View className={`page mock ${themeClass}`}>
        {header}
        <View className='card lock-card'>
          <Text className='lock-icon'>🔒</Text>
          <Text className='lock-body'>{M.mock.subscribeBody}</Text>
          <Button
            className='btn-primary'
            onClick={() => Taro.redirectTo({ url: '/pages/me/index' })}
          >
            {M.mock.subscribeCta}
          </Button>
        </View>
        <TabBar current='mock' />
      </View>
    )
  }

  return (
    <View className={`page mock ${themeClass}`}>
      {header}

      <View className='card quota-card'>
        <View className='quota-main'>
          <Text className='card-title'>{M.mock.remaining}</Text>
          <Text className='quota-num'>{meLoading ? '…' : remaining}</Text>
        </View>
        {stats && (
          <View className='quota-stats'>
            <View className='qs'>
              <Text className='qs-label'>{M.mock.statsRecent3}</Text>
              <Text className='qs-num'>{pct(stats.recent_3_avg_score_percent)}</Text>
            </View>
            <View className='qs'>
              <Text className='qs-label'>{M.mock.statsBest}</Text>
              <Text className='qs-num'>{pct(stats.best_score_percent)}</Text>
            </View>
            <View className='qs'>
              <Text className='qs-label'>{M.mock.statsLatest}</Text>
              <Text className='qs-num'>{pct(stats.latest_score_percent)}</Text>
            </View>
          </View>
        )}
      </View>

      {error && <View className='error-box'><Text>{error}</Text></View>}
      {!meLoading && hasPass && !canStart && (
        <View className='error-box'><Text>{M.mock.noAttemptsLeft}</Text></View>
      )}

      {inProgress ? (
        <Button
          className='btn-primary start-btn'
          onClick={() => Taro.navigateTo({ url: `/pages/mock-exam/index?id=${inProgress.attempt_id}` })}
        >
          {M.mock.continueExam}
        </Button>
      ) : (
        <Button
          className='btn-primary start-btn'
          loading={starting}
          disabled={starting || meLoading || !canStart}
          onClick={start}
        >
          {starting ? M.mock.starting : M.mock.startExam}
        </Button>
      )}

      <View className='history-block'>
        <Text className='card-title'>{M.mock.history}</Text>
        {history && history.attempts.length === 0 && (
          <Text className='muted'>{M.mock.historyEmpty}</Text>
        )}
        {(history?.attempts ?? []).map(a => (
          <View
            key={a.attempt_id}
            className='card history-row'
            onClick={() => Taro.navigateTo({ url: `/pages/mock-exam/index?id=${a.attempt_id}` })}
          >
            <View className='hr-left'>
              <Text className={`hr-status hr-status--${a.status}`}>
                {STATUS_LABEL[a.status] || a.status}
              </Text>
              <Text className='hr-date'>{a.started_at.slice(0, 10)}</Text>
            </View>
            <Text className='hr-score'>
              {a.score_percent >= 0 ? `${a.score_percent}%` : M.mock.notScored}
            </Text>
          </View>
        ))}
      </View>

      <TabBar current='mock' />
    </View>
  )
}
