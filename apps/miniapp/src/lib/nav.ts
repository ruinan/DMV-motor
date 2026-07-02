import Taro from '@tarojs/taro'

/**
 * Tab pages are reached via redirectTo (see TabBar), which leaves them as the
 * sole non-entry page in the stack — WeChat then injects a home capsule in the
 * top-left corner. The in-page TabBar already offers 学习 as home, so hide it.
 */
export function hideHomeCapsule() {
  try {
    const res = Taro.hideHomeButton() as Promise<unknown> | undefined
    void res?.catch?.(() => {})
  } catch {
    // Base library < 2.8.3 — API missing; the capsule stays, which is harmless.
  }
}
