import { useApi } from './useApi'
import { examThemeClass } from './theme'

export type Me = {
  user_id: string
  email: string
  language: string
  access: { state: string; has_active_pass: boolean; expires_at: string | null; mock_remaining: number }
  learning: { has_in_progress_practice: boolean; in_progress_practice: { session_id: string; answered_count: number; total_count: number } | null; has_in_progress_review: boolean }
  current_exam: { id: string; state_code: string; license_class: string; name_en: string; name_zh: string } | null
}

/**
 * Loads /me and derives the per-exam theme class for the page's root View
 * (amber for motorcycle, default blue otherwise) — the miniapp equivalent of
 * web's data-exam attribute. The in-page TabBar picks the accent up through
 * the cascading --primary variable automatically.
 */
export function useExamTheme(): { themeClass: string; me: Me | undefined; loading: boolean } {
  const { data: me, loading } = useApi<Me>('/api/v1/me')
  return { themeClass: examThemeClass(me?.current_exam?.license_class), me, loading }
}
