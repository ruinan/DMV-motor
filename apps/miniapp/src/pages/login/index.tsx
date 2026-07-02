import { View, Text, Button, Input } from '@tarojs/components'
import Taro, { useLoad } from '@tarojs/taro'
import { useState } from 'react'
import { loginWithWeChat, isSignedIn, devSignIn } from '@/lib/auth'
import { DEV_BYPASS } from '@/config'
import { M } from '@/messages'
import './index.scss'

/**
 * WeChat one-tap login. New user without an email -> show an email field; an email
 * already in use -> hint to sign in and link (full sign-in-to-link flow is a
 * follow-up). Shows a loading state on every wait (min 0.3s).
 */
export default function Login() {
  const [loading, setLoading] = useState(false)
  const [needEmail, setNeedEmail] = useState(false)
  const [emailInUse, setEmailInUse] = useState(false)
  const [email, setEmail] = useState('')
  const [hint, setHint] = useState('')

  useLoad(() => {
    if (isSignedIn()) {
      Taro.redirectTo({ url: '/pages/dashboard/index' })
      return
    }
    // Dev bypass: never sit on the login wall — stub-sign-in and go straight in.
    if (DEV_BYPASS) {
      devSignIn()
      Taro.redirectTo({ url: '/pages/dashboard/index' })
    }
  })

  const go = async (withEmail?: string) => {
    setLoading(true)
    const started = Date.now()
    try {
      const r = await loginWithWeChat(withEmail)
      if (r.status === 'authenticated') {
        Taro.redirectTo({ url: '/pages/dashboard/index' })
      } else if (r.status === 'email_required') {
        setNeedEmail(true); setEmailInUse(false); setHint(M.login.emailRequiredHint)
      } else if (r.status === 'email_in_use') {
        setNeedEmail(true); setEmailInUse(true); setHint(M.login.emailInUseHint)
      }
    } catch (e: any) {
      Taro.showToast({ title: e?.message || M.app.error, icon: 'none' })
    } finally {
      const elapsed = Date.now() - started
      if (elapsed < 300) await new Promise(res => setTimeout(res, 300 - elapsed))
      setLoading(false)
    }
  }

  return (
    <View className='login'>
      <Text className='brand'>{M.app.name}</Text>
      <Text className='tagline'>{M.app.tagline}</Text>

      {needEmail && (
        <View className='email-row'>
          <Text className='hint'>{hint}</Text>
          {!emailInUse && (
            <Input
              className='input'
              type='text'
              placeholder={M.login.emailPlaceholder}
              value={email}
              onInput={e => setEmail(e.detail.value)}
            />
          )}
        </View>
      )}

      <Button
        className='cta'
        loading={loading}
        disabled={loading || (needEmail && !emailInUse && !email)}
        onClick={() => go(needEmail && !emailInUse ? email : undefined)}
      >
        {needEmail && !emailInUse ? M.login.continue : M.login.loginWeChat}
      </Button>
    </View>
  )
}
