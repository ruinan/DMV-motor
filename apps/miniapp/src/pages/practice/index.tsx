import { View, Text } from '@tarojs/components'
import { useLoad } from '@tarojs/taro'
import { ensureAuthed } from '@/lib/auth'
import { useExamTheme } from '@/lib/useExamTheme'
import { TabBar } from '@/components/TabBar'
import { M } from '@/messages'
import './index.scss'

/** Practice (tab 2) — placeholder shell; the full session flow lands in M3. */
export default function Practice() {
  const { themeClass } = useExamTheme()

  useLoad(() => { ensureAuthed() })

  return (
    <View className={`page practice ${themeClass}`}>
      <View className='card'>
        <Text className='card-title'>{M.practice.title}</Text>
        <Text className='muted'>{M.common.comingSoonPractice}</Text>
      </View>

      <TabBar current='practice' />
    </View>
  )
}
