import { createRouter, createWebHistory } from 'vue-router'
import SearchView from './views/SearchView.vue'
import HistoryView from './views/HistoryView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'search', component: SearchView },
    { path: '/history', name: 'history', component: HistoryView },
  ],
})
