<script setup lang="ts">
import { computed } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { sessionState } from '../../../auth/session'

const user = computed(() => sessionState.user)
</script>

<template>
  <AppShell title="Profile">
    <view class="panel glass-card">
      <text class="section-title">Profile details</text>
      <view class="row">
        <text class="label">Username</text>
        <text class="value">{{ user?.username || '--' }}</text>
      </view>
      <view class="row">
        <text class="label">Display name</text>
        <text class="value">{{ user?.nickname || '--' }}</text>
      </view>
      <view class="row">
        <text class="label">Email</text>
        <text class="value">{{ user?.email || '--' }}</text>
      </view>
      <view class="row">
        <text class="label">Roles</text>
        <text class="value">{{ (user?.roles || []).join(', ') || '--' }}</text>
      </view>
    </view>
  </AppShell>
</template>

<style scoped>
.panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.label {
  color: var(--text-muted);
  font-size: 12px;
}

.value {
  font-size: 12px;
  font-weight: 600;
}
</style>
