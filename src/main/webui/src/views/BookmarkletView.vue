<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const copied = ref(false)

const bookmarklet = computed(() => {
  const base = JSON.stringify(window.location.origin)
  const code = `(()=>{const NANA=${base};const m=location.pathname.match(/\\/md5\\/([0-9a-fA-F]{32})/);if(!m){alert('No MD5 found on this page');return;}const t=(document.title||'').replace(/\\s*[-–—|]\\s*Anna's Archive.*$/i,'').trim();const ext=((document.body.innerText||'').match(/(?:\\.|\\b)(epub|pdf|mobi|azw3|kepub|cbz|djvu|fb2)\\b/i)||[])[1]||'';const u=new URL('/md5/'+m[1].toLowerCase(),NANA);if(t)u.searchParams.set('title',t);if(ext)u.searchParams.set('extension',ext);window.open(u.toString(),'_blank');})()`
  return 'javascript:' + code
})

async function copy() {
  try {
    await navigator.clipboard.writeText(bookmarklet.value)
    copied.value = true
    window.setTimeout(() => (copied.value = false), 2000)
  } catch {
    // Clipboard API can be unavailable on non-secure origins; the user can select the text manually.
  }
}
</script>

<template>
  <div class="mx-auto max-w-2xl space-y-4">
    <h1 class="font-display text-2xl uppercase tracking-wide text-pop-ink [text-shadow:2px_2px_0_var(--color-pop-pink)]">
      {{ t('bookmarklet.title') }}
    </h1>
    <p class="text-sm text-pop-ink/80">{{ t('bookmarklet.intro') }}</p>

    <div class="pop-card p-4 space-y-3">
      <div class="flex items-center justify-between gap-3">
        <a
          :href="bookmarklet"
          draggable="true"
          class="font-display text-sm uppercase tracking-wide text-pop-blue underline decoration-pop-pink decoration-2"
          @click.prevent
        >
          {{ t('bookmarklet.name') }}
        </a>
        <button type="button" class="pop-btn px-3 py-1.5 text-xs" @click="copy">
          {{ copied ? t('bookmarklet.copied') : t('bookmarklet.copy') }}
        </button>
      </div>
      <textarea
        readonly
        :value="bookmarklet"
        rows="5"
        spellcheck="false"
        class="pop-input w-full resize-none p-2 font-mono text-xs"
      ></textarea>
    </div>
  </div>
</template>
