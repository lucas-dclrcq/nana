<script setup lang="ts">
import type { DownloadPage } from '../api/generated/nana'
import StatusBadge from './StatusBadge.vue'
import { formatBytes, formatDateTime } from '../lib/format'

const props = defineProps<{ page: DownloadPage }>()
const emit = defineEmits<{ previous: []; next: [] }>()

const currentPage = () => props.page.page ?? 0
const totalPages = () => Math.max(props.page.totalPages ?? 1, 1)
</script>

<template>
  <div class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
    <div class="overflow-x-auto">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-slate-200 bg-slate-50 text-xs uppercase text-slate-500">
          <tr>
            <th class="px-4 py-2">Book</th>
            <th class="px-4 py-2">Status</th>
            <th class="px-4 py-2">Size</th>
            <th class="px-4 py-2">Requested by</th>
            <th class="px-4 py-2">Requested at</th>
            <th class="px-4 py-2">Details</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="download in page.content" :key="download.id" class="border-b border-slate-100 last:border-0">
            <td class="max-w-56 px-4 py-2">
              <p class="truncate font-medium" :title="download.title">{{ download.title }}</p>
              <p v-if="download.author" class="truncate text-xs text-slate-500">{{ download.author }}</p>
            </td>
            <td class="px-4 py-2"><StatusBadge :status="download.status" /></td>
            <td class="whitespace-nowrap px-4 py-2">{{ formatBytes(download.sizeBytes) }}</td>
            <td class="px-4 py-2">{{ download.requestedBy }}</td>
            <td class="whitespace-nowrap px-4 py-2">{{ formatDateTime(download.requestedAt) }}</td>
            <td class="max-w-64 px-4 py-2">
              <span v-if="download.errorMessage" class="line-clamp-2 text-xs text-red-600" :title="download.errorMessage">
                {{ download.errorMessage }}
              </span>
              <span v-else-if="download.filePath" class="line-clamp-2 break-all text-xs text-slate-500" :title="download.filePath">
                {{ download.filePath }}
              </span>
            </td>
          </tr>
          <tr v-if="!page.content || page.content.length === 0">
            <td colspan="6" class="px-4 py-6 text-center text-sm text-slate-500">No downloads yet.</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="flex items-center justify-between border-t border-slate-200 px-4 py-2 text-sm">
      <span class="text-slate-500">
        Page {{ currentPage() + 1 }} of {{ totalPages() }} · {{ page.totalElements ?? 0 }} download(s)
      </span>
      <div class="flex gap-2">
        <button
          type="button"
          :disabled="currentPage() <= 0"
          class="rounded-lg border border-slate-300 px-3 py-1 text-xs font-medium hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          @click="emit('previous')"
        >
          Previous
        </button>
        <button
          type="button"
          :disabled="currentPage() + 1 >= totalPages()"
          class="rounded-lg border border-slate-300 px-3 py-1 text-xs font-medium hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          @click="emit('next')"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>
