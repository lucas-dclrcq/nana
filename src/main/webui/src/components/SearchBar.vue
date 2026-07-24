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
      class="pop-input w-full px-4 py-2 text-sm"
    />
    <button
      type="submit"
      :disabled="!query.trim()"
      class="pop-btn px-4 py-2 text-sm"
    >
      Search
    </button>
  </form>
</template>
