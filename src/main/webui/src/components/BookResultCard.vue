<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCreateDownload } from '../api/generated/nana'
import type { SearchResult } from '../api/generated/nana'
import { useDownloadEvents } from '../composables/useDownloadEvents'
import { formatBytes } from '../lib/format'

const { t } = useI18n()

const props = defineProps<{ book: SearchResult }>()

const { downloads } = useDownloadEvents()
const queued = ref(false)
const errorMessage = ref('')

const event = computed(() => (props.book.md5 ? downloads.get(props.book.md5) : undefined))
const status = computed(() => event.value?.status)

const { mutate, isPending } = useCreateDownload<Error>({
  mutation: {
    onSuccess: () => {
      queued.value = true
    },
    onError: (error) => {
      errorMessage.value = error.message
    },
  },
})

const busy = computed(() => {
  if (isPending.value) return true
  if (status.value === 'FAILED') return false
  if (status.value) return true // PENDING / DOWNLOADING / SUCCESS
  return queued.value
})

const label = computed(() => {
  if (isPending.value) return t('card.queuing')
  switch (status.value) {
    case 'PENDING':
      return t('card.queued')
    case 'DOWNLOADING':
      return t('card.downloading')
    case 'SUCCESS':
      return t('card.success')
    case 'FAILED':
      return t('card.retry')
  }
  return queued.value ? t('card.queued') : t('card.download')
})

const failureMessage = computed(
  () => errorMessage.value || (status.value === 'FAILED' ? event.value?.errorMessage ?? '' : ''),
)

function download() {
  if (!props.book.md5 || !props.book.title) {
    return
  }
  errorMessage.value = ''
  queued.value = false
  mutate({
    data: {
      md5: props.book.md5,
      title: props.book.title,
      author: props.book.author,
      extension: props.book.extension,
    },
  })
}
</script>

<template>
  <li class="pop-card flex gap-3 p-3 hover:-translate-y-1">
    <img
      v-if="book.coverUrl"
      :src="book.coverUrl"
      alt=""
      loading="lazy"
      class="pop-cover h-28 w-20 shrink-0 rounded border-2 border-pop-ink object-cover"
    />
    <div v-else class="pop-cover flex h-28 w-20 shrink-0 items-center justify-center rounded border-2 border-pop-ink text-2xl">📕</div>
    <div class="flex min-w-0 grow flex-col">
      <h3 class="truncate font-display text-sm uppercase tracking-wide" :title="book.title">{{ book.title }}</h3>
      <p v-if="book.author" class="truncate text-sm font-semibold text-pop-ink/70">{{ book.author }}</p>
      <p class="mt-1 flex flex-wrap gap-1 text-xs text-pop-ink">
        <span v-if="book.extension" class="pop-chip">{{ book.extension }}</span>
        <span v-if="book.language" class="pop-chip">{{ book.language }}</span>
        <span v-if="book.year" class="pop-chip">{{ book.year }}</span>
        <span v-if="book.sizeBytes" class="pop-chip">{{ formatBytes(book.sizeBytes) }}</span>
      </p>
      <div class="mt-auto flex min-w-0 items-center gap-2 pt-2">
        <button
          type="button"
          :disabled="busy"
          class="pop-btn px-3 py-1.5 text-xs"
          @click="download"
        >
          {{ label }}
        </button>
        <span v-if="failureMessage" class="truncate text-xs font-bold text-pop-red" :title="failureMessage">{{ failureMessage }}</span>
      </div>
    </div>
  </li>
</template>
