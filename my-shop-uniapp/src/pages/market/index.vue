<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  listSearchHotKeywordsWithFallback,
  listSearchKeywordRecommendationsWithFallback,
  listTodayHotSellingProductsWithFallback,
  smartSearchProductsWithFallback
} from '../../api/product'
import { getSpu } from '../../api/product-catalog'
import type { ProductItem, SearchProductDocument } from '../../types/domain'
import { addToCart } from '../../store/cart'
import { isAuthenticated } from '../../auth/session'
import { navigateTo } from '../../router/navigation'
import { Routes } from '../../router/routes'
import { formatPrice } from '../../utils/format'
import { toast } from '../../utils/ui'

const keyword = ref('')
const loading = ref(false)
const rows = ref<ProductItem[]>([])
const page = ref(1)
const size = ref(10)
const hasMore = ref(true)
const hotKeywords = ref<string[]>([])
const recommendations = ref<string[]>([])

const loggedIn = computed(() => isAuthenticated())
const activeKeyword = computed(() => keyword.value.trim())
const resultsTitle = computed(() => (activeKeyword.value ? 'Search Results' : 'Today Hot Selling'))
const resultsHint = computed(() =>
  activeKeyword.value ? `Keyword: ${activeKeyword.value}` : 'Ranked by completed sales today'
)

function mapSearchDocumentToProduct(item: SearchProductDocument): ProductItem {
  return {
    id: typeof item.productId === 'number' ? item.productId : 0,
    shopId: item.shopId,
    name: item.productName || 'Unnamed product',
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
    const result = activeKeyword.value
      ? await smartSearchProductsWithFallback({
          keyword: activeKeyword.value,
          page: page.value,
          size: size.value,
          sortField: 'score',
          sortOrder: 'desc'
        })
      : await listTodayHotSellingProductsWithFallback(page.value, size.value)
    const items = result.documents.map(mapSearchDocumentToProduct)
    rows.value = reset ? items : rows.value.concat(items)
    hasMore.value = rows.value.length < result.total
    await refreshKeywords(activeKeyword.value)
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to load products')
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
    toast('Product price is unavailable')
    return
  }
  if (typeof item.shopId !== 'number' || item.shopId <= 0) {
    toast('Shop information is unavailable')
    return
  }
  if (typeof item.stockQuantity === 'number' && item.stockQuantity <= 0) {
    toast('Product is out of stock')
    return
  }
  try {
    const spu = await getSpu(item.id)
    const skuId = spu?.skus?.[0]?.skuId
    if (typeof skuId !== 'number') {
      toast('Sku information is unavailable')
      return
    }
    addToCart({
      productId: item.id,
      skuId,
      productName: item.name,
      price: item.price,
      shopId: item.shopId
    })
    toast('Added to cart', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Failed to add product to cart')
  }
}

function goLogin(): void {
  navigateTo(Routes.login, { redirect: Routes.appHome })
}

onMounted(() => {
  void loadProducts(true)
  void refreshKeywords('')
})
</script>

<template>
  <view class="page">
    <view class="hero glass-card">
      <view class="hero-main">
        <text class="hero-title">My Shop</text>
        <text class="hero-sub">Mobile storefront</text>
      </view>
      <button v-if="!loggedIn" class="btn-primary" @click="goLogin">Sign in</button>
      <button v-else class="btn-outline" @click="navigateTo(Routes.appHome, undefined, { requiresAuth: true })">
        Open dashboard
      </button>
    </view>

    <view class="search-row glass-card">
      <input v-model="keyword" class="search-input" placeholder="Search products" @confirm="onSearch" />
      <button class="btn-primary" @click="onSearch">Search</button>
    </view>

    <view class="keyword-block" v-if="hotKeywords.length">
      <text class="keyword-title">Trending</text>
      <view class="keyword-list">
        <text v-for="item in hotKeywords" :key="`hot-${item}`" class="keyword-chip" @click="onKeywordSelect(item)">
          {{ item }}
        </text>
      </view>
    </view>

    <view class="keyword-block" v-if="recommendations.length">
      <text class="keyword-title">Recommended</text>
      <view class="keyword-list">
        <text v-for="item in recommendations" :key="`rec-${item}`" class="keyword-chip" @click="onKeywordSelect(item)">
          {{ item }}
        </text>
      </view>
    </view>

    <view class="section-head">
      <text class="section-title">{{ resultsTitle }}</text>
      <text class="section-hint">{{ resultsHint }}</text>
    </view>

    <view class="product-list">
      <view v-for="item in rows" :key="item.id" class="product-card glass-card">
        <image v-if="item.imageUrl" :src="item.imageUrl" class="product-image" mode="aspectFill" />
        <view v-else class="product-image placeholder">No image</view>
        <view class="product-main">
          <text class="product-name">{{ item.name }}</text>
          <text class="product-meta">{{ formatPrice(item.price) }}</text>
          <text class="product-meta">Stock {{ item.stockQuantity ?? '--' }}</text>
        </view>
        <button class="btn-outline" @click="onAddToCart(item)">Add to cart</button>
      </view>
    </view>

    <view class="load-more">
      <button v-if="hasMore" class="btn-outline" :loading="loading" @click="onLoadMore">Load more</button>
      <text v-else class="text-muted">No more products</text>
    </view>
  </view>
</template>

<style scoped>
.page {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hero-title {
  font-size: 18px;
  font-weight: 600;
}

.hero-sub {
  color: var(--text-muted);
  font-size: 12px;
  margin-top: 4px;
  display: block;
}

.search-row {
  padding: 12px;
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

.section-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
}

.section-hint {
  font-size: 12px;
  color: var(--text-muted);
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
  padding: 12px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.product-image {
  width: 72px;
  height: 72px;
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
  padding: 8px 0 20px;
}
</style>
