<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { listProducts, updateProductStatus } from '../../../api/product'
import type { ProductItem } from '../../../types/domain'
import { formatPrice, formatProductStatus } from '../../../utils/format'
import { confirm, toast } from '../../../utils/ui'

const keyword = ref('')
const loading = ref(false)
const rows = ref<ProductItem[]>([])

const publishedCount = () => rows.value.filter(r => r.status === 1).length
const unpublishedCount = () => rows.value.filter(r => r.status !== 1).length

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
    <view class="page-wrap">
      <!-- Hero -->
      <view class="hero surface-card fade-in-up">
        <view class="hero-left">
          <text class="hero-eyebrow">MERCHANT</text>
          <text class="hero-title">Product Catalog</text>
          <text class="hero-subtitle">Publish, unpublish and monitor your product listings.</text>
        </view>
        <view class="hero-stats">
          <view class="info-card">
            <text class="info-label">Published</text>
            <text class="info-value">{{ publishedCount() }}</text>
          </view>
          <view class="info-card">
            <text class="info-label">Unpublished</text>
            <text class="info-value">{{ unpublishedCount() }}</text>
          </view>
          <view class="info-card">
            <text class="info-label">Total</text>
            <text class="info-value">{{ rows.length }}</text>
          </view>
        </view>
      </view>

      <!-- Toolbar -->
      <view class="toolbar surface-card fade-in-up">
        <view class="search-row">
          <input
            v-model="keyword"
            class="std-input"
            placeholder="Search products"
            @confirm="loadProducts"
          />
          <button class="btn-primary" :loading="loading" @click="loadProducts">Search</button>
        </view>
        <button class="btn-outline" :loading="loading" @click="loadProducts">Refresh</button>
      </view>

      <!-- List -->
      <view v-if="rows.length === 0" class="empty-state">
        <text class="empty-state-text">No products found</text>
      </view>

      <view v-else class="list fade-in-up">
        <view v-for="item in rows" :key="item.id" class="row surface-card">
          <view class="row-info">
            <text class="row-name">{{ item.name }}</text>
            <text class="row-meta">{{ formatPrice(item.price) }}</text>
            <view class="chip" :class="item.status === 1 ? 'chip-success' : 'chip-muted'">
              <text>{{ formatProductStatus(item.status) }}</text>
            </view>
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
.page-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
  max-width: 960px;
  margin: 0 auto;
}

.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 32px 36px;
  border-radius: var(--radius-lg);
  flex-wrap: wrap;
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hero-eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--accent);
  text-transform: uppercase;
}

.hero-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
  letter-spacing: -0.02em;
}

.hero-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.5;
}

.hero-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: var(--radius-md);
  flex-wrap: wrap;
  border: 1px solid rgba(20, 20, 20, 0.08);
}
.search-row {
  flex: 1;
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 200px;
}

.std-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(20, 20, 20, 0.12);
  border-radius: 16px;
  padding: 13px 16px;
  font-size: 14px;
  color: var(--text-main);
}

.std-input:focus {
  border-color: rgba(11, 107, 95, 0.4);
  box-shadow: 0 0 0 3px rgba(11, 107, 95, 0.12);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(20, 20, 20, 0.08);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}
.row-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.row-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
}

.row-meta {
  font-size: 13px;
  color: var(--text-muted);
}

.chip-success {
  background: rgba(11, 107, 95, 0.16);
  color: var(--accent);
}

.chip-muted {
  background: rgba(20, 20, 20, 0.08);
  color: var(--text-muted);
}

@media (hover: hover) {
  .row:hover,
  .toolbar:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 30px rgba(20, 20, 20, 0.12);
    border-color: rgba(20, 20, 20, 0.12);
  }
}

@media (max-width: 600px) {
  .page-wrap { padding: 16px; }
  .hero { padding: 24px 20px; }
  .row { flex-direction: column; align-items: flex-start; }
}
</style>
