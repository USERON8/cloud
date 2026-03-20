<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { cancelOrder, listOrders } from '../../../api/order'
import type { OrderItem } from '../../../types/domain'
import { formatDate, formatPrice } from '../../../utils/format'
import { confirm, toast } from '../../../utils/ui'

const rows = ref<OrderItem[]>([])
const loading = ref(false)

function statusText(status?: number): string {
  if (status === 0) return 'Awaiting payment'
  if (status === 1) return 'Paid'
  if (status === 2) return 'Shipped'
  if (status === 3) return 'Completed'
  if (status === 4) return 'Cancelled'
  return status != null ? String(status) : 'Unknown'
}

async function loadOrders(): Promise<void> {
  if (loading.value) return
  loading.value = true
  try {
    const result = await listOrders({ page: 1, size: 30 })
    rows.value = result.records
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to load orders')
  } finally {
    loading.value = false
  }
}

function onPay(order: OrderItem): void {
  toast(
    `Direct order payment is disabled for ${order.orderNo}. Complete payment through the payment service flow.`
  )
}

async function onCancel(order: OrderItem): Promise<void> {
  if (typeof order.id !== 'number') return
  const ok = await confirm(`Cancel order ${order.orderNo}?`)
  if (!ok) return
  try {
    await cancelOrder(order.id)
    toast('Order cancelled', 'success')
    await loadOrders()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to cancel order')
  }
}

onMounted(() => {
  void loadOrders()
})
</script>

<template>
  <AppShell title="Orders">
    <view class="panel glass-card">
      <view class="header">
        <text class="section-title">My Orders</text>
        <button class="btn-outline" @click="loadOrders">Refresh</button>
      </view>

      <view v-if="rows.length === 0" class="empty">
        <text class="text-muted">No orders yet</text>
      </view>

      <view v-else class="list">
        <view v-for="item in rows" :key="item.id" class="row">
          <view class="info">
            <text class="name">Order: {{ item.orderNo }}</text>
            <text class="meta">Amount: {{ formatPrice(item.payAmount ?? item.totalAmount) }}</text>
            <text class="meta">Status: {{ statusText(item.status) }}</text>
            <text class="meta">Created: {{ formatDate(item.createdAt) }}</text>
          </view>
          <view class="actions">
            <button v-if="item.status === 0" class="btn-outline" @click="onPay(item)">
              Payment Info
            </button>
            <button class="btn-outline" @click="onCancel(item)">Cancel</button>
          </view>
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
  gap: 12px;
}

.row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.name {
  font-size: 14px;
  font-weight: 600;
}

.meta {
  font-size: 12px;
  color: var(--text-muted);
}

.actions {
  display: flex;
  gap: 8px;
}

.empty {
  padding: 16px 0;
  text-align: center;
}
</style>
