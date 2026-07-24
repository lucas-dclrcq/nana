<script setup lang="ts">
import type { DownloadStatus } from '../api/generated/nana'

const props = defineProps<{ status?: DownloadStatus }>()

const STYLES: Record<string, string> = {
  PENDING: 'bg-slate-100 text-slate-700',
  DOWNLOADING: 'bg-blue-100 text-blue-700 animate-pulse',
  SUCCESS: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
}
</script>

<template>
  <span
    class="inline-block rounded-full px-2 py-0.5 text-xs font-semibold"
    :class="STYLES[props.status ?? ''] ?? 'bg-slate-100 text-slate-700'"
  >
    {{ props.status ?? '—' }}
  </span>
</template>
