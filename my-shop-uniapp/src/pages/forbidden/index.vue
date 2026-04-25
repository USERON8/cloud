<script setup lang="ts">
import { navigateTo } from '../../router/navigation'
import { Routes } from '../../router/routes'

function backToMarket(): void {
  navigateTo(Routes.market)
}
</script>

<template>
  <view class="page">
    <view class="error-card glass-card">
      <view class="status-mark">
        <text class="status-code">403</text>
      </view>
      <text class="hero-eyebrow">Access denied</text>
      <text class="title">This account cannot open the requested page.</text>
      <text class="description">Use an account with the required role, or return to the storefront.</text>
      <view class="actions">
        <button class="btn-primary" @click="backToMarket">Back to market</button>
      </view>
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

.error-card {
  padding: 24px;
  width: min(520px, 100%);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.status-mark {
  width: 64px;
  height: 64px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--highlight), var(--accent));
  color: #07131f;
  box-shadow: 0 18px 34px rgba(1, 7, 14, 0.28);
}

.status-code {
  font-size: 18px;
  font-weight: 800;
}

.title {
  font-size: 30px;
  line-height: 1.12;
  font-weight: 800;
}

.description {
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.7;
}

.actions {
  display: flex;
  width: 100%;
  padding-top: 4px;
}

.actions button {
  flex: 1;
}

@media (max-width: 520px) {
  .error-card {
    padding: 22px;
  }

  .title {
    font-size: 24px;
  }
}
</style>
