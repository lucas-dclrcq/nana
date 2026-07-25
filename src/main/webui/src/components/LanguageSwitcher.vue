<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale, SUPPORTED_LOCALES, type Locale } from '../i18n'

const { t, locale } = useI18n()

const FLAGS: Record<Locale, string> = {
  en: '🇬🇧',
  fr: '🇫🇷',
}

const options = computed(() =>
  SUPPORTED_LOCALES.map((code) => ({
    code,
    flag: FLAGS[code],
    label: t(`language.${code}`),
  })),
)

const current = computed({
  get: () => locale.value as Locale,
  set: (value: Locale) => setLocale(value),
})
</script>

<template>
  <select
    v-model="current"
    :aria-label="t('language.label')"
    :title="t('language.label')"
    class="pop-input cursor-pointer bg-pop-pink px-2 py-1.5 text-sm font-semibold"
  >
    <option v-for="option in options" :key="option.code" :value="option.code">
      {{ option.flag }} {{ option.label }}
    </option>
  </select>
</template>
