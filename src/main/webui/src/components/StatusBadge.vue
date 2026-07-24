<script setup lang="ts">
import type { DownloadStatus } from '../api/generated/nana'

const props = defineProps<{ status?: DownloadStatus }>()

const STYLES: Record<string, string> = {
  PENDING: 'bg-pop-yellow text-pop-ink',
  DOWNLOADING: 'bg-pop-cyan text-pop-ink animate-pulse',
  SUCCESS: 'bg-pop-green text-pop-ink',
  FAILED: 'bg-pop-red text-white',
}
</script>

<template>
  <span
    class="pop-badge"
    :class="STYLES[props.status ?? ''] ?? 'bg-pop-yellow text-pop-ink'"
  >
    {{ props.status ?? '—' }}
  </span>
</template>
