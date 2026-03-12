<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { StockLedger, StockOperatePayload } from '../types/domain'
import {
  confirmStock,
  getStockLedger,
  releaseStock,
  reserveStock,
  rollbackStock
} from '../api/stock'

const skuIdInput = ref('')
const ledger = ref<StockLedger | null>(null)
const loading = ref(false)
const operating = ref(false)

const operationForm = reactive<StockOperatePayload>({
  subOrderNo: '',
  skuId: 0,
  quantity: 1,
  reason: ''
})

async function loadLedger(): Promise<void> {
  const skuId = Number(skuIdInput.value)
  if (!skuId) {
    ElMessage.warning('Provide a valid SKU ID.')
    return
  }
  loading.value = true
  try {
    ledger.value = await getStockLedger(skuId)
    operationForm.skuId = skuId
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load stock ledger'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

async function runOperation(type: 'reserve' | 'confirm' | 'release' | 'rollback'): Promise<void> {
  if (!operationForm.subOrderNo || !operationForm.skuId || !operationForm.quantity) {
    ElMessage.warning('Provide sub-order number, sku id, and quantity.')
    return
  }
  operating.value = true
  try {
    if (type === 'reserve') {
      await reserveStock(operationForm)
    } else if (type === 'confirm') {
      await confirmStock(operationForm)
    } else if (type === 'release') {
      await releaseStock(operationForm)
    } else {
      await rollbackStock(operationForm)
    }
    ElMessage.success(`Stock ${type} completed.`)
    await loadLedger()
  } catch (error) {
    const message = error instanceof Error ? error.message : `Stock ${type} failed`
    ElMessage.error(message)
  } finally {
    operating.value = false
  }
}
</script>

<template>
  <section class="glass-card panel">
    <div class="header">
      <h3>Stock Ledger</h3>
      <div class="lookup-row">
        <el-input v-model="skuIdInput" placeholder="SKU ID" />
        <el-button :loading="loading" round type="primary" @click="loadLedger">Load</el-button>
      </div>
    </div>

    <el-descriptions v-if="ledger" :column="2" border class="desc">
      <el-descriptions-item label="SKU">{{ ledger.skuId }}</el-descriptions-item>
      <el-descriptions-item label="On Hand">{{ ledger.onHandQty }}</el-descriptions-item>
      <el-descriptions-item label="Reserved">{{ ledger.reservedQty }}</el-descriptions-item>
      <el-descriptions-item label="Salable">{{ ledger.salableQty }}</el-descriptions-item>
      <el-descriptions-item label="Alert Threshold">{{ ledger.alertThreshold }}</el-descriptions-item>
      <el-descriptions-item label="Status">{{ ledger.status }}</el-descriptions-item>
    </el-descriptions>

    <div class="divider" />

    <h4>Stock Operation</h4>
    <el-form label-position="top" class="form">
      <el-form-item label="Sub Order No">
        <el-input v-model="operationForm.subOrderNo" placeholder="SUB-ORDER-NO" />
      </el-form-item>
      <div class="grid">
        <el-form-item label="SKU ID">
          <el-input-number v-model="operationForm.skuId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="Quantity">
          <el-input-number v-model="operationForm.quantity" :min="1" controls-position="right" />
        </el-form-item>
      </div>
      <el-form-item label="Reason">
        <el-input v-model="operationForm.reason" placeholder="Optional reason" />
      </el-form-item>
    </el-form>

    <div class="actions">
      <el-button :loading="operating" round type="primary" @click="runOperation('reserve')">Reserve</el-button>
      <el-button :loading="operating" round @click="runOperation('confirm')">Confirm</el-button>
      <el-button :loading="operating" round @click="runOperation('release')">Release</el-button>
      <el-button :loading="operating" round type="danger" plain @click="runOperation('rollback')">Rollback</el-button>
    </div>
  </section>
</template>

<style scoped>
.panel {
  padding: clamp(0.9rem, 1.2vw, 1.1rem);
}

.header {
  display: grid;
  gap: 12px;
}

.lookup-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.desc {
  margin-top: 10px;
}

.divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 16px 0;
}

.form {
  margin-top: 10px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 720px) {
  .lookup-row {
    grid-template-columns: 1fr;
  }

  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
