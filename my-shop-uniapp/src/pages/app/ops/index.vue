<script setup lang="ts">
import { ref, watch } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { Routes } from '../../../router/routes'
import {
  advancedSearch,
  basicSearch,
  combinedSearchProducts,
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
  smartSearchProducts,
  searchByCategory,
  searchByShop,
  searchProducts
} from '../../../api/search-ops'
import {
  getShopById,
  getShopFilters,
  listHotShops,
  listRecommendedShops,
  listShopSuggestions,
  searchShops,
  searchShopsByLocation,
  type ShopSearchRequest
} from '../../../api/shop-search'
import { useBatchGovernance } from '../../../features/ops/composables/useBatchGovernance'
import { useCatalogGovernance } from '../../../features/ops/composables/useCatalogGovernance'
import { useCategoryGovernance } from '../../../features/ops/composables/useCategoryGovernance'
import { usePaymentGovernance } from '../../../features/ops/composables/usePaymentGovernance'
import { useStatisticsGovernance } from '../../../features/ops/composables/useStatisticsGovernance'
import { useThreadPoolGovernance } from '../../../features/ops/composables/useThreadPoolGovernance'
import { useTokenGovernance } from '../../../features/ops/composables/useTokenGovernance'
import { createOpsInputTools } from '../../../features/ops/model/input-tools'
import type {
  ProductFilterRequest,
  ProductSearchRequest
} from '../../../types/domain'
import { toast } from '../../../utils/ui'

const tabs = [
  { key: 'tokens', label: 'Tokens and authorization' },
  { key: 'system', label: 'Thread pools' },
  { key: 'category', label: 'Category operations' },
  { key: 'catalog', label: 'Product catalog' },
  { key: 'search', label: 'Search operations' },
  { key: 'shops', label: 'Shop search' },
  { key: 'batch', label: 'Batch operations' },
  { key: 'payments', label: 'Payment operations' },
  { key: 'stats', label: 'Statistics operations' }
]

const activeTab = ref('tokens')

const opsInputTools = createOpsInputTools((message) => toast(message))
const {
  compactPayload,
  formatJson,
  parseJson,
  parseNumberList,
  parseOptionalBoolean,
  parseOptionalNumber,
  parseStringList,
  requirePositiveId,
  toPrettyJson
} = opsInputTools

function resolveSearchPaging(): { page: number; size: number } | null {
  const page = Number(searchPage.value)
  const size = Number(searchSize.value)
  if (!Number.isInteger(page) || page < 0) {
    toast('Page must be a non-negative integer')
    return null
  }
  if (!Number.isInteger(size) || size <= 0) {
    toast('Page size must be a positive integer')
    return null
  }
  searchPage.value = String(page)
  searchSize.value = String(size)
  return { page, size }
}

function resolvePriceRange(): { minPrice?: number; maxPrice?: number } | null {
  const minPrice = parseOptionalNumber(searchMinPrice.value, 'Minimum price')
  if (minPrice === null) return null
  const maxPrice = parseOptionalNumber(searchMaxPrice.value, 'Maximum price')
  if (maxPrice === null) return null
  if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
    toast('Minimum price cannot be greater than maximum price')
    return null
  }
  return { minPrice: minPrice ?? undefined, maxPrice: maxPrice ?? undefined }
}

// Token and authorization
const tokenGovernance = useTokenGovernance()
const {
  addBlacklist,
  authorizationDetail,
  authorizationId,
  blacklistCheckResult,
  blacklistCleanupResult,
  blacklistReason,
  blacklistStats,
  blacklistToken,
  checkBlacklistStatus,
  cleanupBlacklistEntries,
  cleanupResult,
  gitHubStatus,
  gitHubUserInfo,
  loadAuthorizationDetail,
  loadBlacklistStats,
  loadGitHubStatus,
  loadGitHubUser,
  loadStorageStructure,
  loadTokenStats,
  logoutAll,
  logoutUsername,
  revokeAuthorizationRow,
  runTokenCleanup,
  storageStructure,
  tokenStats,
  tokenValidationMessage,
  validateCurrentToken
} = tokenGovernance

// Thread pools
const {
  loadThreadPoolDetail,
  loadThreadPools,
  threadPoolDetail,
  threadPoolName,
  threadPools
} = useThreadPoolGovernance()

// Category operations
const {
  categoryBatchIds,
  categoryBatchPayload,
  categoryBatchResult,
  categoryBatchStatus,
  categoryById,
  categoryChildren,
  categoryIdInput,
  categoryMoveParent,
  categorySortInput,
  categoryTree,
  createCategoriesBatchAction,
  deleteCategoriesBatchAction,
  loadCategoryById,
  loadCategoryChildren,
  loadCategoryTree,
  moveCategoryAction,
  updateCategorySortAction,
  updateCategoryStatusBatchAction
} = useCategoryGovernance(opsInputTools)

// Product catalog
const {
  createSpuAction,
  loadSkuList,
  loadSpuByCategory,
  loadSpuDetail,
  skuIds,
  skuList,
  spuActionResult,
  spuCategoryId,
  spuDetail,
  spuId,
  spuList,
  spuPayloadJson,
  spuStatus,
  updateSpuAction,
  updateSpuStatusAction
} = useCatalogGovernance(opsInputTools)


