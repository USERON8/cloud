<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { getStockLedger } from '../../../api/stock'
import { useRole } from '../../../auth/permission'
import { redirectTo } from '../../../router/navigation'
import { Routes } from '../../../router/routes'
import type { StockLedger } from '../../../types/domain'
import { toast } from '../../../utils/ui'

const { isAdmin } = useRole()

const skuId = ref('')
const ledger = ref<StockLedger | null>(null)

function ensureAdminAccess(): boolean {
  if (isAdmin.value) {
    return true
  }
  toast('Administrator access is required')
  redirectTo(Routes.forbidden)
  return false
}

async function queryLedger(): Promise<void> {
  if (!ensureAdminAccess()) {
    return
  }
  const id = Number(skuId.value)
  if (!Number.isFinite(id) || id <= 0) {
    toast('Enter a valid SKU ID')
    return
  }
  try {
    ledger.value = await getStockLedger(id)
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to query the stock ledger')
  }
}

onShow(() => {
  void Promise.resolve().then(() => {
    ensureAdminAccess()
  })
})
</script>

<template>
  <AppShell title="Stock Ledger">
    <view class="panel glass-card">
      <text class="section-title">Stock overview</text>
      <view class="search-row">
        <input v-model="skuId" class="search-input" placeholder="SKU ID" />
        <button class="btn-primary" @click="queryLedger">Search</button>
      </view>

      <view v-if="ledger" class="result">
        <text class="meta">Salable quantity: {{ ledger.salableQty ?? '--' }}</text>
        <text class="meta">On-hand quantity: {{ ledger.onHandQty ?? '--' }}</text>
        <text class="meta">Reserved quantity: {{ ledger.reservedQty ?? '--' }}</text>
        <text class="meta">Alert threshold: {{ ledger.alertThreshold ?? '--' }}</text>
        <text class="meta">Status: {{ ledger.status ?? '--' }}</text>
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

.search-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-input {
  flex: 1;
  background: #fff;
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 14px;
}

.result {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
