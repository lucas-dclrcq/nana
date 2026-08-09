<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { DownloadStatus } from '../api/generated/nana'

const { t } = useI18n()

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
    data-testid="status-badge"
    class="pop-badge"
    :class="STYLES[props.status ?? ''] ?? 'bg-pop-yellow text-pop-ink'"
  >
    {{ props.status ? t(`status.${props.status}`) : '—' }}
  </span>
</template>
