<script setup lang="ts">
import { computed, ref } from 'vue'
import { useCreateDownload } from '../api/generated/nana'
import type { SearchResult } from '../api/generated/nana'
import { useDownloadEvents } from '../composables/useDownloadEvents'
import { formatBytes } from '../lib/format'

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
  if (isPending.value) return 'Queuing…'
  switch (status.value) {
    case 'PENDING':
      return 'Queued ✓'
    case 'DOWNLOADING':
      return 'Downloading…'
    case 'SUCCESS':
      return 'Success ✓'
    case 'FAILED':
      return 'Retry'
  }
  return queued.value ? 'Queued ✓' : 'Download'
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
  <li class="flex gap-3 rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
    <img
      v-if="book.coverUrl"
      :src="book.coverUrl"
      alt=""
      loading="lazy"
      class="h-28 w-20 shrink-0 rounded bg-slate-100 object-cover"
    />
    <div v-else class="flex h-28 w-20 shrink-0 items-center justify-center rounded bg-slate-100 text-2xl">📕</div>
    <div class="flex min-w-0 grow flex-col">
      <h3 class="truncate font-semibold" :title="book.title">{{ book.title }}</h3>
      <p v-if="book.author" class="truncate text-sm text-slate-600">{{ book.author }}</p>
      <p class="mt-1 flex flex-wrap gap-1 text-xs text-slate-500">
        <span v-if="book.extension" class="rounded bg-slate-100 px-1.5 py-0.5 font-medium uppercase">{{ book.extension }}</span>
        <span v-if="book.language" class="rounded bg-slate-100 px-1.5 py-0.5 uppercase">{{ book.language }}</span>
        <span v-if="book.year" class="rounded bg-slate-100 px-1.5 py-0.5">{{ book.year }}</span>
        <span v-if="book.sizeBytes" class="rounded bg-slate-100 px-1.5 py-0.5">{{ formatBytes(book.sizeBytes) }}</span>
      </p>
      <div class="mt-auto flex min-w-0 items-center gap-2 pt-2">
        <button
          type="button"
          :disabled="busy"
          class="rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-60"
          @click="download"
        >
          {{ label }}
        </button>
        <span v-if="failureMessage" class="truncate text-xs text-red-600" :title="failureMessage">{{ failureMessage }}</span>
      </div>
    </div>
  </li>
</template>