const searchKeyword = ref('')
const searchPage = ref('0')
const searchSize = ref('20')
const searchCategoryId = ref('')
const searchShopId = ref('')
const searchBrandId = ref('')
const searchMinPrice = ref('')
const searchMaxPrice = ref('')
const searchShopName = ref('')
const searchCategoryName = ref('')
const searchBrandName = ref('')
const searchStatus = ref('')
const searchStockStatus = ref('')
const searchRecommended = ref('')
const searchIsNew = ref('')
const searchIsHot = ref('')
const searchTags = ref('')
const searchMinSalesCount = ref('')
const searchMinRating = ref('')
const searchSortBy = ref('')
const searchSortOrder = ref('')
const searchHighlight = ref('')
const searchIncludeAggregations = ref('')
const searchComplexJson = ref('')
const searchFilterJson = ref('')
const searchResult = ref<unknown>(null)

watch(
  () => [
    searchKeyword.value,
    searchCategoryId.value,
    searchShopId.value,
    searchBrandId.value,
    searchMinPrice.value,
    searchMaxPrice.value
  ],
  () => {
    searchPage.value = '0'
  }
)

async function runBasicSearch(): Promise<void> {
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await basicSearch({
      keyword: searchKeyword.value || undefined,
      page: paging.page,
      size: paging.size
    })
    toast('Basic search completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Search failed')
  }
}

async function runAdvancedSearch(): Promise<void> {
  if (!searchKeyword.value.trim()) {
    toast('Please enter a keyword')
    return
  }
  const paging = resolveSearchPaging()
  if (!paging) return
  const priceRange = resolvePriceRange()
  if (!priceRange) return
  try {
    searchResult.value = await advancedSearch({
      keyword: searchKeyword.value.trim(),
      minPrice: priceRange.minPrice,
      maxPrice: priceRange.maxPrice,
      page: paging.page,
      size: paging.size
    })
    toast('Advanced search completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Search failed')
  }
}

async function runSearchByCategory(): Promise<void> {
  const id = requirePositiveId(searchCategoryId.value, 'category ID')
  if (!id) return
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await searchByCategory(id, {
      keyword: searchKeyword.value || undefined,
      page: paging.page,
      size: paging.size
    })
    toast('Category search completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Search failed')
  }
}

async function runSearchByShop(): Promise<void> {
  const id = requirePositiveId(searchShopId.value, 'shop ID')
  if (!id) return
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await searchByShop(id, {
      keyword: searchKeyword.value || undefined,
      page: paging.page,
      size: paging.size
    })
    toast('Shop search completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Search failed')
  }
}

async function runFilterByCategory(): Promise<void> {
  const id = requirePositiveId(searchCategoryId.value, 'category ID')
  if (!id) return
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await filterByCategory(id, {
      page: paging.page,
      size: paging.size
    })
    toast('Category filter completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Filter failed')
  }
}

async function runFilterByBrand(): Promise<void> {
  const id = requirePositiveId(searchBrandId.value, 'brand ID')
  if (!id) return
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await filterByBrand(id, {
      page: paging.page,
      size: paging.size
    })
    toast('Brand filter completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Filter failed')
  }
}

async function runFilterByPrice(): Promise<void> {
  const paging = resolveSearchPaging()
  if (!paging) return
  const priceRange = resolvePriceRange()
  if (!priceRange) return
  try {
    searchResult.value = await filterByPrice({
      minPrice: priceRange.minPrice,
      maxPrice: priceRange.maxPrice,
      page: paging.page,
      size: paging.size
    })
    toast('Price filter completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Filter failed')
  }
}

async function runFilterByShop(): Promise<void> {
  const id = requirePositiveId(searchShopId.value, 'shop ID')
  if (!id) return
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await filterByShop(id, {
      page: paging.page,
      size: paging.size
    })
    toast('Shop filter completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Filter failed')
  }
}

