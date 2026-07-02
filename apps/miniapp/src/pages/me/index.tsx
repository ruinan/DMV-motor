import { View, Text, Button } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { signOut } from '@/lib/auth'
import { useExamTheme } from '@/lib/useExamTheme'
import { getLang, t } from '@/lib/i18n'
import './index.scss'

/** Me (tab 4) — M1 shell: account basics + sign out. Exam catalog, activation
 * codes, language switch and backup status land in M6. */
export default function Me() {
  const { themeClass, me, loading } = useExamTheme()
  const lang = getLang()

  const out = () => {
    signOut()
    Taro.redirectTo({ url: '/pages/login/index' })
  }

  const examName = me?.current_exam
    ? (lang === 'zh' ? me.current_exam.name_zh : me.current_exam.name_en)
    : '—'

  return (
    <View className={`page me ${themeClass}`}>
      {loading ? (
        <Text className='muted'>{t('loading')}</Text>
      ) : (
        <>
          <View className='card'>
            <Text className='card-title'>{t('account')}</Text>
            <Text className='email'>{me?.email || '—'}</Text>
          </View>
          <View className='card'>
            <Text className='card-title'>{t('currentExam')}</Text>
            <Text className='exam'>{examName}</Text>
          </View>
          <Button className='btn-out' onClick={out}>{t('signOut')}</Button>
        </>
      )}
    </View>
  )
}
