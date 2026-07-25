<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useListDownloads } from '../api/generated/nana'
import HistoryTable from '../components/HistoryTable.vue'

const { t } = useI18n()

const page = ref(0)
const size = 20
const params = computed(() => ({ page: page.value, size }))

const { data, isLoading, isError, error } = useListDownloads(params)

const downloadPage = computed(() => data.value?.data)
</script>

<template>
  <div class="space-y-4">
    <h1 class="font-display text-2xl uppercase tracking-wide text-pop-ink [text-shadow:2px_2px_0_var(--color-pop-cyan)]">{{ t('history.title') }}</h1>
    <p v-if="isError" class="rounded-lg border-2 border-pop-ink bg-pop-red px-4 py-3 text-sm font-bold text-white shadow-pop">
      {{ (error as Error | null)?.message ?? t('history.error') }}
    </p>
    <p v-else-if="isLoading" class="font-display text-sm uppercase tracking-wide text-pop-ink">{{ t('history.loading') }}</p>
    <HistoryTable
      v-else-if="downloadPage"
      :page="downloadPage"
      @previous="page = Math.max(0, page - 1)"
      @next="page = page + 1"
    />
  </div>
</template>