function buildProductSearchRequest(): ProductSearchRequest | null {
  const paging = resolveSearchPaging()
  if (!paging) return null
  const priceRange = resolvePriceRange()
  if (!priceRange) return null
  const categoryId = parseOptionalNumber(searchCategoryId.value, 'Category ID')
  if (categoryId === null) return null
  const shopId = parseOptionalNumber(searchShopId.value, 'Shop ID')
  if (shopId === null) return null
  const brandId = parseOptionalNumber(searchBrandId.value, 'Brand ID')
  if (brandId === null) return null
  const status = parseOptionalNumber(searchStatus.value, 'Status')
  if (status === null) return null
  const stockStatus = parseOptionalNumber(searchStockStatus.value, 'Stock status')
  if (stockStatus === null) return null
  const minSalesCount = parseOptionalNumber(searchMinSalesCount.value, 'Minimum sales count')
  if (minSalesCount === null) return null
  const minRating = parseOptionalNumber(searchMinRating.value, 'Minimum rating')
  if (minRating === null) return null
  const recommended = parseOptionalBoolean(searchRecommended.value, 'Recommended')
  if (recommended === null) return null
  const isNew = parseOptionalBoolean(searchIsNew.value, 'Is new')
  if (isNew === null) return null
  const isHot = parseOptionalBoolean(searchIsHot.value, 'Is hot')
  if (isHot === null) return null
  const highlight = parseOptionalBoolean(searchHighlight.value, 'Highlight')
  if (highlight === null) return null
  const includeAggregations = parseOptionalBoolean(searchIncludeAggregations.value, 'Include aggregations')
  if (includeAggregations === null) return null
  return compactPayload({
    keyword: searchKeyword.value.trim() || undefined,
    shopId: shopId ?? undefined,
    shopName: searchShopName.value.trim() || undefined,
    categoryId: categoryId ?? undefined,
    categoryName: searchCategoryName.value.trim() || undefined,
    brandId: brandId ?? undefined,
    brandName: searchBrandName.value.trim() || undefined,
    minPrice: priceRange.minPrice,
    maxPrice: priceRange.maxPrice,
    status: status ?? undefined,
    stockStatus: stockStatus ?? undefined,
    recommended: recommended ?? undefined,
    isNew: isNew ?? undefined,
    isHot: isHot ?? undefined,
    tags: parseStringList(searchTags.value),
    minSalesCount: minSalesCount ?? undefined,
    minRating: minRating ?? undefined,
    page: paging.page,
    size: paging.size,
    sortBy: searchSortBy.value.trim() || undefined,
    sortOrder: searchSortOrder.value.trim() || undefined,
    highlight: highlight ?? undefined,
    includeAggregations: includeAggregations ?? undefined
  })
}

function buildProductFilterRequest(): ProductFilterRequest | null {
  const paging = resolveSearchPaging()
  if (!paging) return null
  const priceRange = resolvePriceRange()
  if (!priceRange) return null
  const categoryId = parseOptionalNumber(searchCategoryId.value, 'Category ID')
  if (categoryId === null) return null
  const shopId = parseOptionalNumber(searchShopId.value, 'Shop ID')
  if (shopId === null) return null
  const brandId = parseOptionalNumber(searchBrandId.value, 'Brand ID')
  if (brandId === null) return null
  const minSalesCount = parseOptionalNumber(searchMinSalesCount.value, 'Minimum sales count')
  if (minSalesCount === null) return null
  const recommended = parseOptionalBoolean(searchRecommended.value, 'Recommended')
  if (recommended === null) return null
  const isNew = parseOptionalBoolean(searchIsNew.value, 'Is new')
  if (isNew === null) return null
  const isHot = parseOptionalBoolean(searchIsHot.value, 'Is hot')
  if (isHot === null) return null
  return compactPayload({
    keyword: searchKeyword.value.trim() || undefined,
    categoryId: categoryId ?? undefined,
    brandId: brandId ?? undefined,
    shopId: shopId ?? undefined,
    minPrice: priceRange.minPrice,
    maxPrice: priceRange.maxPrice,
    minSalesCount: minSalesCount ?? undefined,
    recommended: recommended ?? undefined,
    isNew: isNew ?? undefined,
    isHot: isHot ?? undefined,
    sortBy: searchSortBy.value.trim() || undefined,
    sortOrder: searchSortOrder.value.trim() || undefined,
    page: paging.page,
    size: paging.size
  })
}

function populateProductSearchPayload(mode: 'complex' | 'filter'): void {
  const payload = mode === 'complex' ? buildProductSearchRequest() : buildProductFilterRequest()
  if (!payload) return
  if (mode === 'complex') {
    searchComplexJson.value = toPrettyJson(payload)
  } else {
    searchFilterJson.value = toPrettyJson(payload)
  }
  toast(`Prepared ${mode} search payload`, 'success')
}

async function runComplexSearch(): Promise<void> {
  const payload = searchComplexJson.value.trim()
    ? parseJson<ProductSearchRequest>(searchComplexJson.value, 'Complex search')
    : buildProductSearchRequest()
  if (!payload) return
  try {
    searchResult.value = await complexSearch(payload)
    toast('Complex search completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Search failed')
  }
}

async function runFilterSearch(): Promise<void> {
  const payload = searchFilterJson.value.trim()
    ? parseJson<ProductFilterRequest>(searchFilterJson.value, 'Filter search')
    : buildProductFilterRequest()
  if (!payload) return
  try {
    searchResult.value = await filterSearch(payload)
    toast('Filter search completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Search failed')
  }
}

async function runSearchRecommend(type: 'hot' | 'new' | 'recommended'): Promise<void> {
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    if (type === 'hot') {
      searchResult.value = await listHotProducts(paging.page, paging.size)
    } else if (type === 'new') {
      searchResult.value = await listNewProducts(paging.page, paging.size)
    } else {
      searchResult.value = await listRecommendedProducts(paging.page, paging.size)
    }
    toast('Recommendation list loaded', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Load failed')
  }
}

