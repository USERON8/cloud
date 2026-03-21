<script setup lang="ts">
import { onHide, onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { useTimeoutPoll } from '@vueuse/core'
import { getPaymentStatus } from '../../api/payment'
import { navigateTo, redirectTo } from '../../router/navigation'
import { Routes } from '../../router/routes'

const url = ref('')
const paymentNo = ref('')
const paymentStatus = ref('')

const FINAL_PAYMENT_STATUSES = new Set(['PAID', 'FAILED'])
const MAX_POLL_ATTEMPTS = 20
const POLL_INTERVAL_MS = 3000

let pollAttempts = 0
let redirecting = false

onLoad((query) => {
  if (typeof query.url === 'string') {
    url.value = decodeURIComponent(query.url)
  }
  if (typeof query.paymentNo === 'string') {
    paymentNo.value = query.paymentNo
    void startPaymentPolling()
  }
})

function goToPaymentStatus(): void {
  if (!paymentNo.value) {
    return
  }
  navigateTo(
    Routes.appPayments,
    { paymentNo: paymentNo.value, autoPoll: 1 },
    {
      requiresAuth: true,
      roles: ['USER', 'MERCHANT', 'ADMIN']
    }
  )
}

function isFinalPaymentStatus(status?: string): boolean {
  return !!status && FINAL_PAYMENT_STATUSES.has(status)
}

function redirectToPaymentStatus(): void {
  if (!paymentNo.value || redirecting) {
    return
  }
  redirecting = true
  redirectTo(
    Routes.appPayments,
    { paymentNo: paymentNo.value, autoPoll: 1, payment_return: 1 },
    {
      requiresAuth: true,
      roles: ['USER', 'MERCHANT', 'ADMIN']
    }
  )
}

async function pollPaymentStatus(): Promise<void> {
  if (!paymentNo.value || redirecting || pollAttempts >= MAX_POLL_ATTEMPTS) {
    webviewPoller.pause()
    return
  }
  pollAttempts += 1
  try {
    const result = await getPaymentStatus(paymentNo.value)
    paymentStatus.value = result.status || ''
    if (isFinalPaymentStatus(result.status)) {
      webviewPoller.pause()
      redirectToPaymentStatus()
    }
  } catch {
    // Ignore polling failures and keep manual navigation available.
  }
}

const webviewPoller = useTimeoutPoll(() => {
  void pollPaymentStatus()
}, POLL_INTERVAL_MS, { immediate: false })

async function startPaymentPolling(): Promise<void> {
  webviewPoller.pause()
  pollAttempts = 0
  redirecting = false
  paymentStatus.value = ''
  webviewPoller.resume()
}

onShow(() => {
  if (!paymentNo.value || redirecting || isFinalPaymentStatus(paymentStatus.value)) {
    return
  }
  void startPaymentPolling()
})

onHide(() => {
  webviewPoller.pause()
})

onUnload(() => {
  webviewPoller.pause()
})
</script>

<template>
  <view class="page">
    <web-view v-if="url" :src="url" />
    <view v-if="paymentNo" class="status-banner">
      <text class="status-text">
        {{ paymentStatus ? `Payment status: ${paymentStatus}` : 'Waiting for payment confirmation...' }}
      </text>
    </view>
    <button v-if="paymentNo" class="status-button" @click="goToPaymentStatus">
      View payment status
    </button>
    <view v-else class="empty">
      <text>No page is available to open.</text>
    </view>
  </view>
</template>

<style scoped>
.page {
  width: 100%;
  height: 100vh;
}

.empty {
  padding: 24px;
  text-align: center;
  color: var(--text-muted);
}

.status-banner {
  position: fixed;
  left: 16px;
  right: 16px;
  bottom: 80px;
  z-index: 10;
  padding: 10px 14px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.78);
  backdrop-filter: blur(12px);
}

.status-text {
  color: #ffffff;
  font-size: 12px;
}

.status-button {
  position: fixed;
  right: 16px;
  bottom: 24px;
  z-index: 10;
  padding: 10px 16px;
  border-radius: 999px;
  background: #ffffff;
  color: #1f2a37;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.16);
}
</style>
