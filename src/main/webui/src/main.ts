import { createApp } from 'vue'
import { VueQueryPlugin } from '@tanstack/vue-query'
import App from './App.vue'
import { router } from './router'
import { i18n } from './i18n'
import './style.css'

createApp(App).use(router).use(VueQueryPlugin).use(i18n).mount('#app')
