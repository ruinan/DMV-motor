import { View, Text, Button } from '@tarojs/components'
import Taro, { useLoad } from '@tarojs/taro'
import { useState } from 'react'
import { ensureAuthed } from '@/lib/auth'
import { api } from '@/lib/request'
import { invalidate } from '@/lib/bus'
import { useApi } from '@/lib/useApi'
import { useExamTheme } from '@/lib/useExamTheme'
import { M } from '@/messages'
import { LANG } from '@/lib/i18n'
import './index.scss'

/** Learning progress — port of web ProgressView: completion, readiness score
 * + the four pass gates (paid; locked card otherwise), weak topics with a
 * practice CTA, and the next best action. Reached from the dashboard. */

type Summary = {
  access_state: string
  completion_score: number
  weak_topics: { topic_id: string; label: string }[]
  next_action: { type: string; label: string } | null
  readiness_score?: number
  is_ready_candidate?: boolean
}

type Readiness = {
  readiness_score: number
  is_ready_candidate: boolean
  missing_gates: string[]
}

const GATES: { code: string; label: string; body: string }[] = [
  { code: 'MOCK_SCORE_NOT_STABLE', label: '模拟考成绩稳定', body: '至少完成 2 次模拟考，平均分 ≥ 85%。' },
  { code: 'KEY_COVERAGE_INCOMPLETE', label: '关键知识点覆盖', body: '关键知识点必考题覆盖 ≥ 90%。' },
  { code: 'HIGH_RISK_REVIEW_LOW', label: '高风险复习完成', body: '复习包任务完成 ≥ 80%。' },
  { code: 'PERSISTENT_WEAK_POINT', label: '无顽固薄弱点', body: '把反复错在同一知识点的题彻底搞清。' }
]

export default function Progress() {
  const { themeClass, me } = useExamTheme()
  const hasPass = me?.access?.has_active_pass ?? false
  const { data: summary, loading } = useApi<Summary>('/api/v1/summary')
  const { data: readiness } = useApi<Readiness>(hasPass ? '/api/v1/readiness' : null)
  const [startingTopic, setStartingTopic] = useState<string | null>(null)

  useLoad(() => { ensureAuthed() })

  async function practiceTopic(topicId: string) {
    if (startingTopic) return
    setStartingTopic(topicId)
    try {
      await api('/api/v1/practice/sessions', {
        method: 'POST',
        data: {
          entry_type: hasPass ? 'full' : 'free_trial',
          language: LANG,
          topic_filter: [isNaN(Number(topicId)) ? topicId : Number(topicId)]
        }
      })
      invalidate('/api/v1/me')
      Taro.redirectTo({ url: '/pages/practice/index' })
    } catch (err: any) {
      Taro.showToast({ title: err?.message || M.app.error, icon: 'none' })
      setStartingTopic(null)
    }
  }

  const nextCta = (type: string): { label: string; go: () => void } => {
    if (type === 'mock_exam') {
      return { label: M.progress.ctaMock, go: () => Taro.redirectTo({ url: '/pages/mock/index' }) }
    }
    return { label: M.progress.ctaPractice, go: () => Taro.redirectTo({ url: '/pages/practice/index' }) }
  }

  return (
    <View className={`page progress ${themeClass}`}>
      <View className='head'>
        <Text className='head-title'>{M.progress.title}</Text>
        <Text className='head-sub'>{M.progress.subtitle}</Text>
      </View>

      {loading ? (
        <Text className='muted'>{M.app.loading}</Text>
      ) : !summary ? (
        <View className='error-box'><Text>{M.progress.loadError}</Text></View>
      ) : (
        <>
          <View className='card'>
            <Text className='card-title'>{M.progress.completionTitle}</Text>
            <View className='big-row'>
              <Text className='big-num'>{summary.completion_score}%</Text>
            </View>
            <View className='bar-track'>
              <View className='bar-fill' style={{ width: `${summary.completion_score}%` }} />
            </View>
            <Text className='hint'>{M.progress.completionHint}</Text>
          </View>

          {hasPass && readiness ? (
            <>
              <View className='card'>
                <Text className='card-title'>{M.progress.readinessTitle}</Text>
                <View className='big-row'>
                  <Text className='big-num'>{readiness.readiness_score}</Text>
                  <Text
                    className={`badge ${readiness.is_ready_candidate ? 'badge--ready' : 'badge--not'}`}
                  >
                    {readiness.is_ready_candidate ? M.progress.readyBadge : M.progress.notReadyBadge}
                  </Text>
                </View>
                <Text className='hint'>{M.progress.readinessHint}</Text>
              </View>

              <View className='card'>
                <Text className='card-title'>{M.progress.gatesTitle}</Text>
                <Text className='hint gates-sub'>{M.progress.gatesSubtitle}</Text>
                {GATES.map(g => {
                  const passed = !readiness.missing_gates.includes(g.code)
                  return (
                    <View key={g.code} className='gate-row'>
                      <Text className={`gate-mark ${passed ? 'ok' : 'bad'}`}>{passed ? '✓' : '✕'}</Text>
                      <View className='gate-texts'>
                        <Text className='gate-label'>
                          {g.label}
                          <Text className={`gate-state ${passed ? 'ok' : 'bad'}`}>
                            {' '}{passed ? M.progress.gatePassed : M.progress.gateOpen}
                          </Text>
                        </Text>
                        <Text className='gate-body'>{g.body}</Text>
                      </View>
                    </View>
                  )
                })}
              </View>
            </>
          ) : (
            <View className='card locked-card'>
              <Text className='lock-icon'>🔒</Text>
              <Text className='locked-title'>{M.progress.readinessLockedTitle}</Text>
              <Text className='locked-body'>{M.progress.readinessLockedBody}</Text>
              <Button
                className='btn-primary'
                onClick={() => Taro.redirectTo({ url: '/pages/me/index' })}
              >
                {M.progress.goSubscribe}
              </Button>
            </View>
          )}

          <View className='card'>
            <Text className='card-title'>{M.progress.weakTopicsTitle}</Text>
            {summary.weak_topics.length === 0 ? (
              <Text className='muted'>{M.progress.noWeakTopics}</Text>
            ) : (
              summary.weak_topics.map(w => (
                <View key={w.topic_id} className='weak-row'>
                  <Text className='weak-label'>{w.label}</Text>
                  <Text
                    className={`weak-cta ${startingTopic === w.topic_id ? 'weak-cta--busy' : ''}`}
                    onClick={() => practiceTopic(w.topic_id)}
                  >
                    {startingTopic === w.topic_id ? M.mistakes.practiceTheseStarting : M.progress.practiceTopic}
                  </Text>
                </View>
              ))
            )}
          </View>

          <View className='card'>
            <Text className='card-title'>{M.progress.nextActionTitle}</Text>
            {summary.next_action ? (
              <>
                <Text className='next-label'>{summary.next_action.label}</Text>
                <Button
                  className='btn-primary next-btn'
                  onClick={nextCta(summary.next_action.type).go}
                >
                  {nextCta(summary.next_action.type).label}
                </Button>
              </>
            ) : (
              <Text className='muted'>{M.progress.noNextAction}</Text>
            )}
          </View>
        </>
      )}
    </View>
  )
}
