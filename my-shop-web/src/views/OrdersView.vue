<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  batchCancelOrders,
  batchCompleteOrders,
  batchPayOrders,
  batchShipOrders,
  cancelOrder,
  completeOrder,
  getOrderById,
  listOrders,
  payOrder,
  shipOrder
} from '../api/order'
import type { OrderItem } from '../types/domain'
import { useRole } from '../auth/permission'

const route = useRoute()
const loading = ref(false)
const batchRunning = ref(false)
const exportLoading = ref(false)
const rows = ref<OrderItem[]>([])
const selectedRows = ref<OrderItem[]>([])
const total = ref(0)
const params = reactive({
  page: 1,
  size: 10,
  status: undefined as number | undefined,
  userId: undefined as number | undefined,
  shopId: undefined as number | undefined
})

const { isAdmin, isMerchant, isUser } = useRole()
const isManagementMode = computed(() => Boolean(route.meta.manageOrder))
const canEnterManagePage = computed(() => isMerchant.value || isAdmin.value)
const canUserOperate = computed(() => !isManagementMode.value && (isUser.value || isAdmin.value))
const canMerchantOperate = computed(() => isManagementMode.value && (isMerchant.value || isAdmin.value))
const batchConcurrency = 5
type BatchAction = 'pay' | 'cancel' | 'ship' | 'complete'

function statusText(status?: number): string {
  switch (status) {
    case 0:
      return 'Pending Payment'
    case 1:
      return 'Paid'
    case 2:
      return 'Shipped'
    case 3:
      return 'Completed'
    case 4:
      return 'Cancelled'
    default:
      return 'Unknown'
  }
}

function statusType(status?: number): 'warning' | 'success' | 'info' {
  if (status === 0) {
    return 'warning'
  }
  if (status === 1 || status === 3) {
    return 'success'
  }
  return 'info'
}

function amountText(value?: number): string {
  if (typeof value !== 'number') {
    return '--'
  }
  return `CNY ${value.toFixed(2)}`
}

function canPay(order: OrderItem): boolean {
  return order.status === 0 && canUserOperate.value
}

function canCancel(order: OrderItem): boolean {
  return (order.status === 0 || order.status === 1) && canUserOperate.value
}

function canShip(order: OrderItem): boolean {
  return order.status === 1 && canMerchantOperate.value
}

function canComplete(order: OrderItem): boolean {
  return order.status === 2 && canUserOperate.value
}

function selectedIdsByStatus(allowedStatuses: number[]): number[] {
  return selectedRows.value
    .filter((row) => typeof row.id === 'number' && row.status != null && allowedStatuses.includes(row.status))
    .map((row) => row.id)
}

function ensureSelected(ids: number[], action: string): boolean {
  if (ids.length > 0) {
    return true
  }
  ElMessage.warning(`Select at least one order for ${action}.`)
  return false
}

async function loadOrders(): Promise<void> {
  loading.value = true
  selectedRows.value = []
  try {
    const result = await listOrders({
      page: params.page,
      size: params.size,
      status: params.status,
      userId: isAdmin.value ? params.userId : undefined,
      shopId: isAdmin.value ? params.shopId : undefined
    })
    rows.value = result.records
    total.value = result.total
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load orders'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  params.page = 1
  void loadOrders()
}

function onResetFilters(): void {
  params.status = undefined
  params.userId = undefined
  params.shopId = undefined
  params.page = 1
  void loadOrders()
}

function onSelectionChange(selection: OrderItem[]): void {
  selectedRows.value = selection
}

async function onPay(order: OrderItem): Promise<void> {
  try {
    await payOrder(order.id)
    ElMessage.success(`Order ${order.orderNo} paid.`)
    await loadOrders()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Pay action failed'
    ElMessage.error(message)
  }
}

async function onShip(order: OrderItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`Ship order ${order.orderNo}?`, 'Confirm', {
      type: 'warning'
    })
    await shipOrder(order.id)
    ElMessage.success(`Order ${order.orderNo} shipped.`)
    await loadOrders()
  } catch (error) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}

async function onComplete(order: OrderItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`Complete order ${order.orderNo}?`, 'Confirm', {
      type: 'warning'
    })
    await completeOrder(order.id)
    ElMessage.success(`Order ${order.orderNo} completed.`)
    await loadOrders()
  } catch (error) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}

async function onCancel(order: OrderItem): Promise<void> {
  try {
    await ElMessageBox.confirm(`Cancel order ${order.orderNo}?`, 'Confirm', {
      type: 'warning'
    })
    await cancelOrder(order.id, 'Cancelled from web portal')
    ElMessage.success(`Order ${order.orderNo} cancelled.`)
    await loadOrders()
  } catch (error) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}

async function onBatchPay(): Promise<void> {
  const ids = selectedIdsByStatus([0])
  if (!ensureSelected(ids, 'batch pay')) {
    return
  }
  await runBatchWithBackend(ids, 'pay', (orderIds) => batchPayOrders(orderIds))
}

