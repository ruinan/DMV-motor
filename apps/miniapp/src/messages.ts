// Bilingual strings. Mirrors the web app's keys where they overlap; extend as
// pages are ported. Source of truth for mini-program copy.
export const messages = {
  en: {
    appName: 'DMV Prep',
    tagline: 'California DMV written-exam practice',
    loginWeChat: 'Continue with WeChat',
    emailLabel: 'Email',
    emailPlaceholder: 'you@example.com',
    emailRequiredHint: 'Add your email to create your account.',
    emailInUseHint: 'This email already has an account — sign in to link WeChat.',
    continue: 'Continue',
    loading: 'Loading…',
    signedInAs: 'Signed in',
    signOut: 'Sign out',
    home: 'Home',
    devSkip: 'Skip login (dev)'
  },
  zh: {
    appName: 'DMV 备考',
    tagline: '加州 DMV 笔试练习',
    loginWeChat: '微信登录',
    emailLabel: '邮箱',
    emailPlaceholder: 'you@example.com',
    emailRequiredHint: '填写邮箱以创建账户。',
    emailInUseHint: '该邮箱已注册——请先登录后再绑定微信。',
    continue: '继续',
    loading: '加载中…',
    signedInAs: '已登录',
    signOut: '退出登录',
    home: '首页',
    devSkip: '跳过登录（开发）'
  }
} as const

export type MessageKey = keyof typeof messages['en']
