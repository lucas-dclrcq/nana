<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { useDownloadEvents } from './composables/useDownloadEvents'

useDownloadEvents()
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-900">
    <header class="border-b border-slate-200 bg-white">
      <nav class="mx-auto flex max-w-5xl items-center gap-6 px-4 py-3">
        <RouterLink to="/" class="text-lg font-bold tracking-tight text-indigo-600">nana</RouterLink>
        <RouterLink
          to="/"
          class="text-sm font-medium text-slate-600 hover:text-slate-900"
          exact-active-class="text-slate-900"
        >
          Search
        </RouterLink>
        <RouterLink
          to="/history"
          class="text-sm font-medium text-slate-600 hover:text-slate-900"
          active-class="text-slate-900"
        >
          History
        </RouterLink>
      </nav>
    </header>
    <main class="mx-auto max-w-5xl px-4 py-6">
      <RouterView />
    </main>
  </div>
</template>
