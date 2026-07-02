import { View, Text, Button } from '@tarojs/components'
import Taro, { useLoad } from '@tarojs/taro'
import { ensureAuthed } from '@/lib/auth'
import { useApi } from '@/lib/useApi'
import { useExamTheme } from '@/lib/useExamTheme'
import { TabBar } from '@/components/TabBar'
import { M } from '@/messages'
import './index.scss'

type Summary = {
  access_state: string
  completion_score: number
  weak_topics: { topic_id: string; label: string }[]
  next_action: { type: string; label: string } | null
  readiness_score?: number
  is_ready_candidate?: boolean
}

type Engagement = {
  current_streak_days: number
  answered_today: number
  daily_goal: number
}

/** Study Hub (tab 1) — readiness + coverage + engagement + next step. */
export default function Dashboard() {
  const { themeClass, me, loading } = useExamTheme()
  const { data: summary } = useApi<Summary>('/api/v1/summary')
  const { data: engagement } = useApi<Engagement>(
    '/api/v1/engagement?tz_offset_minutes=' + -new Date().getTimezoneOffset()
  )

  useLoad(() => { ensureAuthed() })

  const examName = me?.current_exam?.name_zh || ''

  return (
    <View className={`page dashboard ${themeClass}`}>
      <View className='header'>
        <Text className='title'>{M.dashboard.studyHub}</Text>
        {examName && <Text className='exam-chip'>{examName}</Text>}
      </View>

      {loading ? (
        <Text className='muted'>{M.app.loading}</Text>
      ) : (
        <>
          <View className='card'>
            <Text className='card-title'>{M.dashboard.readiness}</Text>
            <View className='score-row'>
              <Text className='score'>{summary?.readiness_score ?? '—'}</Text>
              <Text className='score-max'>/100</Text>
            </View>
            <Text className='muted verdict'>
              {summary?.is_ready_candidate ? M.dashboard.readyCandidate : M.dashboard.notReadyYet}
            </Text>
          </View>

          <View className='card row-cards'>
            <View className='stat'>
              <Text className='card-title'>{M.dashboard.completion}</Text>
              <Text className='stat-num'>{summary?.completion_score ?? 0}%</Text>
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

          {summary?.next_action && (
            <View className='card'>
              <Text className='card-title'>{M.dashboard.nextStep}</Text>
              <Text className='next-label'>{summary.next_action.label}</Text>
              <Button
                className='btn-primary next-btn'
                onClick={() => Taro.redirectTo({ url: '/pages/practice/index' })}
              >
                {M.dashboard.startPractice}
              </Button>
            </View>
          )}
        </>
      )}

      <TabBar current='dashboard' />
    </View>
  )
}
