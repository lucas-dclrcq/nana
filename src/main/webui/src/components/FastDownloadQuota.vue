<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useGetFastDownloadQuota } from '../api/generated/nana'

const { t } = useI18n()

const { data } = useGetFastDownloadQuota({
  query: { staleTime: 1000 * 60 * 5, gcTime: 1000 * 60 * 30 },
})

const quota = computed(() => data.value?.data)
const hasQuota = computed(() => quota.value?.remaining != null && quota.value?.total != null)
const tooltip = computed(() =>
  hasQuota.value
    ? t('quota.tooltip', {
        remaining: quota.value!.remaining!,
        total: quota.value!.total!,
        time: quota.value!.updatedAt ? new Date(quota.value!.updatedAt).toLocaleString() : '—',
      })
    : '',
)
</script>

<template>
  <span
    v-if="hasQuota"
    data-testid="quota-badge"
    class="pop-badge bg-pop-cyan text-pop-ink"
    :title="tooltip"
    :aria-label="tooltip"
  >
    ⚡ {{ quota!.remaining }} / {{ quota!.total }}
  </span>
</template>
