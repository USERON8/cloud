<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useDebounceFn, useInfiniteScroll } from '@vueuse/core'
import { useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RecycleScroller } from 'vue-virtual-scroller'
import {
  combinedSearchProducts,
  createProduct,
  listProducts,
  listSearchHotKeywordsWithFallback,
  listSearchKeywordRecommendationsWithFallback,
  listSearchSuggestionsWithFallback,
  smartSearchProductsWithFallback,
  updateProduct,
  updateProductStatus
} from '../api/product'
import type { ProductDocument, ProductItem, ProductUpsertPayload, SearchProductDocument } from '../types/domain'
import { useRole } from '../auth/permission'
import { sessionState } from '../auth/session'
import { addToCart } from '../store/cart'
import RichTextEditor from '../components/RichTextEditor.vue'

interface ProductFormModel {
  shopId: number
  name: string
  price: number
  stockQuantity: number
  categoryId: number
  brandId?: number
  status: number
  description: string
  imageUrl: string
}

interface SearchSuggestionItem {
  value: string
}

const route = useRoute()
const loading = ref(false)
const loadingMore = ref(false)
const rows = ref<ProductItem[]>([])
const total = ref(0)
const params = reactive({
  page: 1,
  size: 10,
  name: ''
})
const showAdvanced = ref(false)
const advancedFilters = reactive({
  categoryId: undefined as number | undefined,
  brandId: undefined as number | undefined,
  shopId: undefined as number | undefined,
  minPrice: undefined as number | undefined,
  maxPrice: undefined as number | undefined,
  sortBy: 'hotScore',
  sortOrder: 'desc' as 'asc' | 'desc'
})

const { isAdmin, isMerchant } = useRole()
const isManagementMode = computed(() => Boolean(route.meta.manageProduct))
const canManageProducts = computed(() => isManagementMode.value && (isAdmin.value || isMerchant.value))
const canEnterManagePage = computed(() => isAdmin.value || isMerchant.value)
const isMerchantOnly = computed(() => isMerchant.value && !isAdmin.value)
const currentMerchantId = computed(() =>
  typeof sessionState.user?.id === 'number' ? sessionState.user.id : null
)

const hotKeywords = ref<string[]>([])
const recommendedKeywords = ref<string[]>([])
const blurTimer = ref<number | null>(null)
const infiniteTarget = ref<Window | null>(null)
const hasMore = ref(true)
const productItemSize = 128
const hasAdvancedFilters = computed(() => {
  if (advancedFilters.categoryId || advancedFilters.brandId || advancedFilters.shopId) {
    return true
  }
  if (advancedFilters.minPrice != null || advancedFilters.maxPrice != null) {
    return true
  }
  return advancedFilters.sortBy !== 'hotScore' || advancedFilters.sortOrder !== 'desc'
})

const dialogVisible = ref(false)
const formSubmitting = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<ProductFormModel>({
  shopId: 0,
  name: '',
  price: 0,
  stockQuantity: 0,
  categoryId: 0,
  brandId: undefined,
  status: 1,
  description: '',
  imageUrl: ''
})

const rules: FormRules<ProductFormModel> = {
  shopId: [
    { required: true, message: 'Shop ID is required.', trigger: 'blur' },
    { type: 'number', min: 1, message: 'Shop ID must be greater than 0.', trigger: 'blur' }
  ],
  name: [
    { required: true, message: 'Product name is required.', trigger: 'blur' },
    { min: 1, max: 255, message: 'Name length must be 1-255.', trigger: 'blur' }
  ],
  price: [
    { required: true, message: 'Price is required.', trigger: 'blur' },
    { type: 'number', min: 0.01, message: 'Price must be greater than 0.', trigger: 'blur' }
  ],
  stockQuantity: [
    { required: true, message: 'Stock quantity is required.', trigger: 'blur' },
    { type: 'number', min: 0, message: 'Stock quantity cannot be negative.', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: 'Category ID is required.', trigger: 'blur' },
    { type: 'number', min: 1, message: 'Category ID must be greater than 0.', trigger: 'blur' }
  ],
  status: [{ required: true, message: 'Status is required.', trigger: 'change' }]
}

const dialogTitle = computed(() => (editingId.value ? 'Edit Product' : 'Create Product'))