async function onBatchCancel(): Promise<void> {
  const ids = selectedIdsByStatus([0, 1])
  if (!ensureSelected(ids, 'batch cancel')) {
    return
  }
  await runBatchWithBackend(ids, 'cancel', (orderIds) => batchCancelOrders(orderIds, 'Cancelled from web portal'))
}

async function onBatchComplete(): Promise<void> {
  const ids = selectedIdsByStatus([2])
  if (!ensureSelected(ids, 'batch complete')) {
    return
  }
  await runBatchWithBackend(ids, 'complete', (orderIds) => batchCompleteOrders(orderIds))
}

async function onBatchShip(): Promise<void> {
  const ids = selectedIdsByStatus([1])
  if (!ensureSelected(ids, 'batch ship')) {
    return
  }
  await runBatchWithBackend(ids, 'ship', (orderIds) => batchShipOrders(orderIds))
}

function expectedStatus(action: BatchAction): number {
  if (action === 'pay') {
    return 1
  }
  if (action === 'ship') {
    return 2
  }
  if (action === 'complete') {
    return 3
  }
  return 4
}

async function collectFailedIds(ids: number[], targetStatus: number): Promise<number[]> {
  const statusMap = await executeWithConcurrency(ids, batchConcurrency, async (id) => {
    const order = await getOrderById(id)
    return order?.status === targetStatus
  })
  return ids.filter((id) => statusMap.get(id) !== true)
}

async function runBatchWithBackend(
  ids: number[],
  action: BatchAction,
  batchExecutor: (ids: number[]) => Promise<number>
): Promise<void> {
  if (batchRunning.value) {
    return
  }
  batchRunning.value = true

  try {
    const successCountRaw = await batchExecutor(ids)
    const successCount = typeof successCountRaw === 'number' ? successCountRaw : 0
    let failedIds: number[] = []
    if (successCount < ids.length) {
      failedIds = await collectFailedIds(ids, expectedStatus(action))
    }

    ElMessage.success(`Batch ${action} completed: ${successCount}/${ids.length}.`)

    if (failedIds.length > 0) {
      await ElMessageBox.alert(
        `Failed order IDs: ${failedIds.join(', ')}`,
        `Batch ${action} partial failure`,
        { type: 'warning' }
      )
    }

    await loadOrders()
  } catch (error) {
    const message = error instanceof Error ? error.message : `Batch ${action} failed`
    ElMessage.error(message)
  } finally {
    batchRunning.value = false
  }
}

async function executeWithConcurrency(
  ids: number[],
  concurrency: number,
  executor: (id: number) => Promise<boolean>
): Promise<Map<number, boolean>> {
  const result = new Map<number, boolean>()
  const maxWorkers = Math.max(1, Math.min(concurrency, ids.length))
  let cursor = 0

  const workers = Array.from({ length: maxWorkers }, async () => {
    while (true) {
      const index = cursor
      cursor += 1
      if (index >= ids.length) {
        return
      }
      const id = ids[index]
      if (typeof id !== 'number') {
        continue
      }
      try {
        result.set(id, (await executor(id)) === true)
      } catch {
        result.set(id, false)
      }
    }
  })

  await Promise.all(workers)
  return result
}

function escapeCsvCell(value: string): string {
  if (value.includes('"') || value.includes(',') || value.includes('\n')) {
    return `"${value.replace(/"/g, '""')}"`
  }
  return value
}

