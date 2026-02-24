<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logout } from '../api/auth'
import { clearSession, sessionState } from '../auth/session'
import { useRole, type UserRole } from '../auth/permission'

interface NavItem {
  label: string
  path: string
  icon: string
  roles: UserRole[]
}

const route = useRoute()
const router = useRouter()
const { role } = useRole()

const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/home', icon: 'D', roles: ['USER', 'MERCHANT', 'ADMIN'] },
  { label: 'Products', path: '/catalog', icon: 'P', roles: ['USER', 'MERCHANT', 'ADMIN'] },
  { label: 'Product Admin', path: '/catalog/manage', icon: 'M', roles: ['MERCHANT', 'ADMIN'] },
  { label: 'Orders', path: '/orders', icon: 'O', roles: ['USER', 'MERCHANT', 'ADMIN'] },
  { label: 'Order Admin', path: '/orders/manage', icon: 'R', roles: ['MERCHANT', 'ADMIN'] },
  { label: 'Profile', path: '/profile', icon: 'U', roles: ['USER', 'MERCHANT', 'ADMIN'] }
]

const visibleNavItems = computed(() => navItems.filter((item) => item.roles.includes(role.value)))
const currentTitle = computed(() => String(route.meta.title || 'My Shop'))
const displayName = computed(() => sessionState.user?.nickname || sessionState.user?.username || 'Current User')
const roleLabel = computed(() => role.value)

function isNavActive(path: string): boolean {
  if (path === '/catalog') {
    return route.path === '/catalog'
  }
  if (path === '/catalog/manage') {
    return route.path === '/catalog/manage'
  }
  if (path === '/orders') {
    return route.path === '/orders'
  }
  if (path === '/orders/manage') {
    return route.path === '/orders/manage'
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
        <span class="dot">{{ item.icon }}</span>
        {{ item.label }}
      </router-link>
    </aside>

    <main class="main-panel">
      <header class="top-bar glass-card">
        <div>
          <div class="muted">Apple-like mobile-first UI</div>
          <h2>{{ currentTitle }}</h2>
        </div>

        <div class="top-actions">
          <span class="role-chip">{{ roleLabel }}</span>
          <span class="user-chip">{{ displayName }}</span>
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
        <span class="dot">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 18px;
  padding: 18px;
}

.desktop-nav {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.brand {
  font-size: 1.5rem;
  margin: 0 0 8px 0;
}

.main-panel {
  min-width: 0;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding: 16px;
  gap: 12px;
}

.top-bar h2 {
  margin: 4px 0 0 0;
  font-size: 1.3rem;
}

.top-actions {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.role-chip {
  font-size: 0.74rem;
  border: 1px solid rgba(36, 107, 255, 0.25);
  border-radius: 999px;
  color: var(--accent);
  padding: 2px 8px;
  background: rgba(36, 107, 255, 0.08);
}

.user-chip {
  color: var(--text-muted);
  font-size: 0.84rem;
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
}

.nav-link {
  padding: 12px;
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

.muted {
  color: var(--text-muted);
  font-size: 0.8rem;
}

@media (max-width: 900px) {
  .app-shell {
    grid-template-columns: 1fr;
    padding: 12px;
  }

  .desktop-nav {
    display: none;
  }

  .top-bar {
    position: sticky;
    top: 10px;
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
