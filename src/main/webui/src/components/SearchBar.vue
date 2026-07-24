<script setup lang="ts">
const query = defineModel<string>({ required: true })
const emit = defineEmits<{ search: [] }>()
</script>

<template>
  <form class="flex gap-2" @submit.prevent="emit('search')">
    <input
      v-model="query"
      type="search"
      placeholder="Search books, authors, ISBN…"
      class="w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-200"
    />
    <button
      type="submit"
      :disabled="!query.trim()"
      class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-50"
    >
      Search
    </button>
  </form>
</template>
