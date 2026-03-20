<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import {
  listSearchHotKeywordsWithFallback,
  listSearchKeywordRecommendationsWithFallback,
  smartSearchProductsWithFallback
} from '../../../api/search-ops'
import { getSpu } from '../../../api/product-catalog'
import type { ProductItem, SearchProductDocument } from '../../../types/domain'
import { addToCart } from '../../../store/cart'
import { formatPrice } from '../../../utils/format'
import { toast } from '../../../utils/ui'
import { useRole } from '../../../auth/permission'
import { navigateTo } from '../../../router/navigation'
import { Routes } from '../../../router/routes'

const keyword = ref('')
const loading = ref(false)
const rows = ref<ProductItem[]>([])
const page = ref(1)
const size = ref(10)
const hasMore = ref(true)
const hotKeywords = ref<string[]>([])
const recommendations = ref<string[]>([])

const { isAdmin, isMerchant } = useRole()
const canManage = computed(() => isAdmin.value || isMerchant.value)

function mapSearchDocumentToProduct(item: SearchProductDocument): ProductItem {
  return {
    id: typeof item.productId === 'number' ? item.productId : 0,
    shopId: item.shopId,
    name: item.productName || '未命名商品',
    price: item.price,
    stockQuantity: item.stockQuantity,
    categoryId: item.categoryId,
    brandId: item.brandId,
    status: item.status,
    description: item.description,
    imageUrl: item.imageUrl
  }
}

async function refreshKeywords(seed = ''): Promise<void> {
  const [hotResult, recResult] = await Promise.allSettled([
    listSearchHotKeywordsWithFallback(8),
    listSearchKeywordRecommendationsWithFallback(seed, 10)
  ])
  hotKeywords.value = hotResult.status === 'fulfilled' ? hotResult.value : []
  recommendations.value = recResult.status === 'fulfilled' ? recResult.value : []
}

async function loadProducts(reset = false): Promise<void> {
  if (loading.value) {
    return
  }
  if (reset) {
    page.value = 1
    hasMore.value = true
  }
  loading.value = true
  try {
    const result = await smartSearchProductsWithFallback({
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value,
      sortField: 'score',
      sortOrder: 'desc'
    })
    const items = result.documents.map(mapSearchDocumentToProduct)
    rows.value = reset ? items : rows.value.concat(items)
    hasMore.value = rows.value.length < result.total
    await refreshKeywords(keyword.value)
  } catch (error) {
    toast(error instanceof Error ? error.message : '加载商品失败')
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  void loadProducts(true)
}

function onKeywordSelect(value: string): void {
  keyword.value = value
  void loadProducts(true)
}

function onLoadMore(): void {
  if (!hasMore.value || loading.value) {
    return
  }
  page.value += 1
  void loadProducts()
}

async function onAddToCart(item: ProductItem): Promise<void> {
  if (typeof item.price !== 'number' || item.price <= 0) {
    toast('??????')
    return
  }
  if (typeof item.shopId !== 'number' || item.shopId <= 0) {
    toast('????????')
    return
  }
  if (typeof item.stockQuantity === 'number' && item.stockQuantity <= 0) {
    toast('??????')
    return
  }
  try {
    const spu = await getSpu(item.id)
    const skuId = spu?.skus?.[0]?.skuId
    if (typeof skuId !== 'number') {
      toast('???????')
      return
    }
    addToCart({
      productId: item.id,
      skuId,
      productName: item.name,
      price: item.price,
      shopId: item.shopId
    })
    toast('??????', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '??????')
  }
}

onMounted(() => {
  void loadProducts(true)
  void refreshKeywords('')
})
</script>

<template>
  <AppShell title="Products">
    <view class="panel glass-card">
      <view class="header">
        <text class="section-title">商品目录</text>
        <button v-if="canManage" class="btn-outline" @click="navigateTo(Routes.appCatalogManage, undefined, { requiresAuth: true })">
          管理商品
        </button>
      </view>

      <view class="search-row">
        <input v-model="keyword" class="search-input" placeholder="搜索商品" @confirm="onSearch" />
        <button class="btn-primary" @click="onSearch">搜索</button>
      </view>

      <view class="keyword-block" v-if="hotKeywords.length">
        <text class="keyword-title">热门</text>
        <view class="keyword-list">
          <text v-for="item in hotKeywords" :key="`hot-${item}`" class="keyword-chip" @click="onKeywordSelect(item)">
            {{ item }}
          </text>
        </view>
      </view>

      <view class="keyword-block" v-if="recommendations.length">
        <text class="keyword-title">推荐</text>
        <view class="keyword-list">
          <text v-for="item in recommendations" :key="`rec-${item}`" class="keyword-chip" @click="onKeywordSelect(item)">
            {{ item }}
          </text>
        </view>
      </view>

      <view class="product-list">
        <view v-for="item in rows" :key="item.id" class="product-card">
          <image v-if="item.imageUrl" :src="item.imageUrl" class="product-image" mode="aspectFill" />
          <view v-else class="product-image placeholder">暂无图片</view>
          <view class="product-main">
            <text class="product-name">{{ item.name }}</text>
            <text class="product-meta">{{ formatPrice(item.price) }}</text>
            <text class="product-meta">库存 {{ item.stockQuantity ?? '--' }}</text>
          </view>
          <button class="btn-outline" @click="onAddToCart(item)">加入购物车</button>
        </view>
      </view>

      <view class="load-more">
        <button v-if="hasMore" class="btn-outline" :loading="loading" @click="onLoadMore">加载更多</button>
        <text v-else class="text-muted">没有更多商品</text>
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

.keyword-block {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.keyword-title {
  font-size: 12px;
  color: var(--text-muted);
}

.keyword-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.keyword-chip {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  font-size: 12px;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.product-card {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.product-image {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: #f5f7fb;
}

.product-image.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-muted);
}

.product-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
}

.product-meta {
  font-size: 12px;
  color: var(--text-muted);
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 8px 0 4px;
}
</style>
