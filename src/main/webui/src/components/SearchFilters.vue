<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const lang = defineModel<string>('lang', {required: true})
const ext = defineModel<string>('ext', {required: true})
const content = defineModel<string>('content', {required: true})

const LANGUAGES = ['en', 'fr', 'de', 'es', 'it', 'nl', 'pt']

const EXTENSIONS = ['epub', 'pdf', 'mobi', 'azw3', 'cbz', 'djvu', 'fb2']

const CONTENT_TYPES = [
  'book_fiction',
  'book_nonfiction',
  'book_unknown',
  'book_comic',
  'magazine',
  'standards_document',
  'journal_article',
]
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
        v-model="ext"
        class="pop-input bg-pop-yellow px-2 py-1.5 text-sm font-semibold"
    >
      <!--      TODO: rend filtrable par configuration la list des formats acceptés, si un seul format accepté, cache le select-->
      <option value="">{{ t('filters.anyFormat') }}</option>
      <option v-for="option in EXTENSIONS" :key="option" :value="option">.{{ option }}</option>
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
