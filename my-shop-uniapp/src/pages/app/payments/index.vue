<script setup lang="ts">
import { computed, ref } from 'vue'
import { onHide, onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { useTimeoutPoll } from '@vueuse/core'
import AppShell from '../../../components/AppShell.vue'
import { resolveApiUrl } from '../../../api/http'
import {
  createPaymentCheckoutSession,
  getPaymentOrderByNo,
  getPaymentStatus,
  getRefundByNo
} from '../../../api/payment'
import { navigateTo } from '../../../router/navigation'
import { Routes } from '../../../router/routes'
import type { PaymentOrderInfo, PaymentRefundInfo, PaymentStatusInfo } from '../../../types/domain'
import { formatDate, formatPrice } from '../../../utils/format'
import { toast } from '../../../utils/ui'

const paymentNo = ref('')
const refundNo = ref('')
const paymentInfo = ref<PaymentOrderInfo | null>(null)
const refundInfo = ref<PaymentRefundInfo | null>(null)
const checkoutLoading = ref(false)
const pollAttempts = ref(0)
const isPolling = ref(false)

const FINAL_PAYMENT_STATUSES = new Set(['PAID', 'FAILED'])
const MAX_POLL_ATTEMPTS = 15
const POLL_INTERVAL_MS = 2000

function canOpenCheckout(): boolean {
  return paymentInfo.value?.status === 'CREATED' && !!paymentInfo.value?.paymentNo
}

const paymentStatusHint = computed(() => {
  if (isPolling.value) {
    return 'Checking the latest payment status...'
  }
  if (paymentInfo.value?.status === 'CREATED') {
    return 'Payment is still pending.'
  }
  return ''
})

function openCheckout(url: string): void {
  navigateTo(
    Routes.webview,
    { url, paymentNo: paymentNo.value.trim() },
    {
      requiresAuth: true,
      roles: ['USER', 'MERCHANT', 'ADMIN']
    }
  )
}

function shouldKeepPolling(status?: string): boolean {
  return !!status && !FINAL_PAYMENT_STATUSES.has(status)
}

function applyPaymentStatus(statusPayload: PaymentStatusInfo | null): void {
  if (!statusPayload?.status) {
    return
  }
  paymentInfo.value = {
    ...(paymentInfo.value ?? {}),
    paymentNo: statusPayload.paymentNo ?? paymentInfo.value?.paymentNo ?? paymentNo.value.trim(),
    status: statusPayload.status
  }
}

async function queryPayment(showError = true): Promise<void> {
  const targetPaymentNo = paymentNo.value.trim()
  if (!targetPaymentNo) {
    if (showError) {
      toast('Enter a payment number')
    }
    return
  }
  try {
    paymentInfo.value = await getPaymentOrderByNo(targetPaymentNo)
  } catch (error) {
    if (showError) {
      toast(error instanceof Error ? error.message : 'Failed to query the payment order')
    }
  }
}

async function pollPaymentStatus(): Promise<void> {
  const targetPaymentNo = paymentNo.value.trim()
  if (!targetPaymentNo || pollAttempts.value >= MAX_POLL_ATTEMPTS) {
    isPolling.value = false
    paymentPoller.pause()
    return
  }
  isPolling.value = true
  pollAttempts.value += 1
  try {
    const statusPayload = await getPaymentStatus(targetPaymentNo)
    applyPaymentStatus(statusPayload)
    if (!shouldKeepPolling(statusPayload.status)) {
      isPolling.value = false
      paymentPoller.pause()
      await queryPayment(false)
    }
  } catch (error) {
    isPolling.value = false
    paymentPoller.pause()
    toast(error instanceof Error ? error.message : 'Failed to refresh payment status')
  }
}

const paymentPoller = useTimeoutPoll(() => {
  void pollPaymentStatus()
}, POLL_INTERVAL_MS, { immediate: false })

async function startPaymentPolling(): Promise<void> {
  paymentPoller.pause()
  pollAttempts.value = 0
  isPolling.value = false
  await queryPayment(false)
  if (!shouldKeepPolling(paymentInfo.value?.status)) {
    return
  }
  paymentPoller.resume()
}

async function queryRefund(): Promise<void> {
  if (!refundNo.value.trim()) {
    toast('Enter a refund number')
    return
  }
  try {
    refundInfo.value = await getRefundByNo(refundNo.value.trim())
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to query the refund order')
  }
}

async function openPaymentCheckout(): Promise<void> {
  const targetPaymentNo = paymentInfo.value?.paymentNo || paymentNo.value.trim()
  if (!targetPaymentNo) {
    toast('Enter a payment number')
    return
  }
  checkoutLoading.value = true
  try {
    const session = await createPaymentCheckoutSession(targetPaymentNo)
    if (!session.checkoutPath) {
      throw new Error('Checkout session is missing checkoutPath')
    }
    openCheckout(resolveApiUrl(session.checkoutPath))
    void startPaymentPolling()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to open checkout')
  } finally {
    checkoutLoading.value = false
  }
}

onLoad((query) => {
  const queryPaymentNo =
    typeof query.paymentNo === 'string' && query.paymentNo.trim()
      ? query.paymentNo.trim()
      : typeof query.out_trade_no === 'string' && query.out_trade_no.trim()
        ? query.out_trade_no.trim()
        : ''
  const shouldAutoPoll = query.autoPoll === '1' || query.payment_return === '1'

  if (queryPaymentNo) {
    paymentNo.value = queryPaymentNo
    if (shouldAutoPoll) {
      void startPaymentPolling()
    } else {
      void queryPayment(false)
    }
  }
  if (typeof query.refundNo === 'string' && query.refundNo.trim()) {
    refundNo.value = query.refundNo.trim()
    void queryRefund()
  }
})

onShow(() => {
  if (paymentNo.value.trim() && shouldKeepPolling(paymentInfo.value?.status)) {
    void startPaymentPolling()
  }
})

onHide(() => {
  paymentPoller.pause()
  isPolling.value = false
})

onUnload(() => {
  paymentPoller.pause()
})
</script>

<template>
  <AppShell title="Payments">
    <view class="panel glass-card">
      <text class="section-title">Payment lookup</text>
      <view class="search-row">
        <input v-model="paymentNo" class="search-input" placeholder="Payment number" />
        <button class="btn-primary" @click="queryPayment">Search</button>
      </view>

      <view v-if="paymentInfo" class="result">
        <text class="name">Payment number: {{ paymentInfo.paymentNo }}</text>
        <text class="meta">Main order: {{ paymentInfo.mainOrderNo || '--' }}</text>
        <text class="meta">Sub order: {{ paymentInfo.subOrderNo || '--' }}</text>
        <text class="meta">Amount: {{ formatPrice(paymentInfo.amount) }}</text>
        <text class="meta">Status: {{ paymentInfo.status || '--' }}</text>
        <text class="meta">Channel: {{ paymentInfo.channel || '--' }}</text>
        <text class="meta">Paid at: {{ formatDate(paymentInfo.paidAt) }}</text>
        <text v-if="paymentStatusHint" class="meta status-hint">{{ paymentStatusHint }}</text>
        <button
          v-if="paymentInfo.status === 'CREATED'"
          class="btn-outline"
          :loading="isPolling"
          @click="startPaymentPolling"
        >
          {{ isPolling ? 'Checking status...' : 'Refresh status' }}
        </button>
        <button
          v-if="canOpenCheckout()"
          class="btn-outline"
          :loading="checkoutLoading"
          @click="openPaymentCheckout"
        >
          Open checkout
        </button>
      </view>
    </view>

    <view class="panel glass-card">
      <text class="section-title">Refund lookup</text>
      <view class="search-row">
        <input v-model="refundNo" class="search-input" placeholder="Refund number" />
        <button class="btn-primary" @click="queryRefund">Search</button>
      </view>

      <view v-if="refundInfo" class="result">
        <text class="name">Refund number: {{ refundInfo.refundNo }}</text>
        <text class="meta">Payment number: {{ refundInfo.paymentNo || '--' }}</text>
        <text class="meta">After-sale number: {{ refundInfo.afterSaleNo || '--' }}</text>
        <text class="meta">Amount: {{ formatPrice(refundInfo.refundAmount) }}</text>
        <text class="meta">Status: {{ refundInfo.status || '--' }}</text>
        <text class="meta">Refunded at: {{ formatDate(refundInfo.refundedAt) }}</text>
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
  margin-bottom: 12px;
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

.name {
  font-size: 14px;
  font-weight: 600;
}

.meta {
  font-size: 12px;
  color: var(--text-muted);
}

.status-hint {
  color: #1f6feb;
}
</style>
