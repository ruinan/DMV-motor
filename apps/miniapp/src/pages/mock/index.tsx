import { View, Text } from '@tarojs/components'
import { useLoad } from '@tarojs/taro'
import { ensureAuthed } from '@/lib/auth'
import { useExamTheme } from '@/lib/useExamTheme'
import { TabBar } from '@/components/TabBar'
import { M } from '@/messages'
import './index.scss'

/** Mock exam (tab 3) — placeholder shell; landing + attempt flow lands in M4. */
export default function Mock() {
  const { themeClass, me } = useExamTheme()

  useLoad(() => { ensureAuthed() })

  return (
    <View className={`page mock ${themeClass}`}>
      <View className='card'>
        <Text className='card-title'>{M.me.mockRemaining}</Text>
        <Text className='remaining'>{me?.access?.mock_remaining ?? 0}</Text>
        <Text className='muted'>{M.common.comingSoonMock}</Text>
      </View>

      <TabBar current='mock' />
    </View>
  )
}
