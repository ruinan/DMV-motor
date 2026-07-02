export default defineAppConfig({
  // Only inject components a page actually uses (clears DevTools' "组件按需注入"
  // code-quality hint and speeds first render).
  lazyCodeLoading: 'requiredComponents',
  // Dashboard is the entry page — its ensureAuthed() guard bounces anonymous
  // visitors to login (and dev bypass signs in silently, never showing login).
  // Bottom navigation is our own TabBar component (no native tabBar), so these
  // are all plain pages switched via redirectTo.
  pages: [
    'pages/dashboard/index',
    'pages/login/index',
    'pages/practice/index',
    'pages/mock/index',
    'pages/me/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#ffffff',
    navigationBarTitleText: 'DMV 备考',
    navigationBarTextStyle: 'black'
  }
})