async function runCombinedSearch(): Promise<void> {
  const paging = resolveSearchPaging()
  if (!paging) return
  const priceRange = resolvePriceRange()
  if (!priceRange) return
  const categoryId = parseOptionalNumber(searchCategoryId.value, 'Category ID')
  if (categoryId === null) return
  const brandId = parseOptionalNumber(searchBrandId.value, 'Brand ID')
  if (brandId === null) return
  const shopId = parseOptionalNumber(searchShopId.value, 'Shop ID')
  if (shopId === null) return
  try {
    searchResult.value = await combinedSearchProducts({
      keyword: searchKeyword.value || undefined,
      categoryId: categoryId ?? undefined,
      brandId: brandId ?? undefined,
      shopId: shopId ?? undefined,
      minPrice: priceRange.minPrice,
      maxPrice: priceRange.maxPrice,
      page: paging.page,
      size: paging.size
    })
    toast('Combined search completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Search failed')
  }
}

async function runSmartSearch(): Promise<void> {
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await smartSearchProducts({
      keyword: searchKeyword.value || undefined,
      page: paging.page,
      size: paging.size,
      sortField: 'score',
      sortOrder: 'desc'
    })
    toast('Smart search completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Search failed')
  }
}

async function runSearchSuggest(): Promise<void> {
  try {
    searchResult.value = await getProductFilters({
      keyword: searchKeyword.value || undefined
    })
    toast('Search filters loaded', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Load failed')
  }
}

async function runSearchProducts(): Promise<void> {
  if (!searchKeyword.value.trim()) {
    toast('Please enter a keyword')
    return
  }
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await searchProducts({
      keyword: searchKeyword.value.trim(),
      page: paging.page,
      size: paging.size
    })
    toast('Search completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Search failed')
  }
}

// Shop search
const shopId = ref('')
const shopLocation = ref('')
const shopSuggestKeyword = ref('')
const shopKeyword = ref('')
const shopMerchantId = ref('')
const shopStatus = ref('')
const shopMinRating = ref('')
const shopMinProductCount = ref('')
const shopMinFollowCount = ref('')
const shopRecommended = ref('')
const shopAddressKeyword = ref('')
const shopPage = ref('0')
const shopSize = ref('20')
const shopSortBy = ref('')
const shopSortOrder = ref('')
const shopHighlight = ref('')
const shopIncludeAggregations = ref('')
const shopSearchJson = ref('')
const shopResult = ref<unknown>(null)

function buildShopSearchRequest(): ShopSearchRequest | null {
  const merchantId = parseOptionalNumber(shopMerchantId.value, 'Merchant ID')
  if (merchantId === null) return null
  const status = parseOptionalNumber(shopStatus.value, 'Status')
  if (status === null) return null
  const minRating = parseOptionalNumber(shopMinRating.value, 'Minimum rating')
  if (minRating === null) return null
  const minProductCount = parseOptionalNumber(shopMinProductCount.value, 'Minimum product count')
  if (minProductCount === null) return null
  const minFollowCount = parseOptionalNumber(shopMinFollowCount.value, 'Minimum follow count')
  if (minFollowCount === null) return null
  const recommended = parseOptionalBoolean(shopRecommended.value, 'Recommended')
  if (recommended === null) return null
  const page = parseOptionalNumber(shopPage.value, 'Page')
  if (page === null) return null
  const size = parseOptionalNumber(shopSize.value, 'Size')
  if (size === null) return null
  const highlight = parseOptionalBoolean(shopHighlight.value, 'Highlight')
  if (highlight === null) return null
  const includeAggregations = parseOptionalBoolean(shopIncludeAggregations.value, 'Include aggregations')
  if (includeAggregations === null) return null
  return compactPayload({
    keyword: shopKeyword.value.trim() || undefined,
    merchantId: merchantId ?? undefined,
    status: status ?? undefined,
    minRating: minRating ?? undefined,
    minProductCount: minProductCount ?? undefined,
    minFollowCount: minFollowCount ?? undefined,
    recommended: recommended ?? undefined,
    addressKeyword: shopAddressKeyword.value.trim() || undefined,
    page: page ?? 0,
    size: size ?? 20,
    sortBy: shopSortBy.value.trim() || undefined,
    sortOrder: (shopSortOrder.value.trim() as 'asc' | 'desc' | '') || undefined,
    highlight: highlight ?? undefined,
    includeAggregations: includeAggregations ?? undefined
  })
}

function populateShopSearchPayload(): void {
  const payload = buildShopSearchRequest()
  if (!payload) return
  shopSearchJson.value = toPrettyJson(payload)
  toast('Prepared shop search payload', 'success')
}

async function runShopSearch(): Promise<void> {
  const payload = shopSearchJson.value.trim()
    ? parseJson<ShopSearchRequest>(shopSearchJson.value, 'Shop search')
    : buildShopSearchRequest()
  if (!payload) return
  try {
    shopResult.value = await searchShops(payload)
    toast('Shop search completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Search failed')
  }
}

