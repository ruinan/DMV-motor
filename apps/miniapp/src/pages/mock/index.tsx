import { View, Text } from '@tarojs/components'
import { useExamTheme } from '@/lib/useExamTheme'
import { t } from '@/lib/i18n'
import './index.scss'

/** Mock exam (tab 3) — placeholder shell; landing + attempt flow lands in M4. */
export default function Mock() {
  const { themeClass, me } = useExamTheme()

  return (
    <View className={`page mock ${themeClass}`}>
      <View className='card'>
        <Text className='card-title'>{t('mockRemaining')}</Text>
        <Text className='remaining'>{me?.access?.mock_remaining ?? 0}</Text>
        <Text className='muted'>{t('mockComingSoon')}</Text>
      </View>
    </View>
  )
}