function mapSearchDocumentToProduct(item: SearchProductDocument): ProductItem {
  return {
    id: typeof item.productId === 'number' ? item.productId : 0,
    shopId: item.shopId,
    name: item.productName || 'Unnamed Product',
    price: item.price,
    stockQuantity: item.stockQuantity,
    categoryId: item.categoryId,
    brandId: item.brandId,
    status: item.status,
    description: item.description,
    imageUrl: item.imageUrl
  }
}

function mapProductDocumentToProduct(item: ProductDocument): ProductItem {
  const price = item.price != null ? Number(item.price) : undefined
  return {
    id: typeof item.productId === 'number' ? item.productId : 0,
    shopId: item.shopId,
    name: item.productName || 'Unnamed Product',
    price: Number.isFinite(price) ? price : undefined,
    stockQuantity: item.stockQuantity,
    categoryId: item.categoryId,
    brandId: item.brandId,
    status: item.status,
    description: item.description,
    imageUrl: item.imageUrl
  }
}

function resetForm(): void {
  editingId.value = null
  form.shopId = isMerchantOnly.value ? (currentMerchantId.value ?? 0) : 0
  form.name = ''
  form.price = 0
  form.stockQuantity = 0
  form.categoryId = 0
  form.brandId = undefined
  form.status = 1
  form.description = ''
  form.imageUrl = ''
}

function canManageRow(row: ProductItem): boolean {
  if (!canManageProducts.value) {
    return false
  }
  if (isAdmin.value) {
    return true
  }
  if (!isMerchantOnly.value || currentMerchantId.value == null) {
    return false
  }
  return row.shopId === currentMerchantId.value
}

function formatPrice(price?: number): string {
  if (typeof price !== 'number') {
    return '--'
  }
  return `CNY ${price.toFixed(2)}`
}

function statusText(status?: number): string {
  if (status === 1) {
    return 'Active'
  }
  if (status === 0) {
    return 'Inactive'
  }
  return 'Unknown'
}

function statusType(status?: number): 'success' | 'info' | 'warning' {
  if (status === 1) {
    return 'success'
  }
  if (status === 0) {
    return 'info'
  }
  return 'warning'
}

async function refreshKeywordRecommendations(seedKeyword = ''): Promise<void> {
  if (isManagementMode.value) {
    return
  }
  const [hotResult, recommendationResult] = await Promise.allSettled([
    listSearchHotKeywordsWithFallback(8),
    listSearchKeywordRecommendationsWithFallback(seedKeyword, 10)
  ])

  hotKeywords.value = hotResult.status === 'fulfilled' ? hotResult.value : []
  recommendedKeywords.value = recommendationResult.status === 'fulfilled' ? recommendationResult.value : []
}

