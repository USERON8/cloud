<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logout } from '../api/auth'
import { clearSession, sessionState } from '../auth/session'
import { useRole, type UserRole } from '../auth/permission'

interface NavItem {
  label: string
  path: string
  icon: 'dashboard' | 'products' | 'products-admin' | 'orders' | 'orders-admin' | 'profile'
  roles: UserRole[]
}

const route = useRoute()
const router = useRouter()
const { role } = useRole()

const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/app/home', icon: 'dashboard', roles: ['USER', 'MERCHANT', 'ADMIN'] },
  { label: 'Products', path: '/app/catalog', icon: 'products', roles: ['USER', 'MERCHANT', 'ADMIN'] },
  { label: 'Product Admin', path: '/app/catalog/manage', icon: 'products-admin', roles: ['MERCHANT', 'ADMIN'] },
  { label: 'Orders', path: '/app/orders', icon: 'orders', roles: ['USER', 'MERCHANT', 'ADMIN'] },
  { label: 'Order Admin', path: '/app/orders/manage', icon: 'orders-admin', roles: ['MERCHANT', 'ADMIN'] },
  { label: 'Profile', path: '/app/profile', icon: 'profile', roles: ['USER', 'MERCHANT', 'ADMIN'] }
]

const visibleNavItems = computed(() => navItems.filter((item) => item.roles.includes(role.value)))
const currentTitle = computed(() => String(route.meta.title || 'My Shop'))
const displayName = computed(() => sessionState.user?.nickname || sessionState.user?.username || 'Current User')
const roleLabel = computed(() => role.value)
const userInitial = computed(() => (displayName.value.trim()[0] || 'U').toUpperCase())
const avatarLoadFailed = ref(false)

