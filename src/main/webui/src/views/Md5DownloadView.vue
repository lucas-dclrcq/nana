<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCreateDownload } from '../api/generated/nana'
import type { DownloadDto } from '../api/generated/nana'
import { useDownloadEvents } from '../composables/useDownloadEvents'
import StatusBadge from '../components/StatusBadge.vue'

const { t } = useI18n()
const route = useRoute()
const { downloads } = useDownloadEvents()

const md5 = computed(() => {
  const raw = route.params.md5
  return typeof raw === 'string' ? raw.trim().toLowerCase() : ''
})

const valid = computed(() => /^[0-9a-f]{32}$/.test(md5.value))

const title = computed(() => {
  const raw = route.query.title
  const value = typeof raw === 'string' ? raw.trim() : ''
  return value || md5.value || 'Book'
})

const author = computed(() => {
  const raw = route.query.author
  const value = typeof raw === 'string' ? raw.trim() : ''
  return value || undefined
})

const extension = computed(() => {
  const raw = route.query.extension
  const value = typeof raw === 'string' ? raw.trim().replace(/^\./, '') : ''
  return value || undefined
})

const event = computed<DownloadDto | undefined>(() => (md5.value ? downloads.get(md5.value) : undefined))
const status = computed(() => event.value?.status)

const errorMessage = ref('')
const lastRequested = ref('')

const { mutate, isPending } = useCreateDownload<Error>({
  mutation: {
    onSuccess: () => {
      lastRequested.value = md5.value
      errorMessage.value = ''
    },
    onError: (error) => {
      errorMessage.value = error.message
    },
  },
})

function launch() {
  if (!valid.value) return
  errorMessage.value = ''
  lastRequested.value = md5.value
  mutate({
    data: {
      md5: md5.value,
      title: title.value,
      author: author.value,
      extension: extension.value,
    },
  })
}

watch(
  md5,
  (value) => {
    errorMessage.value = ''
    if (valid.value && value && value !== lastRequested.value && !event.value) {
      launch()
    }
  },
  { immediate: true },
)

const failureMessage = computed(() => {
  if (status.value === 'FAILED') return event.value?.errorMessage ?? ''
  if (status.value) return ''
  return errorMessage.value
})
</script>

<template>
  <div class="mx-auto max-w-xl space-y-4">
    <div v-if="!valid" class="pop-card p-6 text-center space-y-3">
      <h1 class="font-display text-xl uppercase tracking-wide text-pop-ink">{{ t('md5.invalid') }}</h1>
      <p class="text-sm text-pop-ink/70">{{ t('md5.invalidHint') }}</p>
      <RouterLink to="/" class="pop-btn inline-block px-4 py-2 text-xs">{{ t('md5.backToSearch') }}</RouterLink>
    </div>

    <div v-else class="pop-card p-6 space-y-4">
      <div>
        <p class="font-display text-xs uppercase tracking-wide text-pop-ink/60">{{ t('md5.label') }}</p>
        <h1 class="font-display text-xl uppercase tracking-wide text-pop-ink">{{ title }}</h1>
        <p v-if="author" class="text-sm font-semibold text-pop-ink/70">{{ author }}</p>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <span class="pop-chip font-mono">{{ md5 }}</span>
        <span v-if="extension" class="pop-chip">{{ extension }}</span>
        <StatusBadge v-if="status" :status="status" />
        <span v-else-if="isPending" class="pop-badge bg-pop-yellow text-pop-ink">{{ t('card.queuing') }}</span>
        <span v-else-if="failureMessage" class="pop-badge bg-pop-red text-white">{{ t('md5.error') }}</span>
        <span v-else class="pop-badge bg-pop-yellow text-pop-ink">{{ t('md5.launching') }}</span>
      </div>

      <p
        v-if="failureMessage"
        class="rounded-lg border-2 border-pop-ink bg-pop-red px-4 py-3 text-sm font-bold text-white shadow-pop"
      >
        {{ failureMessage }}
      </p>

      <div class="flex flex-wrap gap-2">
        <button
          v-if="status === 'FAILED' || (!status && failureMessage)"
          type="button"
          class="pop-btn px-4 py-2 text-xs"
          @click="launch"
        >
          {{ t('md5.retry') }}
        </button>
        <RouterLink to="/history" class="pop-btn-outline px-4 py-2 text-xs">{{ t('nav.history') }}</RouterLink>
        <RouterLink to="/" class="pop-btn-outline px-4 py-2 text-xs">{{ t('md5.backToSearch') }}</RouterLink>
      </div>
    </div>
  </div>
</template>
