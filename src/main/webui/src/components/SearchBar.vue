<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const query = defineModel<string>({ required: true })
const emit = defineEmits<{ search: [] }>()
</script>

<template>
  <form class="flex gap-2" @submit.prevent="emit('search')">
    <input
      v-model="query"
      type="search"
      :placeholder="t('search.placeholder')"
      class="pop-input w-full px-4 py-2 text-sm"
    />
    <button
      type="submit"
      :disabled="!query.trim()"
      class="pop-btn px-4 py-2 text-sm"
    >
      {{ t('search.submit') }}
    </button>
  </form>
</template>