const defaultAvatarDataUrl = computed(() => {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">
    <defs>
      <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#0d2e7d"/>
        <stop offset="100%" stop-color="#3f8cff"/>
      </linearGradient>
    </defs>
    <rect width="120" height="120" rx="36" fill="url(#g)"/>
    <circle cx="60" cy="45" r="18" fill="#ffffff" fill-opacity="0.92"/>
    <path d="M23 100c4-18 20-30 37-30s33 12 37 30" fill="#ffffff" fill-opacity="0.92"/>
    <text x="60" y="112" text-anchor="middle" font-size="18" font-family="Avenir, 'PingFang SC', sans-serif" fill="#f5f8ff">${userInitial.value}</text>
  </svg>`
  return `data:image/svg+xml;base64,${window.btoa(unescape(encodeURIComponent(svg)))}`
})

const avatarUrl = computed(() => {
  const userAvatar = sessionState.user?.avatarUrl?.trim()
  if (userAvatar && !avatarLoadFailed.value) {
    return userAvatar
  }
  return defaultAvatarDataUrl.value
})

const iconMap: Record<NavItem['icon'], string> = {
  dashboard: 'M5 13h6V5H5v8Zm8 6h6V5h-6v14ZM5 19h6v-4H5v4Zm8 0h6v-2h-6v2Z',
  products: 'M4 7.5 12 4l8 3.5-8 3.5L4 7.5Zm0 3.5 8 3.5 8-3.5M4 14.5 12 18l8-3.5',
  'products-admin': 'M4 5h16v4H4V5Zm0 5.5h10V20H4v-9.5Zm12 2h4v7.5h-4v-7.5Z',
  orders: 'M6 5h10l3 3v11H6V5Zm10 0v3h3M9 12h6M9 15h6',
  'orders-admin': 'M7 4h10v2h3v14H4V6h3V4Zm2 2h6M8 12h8M8 16h5',
  profile: 'M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4Zm0 2c-3.3 0-6 1.8-7 4.5h14c-1-2.7-3.7-4.5-7-4.5Z'
}

function getIconPath(icon: NavItem['icon']): string {
  return iconMap[icon]
}

watch(
  () => sessionState.user?.avatarUrl,
  () => {
    avatarLoadFailed.value = false
  }
)

function handleAvatarError(): void {
  avatarLoadFailed.value = true
}

function isNavActive(path: string): boolean {
  if (path === '/app/catalog') {
    return route.path === '/app/catalog'
  }
  if (path === '/app/catalog/manage') {
    return route.path === '/app/catalog/manage'
  }
  if (path === '/app/orders') {
    return route.path === '/app/orders'
  }
  if (path === '/app/orders/manage') {
    return route.path === '/app/orders/manage'
  }
  return route.path === path || route.path.startsWith(`${path}/`)
}

async function handleLogout(): Promise<void> {
  try {
    await logout()
  } catch {
    // Ignore logout API failure and clear local session directly.
  } finally {
    clearSession()
    ElMessage.success('Signed out.')
    await router.replace('/login')
  }
}
</script>

<template>
  <div class="app-shell">
    <aside class="desktop-nav glass-card">
      <h1 class="brand">My Shop</h1>
      <router-link
        v-for="item in visibleNavItems"
        :key="item.path"
        :to="item.path"
        class="nav-link"
        :class="{ active: isNavActive(item.path) }"
      >
        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none">
            <path :d="getIconPath(item.icon)" />
          </svg>
        </span>
        <span class="nav-label">{{ item.label }}</span>
      </router-link>
    </aside>

    <main class="main-panel">
      <header class="top-bar glass-card">
        <div>
          <div class="muted">Apple-like mobile-first UI</div>
          <h2>{{ currentTitle }}</h2>
        </div>

        <div class="top-actions">
          <img class="avatar" :src="avatarUrl" alt="User avatar" @error="handleAvatarError" />
          <span class="role-chip">{{ roleLabel }}</span>
          <span class="user-chip">{{ displayName }}</span>
          <el-button round @click="$router.push('/market')">访客主页</el-button>
          <el-button round @click="handleLogout">Sign Out</el-button>
        </div>
      </header>

      <section class="content">
        <router-view />
      </section>
    </main>

    <nav class="mobile-tab glass-card">
      <router-link
        v-for="item in visibleNavItems"
        :key="item.path"
        :to="item.path"
        class="tab-item"
        :class="{ active: isNavActive(item.path) }"
      >
        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none">
            <path :d="getIconPath(item.icon)" />
          </svg>
        </span>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: clamp(14rem, 21vw, 17.5rem) minmax(0, 1fr);
  gap: clamp(0.85rem, 1.2vw, 1.2rem);
  padding: clamp(0.85rem, 1.2vw, 1.2rem);
  max-width: 96rem;
  margin: 0 auto;
}

.desktop-nav {
  padding: clamp(0.85rem, 1.1vw, 1rem);
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.brand {
  font-size: clamp(1.2rem, 1.5vw, 1.6rem);
  margin: 0 0 0.5rem 0;
  letter-spacing: 0.01em;
}

.main-panel {
  min-width: 0;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.9rem;
  padding: clamp(0.85rem, 1.1vw, 1rem);
  gap: 0.75rem;
  flex-wrap: wrap;
}

.top-bar h2 {
  margin: 4px 0 0 0;
  font-size: clamp(1.1rem, 1.3vw, 1.45rem);
}

.top-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.role-chip {
  font-size: 0.75rem;
  border: 1px solid rgba(36, 107, 255, 0.25);
  border-radius: 999px;
  color: var(--accent);
  padding: 0.2rem 0.55rem;
  background: rgba(36, 107, 255, 0.08);
}

.user-chip {
  color: var(--text-muted);
  font-size: 0.85rem;
  white-space: nowrap;
}

.content {
  padding-bottom: calc(78px + env(safe-area-inset-bottom));
}

.nav-link,
.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--text-main);
  font-weight: 500;
  border-radius: 14px;
  transition: all 0.22s ease;
  min-width: 0;
}

.nav-link {
  padding: 0.72rem 0.74rem;
}

.nav-link:hover,
.tab-item:hover {
  background: rgba(255, 255, 255, 0.75);
}

.active {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 18px rgba(32, 40, 53, 0.08);
}

.dot {
  font-size: 0.8rem;
}

.nav-icon {
  width: 1.15rem;
  height: 1.15rem;
  flex: 0 0 auto;
}

.nav-icon svg {
  width: 100%;
  height: 100%;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.nav-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-tab {
  position: fixed;
  left: 14px;
  right: 14px;
  bottom: calc(10px + env(safe-area-inset-bottom));
  padding: 8px;
  display: none;
  grid-template-columns: repeat(auto-fit, minmax(0, 1fr));
  gap: 6px;
  z-index: 10;
}

.tab-item {
  flex-direction: column;
  justify-content: center;
  padding: 8px 0;
  font-size: 0.8rem;
}

.avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 0.7rem;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 18px rgba(34, 56, 101, 0.2);
  flex: 0 0 auto;
}

.muted {
  color: var(--text-muted);
  font-size: 0.8rem;
}

@media (max-width: 900px) {
  .app-shell {
    grid-template-columns: 1fr;
    padding: 0.75rem;
  }

  .desktop-nav {
    display: none;
  }

  .top-bar {
    position: sticky;
    top: 0.6rem;
    z-index: 8;
  }

  .mobile-tab {
    display: grid;
  }

  .user-chip {
    display: none;
  }
}
</style>
