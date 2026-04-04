<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { advanceAfterSaleStatus, completeOrder, listOrders, shipOrder } from '../../../api/order'
import type { OrderItem } from '../../../types/domain'
import { formatDate, formatOrderStatus, formatPrice, formatRelativeDate } from '../../../utils/format'
import { confirm, toast } from '../../../utils/ui'

const rows = ref<OrderItem[]>([])
const loading = ref(false)
const afterSaleActingOrderId = ref<number | null>(null)
const shippingForm = reactive({
  shippingCompany: '',
  trackingNumber: ''
})
const afterSaleForm = reactive({
  remark: ''
})

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

function canAuditAfterSale(order: OrderItem): boolean {
  return typeof order.afterSaleId === 'number' && order.afterSaleStatus === 'APPLIED'
}

function canApproveAfterSale(order: OrderItem): boolean {
  return typeof order.afterSaleId === 'number' && order.afterSaleStatus === 'AUDITING'
}

function canRejectAfterSale(order: OrderItem): boolean {
  return typeof order.afterSaleId === 'number' && ['APPLIED', 'AUDITING'].includes(order.afterSaleStatus ?? '')
}

function canWaitReturn(order: OrderItem): boolean {
  return (
    typeof order.afterSaleId === 'number' &&
    order.afterSaleStatus === 'APPROVED' &&
    order.afterSaleType === 'RETURN_REFUND'
  )
}

function canProcessRefund(order: OrderItem): boolean {
  return (
    typeof order.afterSaleId === 'number' &&
    ((order.afterSaleStatus === 'APPROVED' && order.afterSaleType === 'REFUND') ||
      order.afterSaleStatus === 'RECEIVED')
  )
}

function canMarkReturned(order: OrderItem): boolean {
  return typeof order.afterSaleId === 'number' && order.afterSaleStatus === 'WAIT_RETURN'
}

