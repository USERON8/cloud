<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRole } from '../auth/permission'
import { getGitHubAuthStatus, getGitHubUserInfo, logoutAllSessions } from '../api/auth'
import {
  addTokenToBlacklist,
  checkBlacklist,
  cleanupBlacklist,
  cleanupExpiredTokens,
  getAuthorizationDetails,
  getBlacklistStats,
  getStorageStructure,
  getTokenStats,
  revokeAuthorization
} from '../api/auth-tokens'
import { getThreadPoolDetail, getThreadPools } from '../api/thread-pool'
import { getCategoryChildren, getCategoryTree } from '../api/category'
import { createSpu, getSpu, listSkuByIds, listSpuByCategory, updateSpu, updateSpuStatus } from '../api/product-catalog'
import {
  applyAfterSale,
  advanceAfterSaleStatus,
  advanceSubOrderStatus,
  createMainOrder,
  getMainOrder,
  listSubOrders
} from '../api/order-legacy'
import {
  advancedSearch,
  basicSearch,
  complexSearch,
  filterByBrand,
  filterByCategory,
  filterByPrice,
  filterByShop,
  filterSearch,
  getProductFilters,
  listHotProducts,
  listNewProducts,
  listRecommendedProducts,
  searchByCategory,
  searchByShop,
  searchProducts
} from '../api/search-ops'
import {
  getShopById,
  getShopFilters,
  listHotShops,
  listRecommendedShops,
  listShopSuggestions,
  searchShops,
  searchShopsByLocation,
  type ShopSearchRequest
} from '../api/shop-search'
import { combinedSearchProducts, smartSearchProducts } from '../api/product'
import type {
  LegacyAfterSale,
  LegacyCreateMainOrderRequest,
  LegacyOrderAggregate,
  LegacyOrderSub,
  ProductFilterRequest,
  ProductSearchRequest,
  SpuCreateRequest,
  SpuDetail,
  ThreadPoolInfo,
  TokenBlacklistStats
} from '../types/domain'

const activeTab = ref('tokens')
const { isAdmin, isMerchant } = useRole()
const canCatalogOps = computed(() => isAdmin.value || isMerchant.value)

function formatJson(value: unknown): string {
  return JSON.stringify(value, null, 2)
}

function parseJson<T>(raw: string, label: string): T | null {
  if (!raw.trim()) {
    ElMessage.warning(`${label} JSON is empty.`)
    return null
  }
  try {
    return JSON.parse(raw) as T
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Invalid JSON'
    ElMessage.error(`${label} JSON error: ${message}`)
    return null
  }
}

// Auth & token management
const tokenStats = ref<Record<string, unknown> | null>(null)
const storageStructure = ref<Record<string, unknown> | null>(null)
const authorizationId = ref('')
const authorizationDetail = ref<Record<string, unknown> | null>(null)
const cleanupResult = ref<Record<string, unknown> | null>(null)
const blacklistStats = ref<TokenBlacklistStats | null>(null)
const blacklistToken = ref('')
const blacklistReason = ref('')
const blacklistCheckResult = ref<Record<string, unknown> | null>(null)
const blacklistCleanupResult = ref<Record<string, unknown> | null>(null)
const logoutUsername = ref('')
const gitHubStatus = ref<boolean | null>(null)
const gitHubUserInfo = ref<Record<string, unknown> | null>(null)

async function loadTokenStats(): Promise<void> {
  try {
    tokenStats.value = await getTokenStats()
    ElMessage.success('Token stats loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load token stats')
  }
}

async function loadAuthorizationDetail(): Promise<void> {
  if (!authorizationId.value.trim()) {
    ElMessage.warning('Provide authorization id.')
    return
  }
  try {
    authorizationDetail.value = await getAuthorizationDetails(authorizationId.value.trim())
    ElMessage.success('Authorization detail loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load authorization detail')
  }
}

async function revokeAuthorizationRow(): Promise<void> {
  if (!authorizationId.value.trim()) {
    ElMessage.warning('Provide authorization id.')
    return
  }
  try {
    await ElMessageBox.confirm('Revoke this authorization?', 'Confirm', { type: 'warning' })
    await revokeAuthorization(authorizationId.value.trim())
    ElMessage.success('Authorization revoked.')
  } catch (error) {
    if (error instanceof Error && error.message === 'cancel') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : 'Failed to revoke authorization')
  }
}

async function runTokenCleanup(): Promise<void> {
  try {
    cleanupResult.value = await cleanupExpiredTokens()
    ElMessage.success('Cleanup triggered.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to cleanup tokens')
  }
}

async function loadStorageStructure(): Promise<void> {
  try {
    storageStructure.value = await getStorageStructure()
    ElMessage.success('Storage structure loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load storage structure')
  }
}

