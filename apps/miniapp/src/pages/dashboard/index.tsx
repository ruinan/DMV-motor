import { View, Text, Button } from '@tarojs/components'
import Taro, { useLoad } from '@tarojs/taro'
import { useState } from 'react'
import { ensureAuthed } from '@/lib/auth'
import { api } from '@/lib/request'
import { invalidate } from '@/lib/bus'
import { useApi } from '@/lib/useApi'
import { useExamTheme } from '@/lib/useExamTheme'
import { TabBar } from '@/components/TabBar'
import { M, fmt } from '@/messages'
import { LANG } from '@/lib/i18n'
import './index.scss'

/** Study Hub (tab 1) — port of web Dashboard: resume/start card, AI next-step
 * (paid), readiness (locked for free), coverage + engagement stats, per-topic
 * mastery progress, and entries to mistakes/progress. */

type Summary = {
  access_state: string
  completion_score: number
  weak_topics: { topic_id: string; label: string }[]
  next_action: { type: string; label: string } | null
  readiness_score?: number
  is_ready_candidate?: boolean
}

type Engagement = { current_streak_days: number; answered_today: number; daily_goal: number }

type Recommendation = {
  topic_id: string
  label: string
  reason_code: string
  mistake_count: number
  topic_filter: string[]
}

type TopicMastery = {
  topic_id: string
  name_zh: string
  is_mastered: boolean
  mastery_progress: {
    attempted: number
    accuracy_percent: number
    recent_correct: number
    recent_window: number
    accuracy_threshold: number
    recent_correct_threshold: number
    progress_percent: number
  }
}

