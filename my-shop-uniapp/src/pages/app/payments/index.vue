<script setup lang="ts">
import { ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { getPaymentOrderByNo, getRefundByNo } from '../../../api/payment'
import type { PaymentOrderInfo, PaymentRefundInfo } from '../../../types/domain'
import { formatPrice, formatDate } from '../../../utils/format'
import { toast } from '../../../utils/ui'

const paymentNo = ref('')
const refundNo = ref('')
const paymentInfo = ref<PaymentOrderInfo | null>(null)
const refundInfo = ref<PaymentRefundInfo | null>(null)

async function queryPayment(): Promise<void> {
  if (!paymentNo.value.trim()) {
    toast('请输入支付单号')
    return
  }
  try {
    paymentInfo.value = await getPaymentOrderByNo(paymentNo.value.trim())
  } catch (error) {
    toast(error instanceof Error ? error.message : '查询失败')
  }
}

async function queryRefund(): Promise<void> {
  if (!refundNo.value.trim()) {
    toast('请输入退款单号')
    return
  }
  try {
    refundInfo.value = await getRefundByNo(refundNo.value.trim())
  } catch (error) {
    toast(error instanceof Error ? error.message : '查询失败')
  }
}
</script>

<template>
  <AppShell title="Payments">
    <view class="panel glass-card">
      <text class="section-title">支付查询</text>
      <view class="search-row">
        <input v-model="paymentNo" class="search-input" placeholder="支付单号" />
        <button class="btn-primary" @click="queryPayment">查询</button>
      </view>

      <view v-if="paymentInfo" class="result">
        <text class="name">支付单号：{{ paymentInfo.paymentNo }}</text>
        <text class="meta">金额：{{ formatPrice(paymentInfo.amount) }}</text>
        <text class="meta">状态：{{ paymentInfo.status || '--' }}</text>
        <text class="meta">完成时间：{{ formatDate(paymentInfo.paidAt) }}</text>
      </view>
    </view>

    <view class="panel glass-card">
      <text class="section-title">退款查询</text>
      <view class="search-row">
        <input v-model="refundNo" class="search-input" placeholder="退款单号" />
        <button class="btn-primary" @click="queryRefund">查询</button>
      </view>

      <view v-if="refundInfo" class="result">
        <text class="name">退款单号：{{ refundInfo.refundNo }}</text>
        <text class="meta">金额：{{ formatPrice(refundInfo.refundAmount) }}</text>
        <text class="meta">状态：{{ refundInfo.status || '--' }}</text>
        <text class="meta">完成时间：{{ formatDate(refundInfo.refundedAt) }}</text>
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
