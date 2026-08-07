<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useDownloadEvents } from './composables/useDownloadEvents'
import LanguageSwitcher from './components/LanguageSwitcher.vue'

const { t } = useI18n()

useDownloadEvents()
</script>

<template>
  <div class="min-h-screen bg-pop-paper text-pop-ink">
    <header class="border-b-2 border-pop-ink bg-pop-yellow">
      <nav class="mx-auto flex max-w-5xl items-center gap-6 px-4 py-3">
        <RouterLink
          to="/"
          class="font-display text-2xl uppercase tracking-wide text-pop-ink [text-shadow:2px_2px_0_var(--color-pop-pink)]"
        >nana</RouterLink>
        <RouterLink
          to="/"
          class="text-sm font-semibold uppercase tracking-wide text-pop-ink/70 hover:text-pop-ink"
          exact-active-class="!text-pop-ink border-b-2 border-pop-pink"
        >
          {{ t('nav.search') }}
        </RouterLink>
        <RouterLink
          to="/history"
          class="text-sm font-semibold uppercase tracking-wide text-pop-ink/70 hover:text-pop-ink"
          active-class="!text-pop-ink border-b-2 border-pop-pink"
        >
          {{ t('nav.history') }}
        </RouterLink>
        <div class="ml-auto flex items-center gap-4">
          <LanguageSwitcher />
        </div>
      </nav>
    </header>
    <main class="mx-auto max-w-5xl px-4 py-6">
      <RouterView />
    </main>
  </div>
</template>