export default function Dashboard() {
  const { themeClass, me, loading } = useExamTheme()
  const hasPass = me?.access?.has_active_pass ?? false
  const { data: summary } = useApi<Summary>('/api/v1/summary')
  const { data: engagement } = useApi<Engagement>(
    '/api/v1/engagement?tz_offset_minutes=' + -new Date().getTimezoneOffset()
  )
  const { data: mastery } = useApi<{ topics: TopicMastery[]; summary: { total_sub_topics: number; mastered_sub_topics: number } }>(
    '/api/v1/topics/mastery'
  )
  const { data: reco } = useApi<{ recommendations: Recommendation[] }>(
    hasPass ? `/api/v1/ai/recommendations?language=${LANG}&limit=1` : null
  )
  const [startingReco, setStartingReco] = useState(false)

  useLoad(() => { ensureAuthed() })

  const examName = me?.current_exam?.name_zh || ''
  const inProgress = me?.learning?.in_progress_practice ?? null
  const topReco = reco?.recommendations?.[0]

  async function practiceReco(r: Recommendation) {
    if (startingReco) return
    setStartingReco(true)
    try {
      await api('/api/v1/practice/sessions', {
        method: 'POST',
        data: {
          entry_type: hasPass ? 'full' : 'free_trial',
          language: LANG,
          topic_filter: r.topic_filter.map(id => (isNaN(Number(id)) ? id : Number(id)))
        }
      })
      invalidate('/api/v1/me')
      Taro.redirectTo({ url: '/pages/practice/index' })
    } catch (err: any) {
      Taro.showToast({ title: err?.message || M.app.error, icon: 'none' })
      setStartingReco(false)
    }
  }

  const touched = (mastery?.topics ?? []).filter(t => t.mastery_progress.attempted > 0)
  const sortedTopics = [...touched].sort((a, b) => {
    if (a.is_mastered !== b.is_mastered) return a.is_mastered ? 1 : -1
    return b.mastery_progress.progress_percent - a.mastery_progress.progress_percent
  })

  return (
    <View className={`page dashboard ${themeClass}`}>
      <View className='header'>
        <Text className='title'>{M.dashboard.studyHub}</Text>
        <View className='header-chips'>
          {examName && <Text className='exam-chip'>{examName}</Text>}
          {me && (
            <Text className={`pass-chip ${hasPass ? 'pass-chip--paid' : 'pass-chip--free'}`}>
              {hasPass ? M.me.examSubscribed : M.me.examFree}
            </Text>
          )}
        </View>
      </View>

      {loading ? (
        <Text className='muted'>{M.app.loading}</Text>
      ) : (
        <>
          {/* Resume / start card */}
          {inProgress ? (
            <View className='card resume-card'>
              <Text className='card-title'>{M.hub.resumeTitle}</Text>
              <Text className='resume-body'>
                {fmt(M.hub.resumeBody, {
                  answered: inProgress.answered_count,
                  total: inProgress.total_count
                })}
              </Text>
              <Button
                className='btn-primary'
                onClick={() => Taro.redirectTo({ url: '/pages/practice/index' })}
              >
                {M.hub.resumeCta}
              </Button>
            </View>
          ) : (
            <View className='card resume-card'>
              <Text className='card-title'>{M.hub.startTitle}</Text>
              <Text className='resume-body'>{M.hub.startBodyFresh}</Text>
              <Button
                className='btn-primary'
                onClick={() => Taro.redirectTo({ url: '/pages/practice/index' })}
              >
                {M.hub.startCta}
              </Button>
            </View>
          )}

          {/* AI next-step (paid) */}
          {hasPass && topReco && (
            <View className='card reco-card'>
              <Text className='card-title'>{M.hub.nextStepTitle}</Text>
              <View className='reco-row'>
                <View className='reco-main'>
                  <Text className='reco-label'>{topReco.label}</Text>
                  <Text className='reco-reason'>
                    {topReco.reason_code === 'active_mistakes'
                      ? M.hub.nextStepReasonActiveMistakes
                      : M.hub.nextStepReasonUncoveredKeyTopic}
                  </Text>
                </View>
                <Button
                  className='btn-primary reco-btn'
                  size='mini'
                  disabled={startingReco}
                  onClick={() => practiceReco(topReco)}
                >
                  {startingReco ? M.hub.nextStepStarting : M.hub.nextStepCta}
                </Button>
              </View>
            </View>
          )}

          {/* Readiness (locked for free users) */}
          <View className='card'>
            <Text className='card-title'>{M.dashboard.readiness}</Text>
            {hasPass && summary?.readiness_score !== undefined ? (
              <>
                <View className='score-row'>
                  <Text className='score'>{summary.readiness_score}</Text>
                  <Text className='score-max'>/100</Text>
                </View>
                <Text className='muted verdict'>
                  {summary.is_ready_candidate ? M.dashboard.readyCandidate : M.dashboard.notReadyYet}
                </Text>
              </>
            ) : (
              <Text className='muted verdict'>{M.hub.readinessLocked}</Text>
            )}
          </View>

          {/* Stats: coverage / streak / today */}
          <View className='card row-cards'>
            <View className='stat'>
              <Text className='card-title'>{M.hub.coverageTitle}</Text>
              <Text className='stat-num'>
                {mastery ? `${mastery.summary.mastered_sub_topics}/${mastery.summary.total_sub_topics}` : '—'}
              </Text>
              <Text className='stat-cap'>{M.hub.coverageSublabel}</Text>
            </View>
            <View className='stat'>
              <Text className='card-title'>{M.dashboard.streak}</Text>
              <Text className='stat-num'>
                {engagement?.current_streak_days ?? 0}
                <Text className='stat-unit'> {M.dashboard.streakDays}</Text>
              </Text>
            </View>
            <View className='stat'>
              <Text className='card-title'>{M.dashboard.today}</Text>
              <Text className='stat-num'>
                {engagement?.answered_today ?? 0}
                <Text className='stat-unit'>/{engagement?.daily_goal ?? 10}</Text>
              </Text>
            </View>
          </View>

          {/* Per-topic mastery progress */}
          {sortedTopics.length > 0 && (
            <View className='card'>
              <Text className='card-title'>{M.hub.topicProgressTitle}</Text>
              <Text className='topic-sub'>{M.hub.topicProgressSubtitle}</Text>
              {sortedTopics.map(t => {
                const p = t.mastery_progress
                return (
                  <View key={t.topic_id} className='topic-row'>
                    <View className='topic-head'>
                      <Text className='topic-name'>
                        {t.is_mastered ? '✓ ' : ''}{t.name_zh}
                      </Text>
                      <Text className='topic-pct'>{p.progress_percent}%</Text>
                    </View>
                    <View className='bar-track'>
                      <View
                        className={`bar-fill ${t.is_mastered ? 'bar-fill--done' : ''}`}
                        style={{ width: `${p.progress_percent}%` }}
                      />
                    </View>
                    <Text className='topic-detail'>
                      {t.is_mastered
                        ? M.hub.topicProgressMastered
                        : fmt(M.hub.topicProgressDetail, {
                            acc: p.accuracy_percent,
                            accT: p.accuracy_threshold,
                            recent: p.recent_correct,
                            recentT: p.recent_correct_threshold
                          })}
                    </Text>
                  </View>
                )
              })}
            </View>
          )}

          <View className='link-row'>
            <View
              className='card link-card'
              onClick={() => Taro.navigateTo({ url: '/pages/mistakes/index' })}
            >
              <Text className='link-icon'>🔖</Text>
              <Text className='link-label'>{M.links.mistakes}</Text>
            </View>
            <View
              className='card link-card'
              onClick={() => Taro.navigateTo({ url: '/pages/progress/index' })}
            >
              <Text className='link-icon'>📈</Text>
              <Text className='link-label'>{M.links.progress}</Text>
            </View>
          </View>
        </>
      )}

      <TabBar current='dashboard' />
    </View>
  )
}
