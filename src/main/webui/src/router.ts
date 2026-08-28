import { createRouter, createWebHistory } from 'vue-router'
import SearchView from './views/SearchView.vue'
import HistoryView from './views/HistoryView.vue'
import BookmarkletView from './views/BookmarkletView.vue'
import Md5DownloadView from './views/Md5DownloadView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'search', component: SearchView },
    { path: '/history', name: 'history', component: HistoryView },
    { path: '/bookmarklet', name: 'bookmarklet', component: BookmarkletView },
    { path: '/md5/:md5', name: 'md5-download', component: Md5DownloadView },
  ],
})
