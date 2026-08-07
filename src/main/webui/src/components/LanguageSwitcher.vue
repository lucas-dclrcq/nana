<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
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

const current = computed(() => locale.value as Locale)

const open = ref(false)
const root = ref<HTMLElement | null>(null)
const activeIndex = ref(0)

function toggle() {
  open.value ? close() : show()
}

function show() {
  activeIndex.value = SUPPORTED_LOCALES.indexOf(current.value)
  open.value = true
}

function close() {
  open.value = false
}

function select(code: Locale) {
  setLocale(code)
  close()
}

function onKeydown(event: KeyboardEvent) {
  if (!open.value) {
    if (['Enter', ' ', 'ArrowDown', 'ArrowUp'].includes(event.key)) {
      event.preventDefault()
      show()
    }
    return
  }
  switch (event.key) {
    case 'Escape':
      event.preventDefault()
      close()
      break
    case 'ArrowDown':
      event.preventDefault()
      activeIndex.value = (activeIndex.value + 1) % options.value.length
      break
    case 'ArrowUp':
      event.preventDefault()
      activeIndex.value =
        (activeIndex.value - 1 + options.value.length) % options.value.length
      break
    case 'Enter':
    case ' ':
      event.preventDefault()
      select(options.value[activeIndex.value].code)
      break
    case 'Tab':
      close()
      break
  }
}

function onClickOutside(event: MouseEvent) {
  if (root.value && !root.value.contains(event.target as Node)) close()
}

onMounted(() => document.addEventListener('mousedown', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('mousedown', onClickOutside))
</script>

<template>
  <div ref="root" class="relative" @keydown="onKeydown">
    <button
      type="button"
      class="pop-input flex cursor-pointer items-center gap-1.5 bg-pop-pink px-2 py-1.5 text-sm font-semibold"
      :aria-label="t('language.label')"
      :title="t('language.label')"
      aria-haspopup="listbox"
      :aria-expanded="open"
      @click="toggle"
    >
      <span>{{ FLAGS[current] }} {{ t(`language.${current}`) }}</span>
      <svg
        class="h-3 w-3 transition-transform duration-100"
        :class="{ 'rotate-180': open }"
        viewBox="0 0 12 12"
        fill="none"
        stroke="currentColor"
        stroke-width="2.5"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path d="M2 4.5 6 8.5 10 4.5" />
      </svg>
    </button>

    <ul
      v-if="open"
      role="listbox"
      :aria-label="t('language.label')"
      class="absolute right-0 z-20 mt-2 min-w-full overflow-hidden rounded-lg border-2 border-pop-ink bg-white shadow-pop"
    >
      <li
        v-for="(option, index) in options"
        :key="option.code"
        role="option"
        :aria-selected="option.code === current"
        class="flex cursor-pointer items-center gap-1.5 whitespace-nowrap px-3 py-1.5 text-sm font-semibold"
        :class="{
          'bg-pop-yellow': index === activeIndex,
          'bg-pop-pink text-white':
            option.code === current && index !== activeIndex,
        }"
        @mouseenter="activeIndex = index"
        @click="select(option.code)"
      >
        <span>{{ option.flag }}</span>
        <span>{{ option.label }}</span>
      </li>
    </ul>
  </div>
</template>
