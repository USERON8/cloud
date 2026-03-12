<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPaymentOrderByNo, getRefundByNo } from '../api/payment'
import type { PaymentOrderInfo, PaymentRefundInfo } from '../types/domain'

const paymentNo = ref('')
const refundNo = ref('')
const paymentLoading = ref(false)
const refundLoading = ref(false)
const paymentInfo = ref<PaymentOrderInfo | null>(null)
const refundInfo = ref<PaymentRefundInfo | null>(null)

const paymentRows = computed(() => {
  if (!paymentInfo.value) return []
  return Object.entries(paymentInfo.value).map(([key, value]) => ({ key, value }))
})

const refundRows = computed(() => {
  if (!refundInfo.value) return []
  return Object.entries(refundInfo.value).map(([key, value]) => ({ key, value }))
})

async function fetchPayment(): Promise<void> {
  const value = paymentNo.value.trim()
  if (!value) {
    ElMessage.warning('Enter payment number.')
    return
  }
  paymentLoading.value = true
  try {
    paymentInfo.value = await getPaymentOrderByNo(value)
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load payment'
    ElMessage.error(message)
  } finally {
    paymentLoading.value = false
  }
}

async function fetchRefund(): Promise<void> {
  const value = refundNo.value.trim()
  if (!value) {
    ElMessage.warning('Enter refund number.')
    return
  }
  refundLoading.value = true
  try {
    refundInfo.value = await getRefundByNo(value)
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load refund'
    ElMessage.error(message)
  } finally {
    refundLoading.value = false
  }
}
</script>

<template>
  <section class="glass-card panel">
    <div class="header">
      <h3>Payment Lookup</h3>
    </div>

    <div class="lookup-row">
      <el-input v-model="paymentNo" placeholder="Payment No" />
      <el-button :loading="paymentLoading" round type="primary" @click="fetchPayment">Lookup Payment</el-button>
    </div>

    <el-descriptions v-if="paymentInfo" :column="2" border class="desc">
      <el-descriptions-item v-for="item in paymentRows" :key="item.key" :label="item.key">
        {{ item.value ?? '--' }}
      </el-descriptions-item>
    </el-descriptions>

    <div class="divider" />

    <div class="header">
      <h3>Refund Lookup</h3>
      <p class="muted">Refund lookup may require internal scope.</p>
    </div>

    <div class="lookup-row">
      <el-input v-model="refundNo" placeholder="Refund No" />
      <el-button :loading="refundLoading" round type="primary" @click="fetchRefund">Lookup Refund</el-button>
    </div>

    <el-descriptions v-if="refundInfo" :column="2" border class="desc">
      <el-descriptions-item v-for="item in refundRows" :key="item.key" :label="item.key">
        {{ item.value ?? '--' }}
      </el-descriptions-item>
    </el-descriptions>
  </section>
</template>

<style scoped>
.panel {
  padding: clamp(0.9rem, 1.2vw, 1.1rem);
}

.header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.header h3 {
  margin: 0;
}

.lookup-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  margin: 12px 0 16px;
}

.desc {
  margin-bottom: 12px;
}

.divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 16px 0;
}

.muted {
  color: var(--text-muted);
  font-size: 0.85rem;
  margin: 0;
}

@media (max-width: 720px) {
  .lookup-row {
    grid-template-columns: 1fr;
  }
}
</style>