async function loadShopById(): Promise<void> {
  const id = Number(shopId.value)
  if (!Number.isFinite(id)) {
    toast('Please enter a shop ID')
    return
  }
  try {
    shopResult.value = await getShopById(id)
    toast('Shop details loaded', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Load failed')
  }
}

async function loadShopSuggestions(): Promise<void> {
  if (!shopSuggestKeyword.value.trim()) {
    toast('Please enter a keyword')
    return
  }
  try {
    shopResult.value = await listShopSuggestions(shopSuggestKeyword.value.trim(), 10)
    toast('Suggestions loaded', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Load failed')
  }
}

async function loadShopFilters(): Promise<void> {
  const payload = shopSearchJson.value.trim()
    ? parseJson<ShopSearchRequest>(shopSearchJson.value, 'Shop filters')
    : buildShopSearchRequest()
  if (!payload) return
  try {
    shopResult.value = await getShopFilters(payload)
    toast('Shop filters loaded', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Load failed')
  }
}

async function loadHotShops(): Promise<void> {
  try {
    shopResult.value = await listHotShops(10)
    toast('Hot shops loaded', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Load failed')
  }
}

async function loadRecommendedShops(): Promise<void> {
  try {
    shopResult.value = await listRecommendedShops(0, 20)
    toast('Recommended shops loaded', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Load failed')
  }
}

async function searchShopsByGeo(): Promise<void> {
  if (!shopLocation.value.trim()) {
    toast('Please enter an address keyword')
    return
  }
  try {
    shopResult.value = await searchShopsByLocation(shopLocation.value.trim(), 0, 20)
    toast('Location search completed', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : 'Search failed')
  }
}
// Bulk operations
const {
  approveMerchantsBatchAction,
  batchResult,
  deleteMerchantsBatchAction,
  findUser,
  merchantAuthBatchIds,
  merchantAuthBatchRemark,
  merchantAuthBatchStatus,
  merchantBatchIds,
  merchantBatchRemark,
  merchantBatchStatus,
  reviewMerchantAuthBatchAction,
  updateMerchantStatusBatchAction,
  updateUsersBatchAction,
  userBatchJson,
  userBatchResult,
  userFindResult,
  userFindUsername
} = useBatchGovernance(opsInputTools)

// Payment operations
const {
  createPaymentOrderAction,
  createPaymentRefundAction,
  loadPaymentByNoAction,
  loadPaymentByOrderAction,
  openPaymentCheckoutAction,
  paymentMainOrderNo,
  paymentNoInput,
  paymentOrderJson,
  paymentRefundJson,
  paymentResult,
  paymentSubOrderNo,
  refreshPaymentStatusAction
} = usePaymentGovernance(opsInputTools)

// Statistics operations
const {
  loadStatsOverviewAsync,
  loadStatsTrendRange,
  refreshStatsCache,
  statsEndDate,
  statsOverviewAsync,
  statsRefreshResult,
  statsStartDate,
  statsTrendRange
} = useStatisticsGovernance(opsInputTools)
</script>
<template>
  <AppShell title="Ops Center" :route-path="Routes.appOps" :requires-auth="true" :roles="['ADMIN']">
    <view class="tabs">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="tab"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </view>
    </view>

    <view v-if="activeTab === 'tokens'" class="panel surface-card fade-in-up">
      <text class="section-title">Token statistics</text>
      <view class="row-inline">
        <button class="btn-outline" @click="loadTokenStats">Load statistics</button>
        <button class="btn-outline" @click="loadStorageStructure">Storage structure</button>
        <button class="btn-outline" @click="runTokenCleanup">Clean expired tokens</button>
        <button class="btn-outline" @click="validateCurrentToken">Validate current token</button>
      </view>
      <text class="code-block">{{ formatJson(tokenStats) }}</text>
      <text class="code-block">{{ formatJson(storageStructure) }}</text>
      <text class="code-block">{{ formatJson(cleanupResult) }}</text>
      <text class="code-block">{{ tokenValidationMessage || '--' }}</text>

      <view class="section">
        <text class="section-title">Authorization details</text>
        <input v-model="authorizationId" class="input" placeholder="Authorization ID" />
        <view class="row-inline">
          <button class="btn-outline" @click="loadAuthorizationDetail">Load</button>
          <button class="btn-outline" @click="revokeAuthorizationRow">Revoke</button>
        </view>
        <text class="code-block">{{ formatJson(authorizationDetail) }}</text>
      </view>

      <view class="section">
        <text class="section-title">Blacklist</text>
        <input v-model="blacklistToken" class="input" placeholder="Token" />
        <input v-model="blacklistReason" class="input" placeholder="Reason (optional)" />
        <view class="row-inline">
          <button class="btn-outline" @click="addBlacklist">Add to blacklist</button>
          <button class="btn-outline" @click="checkBlacklistStatus">Check status</button>
          <button class="btn-outline" @click="cleanupBlacklistEntries">Clean blacklist</button>
          <button class="btn-outline" @click="loadBlacklistStats">Blacklist statistics</button>
        </view>
        <text class="code-block">{{ formatJson(blacklistStats) }}</text>
        <text class="code-block">{{ formatJson(blacklistCheckResult) }}</text>
        <text class="code-block">{{ formatJson(blacklistCleanupResult) }}</text>
      </view>

      <view class="section">
        <text class="section-title">GitHub authorization</text>
        <view class="row-inline">
          <button class="btn-outline" @click="loadGitHubStatus">Authorization status</button>
          <button class="btn-outline" @click="loadGitHubUser">User info</button>
        </view>
        <text class="code-block">{{ formatJson(gitHubStatus) }}</text>
        <text class="code-block">{{ formatJson(gitHubUserInfo) }}</text>
      </view>

      <view class="section">
        <text class="section-title">Global logout</text>
        <input v-model="logoutUsername" class="input" placeholder="Username" />
        <button class="btn-outline" @click="logoutAll">Trigger logout</button>
      </view>
    </view>
    <view v-if="activeTab === 'system'" class="panel surface-card fade-in-up">
      <text class="section-title">Thread pools</text>
      <view class="row-inline">
        <button class="btn-outline" @click="loadThreadPools">Load thread pools</button>
      </view>
      <text class="code-block">{{ formatJson(threadPools) }}</text>

      <view class="section">
        <text class="section-title">Thread pool details</text>
        <input v-model="threadPoolName" class="input" placeholder="Thread pool name" />
        <button class="btn-outline" @click="loadThreadPoolDetail">Load details</button>
        <text class="code-block">{{ formatJson(threadPoolDetail) }}</text>
      </view>
    </view>

    <view v-if="activeTab === 'category'" class="panel surface-card fade-in-up">
      <text class="section-title">Category operations</text>
      <input v-model="categoryIdInput" class="input" placeholder="Category ID" />
      <input v-model="categorySortInput" class="input" placeholder="Sort value" />
      <input v-model="categoryMoveParent" class="input" placeholder="New parent ID" />
      <view class="row-inline">
        <button class="btn-outline" @click="loadCategoryTree">Category tree</button>
        <button class="btn-outline" @click="loadCategoryChildren">Child categories</button>
        <button class="btn-outline" @click="loadCategoryById">Category details</button>
        <button class="btn-outline" @click="updateCategorySortAction">Update sort</button>
        <button class="btn-outline" @click="moveCategoryAction">Move category</button>
      </view>
      <text class="code-block">{{ formatJson(categoryTree) }}</text>
      <text class="code-block">{{ formatJson(categoryChildren) }}</text>
      <text class="code-block">{{ formatJson(categoryById) }}</text>

      <view class="section">
        <text class="section-title">Batch operations</text>
        <input v-model="categoryBatchIds" class="input" placeholder="ID list (comma separated)" />
        <input v-model="categoryBatchStatus" class="input" placeholder="Status" />
        <textarea v-model="categoryBatchPayload" class="textarea" placeholder="Batch create JSON" />
        <view class="row-inline">
          <button class="btn-outline" @click="updateCategoryStatusBatchAction">Batch status</button>
          <button class="btn-outline" @click="createCategoriesBatchAction">Batch create</button>
          <button class="btn-outline" @click="deleteCategoriesBatchAction">Batch delete</button>
        </view>
        <text class="code-block">{{ formatJson(categoryBatchResult) }}</text>
      </view>
    </view>
    <view v-if="activeTab === 'catalog'" class="panel surface-card fade-in-up">
      <text class="section-title">Product catalog</text>
      <input v-model="spuId" class="input" placeholder="SPU ID" />
      <input v-model="spuStatus" class="input" placeholder="Status" />
      <input v-model="spuCategoryId" class="input" placeholder="Category ID" />
      <input v-model="skuIds" class="input" placeholder="SKU ID list" />
      <textarea v-model="spuPayloadJson" class="textarea" placeholder="SPU create/update JSON" />
      <view class="row-inline">
        <button class="btn-outline" @click="createSpuAction">Create SPU</button>
        <button class="btn-outline" @click="updateSpuAction">Update SPU</button>
        <button class="btn-outline" @click="loadSpuDetail">Load SPU</button>
        <button class="btn-outline" @click="loadSpuByCategory">Category SPUs</button>
        <button class="btn-outline" @click="loadSkuList">SKU list</button>
        <button class="btn-outline" @click="updateSpuStatusAction">Update status</button>
      </view>
      <text class="code-block">{{ formatJson(spuActionResult) }}</text>
      <text class="code-block">{{ formatJson(spuDetail) }}</text>
      <text class="code-block">{{ formatJson(spuList) }}</text>
      <text class="code-block">{{ formatJson(skuList) }}</text>
    </view>

    <view v-if="activeTab === 'search'" class="panel surface-card fade-in-up">
      <text class="section-title">Search operations</text>
      <input v-model="searchKeyword" class="input" placeholder="Keyword" />
      <input v-model="searchPage" class="input" placeholder="Page" />
      <input v-model="searchSize" class="input" placeholder="Page size" />
      <input v-model="searchCategoryId" class="input" placeholder="Category ID" />
      <input v-model="searchShopId" class="input" placeholder="Shop ID" />
      <input v-model="searchBrandId" class="input" placeholder="Brand ID" />
      <input v-model="searchMinPrice" class="input" placeholder="Minimum price" />
      <input v-model="searchMaxPrice" class="input" placeholder="Maximum price" />
      <input v-model="searchShopName" class="input" placeholder="Shop name" />
      <input v-model="searchCategoryName" class="input" placeholder="Category name" />
      <input v-model="searchBrandName" class="input" placeholder="Brand name" />
      <input v-model="searchStatus" class="input" placeholder="Status" />
      <input v-model="searchStockStatus" class="input" placeholder="Stock status" />
      <input v-model="searchRecommended" class="input" placeholder="Recommended true/false" />
      <input v-model="searchIsNew" class="input" placeholder="Is new true/false" />
      <input v-model="searchIsHot" class="input" placeholder="Is hot true/false" />
      <input v-model="searchTags" class="input" placeholder="Tags (comma separated)" />
      <input v-model="searchMinSalesCount" class="input" placeholder="Minimum sales count" />
      <input v-model="searchMinRating" class="input" placeholder="Minimum rating" />
      <input v-model="searchSortBy" class="input" placeholder="Sort by" />
      <input v-model="searchSortOrder" class="input" placeholder="Sort order asc/desc" />
      <input v-model="searchHighlight" class="input" placeholder="Highlight true/false" />
      <input v-model="searchIncludeAggregations" class="input" placeholder="Include aggregations true/false" />
      <textarea v-model="searchComplexJson" class="textarea" placeholder="Complex search JSON" />
      <textarea v-model="searchFilterJson" class="textarea" placeholder="Filter search JSON" />
      <view class="row-inline">
        <button class="btn-outline" @click="runBasicSearch">Basic search</button>
        <button class="btn-outline" @click="runAdvancedSearch">Advanced search</button>
        <button class="btn-outline" @click="runSearchByCategory">Search by category</button>
        <button class="btn-outline" @click="runSearchByShop">Search by shop</button>
        <button class="btn-outline" @click="runFilterByCategory">Filter by category</button>
        <button class="btn-outline" @click="runFilterByBrand">Filter by brand</button>
        <button class="btn-outline" @click="runFilterByPrice">Filter by price</button>
        <button class="btn-outline" @click="runFilterByShop">Filter by shop</button>
        <button class="btn-outline" @click="runSearchProducts">Search products</button>
        <button class="btn-outline" @click="runSearchSuggest">Load filters</button>
        <button class="btn-outline" @click="populateProductSearchPayload('complex')">Build complex JSON</button>
        <button class="btn-outline" @click="populateProductSearchPayload('filter')">Build filter JSON</button>
        <button class="btn-outline" @click="runComplexSearch">Complex search</button>
        <button class="btn-outline" @click="runFilterSearch">Filter search</button>
        <button class="btn-outline" @click="runCombinedSearch">Combined search</button>
        <button class="btn-outline" @click="runSmartSearch">Smart search</button>
        <button class="btn-outline" @click="runSearchRecommend('hot')">Hot</button>
        <button class="btn-outline" @click="runSearchRecommend('new')">New</button>
        <button class="btn-outline" @click="runSearchRecommend('recommended')">Recommended</button>
      </view>
      <text class="code-block">{{ formatJson(searchResult) }}</text>
    </view>

    <view v-if="activeTab === 'shops'" class="panel surface-card fade-in-up">
      <text class="section-title">Shop search</text>
      <input v-model="shopId" class="input" placeholder="Shop ID" />
      <input v-model="shopLocation" class="input" placeholder="Location keyword" />
      <input v-model="shopSuggestKeyword" class="input" placeholder="Suggestion keyword" />
      <input v-model="shopKeyword" class="input" placeholder="Keyword" />
      <input v-model="shopMerchantId" class="input" placeholder="Merchant ID" />
      <input v-model="shopStatus" class="input" placeholder="Status" />
      <input v-model="shopMinRating" class="input" placeholder="Minimum rating" />
      <input v-model="shopMinProductCount" class="input" placeholder="Minimum product count" />
      <input v-model="shopMinFollowCount" class="input" placeholder="Minimum follow count" />
      <input v-model="shopRecommended" class="input" placeholder="Recommended true/false" />
      <input v-model="shopAddressKeyword" class="input" placeholder="Address keyword" />
      <input v-model="shopPage" class="input" placeholder="Page" />
      <input v-model="shopSize" class="input" placeholder="Size" />
      <input v-model="shopSortBy" class="input" placeholder="Sort by" />
      <input v-model="shopSortOrder" class="input" placeholder="Sort order asc/desc" />
      <input v-model="shopHighlight" class="input" placeholder="Highlight true/false" />
      <input v-model="shopIncludeAggregations" class="input" placeholder="Include aggregations true/false" />
      <textarea v-model="shopSearchJson" class="textarea" placeholder="Shop search JSON" />
      <view class="row-inline">
        <button class="btn-outline" @click="populateShopSearchPayload">Build shop JSON</button>
        <button class="btn-outline" @click="runShopSearch">Complex search</button>
        <button class="btn-outline" @click="loadShopFilters">Load filters</button>
        <button class="btn-outline" @click="loadShopById">Load shop</button>
        <button class="btn-outline" @click="loadShopSuggestions">Suggestions</button>
        <button class="btn-outline" @click="loadHotShops">Hot shops</button>
        <button class="btn-outline" @click="loadRecommendedShops">Recommended shops</button>
        <button class="btn-outline" @click="searchShopsByGeo">Location search</button>
      </view>
      <text class="code-block">{{ formatJson(shopResult) }}</text>
    </view>
    <view v-if="activeTab === 'batch'" class="panel surface-card fade-in-up">
      <text class="section-title">Batch operations</text>
      <input v-model="userFindUsername" class="input" placeholder="Username" />
      <button class="btn-outline" @click="findUser">Load user</button>
      <text class="code-block">{{ formatJson(userFindResult) }}</text>

      <textarea v-model="userBatchJson" class="textarea" placeholder="User batch update JSON" />
      <button class="btn-outline" @click="updateUsersBatchAction">Batch update users</button>
      <text class="code-block">{{ formatJson(userBatchResult) }}</text>

      <input v-model="merchantBatchIds" class="input" placeholder="Merchant ID list" />
      <input v-model="merchantBatchStatus" class="input" placeholder="Status" />
      <input v-model="merchantBatchRemark" class="input" placeholder="Remark" />
      <view class="row-inline">
        <button class="btn-outline" @click="approveMerchantsBatchAction">Batch approve</button>
        <button class="btn-outline" @click="updateMerchantStatusBatchAction">Batch update status</button>
        <button class="btn-outline" @click="deleteMerchantsBatchAction">Batch delete</button>
      </view>

      <input v-model="merchantAuthBatchIds" class="input" placeholder="Auth merchant ID list" />
      <input v-model="merchantAuthBatchStatus" class="input" placeholder="Auth status" />
      <input v-model="merchantAuthBatchRemark" class="input" placeholder="Auth remark" />
      <button class="btn-outline" @click="reviewMerchantAuthBatchAction">Batch auth review</button>
      <text class="code-block">{{ formatJson(batchResult) }}</text>
    </view>

    <view v-if="activeTab === 'payments'" class="panel surface-card fade-in-up">
      <text class="section-title">Payment operations</text>
      <input v-model="paymentNoInput" class="input" placeholder="Payment number" />
      <input v-model="paymentMainOrderNo" class="input" placeholder="Main order number" />
      <input v-model="paymentSubOrderNo" class="input" placeholder="Sub order number" />
      <view class="row-inline">
        <button class="btn-outline" @click="loadPaymentByNoAction">Load by payment number</button>
        <button class="btn-outline" @click="loadPaymentByOrderAction">Load by order numbers</button>
        <button class="btn-outline" @click="refreshPaymentStatusAction">Load status</button>
        <button class="btn-outline" @click="openPaymentCheckoutAction">Open checkout</button>
      </view>
      <textarea v-model="paymentOrderJson" class="textarea" placeholder="Payment order JSON" />
      <button class="btn-outline" @click="createPaymentOrderAction">Create payment order</button>
      <textarea v-model="paymentRefundJson" class="textarea" placeholder="Payment refund JSON" />
      <button class="btn-outline" @click="createPaymentRefundAction">Create payment refund</button>
      <text class="code-block">{{ formatJson(paymentResult) }}</text>
    </view>

    <view v-if="activeTab === 'stats'" class="panel surface-card fade-in-up">
      <text class="section-title">Statistics operations</text>
      <view class="row-inline">
        <button class="btn-outline" @click="loadStatsOverviewAsync">Async overview</button>
        <button class="btn-outline" @click="refreshStatsCache">Refresh cache</button>
      </view>
      <input v-model="statsStartDate" class="input" placeholder="Start date YYYY-MM-DD" />
      <input v-model="statsEndDate" class="input" placeholder="End date YYYY-MM-DD" />
      <button class="btn-outline" @click="loadStatsTrendRange">Load trend</button>
      <text class="code-block">{{ formatJson(statsOverviewAsync) }}</text>
      <text class="code-block">{{ formatJson(statsTrendRange) }}</text>
      <text class="code-block">{{ formatJson(statsRefreshResult) }}</text>
    </view>
  </AppShell>
</template>

<style scoped>
.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 12px;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid var(--panel-border);
  margin-bottom: 12px;
  box-shadow: var(--shadow-card);
}

.tab {
  padding: 8px 14px;
  border-radius: 4px;
  background: #fbfcfd;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-main);
  border: 1px solid var(--panel-border);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.tab.active {
  background: #111316;
  border-color: var(--panel-border-strong);
  box-shadow: none;
  color: #ffffff;
}

.panel {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  border: 1px solid var(--panel-border);
  background: var(--panel-solid);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.row-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.input {
  background: #fbfcfd;
  border-radius: 4px;
  padding: 12px 14px;
  font-size: 12px;
  color: var(--text-main);
  border: 1px solid var(--panel-border);
}

.textarea {
  background: #fbfcfd;
  border-radius: 4px;
  padding: 12px 14px;
  font-size: 12px;
  color: var(--text-main);
  min-height: 110px;
  border: 1px solid var(--panel-border);
}

.code-block {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 11px;
  white-space: pre-wrap;
  background: #fbfcfd;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid var(--panel-border);
  max-height: 240px;
  overflow: auto;
  color: var(--text-muted);
}

.input:focus,
.textarea:focus {
  border-color: #111316;
  box-shadow: 0 0 0 3px rgba(17, 19, 22, 0.08);
}

@media (hover: hover) {
  .tab:hover {
    transform: translateY(-1px);
    border-color: var(--panel-border-strong);
  }
  .panel:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-float);
    border-color: var(--panel-border-strong);
  }
}

</style>
