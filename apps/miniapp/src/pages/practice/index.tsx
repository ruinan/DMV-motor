import { View, Text } from '@tarojs/components'
import { useExamTheme } from '@/lib/useExamTheme'
import { t } from '@/lib/i18n'
import './index.scss'

/** Practice (tab 2) — placeholder shell; the full session flow lands in M3. */
export default function Practice() {
  const { themeClass } = useExamTheme()

  return (
    <View className={`page practice ${themeClass}`}>
      <View className='card'>
        <Text className='card-title'>{t('startPractice')}</Text>
        <Text className='muted'>{t('practiceComingSoon')}</Text>
      </View>
    </View>
  )
}
