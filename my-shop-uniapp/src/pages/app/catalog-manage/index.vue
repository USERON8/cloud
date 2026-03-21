<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { listProducts, updateProductStatus } from '../../../api/product'
import type { ProductItem } from '../../../types/domain'
import { formatPrice } from '../../../utils/format'
import { confirm, toast } from '../../../utils/ui'

const keyword = ref('')
const loading = ref(false)
const rows = ref<ProductItem[]>([])

function statusText(status?: number): string {
  if (status === 1) return 'Published'
  if (status === 0) return 'Unpublished'
  return 'Unknown'
}

async function loadProducts(): Promise<void> {
  if (loading.value) return
  loading.value = true
  try {
    const result = await listProducts({ page: 1, size: 50, name: keyword.value || undefined })
    rows.value = result.records
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to load products')
  } finally {
    loading.value = false
  }
}

async function toggleStatus(item: ProductItem): Promise<void> {
  if (typeof item.id !== 'number') return
  const nextStatus: 0 | 1 = item.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? 'publish' : 'unpublish'
  const ok = await confirm(`Set "${item.name}" to ${action}?`)
  if (!ok) return
  try {
    await updateProductStatus(item.id, nextStatus)
    toast('Status updated', 'success')
    await loadProducts()
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to update status')
  }
}

onMounted(() => {
  void loadProducts()
})
</script>

<template>
  <AppShell title="Product Admin">
    <view class="panel glass-card">
      <view class="header">
        <text class="section-title">Product management</text>
        <button class="btn-outline" @click="loadProducts">Refresh</button>
      </view>

      <view class="search-row">
        <input v-model="keyword" class="search-input" placeholder="Search products" @confirm="loadProducts" />
        <button class="btn-primary" @click="loadProducts">Search</button>
      </view>

      <view v-if="rows.length === 0" class="empty">
        <text class="text-muted">No products found</text>
      </view>

      <view v-else class="list">
        <view v-for="item in rows" :key="item.id" class="row">
          <view class="info">
            <text class="name">{{ item.name }}</text>
            <text class="meta">{{ formatPrice(item.price) }}</text>
            <text class="meta">Status: {{ statusText(item.status) }}</text>
          </view>
          <button class="btn-outline" @click="toggleStatus(item)">
            {{ item.status === 1 ? 'Unpublish' : 'Publish' }}
          </button>
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

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row {
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 12px;
}

.info {
  flex: 1;
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

.empty {
  padding: 16px 0;
  text-align: center;
}
</style>
