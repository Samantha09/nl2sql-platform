import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Home },
    { path: '/query', component: () => import('../views/Query.vue') },
    { path: '/result', component: () => import('../views/Result.vue') },
    { path: '/history', component: () => import('../views/History.vue') },
    { path: '/datasource', component: () => import('../views/DataSource.vue') },
    { path: '/schema', component: () => import('../views/SchemaViewer.vue') },
    { path: '/statistics', component: () => import('../views/Statistics.vue') }
  ]
})

export default router