function canMarkReceived(order: OrderItem): boolean {
  return typeof order.afterSaleId === 'number' && order.afterSaleStatus === 'RETURNED'
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

async function onAdvanceAfterSale(
  order: OrderItem,
  action: 'AUDIT' | 'APPROVE' | 'REJECT' | 'WAIT_RETURN' | 'RETURN' | 'RECEIVE' | 'PROCESS'
): Promise<void> {
  if (typeof order.afterSaleId !== 'number' || typeof order.id !== 'number') {
    toast('This order is missing after-sale metadata')
    return
  }
  const labels: Record<typeof action, string> = {
    AUDIT: 'start review',
    APPROVE: 'approve',
    REJECT: 'reject',
    WAIT_RETURN: 'wait for return',
    RETURN: 'mark returned',
    RECEIVE: 'mark received',
    PROCESS: 'start refund'
  }
  const ok = await confirm(`Confirm ${labels[action]} for ${order.afterSaleNo ?? order.afterSaleId}?`)
  if (!ok) {
    return
  }
  afterSaleActingOrderId.value = order.id
  try {
    const remark = afterSaleForm.remark.trim() || undefined
    await advanceAfterSaleStatus(order.afterSaleId, action, remark)
    toast(`After-sale ${labels[action]} completed`, 'success')
    await loadOrders()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to update the after-sale request')
  } finally {
    afterSaleActingOrderId.value = null
  }
}

onMounted(() => {
  void loadOrders()
})
</script>

<template>
  <AppShell title="Order Admin">
    <view class="page-wrap">
      <!-- Hero -->
      <view class="hero surface-card fade-in-up">
        <view class="hero-left">
          <text class="hero-eyebrow">MERCHANT</text>
          <text class="hero-title">Order Management</text>
          <text class="hero-subtitle">Ship orders and handle after-sale requests.</text>
        </view>
        <view class="hero-stats">
          <view class="info-card">
            <text class="info-label">Total</text>
            <text class="info-value">{{ rows.length }}</text>
          </view>
        </view>
      </view>

      <!-- Shipping fields (shared across rows) -->
      <view class="toolbar surface-card fade-in-up">
        <view class="field-group">
          <text class="field-label">Shipping company</text>
          <input v-model="shippingForm.shippingCompany" class="std-input" placeholder="e.g. FedEx" />
        </view>
        <view class="field-group">
          <text class="field-label">Tracking number</text>
          <input v-model="shippingForm.trackingNumber" class="std-input" placeholder="Enter tracking no." />
        </view>
        <view class="field-group">
          <text class="field-label">After-sale remark</text>
          <input v-model="afterSaleForm.remark" class="std-input" placeholder="Optional remark" />
        </view>
        <button class="btn-outline" :loading="loading" @click="loadOrders">Refresh</button>
      </view>

      <!-- Empty -->
      <view v-if="rows.length === 0" class="empty-state">
        <text class="empty-state-text">No orders found</text>
      </view>

      <!-- Order list -->
      <view v-else class="list fade-in-up">
        <view v-for="item in rows" :key="item.id" class="row surface-card">
          <view class="row-info">
            <text class="row-name">{{ item.orderNo }}</text>
            <text class="row-meta">{{ formatPrice(item.payAmount ?? item.totalAmount) }}</text>
            <view class="row-meta-row">
              <view class="chip chip-muted"><text>{{ formatOrderStatus(item.status) }}</text></view>
              <text class="row-date">{{ formatDate(item.createdAt) }} | {{ formatRelativeDate(item.createdAt) }}</text>
            </view>
            <text v-if="item.afterSaleStatus && item.afterSaleStatus !== 'NONE'" class="row-aftersale">
              After-sale: {{ item.afterSaleStatus }}{{ item.afterSaleNo ? ` (${item.afterSaleNo})` : '' }}
            </text>
          </view>
          <view class="row-actions">
            <button class="btn-outline" @click="onShip(item)">Ship</button>
            <button class="btn-outline" @click="onComplete(item)">Complete</button>
            <button
              v-if="canAuditAfterSale(item)"
              class="btn-outline"
              :loading="afterSaleActingOrderId === item.id"
              @click="onAdvanceAfterSale(item, 'AUDIT')"
            >Start review</button>
            <button
              v-if="canApproveAfterSale(item)"
              class="btn-outline"
              :loading="afterSaleActingOrderId === item.id"
              @click="onAdvanceAfterSale(item, 'APPROVE')"
            >Approve</button>
            <button
              v-if="canRejectAfterSale(item)"
              class="btn-outline"
              :loading="afterSaleActingOrderId === item.id"
              @click="onAdvanceAfterSale(item, 'REJECT')"
            >Reject</button>
            <button
              v-if="canWaitReturn(item)"
              class="btn-outline"
              :loading="afterSaleActingOrderId === item.id"
              @click="onAdvanceAfterSale(item, 'WAIT_RETURN')"
            >Wait return</button>
            <button
              v-if="canMarkReturned(item)"
              class="btn-outline"
              :loading="afterSaleActingOrderId === item.id"
              @click="onAdvanceAfterSale(item, 'RETURN')"
            >Mark returned</button>
            <button
              v-if="canMarkReceived(item)"
              class="btn-outline"
              :loading="afterSaleActingOrderId === item.id"
              @click="onAdvanceAfterSale(item, 'RECEIVE')"
            >Mark received</button>
            <button
              v-if="canProcessRefund(item)"
              class="btn-outline"
              :loading="afterSaleActingOrderId === item.id"
              @click="onAdvanceAfterSale(item, 'PROCESS')"
            >Start refund</button>
          </view>
        </view>
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
  align-items: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-radius: var(--radius-md);
  flex-wrap: wrap;
  border: 1px solid rgba(20, 20, 20, 0.08);
}
.field-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 160px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0.02em;
}

.std-input {
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

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.row {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 24px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(20, 20, 20, 0.08);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}
.row-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.row-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
}

.row-meta {
  font-size: 13px;
  color: var(--text-muted);
}

.row-meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.row-date {
  font-size: 12px;
  color: var(--text-soft);
}

.row-aftersale {
  font-size: 12px;
  color: var(--accent);
}

.chip-muted {
  background: rgba(20, 20, 20, 0.08);
  color: var(--text-muted);
}

.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 10px;
  border-top: 1px solid rgba(20, 20, 20, 0.08);
}
@media (hover: hover) {
  .row:hover,
  .toolbar:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 30px rgba(20, 20, 20, 0.12);
    border-color: rgba(20, 20, 20, 0.12);
  }
}

@media (max-width: 600px) {
  .page-wrap { padding: 16px; }
  .hero { padding: 24px 20px; }
  .toolbar { flex-direction: column; align-items: stretch; }
}
</style>
