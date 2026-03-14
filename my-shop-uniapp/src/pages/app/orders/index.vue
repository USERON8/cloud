<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { listOrders, cancelOrder, payOrder } from '../../../api/order'
import type { OrderItem } from '../../../types/domain'
import { formatPrice, formatDate } from '../../../utils/format'
import { confirm, toast } from '../../../utils/ui'

const rows = ref<OrderItem[]>([])
const loading = ref(false)

function statusText(status?: number): string {
  if (status === 0) return '待支付'
  if (status === 1) return '已支付'
  if (status === 2) return '已发货'
  if (status === 3) return '已完成'
  if (status === 4) return '已取消'
  return status != null ? String(status) : '未知'
}

async function loadOrders(): Promise<void> {
  if (loading.value) return
  loading.value = true
  try {
    const result = await listOrders({ page: 1, size: 30 })
    rows.value = result.records
  } catch (error) {
    toast(error instanceof Error ? error.message : '加载订单失败')
  } finally {
    loading.value = false
  }
}

async function onPay(order: OrderItem): Promise<void> {
  if (typeof order.id !== 'number') return
  const ok = await confirm(`确认支付订单 ${order.orderNo}？`)
  if (!ok) return
  try {
    await payOrder(order.id)
    toast('已提交支付', 'success')
    await loadOrders()
  } catch (error) {
    toast(error instanceof Error ? error.message : '支付失败')
  }
}

async function onCancel(order: OrderItem): Promise<void> {
  if (typeof order.id !== 'number') return
  const ok = await confirm(`确认取消订单 ${order.orderNo}？`)
  if (!ok) return
  try {
    await cancelOrder(order.id)
    toast('已取消', 'success')
    await loadOrders()
  } catch (error) {
    toast(error instanceof Error ? error.message : '取消失败')
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
        <text class="section-title">我的订单</text>
        <button class="btn-outline" @click="loadOrders">刷新</button>
      </view>

      <view v-if="rows.length === 0" class="empty">
        <text class="text-muted">暂无订单</text>
      </view>

      <view v-else class="list">
        <view v-for="item in rows" :key="item.id" class="row">
          <view class="info">
            <text class="name">订单号：{{ item.orderNo }}</text>
            <text class="meta">金额：{{ formatPrice(item.payAmount ?? item.totalAmount) }}</text>
            <text class="meta">状态：{{ statusText(item.status) }}</text>
            <text class="meta">下单时间：{{ formatDate(item.createdAt) }}</text>
          </view>
          <view class="actions">
            <button class="btn-outline" @click="onPay(item)">支付</button>
            <button class="btn-outline" @click="onCancel(item)">取消</button>
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
