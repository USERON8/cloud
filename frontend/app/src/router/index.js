import { createRouter, createWebHistory } from 'vue-router'
import StockManager from '../components/StockManager.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: StockManager,
    meta: {
      title: '首页'
    }
  },
  {
    path: '/stock',
    name: 'Stock',
    component: StockManager,
    meta: {
      title: '库存管理'
    }
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('../components/Statistics.vue'),
    meta: {
      title: '统计报表'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title} - 云库存管理系统`
  next()
})

export default router