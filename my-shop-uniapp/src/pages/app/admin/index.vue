<script setup lang="ts">
import AppShell from '../../../components/AppShell.vue'
</script>

<template>
  <AppShell title="Admin Center">
    <view class="panel glass-card">
      <text class="section-title">管理中心</text>
      <text class="muted">管理功能已迁移基础框架，后续可接入用户/商家管理。</text>
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
