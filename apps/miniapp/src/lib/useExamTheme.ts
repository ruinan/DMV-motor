import Taro from '@tarojs/taro'
import { useEffect } from 'react'
import { useApi } from './useApi'
import { examPrimary, examThemeClass } from './theme'

export type Me = {
  user_id: string
  email: string
  language: string
  access: { state: string; has_active_pass: boolean; expires_at: string | null; mock_remaining: number }
  learning: { has_in_progress_practice: boolean; in_progress_practice: any; has_in_progress_review: boolean }
  current_exam: { id: string; state_code: string; license_class: string; name_en: string; name_zh: string } | null
}

/**
 * Loads /me and applies the per-exam theme: returns the root class for the
 * page View (amber vs default blue) and re-tints the native tabBar to match —
 * the miniapp equivalent of web's data-exam attribute.
 */
export function useExamTheme(): { themeClass: string; me: Me | undefined; loading: boolean } {
  const { data: me, loading } = useApi<Me>('/api/v1/me')
  const licenseClass = me?.current_exam?.license_class

  useEffect(() => {
    if (!me) return
    Taro.setTabBarStyle({ selectedColor: examPrimary(licenseClass) }).catch(() => {
      // Not on a tabBar page (e.g. deep-linked) — safe to ignore.
    })
  }, [me, licenseClass])

  return { themeClass: examThemeClass(licenseClass), me, loading }
}
