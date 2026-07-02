import { View, Text, Button } from '@tarojs/components'
import Taro, { useLoad } from '@tarojs/taro'
import { isSignedIn } from '@/lib/auth'
import { useApi } from '@/lib/useApi'
import { useExamTheme } from '@/lib/useExamTheme'
import { getLang, t } from '@/lib/i18n'
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

/** Study Hub (tab 1) — M1 shell: readiness + coverage + engagement + next step.
 * Full study-hub parity (topic progress, recommendations) lands in M2. */
export default function Dashboard() {
  const { themeClass, me, loading } = useExamTheme()
  const { data: summary } = useApi<Summary>('/api/v1/summary')
  const { data: engagement } = useApi<Engagement>(
    '/api/v1/engagement?tz_offset_minutes=' + -new Date().getTimezoneOffset()
  )

  useLoad(() => {
    if (!isSignedIn()) Taro.redirectTo({ url: '/pages/login/index' })
  })

  const lang = getLang()
  const examName = me?.current_exam
    ? (lang === 'zh' ? me.current_exam.name_zh : me.current_exam.name_en)
    : ''

  return (
    <View className={`page dashboard ${themeClass}`}>
      <View className='header'>
        <Text className='title'>{t('studyHub')}</Text>
        {examName && <Text className='exam-chip'>{examName}</Text>}
      </View>

      {loading ? (
        <Text className='muted'>{t('loading')}</Text>
      ) : (
        <>
          <View className='card'>
            <Text className='card-title'>{t('readiness')}</Text>
            <View className='score-row'>
              <Text className='score'>{summary?.readiness_score ?? '—'}</Text>
              <Text className='score-max'>/100</Text>
            </View>
            <Text className='muted verdict'>
              {summary?.is_ready_candidate ? t('readyCandidate') : t('notReadyYet')}
            </Text>
          </View>

          <View className='card row-cards'>
            <View className='stat'>
              <Text className='card-title'>{t('completion')}</Text>
              <Text className='stat-num'>{summary?.completion_score ?? 0}%</Text>
            </View>
            <View className='stat'>
              <Text className='card-title'>{t('streak')}</Text>
              <Text className='stat-num'>
                {engagement?.current_streak_days ?? 0}
                <Text className='stat-unit'> {t('streakDays')}</Text>
              </Text>
            </View>
            <View className='stat'>
              <Text className='card-title'>{t('today')}</Text>
              <Text className='stat-num'>
                {engagement?.answered_today ?? 0}
                <Text className='stat-unit'>/{engagement?.daily_goal ?? 10}</Text>
              </Text>
            </View>
          </View>

          {summary?.next_action && (
            <View className='card'>
              <Text className='card-title'>{t('nextStep')}</Text>
              <Text className='next-label'>{summary.next_action.label}</Text>
              <Button
                className='btn-primary next-btn'
                onClick={() => Taro.switchTab({ url: '/pages/practice/index' })}
              >
                {t('startPractice')}
              </Button>
            </View>
          )}
        </>
      )}
    </View>
  )
}
