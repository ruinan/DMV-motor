import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { M } from '@/messages'
import './TabBar.scss'

/**
 * In-page bottom navigation replacing the native tabBar: full style control
 * (bigger labels per user request, per-exam accent via the cascading --primary
 * variable) with none of the custom-tab-bar API fiddliness. Pages are plain
 * (non-tab) pages, so switching uses redirectTo.
 */

const TABS = [
  { key: 'dashboard', label: M.tab.study, url: '/pages/dashboard/index' },
  { key: 'practice', label: M.tab.practice, url: '/pages/practice/index' },
  { key: 'mock', label: M.tab.mock, url: '/pages/mock/index' },
  { key: 'me', label: M.tab.me, url: '/pages/me/index' }
] as const

export type TabKey = (typeof TABS)[number]['key']

export function TabBar({ current }: { current: TabKey }) {
  return (
    <View className='tab-bar'>
      {TABS.map(tab => (
        <View
          key={tab.key}
          className={`tab-item ${tab.key === current ? 'tab-item--active' : ''}`}
          onClick={() => {
            if (tab.key !== current) Taro.redirectTo({ url: tab.url })
          }}
        >
          <Text className='tab-label'>{tab.label}</Text>
        </View>
      ))}
    </View>
  )
}
