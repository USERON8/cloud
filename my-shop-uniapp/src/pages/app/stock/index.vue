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
    toast('ÇëÊäÈëÓÐÐ§ SKU ID')
    return
  }
  try {
    ledger.value = await getStockLedger(id)
  } catch (error) {
    toast(error instanceof Error ? error.message : '²éÑ¯Ê§°Ü')
  }
}
</script>

<template>
  <AppShell title="Stock Ledger">
    <view class="panel glass-card">
      <text class="section-title">¿â´æÌ¨ÕË</text>
      <view class="search-row">
        <input v-model="skuId" class="search-input" placeholder="SKU ID" />
        <button class="btn-primary" @click="queryLedger">²éÑ¯</button>
      </view>

      <view v-if="ledger" class="result">
        <text class="meta">¿ÉÓÃ¿â´æ£º{{ ledger.salableQty ?? '--' }}</text>
        <text class="meta">ÔÚ¿â¿â´æ£º{{ ledger.onHandQty ?? '--' }}</text>
        <text class="meta">Ëø¶¨¿â´æ£º{{ ledger.reservedQty ?? '--' }}</text>
        <text class="meta">Ô¤¾¯ãÐÖµ£º{{ ledger.alertThreshold ?? '--' }}</text>
        <text class="meta">×´Ì¬£º{{ ledger.status ?? '--' }}</text>
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
