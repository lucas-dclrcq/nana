import { createI18n } from 'vue-i18n'
import en from './locales/en'
import fr from './locales/fr'

export const SUPPORTED_LOCALES = ['en', 'fr'] as const
export type Locale = (typeof SUPPORTED_LOCALES)[number]

const STORAGE_KEY = 'nana-locale'
const DEFAULT_LOCALE: Locale = 'en'

function isSupported(value: string | null | undefined): value is Locale {
  return !!value && (SUPPORTED_LOCALES as readonly string[]).includes(value)
}

export function detectLocale(): Locale {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (isSupported(stored)) {
    return stored
  }
  const browser = navigator.language.slice(0, 2).toLowerCase()
  return isSupported(browser) ? browser : DEFAULT_LOCALE
}

export const i18n = createI18n({
  legacy: false,
  locale: detectLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  messages: { en, fr },
})

export function setLocale(locale: Locale): void {
  i18n.global.locale.value = locale
  localStorage.setItem(STORAGE_KEY, locale)
  document.documentElement.lang = locale
}

// Keep <html lang> in sync with the initial locale.
document.documentElement.lang = i18n.global.locale.value
