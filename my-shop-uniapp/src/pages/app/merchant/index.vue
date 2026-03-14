<script setup lang="ts">
import AppShell from '../../../components/AppShell.vue'
</script>

<template>
  <AppShell title="Merchant Center">
    <view class="panel glass-card">
      <text class="section-title">商家中心</text>
      <text class="muted">功能已完成基础迁移，后续模块可在此扩展。</text>
    </view>
  </AppShell>
</template>

<style scoped>
.panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.muted {
  color: var(--text-muted);
  font-size: 12px;
}
</style>
