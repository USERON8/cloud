<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppShell from '../../../components/AppShell.vue'
import { resolveApiUrl } from '../../../api/http'
import {
  createPaymentCheckoutSession,
  getPaymentOrderByNo,
  getRefundByNo
} from '../../../api/payment'
import { navigateTo } from '../../../router/navigation'
import { Routes } from '../../../router/routes'
import type { PaymentOrderInfo, PaymentRefundInfo } from '../../../types/domain'
import { formatDate, formatPrice } from '../../../utils/format'
import { toast } from '../../../utils/ui'

const paymentNo = ref('')
const refundNo = ref('')
const paymentInfo = ref<PaymentOrderInfo | null>(null)
const refundInfo = ref<PaymentRefundInfo | null>(null)
const checkoutLoading = ref(false)

function canOpenCheckout(): boolean {
  return paymentInfo.value?.status === 'CREATED' && !!paymentInfo.value?.paymentNo
}

function openCheckout(url: string): void {
  navigateTo(
    Routes.webview,
    { url },
    {
      requiresAuth: true,
      roles: ['USER', 'MERCHANT', 'ADMIN']
    }
  )
}

async function queryPayment(): Promise<void> {
  if (!paymentNo.value.trim()) {
    toast('Enter a payment number')
    return
  }
  try {
    paymentInfo.value = await getPaymentOrderByNo(paymentNo.value.trim())
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to query the payment order')
  }
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
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to open checkout')
  } finally {
    checkoutLoading.value = false
  }
}

onLoad((query) => {
  if (typeof query.paymentNo === 'string' && query.paymentNo.trim()) {
    paymentNo.value = query.paymentNo.trim()
    void queryPayment()
  }
  if (typeof query.refundNo === 'string' && query.refundNo.trim()) {
    refundNo.value = query.refundNo.trim()
    void queryRefund()
  }
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
</style>
