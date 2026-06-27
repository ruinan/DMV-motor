import { View, Text, Button } from '@tarojs/components'
import Taro, { useLoad } from '@tarojs/taro'
import { useState } from 'react'
import { api } from '@/lib/request'
import { isSignedIn, signOut } from '@/lib/auth'
import { t } from '@/lib/i18n'
import './index.scss'

/** Home placeholder: proves the authed pipeline works (GET /me) end to end. */
export default function Index() {
  const [me, setMe] = useState<any>(null)
  const [loading, setLoading] = useState(true)

  useLoad(async () => {
    if (!isSignedIn()) {
      Taro.redirectTo({ url: '/pages/login/index' })
      return
    }
    try {
      setMe(await api('/api/v1/me'))
    } catch (e: any) {
      Taro.showToast({ title: e?.message || 'Failed to load', icon: 'none' })
    } finally {
      setLoading(false)
    }
  })

  const out = () => {
    signOut()
    Taro.redirectTo({ url: '/pages/login/index' })
  }

  return (
    <View className='home'>
      <Text className='title'>{t('appName')}</Text>
      {loading ? (
        <Text className='muted'>{t('loading')}</Text>
      ) : (
        <Text className='who'>
          {t('signedInAs')}: {me?.email || me?.current_exam?.name_en || '—'}
        </Text>
      )}
      <Button className='out' onClick={out}>{t('signOut')}</Button>
    </View>
  )
}
