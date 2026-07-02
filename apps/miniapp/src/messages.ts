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
    // Study hub / dashboard
    studyHub: 'Study Hub',
    readiness: 'Readiness',
    readyCandidate: 'Ready to book your test',
    notReadyYet: 'Keep going — not exam-ready yet',
    completion: 'Coverage',
    streak: 'Streak',
    streakDays: 'days',
    today: 'Today',
    nextStep: 'Next step',
    startPractice: 'Start practicing',
    // Placeholders (M3/M4 flows land next)
    practiceComingSoon: 'Practice sessions are coming to the mini-program soon.',
    mockComingSoon: 'Mock exams are coming to the mini-program soon.',
    mockRemaining: 'Mock exams left',
    // Me
    account: 'Account',
    currentExam: 'Current exam'
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
    // Study hub / dashboard
    studyHub: '学习中心',
    readiness: '考试准备度',
    readyCandidate: '可以预约考试了',
    notReadyYet: '继续加油——还未达到应考水平',
    completion: '覆盖进度',
    streak: '连续打卡',
    streakDays: '天',
    today: '今日',
    nextStep: '下一步',
    startPractice: '开始练习',
    // Placeholders (M3/M4 flows land next)
    practiceComingSoon: '练习功能即将登陆小程序。',
    mockComingSoon: '模拟考试即将登陆小程序。',
    mockRemaining: '剩余模考次数',
    // Me
    account: '账户',
    currentExam: '当前考试'
  }
} as const

export type MessageKey = keyof typeof messages['en']
