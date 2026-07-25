<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useGetConfig, useSearchBooks } from '../api/generated/nana'
import { useSearchState } from '../composables/useSearchState'
import SearchBar from '../components/SearchBar.vue'
import SearchFilters from '../components/SearchFilters.vue'
import BookResultCard from '../components/BookResultCard.vue'

const { t } = useI18n()

const { query, lang, ext, content, submitted, search } = useSearchState()

const { data: config } = useGetConfig({
  query: { staleTime: Infinity, gcTime: Infinity },
})
const allowedFormats = computed(() => config.value?.data?.allowedFormats ?? null)

const params = computed(() => ({
  q: submitted.value,
  ...(lang.value ? { lang: lang.value } : {}),
  ...(ext.value ? { ext: ext.value } : {}),
  ...(content.value ? { content: content.value } : {}),
}))

const enabled = computed(() => submitted.value.length > 0)

const { data, isFetching, isError, error } = useSearchBooks(params, {
  query: { enabled, staleTime: 1000 * 60 * 5, gcTime: 1000 * 60 * 30 },
})

const results = computed(() => data.value?.data ?? [])
</script>

<template>
  <div class="space-y-4">
    <SearchBar v-model="query" @search="search" />
    <SearchFilters v-model:lang="lang" v-model:ext="ext" v-model:content="content" :allowed-formats="allowedFormats" />
    <p v-if="isError" class="rounded-lg border-2 border-pop-ink bg-pop-red px-4 py-3 text-sm font-bold text-white shadow-pop">
      {{ (error as Error | null)?.message ?? t('search.failed') }}
    </p>
    <p v-else-if="isFetching" class="font-display text-sm uppercase tracking-wide text-pop-ink">{{ t('search.searching') }}</p>
    <template v-else-if="submitted">
      <p v-if="results.length === 0" class="font-display text-sm uppercase tracking-wide text-pop-ink">{{ t('search.noResults', { query: submitted }) }}</p>
      <ul v-else class="grid gap-3 sm:grid-cols-2">
        <BookResultCard v-for="book in results" :key="book.md5" :book="book" />
      </ul>
    </template>
  </div>
</template>
