<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { reactive, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import {
  advanceAfterSaleStatus,
  applyAfterSale,
  cancelOrder,
  completeOrder,
  listOrders
} from '../../../api/order'
import { resolveApiUrl } from '../../../api/http'
import {
  createPaymentCheckoutSession,
  createPaymentOrder,
  getPaymentOrderByOrderNo
} from '../../../api/payment'
import type { AfterSaleInfo, OrderItem } from '../../../types/domain'
import { navigateTo } from '../../../router/navigation'
import { Routes } from '../../../router/routes'
import { formatDate, formatPrice, formatRelativeDate } from '../../../utils/format'
import { confirm, toast } from '../../../utils/ui'

const rows = ref<OrderItem[]>([])
const loading = ref(false)
const refundingOrderId = ref<number | null>(null)
const payingOrderId = ref<number | null>(null)
const completingOrderId = ref<number | null>(null)

const afterSaleDraft = reactive({
  orderId: null as number | null,
  afterSaleType: 'REFUND',
  reason: '',
  description: '',
  applyAmount: ''
})

function statusText(status?: number): string {
  if (status === 0) return 'Awaiting payment'
  if (status === 1) return 'Paid'
  if (status === 2) return 'Shipped'
  if (status === 3) return 'Completed'
  if (status === 4) return 'Cancelled'
  return status != null ? String(status) : 'Unknown'
}

function canApplyAfterSale(order: OrderItem): boolean {
  return (
    typeof order.id === 'number' &&
    typeof order.subOrderId === 'number' &&
    typeof order.merchantId === 'number' &&
    [1, 2, 3].includes(order.status ?? -1) &&
    (!order.afterSaleStatus || order.afterSaleStatus === 'NONE')
  )
}

function canCancelAfterSale(order: OrderItem): boolean {
  return typeof order.afterSaleId === 'number' && order.afterSaleStatus === 'APPLIED'
}

function canPay(order: OrderItem): boolean {
  return order.status === 0 && typeof order.userId === 'number' && !!order.orderNo && !!order.subOrderNo
}

function canComplete(order: OrderItem): boolean {
  return typeof order.id === 'number' && order.status === 2
}

function canViewRefund(order: OrderItem): boolean {
  return !!order.refundNo && ['REFUNDING', 'REFUNDED'].includes(order.afterSaleStatus ?? '')
}

function buildPaymentIdempotencyKey(order: OrderItem): string {
  return `payment:${order.orderNo}:${order.subOrderNo ?? order.id}`
}

function buildPaymentNo(order: OrderItem): string {
  const subOrderNo = order.subOrderNo?.replace(/[^A-Za-z0-9_-]/g, '') || String(order.id)
  return `PAY-${subOrderNo}`
}

function openCheckout(url: string, paymentNo: string): void {
  navigateTo(
    Routes.webview,
    { url, paymentNo },
    {
      requiresAuth: true,
      roles: ['USER', 'MERCHANT', 'ADMIN']
    }
  )
}

function resetAfterSaleDraft(): void {
  afterSaleDraft.orderId = null
  afterSaleDraft.afterSaleType = 'REFUND'
  afterSaleDraft.reason = ''
  afterSaleDraft.description = ''
  afterSaleDraft.applyAmount = ''
}

function openAfterSale(order: OrderItem): void {
  if (!canApplyAfterSale(order)) {
    toast('This order is not eligible for a new after-sale request')
    return
  }
  afterSaleDraft.orderId = order.id
  afterSaleDraft.afterSaleType = 'REFUND'
  afterSaleDraft.reason = ''
  afterSaleDraft.description = ''
  afterSaleDraft.applyAmount = String(order.payAmount ?? order.totalAmount ?? '')
}

function selectedOrder(): OrderItem | null {
  return rows.value.find((item) => item.id === afterSaleDraft.orderId) ?? null
}

function buildAfterSalePayload(order: OrderItem): AfterSaleInfo | null {
  if (
    typeof order.id !== 'number' ||
    typeof order.subOrderId !== 'number' ||
    typeof order.merchantId !== 'number'
  ) {
    toast('The order is missing after-sale metadata')
    return null
  }
  const amount = Number(afterSaleDraft.applyAmount)
  if (!Number.isFinite(amount) || amount <= 0) {
    toast('Apply amount must be greater than 0')
    return null
  }
  if (!afterSaleDraft.reason.trim()) {
    toast('Reason is required')
    return null
  }

  return {
    mainOrderId: order.id,
    subOrderId: order.subOrderId,
    merchantId: order.merchantId,
    afterSaleType: afterSaleDraft.afterSaleType,
    reason: afterSaleDraft.reason.trim(),
    description: afterSaleDraft.description.trim() || undefined,
    applyAmount: Number(amount.toFixed(2))
  }
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

async function onPay(order: OrderItem): Promise<void> {
  if (!canPay(order) || typeof order.userId !== 'number' || !order.subOrderNo) {
    toast('This order is missing payment metadata')
    return
  }
  const amount = Number(order.payAmount ?? order.totalAmount ?? NaN)
  if (!Number.isFinite(amount) || amount <= 0) {
    toast('This order does not have a valid payable amount')
    return
  }

  payingOrderId.value = order.id
  let paymentNo = ''
  try {
    const existingOrder = await getPaymentOrderByOrderNo(order.orderNo, order.subOrderNo)
    paymentNo = existingOrder?.paymentNo ?? buildPaymentNo(order)
    if (!existingOrder) {
      await createPaymentOrder({
        paymentNo,
        mainOrderNo: order.orderNo,
        subOrderNo: order.subOrderNo,
        userId: order.userId,
        amount: Number(amount.toFixed(2)),
        channel: 'ALIPAY',
        idempotencyKey: buildPaymentIdempotencyKey(order)
      })
    }
    const session = await createPaymentCheckoutSession(paymentNo)
    if (!session.checkoutPath) {
      throw new Error('Checkout session is missing checkoutPath')
    }
    openCheckout(resolveApiUrl(session.checkoutPath), paymentNo)
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to prepare payment')
    if (paymentNo) {
      navigateTo(
        Routes.appPayments,
        { paymentNo, autoPoll: 1 },
        {
          requiresAuth: true,
          roles: ['USER', 'MERCHANT', 'ADMIN']
        }
      )
    }
  } finally {
    payingOrderId.value = null
  }
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

async function onComplete(order: OrderItem): Promise<void> {
  if (!canComplete(order) || typeof order.id !== 'number') {
    toast('This order cannot be completed')
    return
  }
  const ok = await confirm(`Confirm receipt for order ${order.orderNo}?`)
  if (!ok) {
    return
  }
  completingOrderId.value = order.id
  try {
    await completeOrder(order.id)
    toast('Order completed', 'success')
    await loadOrders()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to complete the order')
  } finally {
    completingOrderId.value = null
  }
}

function onViewRefund(order: OrderItem): void {
  if (!canViewRefund(order)) {
    toast('Refund tracking is not available for this order')
    return
  }
  navigateTo(
    Routes.appPayments,
    { refundNo: order.refundNo },
    {
      requiresAuth: true,
      roles: ['USER', 'MERCHANT', 'ADMIN']
    }
  )
}

async function submitAfterSale(): Promise<void> {
  const order = selectedOrder()
  if (!order || typeof order.id !== 'number') {
    toast('Select an order first')
    return
  }
  const payload = buildAfterSalePayload(order)
  if (!payload) {
    return
  }

  refundingOrderId.value = order.id
  try {
    const result = await applyAfterSale(payload)
    toast(`After-sale request created: ${result.afterSaleNo ?? 'pending number'}`, 'success')
    resetAfterSaleDraft()
    await loadOrders()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to create the after-sale request')
  } finally {
    refundingOrderId.value = null
  }
}

async function cancelAfterSale(order: OrderItem): Promise<void> {
  if (!canCancelAfterSale(order) || typeof order.afterSaleId !== 'number') {
    toast('This after-sale request cannot be cancelled')
    return
  }
  const ok = await confirm(`Cancel after-sale request ${order.afterSaleNo ?? order.afterSaleId}?`)
  if (!ok) {
    return
  }
  refundingOrderId.value = order.id
  try {
    await advanceAfterSaleStatus(order.afterSaleId, 'CANCEL')
    toast('After-sale request cancelled', 'success')
    await loadOrders()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to cancel the after-sale request')
  } finally {
    refundingOrderId.value = null
  }
}

onShow(() => {
  void loadOrders()
})
</script>

<template>
  <AppShell title="Orders">
    <view class="page">
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
              <text class="meta">Age: {{ formatRelativeDate(item.createdAt) }}</text>
              <text v-if="item.afterSaleStatus && item.afterSaleStatus !== 'NONE'" class="meta">
                After-sale: {{ item.afterSaleStatus }}{{ item.afterSaleNo ? ` (${item.afterSaleNo})` : '' }}
              </text>
            </view>
            <view class="actions">
              <button
                v-if="item.status === 0"
                class="btn-outline"
                :loading="payingOrderId === item.id"
                @click="onPay(item)"
              >
                {{ payingOrderId === item.id ? 'Opening checkout...' : 'Pay now' }}
              </button>
              <button
                v-if="canComplete(item)"
                class="btn-outline"
                :loading="completingOrderId === item.id"
                @click="onComplete(item)"
              >
                Confirm receipt
              </button>
              <button v-if="canViewRefund(item)" class="btn-outline" @click="onViewRefund(item)">
                View refund
              </button>
              <button v-if="canApplyAfterSale(item)" class="btn-outline" @click="openAfterSale(item)">
                Apply after-sale
              </button>
              <button v-if="canCancelAfterSale(item)" class="btn-outline" @click="cancelAfterSale(item)">
                Cancel after-sale
              </button>
              <button class="btn-outline" @click="onCancel(item)">Cancel</button>
            </view>
          </view>
        </view>
      </view>

      <view v-if="afterSaleDraft.orderId" class="panel glass-card">
        <view class="header">
          <text class="section-title">After-sale request</text>
          <button class="btn-outline" @click="resetAfterSaleDraft">Close</button>
        </view>

        <picker
          mode="selector"
          :range="['REFUND', 'RETURN_REFUND']"
          :value="afterSaleDraft.afterSaleType === 'RETURN_REFUND' ? 1 : 0"
          @change="afterSaleDraft.afterSaleType = $event.detail.value === 1 ? 'RETURN_REFUND' : 'REFUND'"
        >
          <view class="picker-field">Type: {{ afterSaleDraft.afterSaleType }}</view>
        </picker>

        <input v-model="afterSaleDraft.reason" class="input" placeholder="Reason" />
        <textarea v-model="afterSaleDraft.description" class="textarea" placeholder="Description" />
        <input v-model="afterSaleDraft.applyAmount" class="input" type="digit" placeholder="Apply amount" />

        <button
          class="btn-primary"
          :loading="refundingOrderId === afterSaleDraft.orderId"
          @click="submitAfterSale"
        >
          Submit after-sale request
        </button>
      </view>
    </view>
  </AppShell>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

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
  flex-wrap: wrap;
}

.picker-field,
.input {
  width: 100%;
  background: #fff;
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 14px;
}

.textarea {
  width: 100%;
  min-height: 96px;
  background: #fff;
  border-radius: 14px;
  padding: 10px 12px;
  font-size: 14px;
}

.empty {
  padding: 16px 0;
  text-align: center;
}
</style>
