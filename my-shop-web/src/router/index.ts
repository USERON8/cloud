import { createRouter, createWebHistory } from 'vue-router'
import { hydrateSessionFromStorage, isAuthenticated } from '../auth/session'
import { hasAnyRole, type UserRole } from '../auth/permission'
import { hydrateCartFromStorage } from '../store/cart'

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
      path: '/callback',
      name: 'oauth-success',
      component: () => import('../views/OAuthSuccessView.vue'),
      meta: { title: 'OAuth Callback', public: true }
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
        },
        {
          path: 'addresses',
          name: 'addresses',
          component: () => import('../views/AddressBookView.vue'),
          meta: { title: 'Addresses', roles: ['USER', 'MERCHANT', 'ADMIN'] as UserRole[] }
        },
        {
          path: 'cart',
          name: 'cart',
          component: () => import('../views/CartView.vue'),
          meta: { title: 'Shopping Cart', roles: ['USER', 'MERCHANT', 'ADMIN'] as UserRole[] }
        },
        {
          path: 'payments',
          name: 'payments',
          component: () => import('../views/PaymentLookupView.vue'),
          meta: { title: 'Payments', roles: ['USER', 'MERCHANT', 'ADMIN'] as UserRole[] }
        },
        {
          path: 'stock',
          name: 'stock',
          component: () => import('../views/StockLedgerView.vue'),
          meta: { title: 'Stock Ledger', roles: ['ADMIN'] as UserRole[] }
        },
        {
          path: 'merchant',
          name: 'merchant-center',
          component: () => import('../views/MerchantCenterView.vue'),
          meta: { title: 'Merchant Center', roles: ['MERCHANT'] as UserRole[] }
        },
        {
          path: 'admin',
          name: 'admin-center',
          component: () => import('../views/AdminCenterView.vue'),
          meta: { title: 'Admin Center', roles: ['ADMIN'] as UserRole[] }
        },
        {
          path: 'ops',
          name: 'ops-center',
          component: () => import('../views/OpsCenterView.vue'),
          meta: { title: 'Ops Center', roles: ['MERCHANT', 'ADMIN'] as UserRole[] }
        }
      ]
    },
    { path: '/home', redirect: '/app/home' },
    { path: '/addresses', redirect: '/app/addresses' },
    { path: '/cart', redirect: '/app/cart' },
    { path: '/payments', redirect: '/app/payments' },
    { path: '/stock', redirect: '/app/stock' },
    { path: '/catalog', redirect: '/app/catalog' },
    { path: '/catalog/manage', redirect: '/app/catalog/manage' },
    { path: '/orders', redirect: '/app/orders' },
    { path: '/orders/manage', redirect: '/app/orders/manage' },
    { path: '/profile', redirect: '/app/profile' },
    { path: '/merchant', redirect: '/app/merchant' },
    { path: '/admin', redirect: '/app/admin' },
    { path: '/ops', redirect: '/app/ops' }
  ]
})

hydrateSessionFromStorage()
hydrateCartFromStorage()

router.beforeEach(async (to) => {
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const loggedIn = isAuthenticated()
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
