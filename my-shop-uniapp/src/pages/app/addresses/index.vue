<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { listUserAddresses } from '../../../api/address'
import type { UserAddress } from '../../../types/domain'
import { sessionState } from '../../../auth/session'
import { toast } from '../../../utils/ui'

const rows = ref<UserAddress[]>([])
const loading = ref(false)
const userId = computed(() => sessionState.user?.id)

async function loadAddresses(): Promise<void> {
  if (loading.value) return
  if (typeof userId.value !== 'number') {
    toast('Missing user session')
    return
  }
  loading.value = true
  try {
    rows.value = await listUserAddresses(userId.value)
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to load addresses')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadAddresses()
})
</script>

<template>
  <AppShell title="Addresses">
    <view class="panel glass-card">
      <view class="header">
        <text class="section-title">Address book</text>
        <button class="btn-outline" @click="loadAddresses">Refresh</button>
      </view>

      <view v-if="rows.length === 0" class="empty">
        <text class="text-muted">No address is available</text>
      </view>

      <view v-else class="list">
        <view v-for="item in rows" :key="item.id" class="row">
          <text class="name">{{ item.consignee }} · {{ item.phone }}</text>
          <text class="meta">
            {{ item.province }} {{ item.city }} {{ item.district }} {{ item.street }} {{ item.detailAddress }}
          </text>
          <text class="meta" v-if="item.isDefault === 1">Default address</text>
        </view>
      </view>
    </view>
  </AppShell>
</template>

<style scoped>
.panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.name {
  font-size: 14px;
  font-weight: 600;
}

.meta {
  font-size: 12px;
  color: var(--text-muted);
}

.empty {
  padding: 16px 0;
  text-align: center;
}
</style>
