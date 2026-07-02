import { View, Text, Button } from '@tarojs/components'
import Taro, { useLoad } from '@tarojs/taro'
import { ensureAuthed, signOut } from '@/lib/auth'
import { useExamTheme } from '@/lib/useExamTheme'
import { TabBar } from '@/components/TabBar'
import { M } from '@/messages'
import './index.scss'

/** Me (tab 4) — account basics + sign out. Exam catalog, activation codes and
 * backup status land with M6. */
export default function Me() {
  const { themeClass, me, loading } = useExamTheme()

  useLoad(() => { ensureAuthed() })

  const out = () => {
    signOut()
    Taro.redirectTo({ url: '/pages/login/index' })
  }

  return (
    <View className={`page me ${themeClass}`}>
      {loading ? (
        <Text className='muted'>{M.app.loading}</Text>
      ) : (
        <>
          <View className='card'>
            <Text className='card-title'>{M.me.account}</Text>
            <Text className='email'>{me?.email || '—'}</Text>
          </View>
          <View className='card'>
            <Text className='card-title'>{M.me.currentExam}</Text>
            <Text className='exam'>{me?.current_exam?.name_zh || '—'}</Text>
          </View>
          <Button className='btn-out' onClick={out}>{M.me.signOut}</Button>
        </>
      )}

      <TabBar current='me' />
    </View>
  )
}
