import { createI18n } from 'vue-i18n'
import en from './locales/en'
import zhCN from './locales/zh-CN'

// Detect browser language
function getDefaultLocale(): string {
  const saved = localStorage.getItem('aurora_locale')
  if (saved) return saved
  const browserLang = navigator.language.toLowerCase()
  if (browserLang.startsWith('zh')) return 'zh-CN'
  return 'en'
}

const i18n = createI18n({
  legacy: false,
  locale: getDefaultLocale(),
  fallbackLocale: 'en',
  messages: { en, 'zh-CN': zhCN },
})

export default i18n

// Hot-switch locale at runtime
export function setLocale(locale: string) {
  ;(i18n.global.locale as any).value = locale
  localStorage.setItem('aurora_locale', locale)
}
