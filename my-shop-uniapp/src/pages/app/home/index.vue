<script setup lang="ts">
import { computed } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { sessionState } from '../../../auth/session'
import { navigateTo } from '../../../router/navigation'
import { Routes } from '../../../router/routes'

const displayName = computed(
  () => sessionState.user?.nickname || sessionState.user?.username || 'Current User'
)
const roleLabel = computed(() => (sessionState.user?.roles || []).join(', ') || 'USER')
</script>

<template>
  <AppShell title="Dashboard">
    <view class="card glass-card">
      <text class="section-title">Welcome back</text>
      <text class="muted">{{ displayName }}</text>
      <text class="muted">Role: {{ roleLabel }}</text>
    </view>

    <view class="quick-grid">
      <button class="btn-primary" @click="navigateTo(Routes.appCatalog, undefined, { requiresAuth: true })">
        Browse products
      </button>
      <button class="btn-outline" @click="navigateTo(Routes.appOrders, undefined, { requiresAuth: true })">
        My orders
      </button>
      <button class="btn-outline" @click="navigateTo(Routes.appCart, undefined, { requiresAuth: true })">
        Shopping cart
      </button>
      <button class="btn-outline" @click="navigateTo(Routes.appProfile, undefined, { requiresAuth: true })">
        Profile
      </button>
    </view>
  </AppShell>
</template>

<style scoped>
.card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.muted {
  color: var(--text-muted);
  font-size: 12px;
}

.quick-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
</style>
