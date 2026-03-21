<script setup lang="ts">
import { navigateTo } from '../../router/navigation'
import { Routes } from '../../router/routes'

function backToMarket(): void {
  navigateTo(Routes.market)
}
</script>

<template>
  <view class="page">
    <view class="card glass-card">
      <text class="title">Access denied</text>
      <text class="muted">The current account does not have permission to open this page.</text>
      <button class="btn-primary" @click="backToMarket">Back to the market</button>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.card {
  padding: 24px;
  width: min(420px, 100%);
  display: flex;
  flex-direction: column;
  gap: 12px;
  text-align: center;
}

.title {
  font-size: 20px;
  font-weight: 600;
}

.muted {
  color: var(--text-muted);
  font-size: 12px;
}
</style>
