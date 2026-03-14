<script setup lang="ts">
import { computed } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { sessionState } from '../../../auth/session'
import { navigateTo } from '../../../router/navigation'
import { Routes } from '../../../router/routes'

const displayName = computed(() => sessionState.user?.nickname || sessionState.user?.username || '当前用户')
const roleLabel = computed(() => (sessionState.user?.roles || []).join(', ') || 'USER')
</script>

<template>
  <AppShell title="Dashboard">
    <view class="card glass-card">
      <text class="section-title">欢迎回来</text>
      <text class="muted">{{ displayName }}</text>
      <text class="muted">角色：{{ roleLabel }}</text>
    </view>

    <view class="quick-grid">
      <button class="btn-primary" @click="navigateTo(Routes.appCatalog, undefined, { requiresAuth: true })">
        商品列表
      </button>
      <button class="btn-outline" @click="navigateTo(Routes.appOrders, undefined, { requiresAuth: true })">
        我的订单
      </button>
      <button class="btn-outline" @click="navigateTo(Routes.appCart, undefined, { requiresAuth: true })">
        购物车
      </button>
      <button class="btn-outline" @click="navigateTo(Routes.appProfile, undefined, { requiresAuth: true })">
        个人信息
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