async function loadBlacklistStats(): Promise<void> {
  try {
    blacklistStats.value = await getBlacklistStats()
    ElMessage.success('Blacklist stats loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load blacklist stats')
  }
}

async function addBlacklist(): Promise<void> {
  if (!blacklistToken.value.trim()) {
    ElMessage.warning('Provide token value.')
    return
  }
  try {
    await addTokenToBlacklist(blacklistToken.value.trim(), blacklistReason.value.trim() || undefined)
    ElMessage.success('Token added to blacklist.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to add token to blacklist')
  }
}

async function checkBlacklistStatus(): Promise<void> {
  if (!blacklistToken.value.trim()) {
    ElMessage.warning('Provide token value.')
    return
  }
  try {
    blacklistCheckResult.value = await checkBlacklist(blacklistToken.value.trim())
    ElMessage.success('Blacklist status checked.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to check blacklist')
  }
}

async function cleanupBlacklistEntries(): Promise<void> {
  try {
    blacklistCleanupResult.value = await cleanupBlacklist()
    ElMessage.success('Blacklist cleanup triggered.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to cleanup blacklist')
  }
}

async function logoutAllUserSessions(): Promise<void> {
  if (!logoutUsername.value.trim()) {
    ElMessage.warning('Provide username.')
    return
  }
  try {
    const message = await logoutAllSessions(logoutUsername.value.trim())
    ElMessage.success(message || 'User sessions revoked.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to revoke sessions')
  }
}

async function loadGitHubStatus(): Promise<void> {
  try {
    gitHubStatus.value = await getGitHubAuthStatus()
    ElMessage.success('GitHub status loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load GitHub status')
  }
}

async function loadGitHubUser(): Promise<void> {
  try {
    gitHubUserInfo.value = (await getGitHubUserInfo()) as Record<string, unknown>
    ElMessage.success('GitHub user info loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load GitHub user info')
  }
}

// Thread pool monitor
const threadPoolRows = ref<ThreadPoolInfo[]>([])
const threadPoolName = ref('')
const threadPoolDetail = ref<ThreadPoolInfo | null>(null)
const threadPoolLoading = ref(false)

async function loadThreadPools(): Promise<void> {
  threadPoolLoading.value = true
  try {
    threadPoolRows.value = await getThreadPools()
    ElMessage.success('Thread pools loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load thread pools')
  } finally {
    threadPoolLoading.value = false
  }
}

async function loadThreadPoolDetail(): Promise<void> {
  if (!threadPoolName.value.trim()) {
    ElMessage.warning('Provide thread pool name.')
    return
  }
  try {
    threadPoolDetail.value = await getThreadPoolDetail(threadPoolName.value.trim())
    ElMessage.success('Thread pool detail loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load thread pool detail')
  }
}

// SPU catalog
const spuForm = reactive({
  spuId: undefined as number | undefined,
  spuName: '',
  subtitle: '',
  categoryId: undefined as number | undefined,
  brandId: undefined as number | undefined,
  merchantId: undefined as number | undefined,
  status: 1,
  description: '',
  mainImage: ''
})
const skuJson = ref(
  '[\n  {\n    "skuCode": "SKU-001",\n    "skuName": "Default SKU",\n    "salePrice": 0.01,\n    "status": 1\n  }\n]'
)
const spuDetail = ref<SpuDetail | null>(null)
const categorySpuResult = ref<SpuDetail[]>([])
const categoryIdQuery = ref<number | undefined>(undefined)
const categoryStatusQuery = ref<number | undefined>(undefined)
const skuIdsInput = ref('')
const skuList = ref([])
const spuStatusId = ref<number | undefined>(undefined)
const spuStatusValue = ref<number | undefined>(undefined)
const categoryTree = ref([])
const categoryChildren = ref([])
const categoryTreeEnabledOnly = ref(false)
const categoryParentId = ref<number | undefined>(undefined)

function buildSpuPayload(): SpuCreateRequest | null {
  if (!spuForm.spuName || !spuForm.categoryId || !spuForm.merchantId) {
    ElMessage.warning('SPU name, category ID, and merchant ID are required.')
    return null
  }
  const skus = parseJson<unknown[]>(skuJson.value, 'SKU list')
  if (!skus) {
    return null
  }
  return {
    spu: {
      spuId: spuForm.spuId,
      spuName: spuForm.spuName,
      subtitle: spuForm.subtitle || undefined,
      categoryId: spuForm.categoryId,
      brandId: spuForm.brandId,
      merchantId: spuForm.merchantId,
      status: spuForm.status,
      description: spuForm.description || undefined,
      mainImage: spuForm.mainImage || undefined
    },
    skus: skus as SpuCreateRequest['skus']
  }
}

