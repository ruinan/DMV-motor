import Taro from '@tarojs/taro'
import { messages, MessageKey } from '../messages'

export type Lang = 'en' | 'zh'
const KEY = 'dmv_lang'

export function getLang(): Lang {
  const stored = Taro.getStorageSync(KEY)
  if (stored === 'en' || stored === 'zh') return stored
  try {
    const sys = Taro.getSystemInfoSync()
    return sys.language && sys.language.startsWith('zh') ? 'zh' : 'en'
  } catch {
    return 'en'
  }
}

export function setLang(lang: Lang): void {
  Taro.setStorageSync(KEY, lang)
}

export function t(key: MessageKey): string {
  const lang = getLang()
  return (messages[lang][key] as string) ?? (messages.en[key] as string) ?? key
}
