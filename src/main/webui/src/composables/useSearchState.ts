import { ref } from 'vue'

const query = ref('')
const lang = ref('')
const ext = ref('')
const content = ref('')
const submitted = ref('')

export function useSearchState() {
  function search() {
    submitted.value = query.value.trim()
  }

  return { query, lang, ext, content, submitted, search }
}
