import { createRouter, createWebHistory } from 'vue-router'
import { hydrateSessionFromStorage, isAuthenticated } from '../auth/session'
import { hasAnyRole, type UserRole } from '../auth/permission'
import { ensureAuthenticatedSession } from '../api/http'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/market'
    },
    {
      path: '/market',
      name: 'market',
      component: () => import('../views/MarketplaceView.vue'),
      meta: { title: 'Marketplace', public: true }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: 'Sign In', public: true }
    },
    {
      path: '/forbidden',
      name: 'forbidden',
      component: () => import('../views/ForbiddenView.vue'),
      meta: { title: 'Forbidden', public: true }
    },
    {
      path: '/auth/success',
      name: 'oauth-success',
      component: () => import('../views/OAuthSuccessView.vue'),
      meta: { title: 'GitHub Sign In', public: true }
    },
    {
      path: '/auth/error',
      name: 'oauth-error',
      component: () => import('../views/OAuthErrorView.vue'),
      meta: { title: 'GitHub Error', public: true }
    },
    {
      path: '/app',
      component: () => import('../views/AppLayout.vue'),
      meta: { requiresAuth: true, roles: ['USER', 'MERCHANT', 'ADMIN'] as UserRole[] },
      children: [
        { path: '', redirect: '/app/home' },
        {
          path: 'home',
          name: 'home',
          component: () => import('../views/HomeView.vue'),
          meta: { title: 'Dashboard', roles: ['USER', 'MERCHANT', 'ADMIN'] as UserRole[] }
        },
        {
          path: 'catalog',
          name: 'catalog',
          component: () => import('../views/CatalogView.vue'),
          meta: { title: 'Products', roles: ['USER', 'MERCHANT', 'ADMIN'] as UserRole[], manageProduct: false }
        },
        {
          path: 'catalog/manage',
          name: 'catalog-manage',
          component: () => import('../views/CatalogView.vue'),
          meta: { title: 'Product Admin', roles: ['MERCHANT', 'ADMIN'] as UserRole[], manageProduct: true }
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('../views/OrdersView.vue'),
          meta: { title: 'Orders', roles: ['USER', 'MERCHANT', 'ADMIN'] as UserRole[], manageOrder: false }
        },
        {
          path: 'orders/manage',
          name: 'orders-manage',
          component: () => import('../views/OrdersView.vue'),
          meta: { title: 'Order Admin', roles: ['MERCHANT', 'ADMIN'] as UserRole[], manageOrder: true }
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/ProfileView.vue'),
          meta: { title: 'Profile', roles: ['USER', 'MERCHANT', 'ADMIN'] as UserRole[] }
        }
      ]
    },
    { path: '/home', redirect: '/app/home' },
    { path: '/catalog', redirect: '/app/catalog' },
    { path: '/catalog/manage', redirect: '/app/catalog/manage' },
    { path: '/orders', redirect: '/app/orders' },
    { path: '/orders/manage', redirect: '/app/orders/manage' },
    { path: '/profile', redirect: '/app/profile' }
  ]
})

hydrateSessionFromStorage()

router.beforeEach(async (to) => {
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  let loggedIn = isAuthenticated()

  if (requiresAuth && !loggedIn) {
    loggedIn = await ensureAuthenticatedSession()
  }
  const roleMeta = [...to.matched].reverse().find((record) => Array.isArray(record.meta.roles))?.meta.roles as
    | UserRole[]
    | undefined

  if (requiresAuth && !loggedIn) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (to.path === '/login' && loggedIn) {
    return { path: '/app/home' }
  }

  if (loggedIn && roleMeta && roleMeta.length > 0 && !hasAnyRole(...roleMeta)) {
    return { path: '/forbidden' }
  }

  return true
})

router.afterEach((to) => {
  document.title = `My Shop | ${String(to.meta.title || 'Mall')}`
})

export default router
