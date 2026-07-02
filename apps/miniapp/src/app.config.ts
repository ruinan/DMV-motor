export default defineAppConfig({
  // Only inject components a page actually uses (clears DevTools' "组件按需注入"
  // code-quality hint and speeds first render).
  lazyCodeLoading: 'requiredComponents',
  pages: [
    'pages/login/index',
    'pages/dashboard/index',
    'pages/practice/index',
    'pages/mock/index',
    'pages/me/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#ffffff',
    navigationBarTitleText: 'DMV 备考',
    navigationBarTextStyle: 'black'
  },
  // Mirrors the web MobileTabBar's 4 items (学习/练习/模考/我的). Text-only for
  // now (iconPath is optional); selectedColor is re-tinted per exam at runtime
  // via Taro.setTabBarStyle (see useExamTheme).
  tabBar: {
    color: '#64748b',
    selectedColor: '#1b5e9b',
    backgroundColor: '#ffffff',
    borderStyle: 'black',
    list: [
      { pagePath: 'pages/dashboard/index', text: '学习' },
      { pagePath: 'pages/practice/index', text: '练习' },
      { pagePath: 'pages/mock/index', text: '模考' },
      { pagePath: 'pages/me/index', text: '我的' }
    ]
  }
})
