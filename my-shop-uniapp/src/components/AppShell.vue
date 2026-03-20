<script setup lang="ts">
import { computed } from 'vue'
import { logout } from '../api/auth'
import { useRole, type UserRole } from '../auth/permission'
import { clearSession, sessionState } from '../auth/session'
import { cartCount } from '../store/cart'
import { currentRoutePath, navigateTo, redirectTo } from '../router/navigation'
import { Routes, type RoutePath } from '../router/routes'

defineProps<{ title?: string }>()

interface NavItem {
  label: string
  path: RoutePath
  roles: UserRole[]
  public?: boolean
  showBadge?: boolean
}

const { role } = useRole()

const navItems: NavItem[] = [
  { label: 'Dashboard', path: Routes.appHome, roles: ['USER', 'MERCHANT', 'ADMIN'] },
  { label: 'Marketplace', path: Routes.market, roles: ['USER', 'MERCHANT', 'ADMIN'], public: true },
  { label: 'Products', path: Routes.appCatalog, roles: ['USER', 'MERCHANT', 'ADMIN'] },
  { label: 'Product Admin', path: Routes.appCatalogManage, roles: ['MERCHANT', 'ADMIN'] },
  { label: 'Orders', path: Routes.appOrders, roles: ['USER', 'MERCHANT', 'ADMIN'] },
  { label: 'Order Admin', path: Routes.appOrdersManage, roles: ['MERCHANT', 'ADMIN'] },
  { label: 'Payments', path: Routes.appPayments, roles: ['USER', 'MERCHANT', 'ADMIN'] },
  { label: 'Stock Ledger', path: Routes.appStock, roles: ['ADMIN'] },
  { label: 'Cart', path: Routes.appCart, roles: ['USER', 'MERCHANT', 'ADMIN'], showBadge: true },
  { label: 'Addresses', path: Routes.appAddresses, roles: ['USER', 'MERCHANT', 'ADMIN'] },
  { label: 'Merchant Center', path: Routes.appMerchant, roles: ['MERCHANT'] },
  { label: 'Admin Center', path: Routes.appAdmin, roles: ['ADMIN'] },
  { label: 'Ops Center', path: Routes.appOps, roles: ['MERCHANT', 'ADMIN'] },
  { label: 'Profile', path: Routes.appProfile, roles: ['USER', 'MERCHANT', 'ADMIN'] }
]

const visibleNavItems = computed(() =>
  navItems.filter((item) => item.public || item.roles.includes(role.value))
)

const displayName = computed(() => sessionState.user?.nickname || sessionState.user?.username || 'Current User')
const roleLabel = computed(() => role.value)

function isActive(path: string): boolean {
  const current = currentRoutePath()
  if (!current) {
    return false
  }
  return current === path.replace(/^\//, '') || current === path
}

function handleNav(item: NavItem): void {
  const guard = item.public ? undefined : { requiresAuth: true, roles: item.roles }
  navigateTo(item.path, undefined, guard)
}

async function handleLogout(): Promise<void> {
  try {
    await logout()
  } catch {
    // ignore
  } finally {
    clearSession()
    uni.showToast({ title: '已退出', icon: 'success' })
    redirectTo(Routes.login)
  }
}
</script>

<template>
  <view class="app-shell">
    <view class="top-bar glass-card">
      <view>
        <text class="brand">My Shop</text>
        <text class="title">{{ title || 'Dashboard' }}</text>
      </view>
      <view class="user-meta">
        <view class="role-chip">{{ roleLabel }}</view>
        <text class="user-name">{{ displayName }}</text>
        <button class="btn-outline" @click="handleLogout">退出登录</button>
      </view>
    </view>

    <scroll-view class="nav-row glass-card" scroll-x>
      <view class="nav-items">
        <view
          v-for="item in visibleNavItems"
          :key="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          @click="handleNav(item)"
        >
          <text>{{ item.label }}</text>
          <text v-if="item.showBadge && cartCount > 0" class="badge">{{ cartCount }}</text>
        </view>
      </view>
    </scroll-view>

    <view class="content">
      <slot />
    </view>
  </view>
</template>

<style scoped>
.app-shell {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.top-bar {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.brand {
  display: block;
  font-size: 14px;
  color: var(--accent);
  font-weight: 600;
}

.title {
  display: block;
  font-size: 18px;
  font-weight: 600;
  margin-top: 4px;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.role-chip {
  font-size: 12px;
  border: 1px solid rgba(36, 107, 255, 0.25);
  border-radius: 999px;
  color: var(--accent);
  padding: 4px 10px;
  background: rgba(36, 107, 255, 0.08);
}

.user-name {
  color: var(--text-muted);
  font-size: 12px;
}

.nav-row {
  padding: 8px 12px;
}

.nav-items {
  display: flex;
  gap: 8px;
}

.nav-item {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 18px rgba(32, 40, 53, 0.08);
}

.badge {
  background: #e53e3e;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 999px;
}

.content {
  padding-bottom: 18px;
}

button {
  margin: 0;
  line-height: 1;
}
</style>