async function createSpuRow(): Promise<void> {
  const payload = buildSpuPayload()
  if (!payload) return
  try {
    const id = await createSpu(payload)
    ElMessage.success(`SPU created: ${id}`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to create SPU')
  }
}

async function updateSpuRow(): Promise<void> {
  if (!spuForm.spuId) {
    ElMessage.warning('Provide SPU ID for update.')
    return
  }
  const payload = buildSpuPayload()
  if (!payload) return
  try {
    await updateSpu(spuForm.spuId, payload)
    ElMessage.success('SPU updated.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to update SPU')
  }
}

async function loadSpuDetail(): Promise<void> {
  if (!spuForm.spuId) {
    ElMessage.warning('Provide SPU ID.')
    return
  }
  try {
    const detail = await getSpu(spuForm.spuId)
    spuDetail.value = detail
    if (detail) {
      spuForm.spuName = detail.spuName || ''
      spuForm.subtitle = detail.subtitle || ''
      spuForm.categoryId = detail.categoryId
      spuForm.brandId = detail.brandId
      spuForm.merchantId = detail.merchantId
      spuForm.status = detail.status ?? 1
      spuForm.description = detail.description || ''
      spuForm.mainImage = detail.mainImage || ''
      if (detail.skus && detail.skus.length > 0) {
        skuJson.value = JSON.stringify(detail.skus, null, 2)
      }
    }
    ElMessage.success('SPU detail loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load SPU detail')
  }
}

async function loadSpuByCategory(): Promise<void> {
  if (!categoryIdQuery.value) {
    ElMessage.warning('Provide category ID.')
    return
  }
  try {
    categorySpuResult.value = await listSpuByCategory(categoryIdQuery.value, categoryStatusQuery.value)
    ElMessage.success('Category SPUs loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load category SPUs')
  }
}

async function loadSkuBatch(): Promise<void> {
  const ids = skuIdsInput.value
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isFinite(value) && value > 0)
  if (ids.length === 0) {
    ElMessage.warning('Provide SKU IDs separated by comma.')
    return
  }
  try {
    skuList.value = await listSkuByIds(ids)
    ElMessage.success('SKU batch loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load SKU batch')
  }
}

async function updateSpuStatusRow(): Promise<void> {
  if (!spuStatusId.value || spuStatusValue.value == null) {
    ElMessage.warning('Provide SPU ID and status.')
    return
  }
  try {
    await updateSpuStatus(spuStatusId.value, spuStatusValue.value)
    ElMessage.success('SPU status updated.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to update SPU status')
  }
}

async function loadCategoryTree(): Promise<void> {
  try {
    categoryTree.value = await getCategoryTree(categoryTreeEnabledOnly.value)
    ElMessage.success('Category tree loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load category tree')
  }
}

async function loadCategoryChildren(): Promise<void> {
  if (!categoryParentId.value) {
    ElMessage.warning('Provide category parent ID.')
    return
  }
  try {
    categoryChildren.value = await getCategoryChildren(categoryParentId.value, categoryTreeEnabledOnly.value)
    ElMessage.success('Category children loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load category children')
  }
}

// Legacy orders
const orderPayloadRaw = ref(`{
  "userId": 1,
  "totalAmount": 0.01,
  "payableAmount": 0.01,
  "remark": "Manual order",
  "subOrders": [
    {
      "merchantId": 1,
      "itemAmount": 0.01,
      "shippingFee": 0,
      "discountAmount": 0,
      "payableAmount": 0.01,
      "receiverName": "Receiver",
      "receiverPhone": "13800000000",
      "receiverAddress": "Address line",
      "items": [
        {
          "spuId": 1,
          "skuId": 1,
          "quantity": 1,
          "unitPrice": 0.01,
          "totalPrice": 0.01
        }
      ]
    }
  ]
}`)
const idempotencyKey = ref('')
const legacyOrderResult = ref<LegacyOrderAggregate | null>(null)
const mainOrderIdLookup = ref<number | undefined>(undefined)
const subOrdersResult = ref<LegacyOrderSub[]>([])
const subOrderIdAction = ref<number | undefined>(undefined)
const subOrderAction = ref('')
const afterSalePayloadRaw = ref(`{
  "mainOrderId": 1,
  "subOrderId": 1,
  "userId": 1,
  "merchantId": 1,
  "afterSaleType": "refund",
  "reason": "damaged",
  "description": "Item damaged",
  "applyAmount": 0.01
}`)
const afterSaleResult = ref<LegacyAfterSale | null>(null)
const afterSaleIdAction = ref<number | undefined>(undefined)
const afterSaleAction = ref('')
const afterSaleRemark = ref('')

