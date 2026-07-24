<script setup lang="ts">
import { computed, ref } from 'vue'
import { useListDownloads } from '../api/generated/nana'
import HistoryTable from '../components/HistoryTable.vue'

const page = ref(0)
const size = 20
const params = computed(() => ({ page: page.value, size }))

const { data, isLoading, isError, error } = useListDownloads(params)

const downloadPage = computed(() => data.value?.data)
</script>

<template>
  <div class="space-y-4">
    <h1 class="text-xl font-bold">Download history</h1>
    <p v-if="isError" class="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
      {{ (error as Error | null)?.message ?? 'Could not load the download history' }}
    </p>
    <p v-else-if="isLoading" class="text-sm text-slate-500">Loading…</p>
    <HistoryTable
      v-else-if="downloadPage"
      :page="downloadPage"
      @previous="page = Math.max(0, page - 1)"
      @next="page = page + 1"
    />
  </div>
</template>
