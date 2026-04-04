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
const loading = ref(false)

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
  loading.value = true
  try {
    ledger.value = await getStockLedger(id)
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to query the stock ledger')
  } finally {
    loading.value = false
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
    <view class="page-wrap">
      <!-- Hero -->
      <view class="hero surface-card fade-in-up">
        <view class="hero-left">
          <text class="hero-eyebrow">ADMIN</text>
          <text class="hero-title">Stock Ledger</text>
          <text class="hero-subtitle">Query real-time inventory status by SKU ID.</text>
        </view>
        <view class="hero-stats">
          <view class="info-card">
            <text class="info-label">Available</text>
            <text class="info-value">{{ ledger?.availableQty ?? '--' }}</text>
          </view>
          <view class="info-card">
            <text class="info-label">Locked</text>
            <text class="info-value">{{ ledger?.lockedQty ?? '--' }}</text>
          </view>
          <view class="info-card">
            <text class="info-label">Sold</text>
            <text class="info-value">{{ ledger?.soldQty ?? '--' }}</text>
          </view>
        </view>
      </view>

      <!-- Query bar -->
      <view class="toolbar surface-card fade-in-up">
        <input
          v-model="skuId"
          class="std-input"
          placeholder="Enter SKU ID"
          type="number"
          @confirm="queryLedger"
        />
        <button class="btn-primary" :loading="loading" @click="queryLedger">Query</button>
      </view>

      <!-- Result card -->
      <view v-if="ledger" class="result surface-card fade-in-up">
        <text class="result-title">Ledger Detail</text>
        <view class="result-grid">
          <view class="result-item">
            <text class="result-label">SKU ID</text>
            <text class="result-value">{{ ledger.skuId ?? '--' }}</text>
          </view>
          <view class="result-item">
            <text class="result-label">Available Qty</text>
            <text class="result-value">{{ ledger.availableQty ?? '--' }}</text>
          </view>
          <view class="result-item">
            <text class="result-label">Locked Qty</text>
            <text class="result-value">{{ ledger.lockedQty ?? '--' }}</text>
          </view>
          <view class="result-item">
            <text class="result-label">Sold Qty</text>
            <text class="result-value">{{ ledger.soldQty ?? '--' }}</text>
          </view>
          <view class="result-item">
            <text class="result-label">Segment Count</text>
            <text class="result-value">{{ ledger.segmentCount ?? '--' }}</text>
          </view>
          <view class="result-item">
            <text class="result-label">Alert Threshold</text>
            <text class="result-value">{{ ledger.alertThreshold ?? '--' }}</text>
          </view>
          <view class="result-item">
            <text class="result-label">Status</text>
            <text class="result-value">{{ ledger.status ?? '--' }}</text>
          </view>
          <view class="result-item">
            <text class="result-label">Updated At</text>
            <text class="result-value">{{ ledger.updatedAt ?? '--' }}</text>
          </view>
        </view>
      </view>

      <view v-else-if="!loading" class="empty-state">
        <text class="empty-state-text">Enter a SKU ID and tap Query to view stock details</text>
      </view>
    </view>
  </AppShell>
</template>

<style scoped>
.page-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
  max-width: 960px;
  margin: 0 auto;
}

.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 32px 36px;
  border-radius: var(--radius-lg);
  flex-wrap: wrap;
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--accent);
  text-transform: uppercase;
}

.hero-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
  letter-spacing: -0.02em;
}

.hero-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.5;
}

.hero-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: var(--radius-md);
}

.std-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(20, 20, 20, 0.12);
  border-radius: 16px;
  padding: 13px 16px;
  font-size: 14px;
  color: var(--text-main);
}

.std-input:focus {
  border-color: rgba(11, 107, 95, 0.4);
  box-shadow: 0 0 0 3px rgba(11, 107, 95, 0.12);
}

.result {
  padding: 24px 28px;
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.result-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 16px;
  background: var(--panel-muted);
  border: 1px solid rgba(20, 20, 20, 0.08);
}



.result-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.result-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
  letter-spacing: -0.01em;
}

@media (hover: hover) {
  .hero:hover,
  .toolbar:hover,
  .result:hover {
    box-shadow: 0 16px 30px rgba(20, 20, 20, 0.12);
    border-color: rgba(20, 20, 20, 0.12);
    transform: translateY(-1px);
  }
}


@media (max-width: 600px) {
  .page-wrap { padding: 16px; }
  .hero { padding: 24px 20px; }
  .result-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