function generateIdempotencyKey(): void {
  idempotencyKey.value = `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

async function submitMainOrder(): Promise<void> {
  const payload = parseJson<LegacyCreateMainOrderRequest>(orderPayloadRaw.value, 'Order payload')
  if (!payload) return
  if (!idempotencyKey.value.trim()) {
    ElMessage.warning('Provide Idempotency-Key.')
    return
  }
  try {
    legacyOrderResult.value = await createMainOrder(payload, idempotencyKey.value.trim())
    ElMessage.success('Main order created.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to create main order')
  }
}

async function loadMainOrder(): Promise<void> {
  if (!mainOrderIdLookup.value) {
    ElMessage.warning('Provide main order ID.')
    return
  }
  try {
    legacyOrderResult.value = await getMainOrder(mainOrderIdLookup.value)
    ElMessage.success('Main order loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load main order')
  }
}

async function loadSubOrders(): Promise<void> {
  if (!mainOrderIdLookup.value) {
    ElMessage.warning('Provide main order ID.')
    return
  }
  try {
    subOrdersResult.value = await listSubOrders(mainOrderIdLookup.value)
    ElMessage.success('Sub orders loaded.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to load sub orders')
  }
}

async function advanceSubOrder(): Promise<void> {
  if (!subOrderIdAction.value || !subOrderAction.value.trim()) {
    ElMessage.warning('Provide sub order ID and action.')
    return
  }
  try {
    const updated = await advanceSubOrderStatus(subOrderIdAction.value, subOrderAction.value.trim())
    subOrdersResult.value = [updated]
    ElMessage.success('Sub order advanced.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to advance sub order')
  }
}

async function submitAfterSale(): Promise<void> {
  const payload = parseJson<LegacyAfterSale>(afterSalePayloadRaw.value, 'After-sale payload')
  if (!payload) return
  try {
    afterSaleResult.value = await applyAfterSale(payload)
    ElMessage.success('After-sale applied.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to apply after-sale')
  }
}

async function advanceAfterSale(): Promise<void> {
  if (!afterSaleIdAction.value || !afterSaleAction.value.trim()) {
    ElMessage.warning('Provide after-sale ID and action.')
    return
  }
  try {
    afterSaleResult.value = await advanceAfterSaleStatus(
      afterSaleIdAction.value,
      afterSaleAction.value.trim(),
      afterSaleRemark.value.trim() || undefined
    )
    ElMessage.success('After-sale advanced.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Failed to advance after-sale')
  }
}

// Search lab
type SearchEndpointKey =
  | 'complex-search'
  | 'filters'
  | 'basic'
  | 'search'
  | 'search-category'
  | 'search-shop'
  | 'search-advanced'
  | 'smart-search'
  | 'recommended'
  | 'new'
  | 'hot'
  | 'filter'
  | 'filter-category'
  | 'filter-brand'
  | 'filter-price'
  | 'filter-shop'
  | 'filter-combined'
  | 'shop-complex'
  | 'shop-filters'
  | 'shop-suggestions'
  | 'shop-hot'
  | 'shop-by-id'
  | 'shop-recommended'
  | 'shop-by-location'

const searchEndpoint = ref<SearchEndpointKey>('complex-search')
const searchParamsRaw = ref('{}')
const searchResult = ref<unknown>(null)

const searchSamples: Record<SearchEndpointKey, Record<string, unknown>> = {
  'complex-search': { keyword: 'phone', page: 0, size: 10, sortBy: 'hotScore', sortOrder: 'desc' },
  filters: { keyword: 'phone', page: 0, size: 10 },
  basic: { keyword: 'phone', page: 0, size: 10 },
  search: { keyword: 'phone', page: 0, size: 10, sortBy: 'hotScore', sortDir: 'desc' },
  'search-category': { categoryId: 1, keyword: 'phone', page: 0, size: 10 },
  'search-shop': { shopId: 1, keyword: 'phone', page: 0, size: 10 },
  'search-advanced': { keyword: 'phone', minPrice: 0, maxPrice: 100, page: 0, size: 10 },
  'smart-search': { keyword: 'phone', page: 1, size: 10, sortField: 'score', sortOrder: 'desc' },
  recommended: { page: 0, size: 10 },
  new: { page: 0, size: 10 },
  hot: { page: 0, size: 10 },
  filter: { keyword: 'phone', minPrice: 0, maxPrice: 100, page: 0, size: 10 },
  'filter-category': { categoryId: 1, page: 0, size: 10 },
  'filter-brand': { brandId: 1, page: 0, size: 10 },
  'filter-price': { minPrice: 0, maxPrice: 100, page: 0, size: 10 },
  'filter-shop': { shopId: 1, page: 0, size: 10 },
  'filter-combined': { keyword: 'phone', categoryId: 1, minPrice: 0, maxPrice: 100, page: 0, size: 10 },
  'shop-complex': { keyword: 'store', page: 0, size: 10, sortBy: 'hotScore', sortOrder: 'desc' },
  'shop-filters': { keyword: 'store', page: 0, size: 10 },
  'shop-suggestions': { keyword: 'store', size: 10 },
  'shop-hot': { size: 10 },
  'shop-by-id': { shopId: 1 },
  'shop-recommended': { page: 0, size: 10 },
  'shop-by-location': { location: 'Downtown', page: 0, size: 10 }
}

watch(
  () => searchEndpoint.value,
  () => {
    searchParamsRaw.value = JSON.stringify(searchSamples[searchEndpoint.value], null, 2)
  },
  { immediate: true }
)

async function runSearch(): Promise<void> {
  const params = parseJson<Record<string, unknown>>(searchParamsRaw.value, 'Search params')
  if (!params) return
  try {
    switch (searchEndpoint.value) {
      case 'complex-search':
        searchResult.value = await complexSearch(params as ProductSearchRequest)
        break
      case 'filters':
        searchResult.value = await getProductFilters(params as ProductSearchRequest)
        break
      case 'basic':
        searchResult.value = await basicSearch(params as { keyword?: string; page?: number; size?: number })
        break
      case 'search':
        searchResult.value = await searchProducts(params as { keyword: string; page?: number; size?: number; sortBy?: string; sortDir?: string })
        break
      case 'search-category': {
        const categoryId = Number(params.categoryId)
        if (!categoryId) throw new Error('categoryId is required')
        const { categoryId: _, ...rest } = params
        searchResult.value = await searchByCategory(categoryId, rest as { keyword?: string; page?: number; size?: number })
        break
      }
      case 'search-shop': {
        const shopId = Number(params.shopId)
        if (!shopId) throw new Error('shopId is required')
        const { shopId: _, ...rest } = params
        searchResult.value = await searchByShop(shopId, rest as { keyword?: string; page?: number; size?: number })
        break
      }
      case 'search-advanced':
        searchResult.value = await advancedSearch(params as {
          keyword: string
          minPrice?: number
          maxPrice?: number
          page?: number
          size?: number
        })
        break
      case 'smart-search':
        searchResult.value = await smartSearchProducts(params as {
          keyword?: string
          page?: number
          size?: number
          sortField?: string
          sortOrder?: 'asc' | 'desc'
        })
        break
      case 'recommended':
        searchResult.value = await listRecommendedProducts(Number(params.page ?? 0), Number(params.size ?? 20))
        break
      case 'new':
        searchResult.value = await listNewProducts(Number(params.page ?? 0), Number(params.size ?? 20))
        break
      case 'hot':
        searchResult.value = await listHotProducts(Number(params.page ?? 0), Number(params.size ?? 20))
        break
      case 'filter':
        searchResult.value = await filterSearch(params as ProductFilterRequest)
        break
      case 'filter-category': {
        const categoryId = Number(params.categoryId)
        if (!categoryId) throw new Error('categoryId is required')
        const { categoryId: _, ...rest } = params
        searchResult.value = await filterByCategory(categoryId, rest as { page?: number; size?: number })
        break
      }
      case 'filter-brand': {
        const brandId = Number(params.brandId)
        if (!brandId) throw new Error('brandId is required')
        const { brandId: _, ...rest } = params
        searchResult.value = await filterByBrand(brandId, rest as { page?: number; size?: number })
        break
      }
      case 'filter-price':
        searchResult.value = await filterByPrice(params as { minPrice?: number; maxPrice?: number; page?: number; size?: number })
        break
      case 'filter-shop': {
        const shopId = Number(params.shopId)
        if (!shopId) throw new Error('shopId is required')
        const { shopId: _, ...rest } = params
        searchResult.value = await filterByShop(shopId, rest as { page?: number; size?: number })
        break
      }
      case 'filter-combined':
        searchResult.value = await combinedSearchProducts(params as {
          keyword?: string
          categoryId?: number
          brandId?: number
          minPrice?: number
          maxPrice?: number
          shopId?: number
          sortBy?: string
          sortOrder?: 'asc' | 'desc'
          page?: number
          size?: number
        })
        break
      case 'shop-complex':
        searchResult.value = await searchShops(params as ShopSearchRequest)
        break
      case 'shop-filters':
        searchResult.value = await getShopFilters(params as ShopSearchRequest)
        break
      case 'shop-suggestions': {
        const keyword = String(params.keyword || '').trim()
        if (!keyword) throw new Error('keyword is required')
        searchResult.value = await listShopSuggestions(keyword, Number(params.size ?? 10))
        break
      }
      case 'shop-hot':
        searchResult.value = await listHotShops(Number(params.size ?? 10))
        break
      case 'shop-by-id': {
        const shopId = Number(params.shopId)
        if (!shopId) throw new Error('shopId is required')
        searchResult.value = await getShopById(shopId)
        break
      }
      case 'shop-recommended':
        searchResult.value = await listRecommendedShops(Number(params.page ?? 0), Number(params.size ?? 20))
        break
      case 'shop-by-location': {
        const location = String(params.location || '').trim()
        if (!location) throw new Error('location is required')
        searchResult.value = await searchShopsByLocation(location, Number(params.page ?? 0), Number(params.size ?? 20))
        break
      }
      default:
        throw new Error('Unsupported endpoint')
    }
    ElMessage.success('Search executed.')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Search failed')
  }
}
</script>

<template>
  <section class="glass-card panel">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="Auth & Tokens" name="tokens">
        <el-alert v-if="!isAdmin" type="warning" title="Admin access required to manage tokens." />
        <div v-else class="ops-grid">
          <section class="card-block">
            <h4>Token Stats</h4>
            <el-button round type="primary" @click="loadTokenStats">Refresh</el-button>
            <pre v-if="tokenStats" class="json">{{ formatJson(tokenStats) }}</pre>
          </section>

          <section class="card-block">
            <h4>Authorization Detail</h4>
            <el-input v-model="authorizationId" placeholder="Authorization ID" />
            <div class="row">
              <el-button round type="primary" @click="loadAuthorizationDetail">Load</el-button>
              <el-button round type="danger" plain @click="revokeAuthorizationRow">Revoke</el-button>
            </div>
            <pre v-if="authorizationDetail" class="json">{{ formatJson(authorizationDetail) }}</pre>
          </section>

          <section class="card-block">
            <h4>Cleanup Tokens</h4>
            <el-button round type="warning" @click="runTokenCleanup">Run Cleanup</el-button>
            <pre v-if="cleanupResult" class="json">{{ formatJson(cleanupResult) }}</pre>
          </section>

          <section class="card-block">
            <h4>Storage Structure</h4>
            <el-button round @click="loadStorageStructure">Load Structure</el-button>
            <pre v-if="storageStructure" class="json">{{ formatJson(storageStructure) }}</pre>
          </section>

          <section class="card-block">
            <h4>Blacklist Stats</h4>
            <el-button round @click="loadBlacklistStats">Refresh</el-button>
            <pre v-if="blacklistStats" class="json">{{ formatJson(blacklistStats) }}</pre>
          </section>

          <section class="card-block">
            <h4>Blacklist Check</h4>
            <el-input v-model="blacklistToken" placeholder="Token value" />
            <el-button round type="primary" @click="checkBlacklistStatus">Check</el-button>
            <pre v-if="blacklistCheckResult" class="json">{{ formatJson(blacklistCheckResult) }}</pre>
          </section>

          <section class="card-block">
            <h4>Blacklist Add</h4>
            <el-input v-model="blacklistToken" placeholder="Token value" />
            <el-input v-model="blacklistReason" placeholder="Reason (optional)" />
            <el-button round type="danger" plain @click="addBlacklist">Add to Blacklist</el-button>
          </section>

          <section class="card-block">
            <h4>Blacklist Cleanup</h4>
            <el-button round type="warning" @click="cleanupBlacklistEntries">Cleanup</el-button>
            <pre v-if="blacklistCleanupResult" class="json">{{ formatJson(blacklistCleanupResult) }}</pre>
          </section>

          <section class="card-block">
            <h4>Logout All Sessions</h4>
            <el-input v-model="logoutUsername" placeholder="Username" />
            <el-button round type="danger" plain @click="logoutAllUserSessions">Revoke Sessions</el-button>
          </section>

          <section class="card-block">
            <h4>GitHub OAuth Status</h4>
            <div class="row">
              <el-button round @click="loadGitHubStatus">Check Status</el-button>
              <el-button round @click="loadGitHubUser">Fetch User</el-button>
            </div>
            <div v-if="gitHubStatus !== null" class="muted">Authorized: {{ gitHubStatus }}</div>
            <pre v-if="gitHubUserInfo" class="json">{{ formatJson(gitHubUserInfo) }}</pre>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Thread Pools" name="thread-pool">
        <el-alert v-if="!isAdmin" type="warning" title="Admin access required to view thread pools." />
        <div v-else>
          <div class="toolbar">
            <el-button round type="primary" :loading="threadPoolLoading" @click="loadThreadPools">Refresh</el-button>
            <el-input v-model="threadPoolName" placeholder="Thread pool bean name" />
            <el-button round @click="loadThreadPoolDetail">Load Detail</el-button>
          </div>
          <el-table v-loading="threadPoolLoading" :data="threadPoolRows" stripe>
            <el-table-column label="Name" prop="name" min-width="160" />
            <el-table-column label="Core" prop="corePoolSize" width="90" />
            <el-table-column label="Max" prop="maxPoolSize" width="90" />
            <el-table-column label="Active" prop="activeCount" width="90" />
            <el-table-column label="Queue" prop="queueSize" width="90" />
            <el-table-column label="Completed" prop="completedTaskCount" width="120" />
          </el-table>
          <pre v-if="threadPoolDetail" class="json">{{ formatJson(threadPoolDetail) }}</pre>
        </div>
      </el-tab-pane>

      <el-tab-pane label="SPU Catalog" name="spu">
        <el-alert v-if="!canCatalogOps" type="warning" title="Merchant or admin access required." />
        <div v-else class="ops-grid">
          <section class="card-block wide">
            <h4>SPU Editor</h4>
            <el-form label-position="top" class="form">
              <el-form-item label="SPU ID (for update)">
                <el-input-number v-model="spuForm.spuId" :min="1" controls-position="right" />
              </el-form-item>
              <el-form-item label="SPU Name">
                <el-input v-model="spuForm.spuName" />
              </el-form-item>
              <el-form-item label="Subtitle">
                <el-input v-model="spuForm.subtitle" />
              </el-form-item>
              <el-form-item label="Category ID">
                <el-input-number v-model="spuForm.categoryId" :min="1" controls-position="right" />
              </el-form-item>
              <el-form-item label="Brand ID">
                <el-input-number v-model="spuForm.brandId" :min="1" controls-position="right" />
              </el-form-item>
              <el-form-item label="Merchant ID">
                <el-input-number v-model="spuForm.merchantId" :min="1" controls-position="right" />
              </el-form-item>
              <el-form-item label="Status">
                <el-input-number v-model="spuForm.status" :min="0" :max="1" controls-position="right" />
              </el-form-item>
              <el-form-item label="Description">
                <el-input v-model="spuForm.description" type="textarea" rows="2" />
              </el-form-item>
              <el-form-item label="Main Image URL">
                <el-input v-model="spuForm.mainImage" />
              </el-form-item>
              <el-form-item label="SKU List (JSON array)">
                <el-input v-model="skuJson" type="textarea" rows="6" />
              </el-form-item>
            </el-form>
            <div class="row">
              <el-button round type="primary" @click="createSpuRow">Create</el-button>
              <el-button round @click="updateSpuRow">Update</el-button>
              <el-button round plain @click="loadSpuDetail">Load Detail</el-button>
            </div>
            <pre v-if="spuDetail" class="json">{{ formatJson(spuDetail) }}</pre>
          </section>

          <section class="card-block">
            <h4>SPU Status</h4>
            <el-input-number v-model="spuStatusId" :min="1" controls-position="right" placeholder="SPU ID" />
            <el-input-number v-model="spuStatusValue" :min="0" :max="1" controls-position="right" placeholder="Status" />
            <el-button round type="primary" @click="updateSpuStatusRow">Update Status</el-button>
          </section>

          <section class="card-block">
            <h4>SPU by Category</h4>
            <el-input-number v-model="categoryIdQuery" :min="1" controls-position="right" placeholder="Category ID" />
            <el-input-number v-model="categoryStatusQuery" :min="0" :max="1" controls-position="right" placeholder="Status (optional)" />
            <el-button round @click="loadSpuByCategory">Load</el-button>
            <pre v-if="categorySpuResult.length" class="json">{{ formatJson(categorySpuResult) }}</pre>
          </section>

          <section class="card-block">
            <h4>SKU Batch</h4>
            <el-input v-model="skuIdsInput" placeholder="SKU IDs, comma separated" />
            <el-button round @click="loadSkuBatch">Load SKUs</el-button>
            <pre v-if="skuList.length" class="json">{{ formatJson(skuList) }}</pre>
          </section>

          <section class="card-block">
            <h4>Category Tree</h4>
            <el-switch v-model="categoryTreeEnabledOnly" active-text="Enabled Only" />
            <el-button round @click="loadCategoryTree">Load Tree</el-button>
            <pre v-if="categoryTree.length" class="json">{{ formatJson(categoryTree) }}</pre>
          </section>

          <section class="card-block">
            <h4>Category Children</h4>
            <el-input-number v-model="categoryParentId" :min="1" controls-position="right" placeholder="Parent ID" />
            <el-button round @click="loadCategoryChildren">Load Children</el-button>
            <pre v-if="categoryChildren.length" class="json">{{ formatJson(categoryChildren) }}</pre>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Legacy Orders" name="orders">
        <div class="ops-grid">
          <section class="card-block wide">
            <h4>Create Main Order</h4>
            <el-input v-model="idempotencyKey" placeholder="Idempotency-Key header" />
            <div class="row">
              <el-button round @click="generateIdempotencyKey">Generate Key</el-button>
              <el-button round type="primary" @click="submitMainOrder">Create</el-button>
            </div>
            <el-input v-model="orderPayloadRaw" type="textarea" rows="10" />
            <pre v-if="legacyOrderResult" class="json">{{ formatJson(legacyOrderResult) }}</pre>
          </section>

          <section class="card-block">
            <h4>Lookup Main Order</h4>
            <el-input-number v-model="mainOrderIdLookup" :min="1" controls-position="right" placeholder="Main Order ID" />
            <div class="row">
              <el-button round @click="loadMainOrder">Load</el-button>
              <el-button round @click="loadSubOrders">Load Sub Orders</el-button>
            </div>
            <pre v-if="subOrdersResult.length" class="json">{{ formatJson(subOrdersResult) }}</pre>
          </section>

          <section class="card-block">
            <h4>Advance Sub Order</h4>
            <el-input-number v-model="subOrderIdAction" :min="1" controls-position="right" placeholder="Sub Order ID" />
            <el-input v-model="subOrderAction" placeholder="Action (e.g., ship, cancel)" />
            <el-button round type="primary" @click="advanceSubOrder">Advance</el-button>
          </section>

          <section class="card-block wide">
            <h4>Apply After-sale</h4>
            <el-input v-model="afterSalePayloadRaw" type="textarea" rows="8" />
            <el-button round type="primary" @click="submitAfterSale">Submit</el-button>
            <pre v-if="afterSaleResult" class="json">{{ formatJson(afterSaleResult) }}</pre>
          </section>

          <section class="card-block">
            <h4>Advance After-sale</h4>
            <el-input-number v-model="afterSaleIdAction" :min="1" controls-position="right" placeholder="After-sale ID" />
            <el-input v-model="afterSaleAction" placeholder="Action (approve/reject/close)" />
            <el-input v-model="afterSaleRemark" placeholder="Remark (optional)" />
            <el-button round @click="advanceAfterSale">Advance</el-button>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Search Lab" name="search">
        <div class="search-lab">
          <div class="toolbar">
            <el-select v-model="searchEndpoint" placeholder="Endpoint">
              <el-option label="Complex Search" value="complex-search" />
              <el-option label="Get Filters" value="filters" />
              <el-option label="Basic Search" value="basic" />
              <el-option label="Search (paged)" value="search" />
              <el-option label="Search by Category" value="search-category" />
              <el-option label="Search by Shop" value="search-shop" />
              <el-option label="Advanced Search" value="search-advanced" />
              <el-option label="Smart Search" value="smart-search" />
              <el-option label="Recommended" value="recommended" />
              <el-option label="New Products" value="new" />
              <el-option label="Hot Products" value="hot" />
              <el-option label="Filter Search" value="filter" />
              <el-option label="Filter by Category" value="filter-category" />
              <el-option label="Filter by Brand" value="filter-brand" />
              <el-option label="Filter by Price" value="filter-price" />
              <el-option label="Filter by Shop" value="filter-shop" />
              <el-option label="Combined Filter" value="filter-combined" />
              <el-option label="Shop Search (Complex)" value="shop-complex" />
              <el-option label="Shop Filters" value="shop-filters" />
              <el-option label="Shop Suggestions" value="shop-suggestions" />
              <el-option label="Shop Hot" value="shop-hot" />
              <el-option label="Shop By ID" value="shop-by-id" />
              <el-option label="Shop Recommended" value="shop-recommended" />
              <el-option label="Shop By Location" value="shop-by-location" />
            </el-select>
            <el-button round type="primary" @click="runSearch">Run</el-button>
          </div>
          <el-input v-model="searchParamsRaw" type="textarea" rows="10" />
          <pre v-if="searchResult" class="json">{{ formatJson(searchResult) }}</pre>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<style scoped>
.panel {
  padding: clamp(0.9rem, 1.2vw, 1.1rem);
}

.ops-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.card-block {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.7);
  display: grid;
  gap: 8px;
}

.card-block h4 {
  margin: 0;
}

.wide {
  grid-column: span 2;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.form {
  margin-top: 6px;
}

.json {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.82rem;
  padding: 8px;
  border-radius: 10px;
  border: 1px dashed rgba(36, 107, 255, 0.2);
  background: rgba(255, 255, 255, 0.6);
}

.muted {
  color: var(--text-muted);
}

.search-lab {
  display: grid;
  gap: 10px;
}

@media (max-width: 900px) {
  .wide {
    grid-column: auto;
  }
}
</style>
