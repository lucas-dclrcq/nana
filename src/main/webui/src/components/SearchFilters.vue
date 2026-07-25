<script setup lang="ts">
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps<{ allowedFormats?: string[] | null }>()

const lang = defineModel<string>('lang', {required: true})
const ext = defineModel<string>('ext', {required: true})
const content = defineModel<string>('content', {required: true})

const LANGUAGES = ['en', 'fr', 'de', 'es', 'it', 'nl', 'pt']

// Fallback list used only until the server config (allowedFormats) has loaded.
const EXTENSIONS = ['epub', 'kepub', 'pdf', 'mobi', 'azw3', 'cbz', 'djvu', 'fb2']

const CONTENT_TYPES = [
  'book_fiction',
  'book_nonfiction',
  'book_unknown',
  'book_comic',
  'magazine',
  'standards_document',
  'journal_article',
]

const formats = computed(() =>
  props.allowedFormats && props.allowedFormats.length > 0 ? props.allowedFormats : EXTENSIONS,
)

// When exactly one format is allowed the select is hidden and the filter is forced to that format.
const single = computed(() => props.allowedFormats?.length === 1)

watch(
  () => props.allowedFormats,
  (allowed) => {
    if (allowed?.length === 1) {
      ext.value = allowed[0]
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="flex flex-wrap gap-2">
    <select
        v-model="lang"
        class="pop-input bg-pop-cyan px-2 py-1.5 text-sm font-semibold"
    >
      <option value="">{{ t('filters.anyLanguage') }}</option>
      <option v-for="option in LANGUAGES" :key="option" :value="option">{{ t(`filters.languages.${option}`) }}</option>
    </select>
    <select
        v-if="!single"
        v-model="ext"
        class="pop-input bg-pop-yellow px-2 py-1.5 text-sm font-semibold"
    >
      <option value="">{{ t('filters.anyFormat') }}</option>
      <option v-for="option in formats" :key="option" :value="option">.{{ option }}</option>
    </select>
    <select
        v-model="content"
        class="pop-input bg-pop-blue px-2 py-1.5 text-sm font-semibold text-white"
    >
      <option value="">{{ t('filters.anyType') }}</option>
      <option v-for="option in CONTENT_TYPES" :key="option" :value="option">{{ t(`filters.contentTypes.${option}`) }}</option>
    </select>
  </div>
</template>