async function loadProducts(append = false): Promise<void> {
  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  try {
    if (isManagementMode.value) {
      const result = await listProducts({
        page: params.page,
        size: params.size,
        name: params.name || undefined
      })
      rows.value = result.records
      total.value = result.total
      hasMore.value = rows.value.length < total.value
    } else {
      if (hasAdvancedFilters.value) {
        const result = await combinedSearchProducts({
          keyword: params.name || undefined,
          categoryId: advancedFilters.categoryId,
          brandId: advancedFilters.brandId,
          shopId: advancedFilters.shopId,
          minPrice: advancedFilters.minPrice,
          maxPrice: advancedFilters.maxPrice,
          sortBy: advancedFilters.sortBy,
          sortOrder: advancedFilters.sortOrder,
          page: Math.max(0, params.page - 1),
          size: params.size
        })
        const items = (result.list || []).map(mapProductDocumentToProduct)
        rows.value = append ? rows.value.concat(items) : items
        total.value = result.total
        hasMore.value = rows.value.length < total.value
      } else {
        const searchResult = await smartSearchProductsWithFallback({
          keyword: params.name || undefined,
          page: params.page,
          size: params.size,
          sortField: 'score',
          sortOrder: 'desc'
        })

        const items = searchResult.documents.map(mapSearchDocumentToProduct)
        rows.value = append ? rows.value.concat(items) : items
        total.value = searchResult.total
        hasMore.value = rows.value.length < total.value
        await refreshKeywordRecommendations(params.name)
      }
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to load products'
    ElMessage.error(message)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore(): Promise<void> {
  if (loading.value || loadingMore.value || !hasMore.value) {
    return
  }
  params.page += 1
  await loadProducts(true)
}

function onSearch(): void {
  params.page = 1
  hasMore.value = true
  void loadProducts()
}

function onAdvancedSearch(): void {
  params.page = 1
  hasMore.value = true
  void loadProducts()
}

function resetAdvancedFilters(): void {
  advancedFilters.categoryId = undefined
  advancedFilters.brandId = undefined
  advancedFilters.shopId = undefined
  advancedFilters.minPrice = undefined
  advancedFilters.maxPrice = undefined
  advancedFilters.sortBy = 'hotScore'
  advancedFilters.sortOrder = 'desc'
  params.page = 1
  hasMore.value = true
  void loadProducts()
}

function onSearchFocus(): void {
  if (blurTimer.value != null) {
    window.clearTimeout(blurTimer.value)
    blurTimer.value = null
  }
  void refreshKeywordRecommendations(params.name)
}

function onSearchBlur(): void {
  blurTimer.value = window.setTimeout(() => {
    blurTimer.value = null
  }, 180)
}

function onKeywordClick(keyword: string): void {
  params.name = keyword
  onSearch()
}

function onSuggestionSelect(item: SearchSuggestionItem): void {
  params.name = item.value
  onSearch()
}

const debouncedFetchSuggestions = useDebounceFn(
  async (queryString: string, callback: (items: SearchSuggestionItem[]) => void) => {
    const normalizedQuery = queryString.trim()
    if (!normalizedQuery) {
      callback(recommendedKeywords.value.map((item) => ({ value: item })))
      return
    }
    try {
      const suggestions = await listSearchSuggestionsWithFallback(normalizedQuery, 10)
      callback(suggestions.map((item) => ({ value: item })))
    } catch {
      callback([])
    }
  },
  300
)

function fetchSuggestions(queryString: string, callback: (items: SearchSuggestionItem[]) => void): void {
  debouncedFetchSuggestions(queryString, callback)
}

function onCreate(): void {
  if (isMerchantOnly.value && currentMerchantId.value == null) {
    ElMessage.error('Current merchant ID is not available in session.')
    return
  }
  resetForm()
  dialogVisible.value = true
  void nextTick(() => {
    formRef.value?.clearValidate()
  })
}

function onEdit(row: ProductItem): void {
  if (!canManageRow(row)) {
    ElMessage.warning('You can only manage products under your own shop.')
    return
  }
  editingId.value = row.id
  form.shopId = isMerchantOnly.value ? (currentMerchantId.value ?? 0) : (row.shopId ?? 0)
  form.name = row.name ?? ''
  form.price = typeof row.price === 'number' ? row.price : 0
  form.stockQuantity = row.stockQuantity ?? 0
  form.categoryId = row.categoryId ?? 0
  form.brandId = row.brandId
  form.status = row.status ?? 1
  form.description = row.description ?? ''
  form.imageUrl = row.imageUrl ?? ''
  dialogVisible.value = true
  void nextTick(() => {
    formRef.value?.clearValidate()
  })
}

async function toggleStatus(row: ProductItem): Promise<void> {
  if (typeof row.id !== 'number') {
    return
  }
  if (!canManageRow(row)) {
    ElMessage.warning('You can only manage products under your own shop.')
    return
  }
  const nextStatus: 0 | 1 = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `Set product "${row.name}" to ${nextStatus === 1 ? 'Active' : 'Inactive'}?`,
      'Confirm',
      { type: 'warning' }
    )
    await updateProductStatus(row.id, nextStatus)
    ElMessage.success(`Product ${row.name} status updated.`)
    await loadProducts()
  } catch (error) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}

function toPayload(): ProductUpsertPayload {
  const shopId = isMerchantOnly.value ? (currentMerchantId.value ?? 0) : form.shopId
  return {
    shopId,
    name: form.name.trim(),
    price: form.price,
    stockQuantity: form.stockQuantity,
    categoryId: form.categoryId,
    brandId: form.brandId,
    status: form.status ?? 1,
    description: form.description.trim() || undefined,
    imageUrl: form.imageUrl.trim() || undefined
  }
}

async function submitForm(): Promise<void> {
  if (formSubmitting.value) {
    return
  }

  if (isMerchantOnly.value && currentMerchantId.value == null) {
    ElMessage.error('Current merchant ID is not available in session.')
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  formSubmitting.value = true
  try {
    const payload = toPayload()

    if (editingId.value) {
      await updateProduct(editingId.value, payload)
      ElMessage.success('Product updated.')
    } else {
      await createProduct(payload)
      ElMessage.success('Product created.')
    }

    dialogVisible.value = false
    await loadProducts()
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Failed to save product'
    ElMessage.error(message)
  } finally {
    formSubmitting.value = false
  }
}

function onAddToCart(row: ProductItem): void {
  if (typeof row.price !== 'number' || row.price <= 0) {
    ElMessage.warning('This product has no valid price.')
    return
  }
  if (typeof row.shopId !== 'number' || row.shopId <= 0) {
    ElMessage.warning('This product is missing shop information.')
    return
  }
  if (typeof row.stockQuantity === 'number' && row.stockQuantity <= 0) {
    ElMessage.warning('This product is out of stock.')
    return
  }
  addToCart({
    productId: row.id,
    productName: row.name,
    price: row.price,
    shopId: row.shopId
  })
  ElMessage.success(`"${row.name}" added to cart.`)
}

function onDialogClosed(): void {
  resetForm()
  formRef.value?.clearValidate()
}

onMounted(() => {
  if (typeof route.query.keyword === 'string' && route.query.keyword.trim()) {
    params.name = route.query.keyword.trim()
  }
  void loadProducts()
  infiniteTarget.value = window
})

watch(
  () => route.query.keyword,
  (value) => {
    if (typeof value === 'string' && value.trim() !== params.name) {
      params.name = value.trim()
      params.page = 1
      hasMore.value = true
      void loadProducts()
    }
  }
)

onBeforeUnmount(() => {
  if (blurTimer.value != null) {
    window.clearTimeout(blurTimer.value)
  }
})

useInfiniteScroll(
  infiniteTarget,
  async () => {
    if (isManagementMode.value) {
      return
    }
    await loadMore()
  },
  { distance: 160 }
)
</script>

<template>
  <section class="glass-card panel">
    <div class="header">
      <h3>
        <span class="title-wrap">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 7.5 12 4l8 3.5-8 3.5L4 7.5Zm0 3.5 8 3.5 8-3.5M4 14.5 12 18l8-3.5" />
          </svg>
          {{ isManagementMode ? 'Product Management' : 'Product Catalog' }}
        </span>
      </h3>
      <div class="search-row">
        <el-autocomplete
          v-model="params.name"
          clearable
          placeholder="Search products, categories, or brands"
          :fetch-suggestions="fetchSuggestions"
          @focus="onSearchFocus"
          @blur="onSearchBlur"
          @select="onSuggestionSelect"
          @keyup.enter="onSearch"
        />
        <el-button round type="primary" @click="onSearch">
          <span class="btn-wrap">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14Zm5.4 12.4L20 20" /></svg>
            Search
          </span>
        </el-button>
        <el-button v-if="!isManagementMode" round @click="showAdvanced = !showAdvanced">
          {{ showAdvanced ? 'Hide Filters' : 'Advanced Filters' }}
        </el-button>
        <router-link v-if="canEnterManagePage && !isManagementMode" class="inline-link" to="/app/catalog/manage">
          <el-button round type="success">Open Management</el-button>
        </router-link>
        <router-link v-if="isManagementMode" class="inline-link" to="/app/catalog">
          <el-button round>Back to Catalog</el-button>
        </router-link>
        <el-button v-if="canManageProducts" round type="success" @click="onCreate">New Product</el-button>
      </div>
    </div>

    <div v-if="!isManagementMode && showAdvanced" class="advanced-panel">
      <div class="advanced-grid">
        <el-input-number v-model="advancedFilters.categoryId" :min="1" controls-position="right" placeholder="Category ID" />
        <el-input-number v-model="advancedFilters.brandId" :min="1" controls-position="right" placeholder="Brand ID" />
        <el-input-number v-model="advancedFilters.shopId" :min="1" controls-position="right" placeholder="Shop ID" />
        <el-input-number
          v-model="advancedFilters.minPrice"
          :min="0"
          :step="0.01"
          :precision="2"
          controls-position="right"
          placeholder="Min Price"
        />
        <el-input-number
          v-model="advancedFilters.maxPrice"
          :min="0"
          :step="0.01"
          :precision="2"
          controls-position="right"
          placeholder="Max Price"
        />
        <el-select v-model="advancedFilters.sortBy" placeholder="Sort By">
          <el-option value="hotScore" label="Hot Score" />
          <el-option value="price" label="Price" />
          <el-option value="salesCount" label="Sales Count" />
          <el-option value="rating" label="Rating" />
          <el-option value="createdAt" label="Created At" />
        </el-select>
        <el-select v-model="advancedFilters.sortOrder" placeholder="Sort Order">
          <el-option value="desc" label="Desc" />
          <el-option value="asc" label="Asc" />
        </el-select>
      </div>
      <div class="advanced-actions">
        <el-button round type="primary" @click="onAdvancedSearch">Apply Filters</el-button>
        <el-button round @click="resetAdvancedFilters">Reset</el-button>
      </div>
    </div>

    <div v-if="!isManagementMode" class="keyword-board">
      <div class="keyword-block">
        <span class="keyword-title">Trending</span>
        <div class="keyword-list">
          <el-tag
            v-for="keyword in hotKeywords"
            :key="`hot-${keyword}`"
            class="keyword-chip"
            effect="light"
            @click="onKeywordClick(keyword)"
          >
            {{ keyword }}
          </el-tag>
        </div>
      </div>
      <div class="keyword-block">
        <span class="keyword-title">Recommended</span>
        <div class="keyword-list">
          <el-tag
            v-for="keyword in recommendedKeywords"
            :key="`rec-${keyword}`"
            class="keyword-chip"
            type="success"
            effect="plain"
            @click="onKeywordClick(keyword)"
          >
            {{ keyword }}
          </el-tag>
        </div>
      </div>
    </div>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <div class="table-skeleton">
          <el-skeleton-item v-for="index in 6" :key="index" class="skeleton-row" variant="rect" />
        </div>
      </template>
      <template #default>
        <el-table v-if="isManagementMode" :data="rows" stripe>
          <el-table-column label="ID" prop="id" width="90" />
          <el-table-column label="Name" prop="name" min-width="220" />
          <el-table-column label="Price" min-width="120">
            <template #default="scope">
              {{ formatPrice(scope.row.price) }}
            </template>
          </el-table-column>
          <el-table-column label="Stock" min-width="100" prop="stockQuantity" />
          <el-table-column label="Status" min-width="120">
            <template #default="scope">
              <el-tag :type="statusType(scope.row.status)" round>
                {{ statusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column v-if="!isManagementMode" label="Actions" min-width="160">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === 1"
                round
                size="small"
                type="primary"
                plain
                @click="onAddToCart(scope.row)"
              >
                <span class="btn-wrap">
                  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 2H3L2 11h18l-1-9h-3M6 2l2 9h8l2-9M9 19a2 2 0 1 0 0-4 2 2 0 1 0 0-4Zm7 0a2 2 0 1 0 0-4 2 2 0 1 0 0-4Z" /></svg>
                  Add to Cart
                </span>
              </el-button>
              <span v-else class="out-of-sale">Unavailable</span>
            </template>
          </el-table-column>

          <el-table-column v-if="canManageProducts" label="Actions" min-width="220">
            <template #default="scope">
              <el-button v-if="canManageRow(scope.row)" round size="small" @click="onEdit(scope.row)">Edit</el-button>
              <el-button v-if="canManageRow(scope.row)" round size="small" @click="toggleStatus(scope.row)">
                {{ scope.row.status === 1 ? 'Disable' : 'Enable' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <RecycleScroller
          v-else
          class="product-scroller"
          :items="rows"
          :item-size="productItemSize"
          key-field="id"
          page-mode
        >
          <template #default="{ item }">
            <article class="product-card">
              <div class="product-media">
                <img
                  v-if="item.imageUrl"
                  class="product-image"
                  v-lazy="item.imageUrl"
                  :alt="item.name || 'Product image'"
                  loading="lazy"
                />
                <div v-else class="product-image placeholder">No Image</div>
              </div>
              <div class="product-main">
                <div class="product-title">{{ item.name }}</div>
                <div class="product-meta">
                  <span class="price">{{ formatPrice(item.price) }}</span>
                  <span class="stock">Stock {{ item.stockQuantity ?? '--' }}</span>
                  <el-tag :type="statusType(item.status)" round>
                    {{ statusText(item.status) }}
                  </el-tag>
                </div>
              </div>
              <div class="product-actions">
                <el-button
                  v-if="item.status === 1"
                  round
                  size="small"
                  type="primary"
                  plain
                  @click="onAddToCart(item)"
                >
                  <span class="btn-wrap">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M6 2H3L2 11h18l-1-9h-3M6 2l2 9h8l2-9M9 19a2 2 0 1 0 0-4 2 2 0 1 0 0-4Zm7 0a2 2 0 1 0 0-4 2 2 0 1 0 0-4Z" />
                    </svg>
                    Add to Cart
                  </span>
                </el-button>
                <span v-else class="out-of-sale">Unavailable</span>
              </div>
            </article>
          </template>
        </RecycleScroller>
      </template>
    </el-skeleton>

    <div v-if="isManagementMode" class="pager">
      <el-pagination
        v-model:current-page="params.page"
        v-model:page-size="params.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        background
        layout="total, sizes, prev, pager, next"
        @current-change="loadProducts"
        @size-change="loadProducts"
      />
    </div>

    <div v-else class="infinite-hint">
      <el-button v-if="hasMore" :loading="loadingMore" round @click="loadMore">Load More</el-button>
      <span v-else class="muted">No more products.</span>
    </div>
  </section>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" @closed="onDialogClosed">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid">
        <el-form-item label="Shop ID" prop="shopId">
          <el-input-number v-model="form.shopId" :disabled="isMerchantOnly" :min="1" :step="1" controls-position="right" />
          <div v-if="isMerchantOnly" class="field-hint">Bound to current merchant account.</div>
        </el-form-item>

        <el-form-item label="Category ID" prop="categoryId">
          <el-input-number v-model="form.categoryId" :min="1" :step="1" controls-position="right" />
        </el-form-item>

        <el-form-item label="Brand ID (Optional)">
          <el-input-number v-model="form.brandId" :min="1" :step="1" controls-position="right" />
        </el-form-item>

        <el-form-item label="Status" prop="status">
          <el-select v-model="form.status">
            <el-option :value="1" label="Active" />
            <el-option :value="0" label="Inactive" />
          </el-select>
        </el-form-item>

        <el-form-item class="span-2" label="Product Name" prop="name">
          <el-input v-model="form.name" maxlength="255" />
        </el-form-item>

        <el-form-item label="Price" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" controls-position="right" />
        </el-form-item>

        <el-form-item label="Stock Quantity" prop="stockQuantity">
          <el-input-number v-model="form.stockQuantity" :min="0" :step="1" controls-position="right" />
        </el-form-item>

        <el-form-item class="span-2" label="Image URL (Optional)">
          <el-input v-model="form.imageUrl" />
        </el-form-item>

        <el-form-item class="span-2" label="Description (Optional)">
          <RichTextEditor v-model="form.description" :min-height="160" />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <el-button round @click="dialogVisible = false">Cancel</el-button>
      <el-button :loading="formSubmitting" round type="primary" @click="submitForm">Save</el-button>
    </template>
  </el-dialog>
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

.search-row {
  display: flex;
  gap: 8px;
  width: min(880px, 100%);
  flex-wrap: wrap;
  align-items: center;
}

.search-row :deep(.el-autocomplete) {
  flex: 1;
}

.advanced-panel {
  margin: 10px 0 12px;
  padding: 10px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.7);
  display: grid;
  gap: 8px;
}

.advanced-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 8px;
}

.advanced-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.keyword-board {
  margin-bottom: 12px;
  display: grid;
  gap: 10px;
}

.keyword-block {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.keyword-title {
  color: var(--text-muted);
  min-width: 88px;
  font-size: 0.82rem;
}

.keyword-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.keyword-chip {
  cursor: pointer;
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

.inline-link {
  text-decoration: none;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.span-2 {
  grid-column: span 2;
}

.field-hint {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 0.75rem;
}

.out-of-sale {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.product-scroller {
  display: grid;
  gap: 10px;
}

.product-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  align-items: center;
}

.product-media {
  width: 86px;
  height: 86px;
  border-radius: 14px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.25);
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  font-size: 0.72rem;
  color: var(--text-muted);
}

.product-image.placeholder {
  display: grid;
  place-items: center;
  font-size: 0.72rem;
  color: var(--text-muted);
}

.product-main {
  display: grid;
  gap: 6px;
}

.product-title {
  font-size: 1rem;
  font-weight: 600;
}

.product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: var(--text-muted);
  font-size: 0.85rem;
}

.price {
  color: #0f172a;
  font-weight: 600;
}

.stock {
  padding: 2px 8px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
}

.product-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-skeleton {
  display: grid;
  gap: 8px;
}

.skeleton-row {
  height: 42px;
  border-radius: 10px;
}

@media (max-width: 900px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-row {
    width: 100%;
    flex-wrap: wrap;
  }

  .advanced-grid {
    grid-template-columns: 1fr;
  }

  .keyword-title {
    min-width: auto;
  }

  .pager {
    justify-content: center;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }
}
</style>
