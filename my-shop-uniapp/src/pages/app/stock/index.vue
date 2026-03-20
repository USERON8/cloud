<script setup lang="ts">
import { ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { getStockLedger } from '../../../api/stock'
import type { StockLedger } from '../../../types/domain'
import { toast } from '../../../utils/ui'

const skuId = ref('')
const ledger = ref<StockLedger | null>(null)

async function queryLedger(): Promise<void> {
  const id = Number(skuId.value)
  if (!Number.isFinite(id) || id <= 0) {
    toast('请输入有效 SKU ID')
    return
  }
  try {
    ledger.value = await getStockLedger(id)
  } catch (error) {
    toast(error instanceof Error ? error.message : '查询失败')
  }
}
</script>

<template>
  <AppShell title="Stock Ledger">
    <view class="panel glass-card">
      <text class="section-title">库存台账</text>
      <view class="search-row">
        <input v-model="skuId" class="search-input" placeholder="SKU ID" />
        <button class="btn-primary" @click="queryLedger">查询</button>
      </view>

      <view v-if="ledger" class="result">
        <text class="meta">可用库存：{{ ledger.salableQty ?? '--' }}</text>
        <text class="meta">在库库存：{{ ledger.onHandQty ?? '--' }}</text>
        <text class="meta">锁定库存：{{ ledger.reservedQty ?? '--' }}</text>
        <text class="meta">预警阈值：{{ ledger.alertThreshold ?? '--' }}</text>
        <text class="meta">状态：{{ ledger.status ?? '--' }}</text>
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
