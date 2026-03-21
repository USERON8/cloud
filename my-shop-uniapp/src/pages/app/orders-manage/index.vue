<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { completeOrder, listOrders, shipOrder } from '../../../api/order'
import type { OrderItem } from '../../../types/domain'
import { formatDate, formatPrice } from '../../../utils/format'
import { confirm, toast } from '../../../utils/ui'

const rows = ref<OrderItem[]>([])
const loading = ref(false)
const shippingForm = reactive({
  shippingCompany: '',
  trackingNumber: ''
})

function statusText(status?: number): string {
  if (status === 0) return 'Pending payment'
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

function requireShippingField(value: string, label: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) {
    toast(`${label} is required`)
    return null
  }
  return trimmed
}

async function onShip(order: OrderItem): Promise<void> {
  if (typeof order.id !== 'number') return
  const shippingCompany = requireShippingField(shippingForm.shippingCompany, 'Shipping company')
  if (!shippingCompany) return
  const trackingNumber = requireShippingField(shippingForm.trackingNumber, 'Tracking number')
  if (!trackingNumber) return
  const ok = await confirm(`Confirm shipment for order ${order.orderNo}?`)
  if (!ok) return
  try {
    await shipOrder(order.id, shippingCompany, trackingNumber)
    toast('Order shipped', 'success')
    await loadOrders()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to ship order')
  }
}

async function onComplete(order: OrderItem): Promise<void> {
  if (typeof order.id !== 'number') return
  const ok = await confirm(`Confirm completion for order ${order.orderNo}?`)
  if (!ok) return
  try {
    await completeOrder(order.id)
    toast('Order completed', 'success')
    await loadOrders()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to complete order')
  }
}

onMounted(() => {
  void loadOrders()
})
</script>

<template>
  <AppShell title="Order Admin">
    <view class="panel glass-card">
      <view class="header">
        <text class="section-title">Order Management</text>
        <button class="btn-outline" @click="loadOrders">Refresh</button>
      </view>

      <view class="shipping-bar">
        <input v-model="shippingForm.shippingCompany" class="input" placeholder="Shipping company" />
        <input v-model="shippingForm.trackingNumber" class="input" placeholder="Tracking number" />
      </view>

      <view v-if="rows.length === 0" class="empty">
        <text class="text-muted">No orders</text>
      </view>

      <view v-else class="list">
        <view v-for="item in rows" :key="item.id" class="row">
          <view class="info">
            <text class="name">Order No: {{ item.orderNo }}</text>
            <text class="meta">Amount: {{ formatPrice(item.payAmount ?? item.totalAmount) }}</text>
            <text class="meta">Status: {{ statusText(item.status) }}</text>
            <text class="meta">Created At: {{ formatDate(item.createdAt) }}</text>
          </view>
          <view class="actions">
            <button class="btn-outline" @click="onShip(item)">Ship</button>
            <button class="btn-outline" @click="onComplete(item)">Complete</button>
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

.shipping-bar {
  display: flex;
  flex-wrap: wrap;
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

.input {
  background: #fff;
  border-radius: 10px;
  padding: 6px 10px;
  font-size: 12px;
}

.empty {
  padding: 16px 0;
  text-align: center;
}
</style>
