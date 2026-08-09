<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { DownloadPage } from '../api/generated/nana'
import StatusBadge from './StatusBadge.vue'
import { formatBytes, formatDateTime } from '../lib/format'

const { t } = useI18n()

const props = defineProps<{ page: DownloadPage }>()
const emit = defineEmits<{ previous: []; next: [] }>()

const currentPage = () => props.page.page ?? 0
const totalPages = () => Math.max(props.page.totalPages ?? 1, 1)
</script>

<template>
  <div class="pop-card overflow-hidden">
    <div class="overflow-x-auto">
      <table class="w-full text-left text-sm">
        <thead class="bg-pop-ink text-xs font-display uppercase tracking-wide text-white">
          <tr>
            <th class="px-4 py-2">{{ t('history.columns.book') }}</th>
            <th class="px-4 py-2">{{ t('history.columns.status') }}</th>
            <th class="px-4 py-2">{{ t('history.columns.size') }}</th>
            <th class="px-4 py-2">{{ t('history.columns.requestedBy') }}</th>
            <th class="px-4 py-2">{{ t('history.columns.requestedAt') }}</th>
            <th class="px-4 py-2">{{ t('history.columns.details') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="download in page.content" :key="download.id" data-testid="history-row" class="border-b-2 border-pop-ink/20 last:border-0">
            <td class="max-w-56 px-4 py-2">
              <p class="truncate font-semibold" :title="download.title">{{ download.title }}</p>
              <p v-if="download.author" class="truncate text-xs text-pop-ink/60">{{ download.author }}</p>
            </td>
            <td class="px-4 py-2"><StatusBadge :status="download.status" /></td>
            <td class="whitespace-nowrap px-4 py-2">{{ formatBytes(download.sizeBytes) }}</td>
            <td class="px-4 py-2">{{ download.requestedBy }}</td>
            <td class="whitespace-nowrap px-4 py-2">{{ formatDateTime(download.requestedAt) }}</td>
            <td class="max-w-64 px-4 py-2">
              <span v-if="download.errorMessage" class="line-clamp-2 text-xs font-bold text-pop-red" :title="download.errorMessage">
                {{ download.errorMessage }}
              </span>
              <span v-else-if="download.filePath" class="line-clamp-2 break-all text-xs text-pop-ink/60" :title="download.filePath">
                {{ download.filePath }}
              </span>
            </td>
          </tr>
          <tr v-if="!page.content || page.content.length === 0">
            <td colspan="6" class="px-4 py-6 text-center font-display text-sm uppercase tracking-wide text-pop-ink">{{ t('history.empty') }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="flex items-center justify-between border-t-2 border-pop-ink px-4 py-2 text-sm">
      <span class="font-semibold text-pop-ink/70">
        {{ t('history.pageInfo', { current: currentPage() + 1, total: totalPages(), count: page.totalElements ?? 0 }, page.totalElements ?? 0) }}
      </span>
      <div class="flex gap-2">
        <button
          type="button"
          :disabled="currentPage() <= 0"
          class="pop-btn-outline px-3 py-1 text-xs"
          @click="emit('previous')"
        >
          {{ t('history.prev') }}
        </button>
        <button
          type="button"
          :disabled="currentPage() + 1 >= totalPages()"
          class="pop-btn-outline px-3 py-1 text-xs"
          @click="emit('next')"
        >
          {{ t('history.next') }}
        </button>
      </div>
    </div>
  </div>
</template>
