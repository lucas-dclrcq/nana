<script setup lang="ts">
const lang = defineModel<string>('lang', { required: true })
const ext = defineModel<string>('ext', { required: true })
const content = defineModel<string>('content', { required: true })

const LANGUAGES = [
  { value: '', label: 'Any language' },
  { value: 'en', label: 'English' },
  { value: 'fr', label: 'Français' },
  { value: 'de', label: 'Deutsch' },
  { value: 'es', label: 'Español' },
  { value: 'it', label: 'Italiano' },
  { value: 'nl', label: 'Nederlands' },
  { value: 'pt', label: 'Português' },
]

const EXTENSIONS = ['epub', 'pdf', 'mobi', 'azw3', 'cbz', 'djvu', 'fb2']

const CONTENT_TYPES = [
  { value: '', label: 'Any type' },
  { value: 'book_fiction', label: 'Fiction' },
  { value: 'book_nonfiction', label: 'Non-fiction' },
  { value: 'book_unknown', label: 'Unknown book' },
  { value: 'book_comic', label: 'Comic' },
  { value: 'magazine', label: 'Magazine' },
  { value: 'standards_document', label: 'Standards document' },
  { value: 'journal_article', label: 'Journal article' },
]
</script>

<template>
  <div class="flex flex-wrap gap-2">
    <select
      v-model="lang"
      class="rounded-lg border border-slate-300 bg-white px-2 py-1.5 text-sm shadow-sm focus:border-indigo-500 focus:outline-none"
    >
      <option v-for="option in LANGUAGES" :key="option.value" :value="option.value">{{ option.label }}</option>
    </select>
    <select
      v-model="ext"
      class="rounded-lg border border-slate-300 bg-white px-2 py-1.5 text-sm shadow-sm focus:border-indigo-500 focus:outline-none"
    >
      <option value="">Any format</option>
      <option v-for="option in EXTENSIONS" :key="option" :value="option">.{{ option }}</option>
    </select>
    <select
      v-model="content"
      class="rounded-lg border border-slate-300 bg-white px-2 py-1.5 text-sm shadow-sm focus:border-indigo-500 focus:outline-none"
    >
      <option v-for="option in CONTENT_TYPES" :key="option.value" :value="option.value">{{ option.label }}</option>
    </select>
  </div>
</template>