async function exportCsv(): Promise<void> {
  if (exportLoading.value) {
    return
  }
  exportLoading.value = true

  try {
    const allRows = await loadOrdersForExport()
    if (allRows.length === 0) {
      ElMessage.warning('No orders to export under current filters.')
      return
    }

    const headers = ['id', 'order_no', 'user_id', 'shop_id', 'status', 'total_amount', 'pay_amount', 'created_at']
    const lines = [headers.join(',')]

    allRows.forEach((order) => {
      const cells = [
        String(order.id ?? ''),
        escapeCsvCell(order.orderNo ?? ''),
        String(order.userId ?? ''),
        String(order.shopId ?? ''),
        escapeCsvCell(statusText(order.status)),
        String(order.totalAmount ?? ''),
        String(order.payAmount ?? ''),
        escapeCsvCell(order.createdAt ?? '')
      ]
      lines.push(cells.join(','))
    })

    const csv = lines.join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `orders-export-${Date.now()}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    ElMessage.success(`Exported ${allRows.length} orders.`)
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to export CSV'
    ElMessage.error(message)
  } finally {
    exportLoading.value = false
  }
}

async function loadOrdersForExport(): Promise<OrderItem[]> {
  const exportRows: OrderItem[] = []
  const pageSize = 200
  let page = 1

  while (true) {
    const result = await listOrders({
      page,
      size: pageSize,
      status: params.status,
      userId: isAdmin.value ? params.userId : undefined,
      shopId: isAdmin.value ? params.shopId : undefined
    })
    exportRows.push(...result.records)

    if (result.records.length === 0 || page >= result.pages) {
      break
    }
    page += 1
  }

  return exportRows
}

onMounted(() => {
  void loadOrders()
})
</script>

<template>
  <section class="glass-card panel">
    <div class="header">
      <h3>
        <span class="title-wrap">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 5h10l3 3v11H6V5Zm10 0v3h3M9 12h6M9 15h6" /></svg>
          {{ isManagementMode ? 'Order Management' : 'Orders' }}
        </span>
      </h3>
      <div class="header-actions">
        <router-link v-if="canEnterManagePage && !isManagementMode" class="inline-link" to="/app/orders/manage">
          <el-button round type="success">Open Management</el-button>
        </router-link>
        <router-link v-if="isManagementMode" class="inline-link" to="/app/orders">
          <el-button round>Back to Orders</el-button>
        </router-link>
        <el-button :loading="exportLoading" round @click="exportCsv">
          <span class="btn-wrap">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4v10M8 10l4 4 4-4M5 18h14" /></svg>
            Export CSV
          </span>
        </el-button>
        <el-button :loading="loading" round @click="loadOrders">
          <span class="btn-wrap">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19 6v5h-5M5 18v-5h5M19 11a7 7 0 0 0-12-3M5 13a7 7 0 0 0 12 3" /></svg>
            Refresh
          </span>
        </el-button>
      </div>
    </div>

    <div class="filters">
      <el-select v-model="params.status" clearable placeholder="Status">
        <el-option :value="0" label="Pending Payment" />
        <el-option :value="1" label="Paid" />
        <el-option :value="2" label="Shipped" />
        <el-option :value="3" label="Completed" />
        <el-option :value="4" label="Cancelled" />
      </el-select>
      <el-input-number
        v-if="isAdmin"
        v-model="params.userId"
        :min="1"
        controls-position="right"
        placeholder="User ID"
      />
      <el-input-number
        v-if="isAdmin"
        v-model="params.shopId"
        :min="1"
        controls-position="right"
        placeholder="Shop ID"
      />
      <el-button round type="primary" @click="onSearch">Search</el-button>
      <el-button round @click="onResetFilters">Reset</el-button>
    </div>

    <div class="batch-actions">
      <el-button
        v-if="canUserOperate"
        :disabled="batchRunning"
        :loading="batchRunning"
        round
        size="small"
        type="primary"
        @click="onBatchPay"
      >
        Batch Pay
      </el-button>
      <el-button v-if="canUserOperate" :disabled="batchRunning" round size="small" @click="onBatchCancel">
        Batch Cancel
      </el-button>
      <el-button
        v-if="canUserOperate"
        :disabled="batchRunning"
        round
        size="small"
        type="success"
        plain
        @click="onBatchComplete"
      >
        Batch Complete
      </el-button>
      <el-button
        v-if="canMerchantOperate"
        :disabled="batchRunning"
        round
        size="small"
        type="primary"
        plain
        @click="onBatchShip"
      >
        Batch Ship
      </el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column label="Order No" min-width="220" prop="orderNo" />
      <el-table-column v-if="isAdmin" label="Shop ID" min-width="120" prop="shopId" />
      <el-table-column label="Amount" min-width="130">
        <template #default="scope">
          {{ amountText(scope.row.totalAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="Status" min-width="160">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)" round>
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Created At" min-width="200" prop="createdAt" />
      <el-table-column label="Actions" width="360">
        <template #default="scope">
          <el-button v-if="canPay(scope.row)" round size="small" type="primary" @click="onPay(scope.row)">
            Pay
          </el-button>
          <el-button v-if="canCancel(scope.row)" round size="small" @click="onCancel(scope.row)">
            Cancel
          </el-button>
          <el-button v-if="canShip(scope.row)" round size="small" type="primary" plain @click="onShip(scope.row)">
            Ship
          </el-button>
          <el-button v-if="canComplete(scope.row)" round size="small" type="success" plain @click="onComplete(scope.row)">
            Complete
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="params.page"
        v-model:page-size="params.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        background
        layout="total, sizes, prev, pager, next"
        @current-change="loadOrders"
        @size-change="loadOrders"
      />
    </div>
  </section>
</template>

<style scoped>
.panel {
  padding: clamp(0.9rem, 1.2vw, 1.1rem);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}

.header h3 {
  margin: 0;
  font-size: clamp(1.02rem, 1.25vw, 1.28rem);
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.inline-link {
  text-decoration: none;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  align-items: center;
}

.batch-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  align-items: center;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.title-wrap svg,
.btn-wrap svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.btn-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

@media (max-width: 900px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .pager {
    justify-content: center;
  }
}
</style>
