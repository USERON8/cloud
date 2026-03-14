<script setup lang="ts">
import AppShell from '../../../components/AppShell.vue'
</script>

<template>
  <AppShell title="Ops Center">
    <view class="panel glass-card">
      <text class="section-title">运营中心</text>
      <text class="muted">运营模块已完成基础迁移，后续可接入内容与数据看板。</text>
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
