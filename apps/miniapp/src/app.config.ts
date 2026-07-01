export default defineAppConfig({
  // Only inject components a page actually uses (clears DevTools' "组件按需注入"
  // code-quality hint and speeds first render).
  lazyCodeLoading: 'requiredComponents',
  pages: [
    'pages/login/index',
    'pages/index/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#ffffff',
    navigationBarTitleText: 'DMV 备考',
    navigationBarTextStyle: 'black'
  }
})
