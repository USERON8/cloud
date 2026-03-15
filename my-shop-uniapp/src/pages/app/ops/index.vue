<script setup lang="ts">
import { ref, watch } from 'vue'
import AppShell from '../../../components/AppShell.vue'
import { getGitHubAuthStatus, getGitHubUserInfo, logoutAllSessions, validateToken } from '../../../api/auth'
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
import { combinedSearchProducts, smartSearchProducts } from '../../../api/product'
import { findUserByUsername, updateUsersBatch } from '../../../api/user-management'
import { approveMerchantsBatch, deleteMerchantsBatch, updateMerchantStatusBatch } from '../../../api/merchant'
import { reviewMerchantAuthBatch } from '../../../api/merchant-auth'
import { createPaymentOrder, createPaymentRefund, handlePaymentCallback } from '../../../api/payment'
import { getRegistrationTrendRange, getStatisticsOverviewAsync, refreshStatisticsCache } from '../../../api/statistics'
import type {
  PaymentCallbackCommand,
  PaymentOrderCommand,
  PaymentRefundCommand,
  ProductFilterRequest,
  ProductSearchRequest,
  SpuCreateRequest
} from '../../../types/domain'
import { confirm, toast } from '../../../utils/ui'

const tabs = [
  { key: 'tokens', label: 'Token与授权' },
  { key: 'system', label: '线程池' },
  { key: 'category', label: '分类运维' },
  { key: 'catalog', label: '商品目录' },
  { key: 'search', label: '搜索运维' },
  { key: 'shops', label: '店铺搜索' },
  { key: 'batch', label: '批量运维' },
  { key: 'payments', label: '支付运维' },
  { key: 'stats', label: '统计运维' }
]

const activeTab = ref('tokens')

function formatJson(value: unknown): string {
  if (value == null) {
    return '--'
  }
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

function parseJson<T>(raw: string, label: string): T | null {
  if (!raw.trim()) {
    toast(`${label} JSON 不能为空`)
    return null
  }
  try {
    return JSON.parse(raw) as T
  } catch (error) {
    toast(error instanceof Error ? error.message : `${label} JSON 解析失败`)
    return null
  }
}

function parseNumberList(raw: string): number[] {
  return raw
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isFinite(value) && value > 0)
}

function requirePositiveId(raw: string, label: string): number | null {
  const value = Number(raw)
  if (!Number.isFinite(value) || value <= 0) {
    toast(`请输入${label}`)
    return null
  }
  return value
}

function parseOptionalNumber(raw: string, label: string): number | undefined | null {
  if (!raw.trim()) {
    return undefined
  }
  const value = Number(raw)
  if (!Number.isFinite(value)) {
    toast(`${label}需要为数字`)
    return null
  }
  return value
}

function resolveSearchPaging(): { page: number; size: number } | null {
  const page = Number(searchPage.value)
  const size = Number(searchSize.value)
  if (!Number.isInteger(page) || page < 0) {
    toast('页码需为非负整数')
    return null
  }
  if (!Number.isInteger(size) || size <= 0) {
    toast('分页大小需为正整数')
    return null
  }
  searchPage.value = String(page)
  searchSize.value = String(size)
  return { page, size }
}

function resolvePriceRange(): { minPrice?: number; maxPrice?: number } | null {
  const minPrice = parseOptionalNumber(searchMinPrice.value, '最低价')
  if (minPrice === null) return null
  const maxPrice = parseOptionalNumber(searchMaxPrice.value, '最高价')
  if (maxPrice === null) return null
  if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
    toast('最低价不能大于最高价')
    return null
  }
  return { minPrice: minPrice ?? undefined, maxPrice: maxPrice ?? undefined }
}

function normalizeDateInput(value: string, label: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) {
    toast(`请输入${label}`)
    return null
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
    toast(`${label}格式应为 YYYY-MM-DD`)
    return null
  }
  return trimmed
}

// Token & 授权
const tokenStats = ref<Record<string, unknown> | null>(null)
const storageStructure = ref<Record<string, unknown> | null>(null)
const authorizationId = ref('')
const authorizationDetail = ref<Record<string, unknown> | null>(null)
const cleanupResult = ref<Record<string, unknown> | null>(null)
const tokenValidationMessage = ref<string | null>(null)
const blacklistStats = ref<Record<string, unknown> | null>(null)
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
    toast('已获取 Token 统计', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadAuthorizationDetail(): Promise<void> {
  if (!authorizationId.value.trim()) {
    toast('请输入授权 ID')
    return
  }
  try {
    authorizationDetail.value = await getAuthorizationDetails(authorizationId.value.trim())
    toast('已获取授权详情', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function revokeAuthorizationRow(): Promise<void> {
  if (!authorizationId.value.trim()) {
    toast('请输入授权 ID')
    return
  }
  const ok = await confirm('确认撤销该授权？')
  if (!ok) return
  try {
    await revokeAuthorization(authorizationId.value.trim())
    toast('已撤销', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '撤销失败')
  }
}

async function runTokenCleanup(): Promise<void> {
  try {
    cleanupResult.value = await cleanupExpiredTokens()
    toast('已触发清理', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '清理失败')
  }
}

async function validateCurrentToken(): Promise<void> {
  try {
    tokenValidationMessage.value = await validateToken()
    toast('Token 校验成功', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '校验失败')
  }
}

async function loadStorageStructure(): Promise<void> {
  try {
    storageStructure.value = await getStorageStructure()
    toast('已获取存储结构', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadBlacklistStats(): Promise<void> {
  try {
    blacklistStats.value = await getBlacklistStats()
    toast('已获取黑名单统计', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function addBlacklist(): Promise<void> {
  if (!blacklistToken.value.trim()) {
    toast('请输入 Token')
    return
  }
  try {
    await addTokenToBlacklist(blacklistToken.value.trim(), blacklistReason.value.trim() || undefined)
    toast('已加入黑名单', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '添加失败')
  }
}

async function checkBlacklistStatus(): Promise<void> {
  if (!blacklistToken.value.trim()) {
    toast('请输入 Token')
    return
  }
  try {
    blacklistCheckResult.value = await checkBlacklist(blacklistToken.value.trim())
    toast('已查询黑名单状态', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '查询失败')
  }
}

async function cleanupBlacklistEntries(): Promise<void> {
  try {
    blacklistCleanupResult.value = await cleanupBlacklist()
    toast('已触发黑名单清理', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '清理失败')
  }
}

async function loadGitHubStatus(): Promise<void> {
  try {
    gitHubStatus.value = await getGitHubAuthStatus()
    toast('已获取 GitHub 授权状态', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadGitHubUser(): Promise<void> {
  try {
    gitHubUserInfo.value = await getGitHubUserInfo()
    toast('已获取 GitHub 用户', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function logoutAll(): Promise<void> {
  if (!logoutUsername.value.trim()) {
    toast('请输入用户名')
    return
  }
  try {
    await logoutAllSessions(logoutUsername.value.trim())
    toast('已触发全量下线', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '操作失败')
  }
}
// 线程池
const threadPools = ref<Record<string, unknown>[] | null>(null)
const threadPoolName = ref('')
const threadPoolDetail = ref<Record<string, unknown> | null>(null)

async function loadThreadPools(): Promise<void> {
  try {
    threadPools.value = await getThreadPools()
    toast('已获取线程池信息', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadThreadPoolDetail(): Promise<void> {
  if (!threadPoolName.value.trim()) {
    toast('请输入线程池名称')
    return
  }
  try {
    threadPoolDetail.value = await getThreadPoolDetail(threadPoolName.value.trim())
    toast('已获取线程池详情', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

// 分类运维
const categoryIdInput = ref('')
const categorySortInput = ref('')
const categoryMoveParent = ref('')
const categoryBatchIds = ref('')
const categoryBatchStatus = ref('')
const categoryBatchPayload = ref('')
const categoryTree = ref<unknown>(null)
const categoryChildren = ref<unknown>(null)
const categoryById = ref<unknown>(null)
const categoryBatchResult = ref<unknown>(null)

async function loadCategoryTree(): Promise<void> {
  try {
    categoryTree.value = await getCategoryTree(false)
    toast('已获取分类树', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadCategoryChildren(): Promise<void> {
  const id = Number(categoryIdInput.value)
  if (!Number.isFinite(id)) {
    toast('请输入分类 ID')
    return
  }
  try {
    categoryChildren.value = await getCategoryChildren(id, false)
    toast('已获取子分类', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadCategoryById(): Promise<void> {
  const id = Number(categoryIdInput.value)
  if (!Number.isFinite(id)) {
    toast('请输入分类 ID')
    return
  }
  try {
    categoryById.value = await getCategoryById(id)
    toast('已获取分类详情', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function updateCategoryStatusBatchAction(): Promise<void> {
  const ids = parseNumberList(categoryBatchIds.value)
  const status = Number(categoryBatchStatus.value)
  if (ids.length === 0 || !Number.isFinite(status)) {
    toast('请输入批量 ID 和状态')
    return
  }
  try {
    categoryBatchResult.value = await updateCategoryStatusBatch(ids, status)
    toast('批量状态已更新', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
  }
}

async function createCategoriesBatchAction(): Promise<void> {
  const payload = parseJson<unknown[]>(categoryBatchPayload.value, '分类批量')
  if (!payload) return
  try {
    categoryBatchResult.value = await createCategoriesBatch(payload as never[])
    toast('批量创建已提交', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '创建失败')
  }
}

async function deleteCategoriesBatchAction(): Promise<void> {
  const ids = parseNumberList(categoryBatchIds.value)
  if (ids.length === 0) {
    toast('请输入批量 ID')
    return
  }
  try {
    categoryBatchResult.value = await deleteCategoriesBatch(ids)
    toast('批量删除已提交', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '删除失败')
  }
}

async function updateCategorySortAction(): Promise<void> {
  const id = Number(categoryIdInput.value)
  const sort = Number(categorySortInput.value)
  if (!Number.isFinite(id) || !Number.isFinite(sort)) {
    toast('请输入分类 ID 和排序值')
    return
  }
  try {
    await updateCategorySort(id, sort)
    toast('排序已更新', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
  }
}

async function moveCategoryAction(): Promise<void> {
  const id = Number(categoryIdInput.value)
  const parentId = Number(categoryMoveParent.value)
  if (!Number.isFinite(id) || !Number.isFinite(parentId)) {
    toast('请输入分类 ID 和新父级 ID')
    return
  }
  try {
    await moveCategory(id, parentId)
    toast('分类已移动', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '移动失败')
  }
}
// 商品目录
const spuId = ref('')
const spuStatus = ref('')
const spuCategoryId = ref('')
const skuIds = ref('')
const spuPayloadJson = ref('')
const spuDetail = ref<unknown>(null)
const spuList = ref<unknown>(null)
const skuList = ref<unknown>(null)
const spuActionResult = ref<unknown>(null)

async function createSpuAction(): Promise<void> {
  const payload = parseJson<SpuCreateRequest>(spuPayloadJson.value, 'SPU')
  if (!payload) return
  try {
    spuActionResult.value = await createSpu(payload)
    toast('SPU 创建成功', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '创建失败')
  }
}

async function updateSpuAction(): Promise<void> {
  const id = Number(spuId.value)
  const payload = parseJson<SpuCreateRequest>(spuPayloadJson.value, 'SPU')
  if (!Number.isFinite(id) || !payload) {
    toast('请输入 SPU ID 并提供 JSON')
    return
  }
  try {
    spuActionResult.value = await updateSpu(id, payload)
    toast('SPU 已更新', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
  }
}

async function loadSpuDetail(): Promise<void> {
  const id = Number(spuId.value)
  if (!Number.isFinite(id)) {
    toast('请输入 SPU ID')
    return
  }
  try {
    spuDetail.value = await getSpu(id)
    toast('已获取 SPU 详情', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadSpuByCategory(): Promise<void> {
  const id = Number(spuCategoryId.value)
  const status = spuStatus.value ? Number(spuStatus.value) : undefined
  if (!Number.isFinite(id)) {
    toast('请输入分类 ID')
    return
  }
  try {
    spuList.value = await listSpuByCategory(id, Number.isFinite(status) ? status : undefined)
    toast('已获取分类 SPU', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadSkuList(): Promise<void> {
  const ids = parseNumberList(skuIds.value)
  if (ids.length === 0) {
    toast('请输入 SKU ID 列表')
    return
  }
  try {
    skuList.value = await listSkuByIds(ids)
    toast('已获取 SKU 列表', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function updateSpuStatusAction(): Promise<void> {
  const id = Number(spuId.value)
  const status = Number(spuStatus.value)
  if (!Number.isFinite(id) || !Number.isFinite(status)) {
    toast('请输入 SPU ID 和状态')
    return
  }
  try {
    spuActionResult.value = await updateSpuStatus(id, status)
    toast('状态已更新', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
  }
}


const searchKeyword = ref('')
const searchPage = ref('0')
const searchSize = ref('20')
const searchCategoryId = ref('')
const searchShopId = ref('')
const searchBrandId = ref('')
const searchMinPrice = ref('')
const searchMaxPrice = ref('')
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
    toast('基础搜索完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '搜索失败')
  }
}

async function runAdvancedSearch(): Promise<void> {
  if (!searchKeyword.value.trim()) {
    toast('请输入关键词')
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
    toast('高级搜索完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '搜索失败')
  }
}

async function runSearchByCategory(): Promise<void> {
  const id = requirePositiveId(searchCategoryId.value, '分类 ID')
  if (!id) return
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await searchByCategory(id, {
      keyword: searchKeyword.value || undefined,
      page: paging.page,
      size: paging.size
    })
    toast('分类搜索完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '搜索失败')
  }
}

async function runSearchByShop(): Promise<void> {
  const id = requirePositiveId(searchShopId.value, '店铺 ID')
  if (!id) return
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await searchByShop(id, {
      keyword: searchKeyword.value || undefined,
      page: paging.page,
      size: paging.size
    })
    toast('店铺搜索完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '搜索失败')
  }
}

async function runFilterByCategory(): Promise<void> {
  const id = requirePositiveId(searchCategoryId.value, '分类 ID')
  if (!id) return
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await filterByCategory(id, {
      page: paging.page,
      size: paging.size
    })
    toast('分类筛选完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '筛选失败')
  }
}

async function runFilterByBrand(): Promise<void> {
  const id = requirePositiveId(searchBrandId.value, '品牌 ID')
  if (!id) return
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await filterByBrand(id, {
      page: paging.page,
      size: paging.size
    })
    toast('品牌筛选完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '筛选失败')
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
    toast('价格筛选完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '筛选失败')
  }
}

async function runFilterByShop(): Promise<void> {
  const id = requirePositiveId(searchShopId.value, '店铺 ID')
  if (!id) return
  const paging = resolveSearchPaging()
  if (!paging) return
  try {
    searchResult.value = await filterByShop(id, {
      page: paging.page,
      size: paging.size
    })
    toast('店铺筛选完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '筛选失败')
  }
}

async function runComplexSearch(): Promise<void> {
  const payload = parseJson<ProductSearchRequest>(searchComplexJson.value, '复杂搜索')
  if (!payload) return
  try {
    searchResult.value = await complexSearch(payload)
    toast('复杂搜索完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '搜索失败')
  }
}

async function runFilterSearch(): Promise<void> {
  const payload = parseJson<ProductFilterRequest>(searchFilterJson.value, '筛选搜索')
  if (!payload) return
  try {
    searchResult.value = await filterSearch(payload)
    toast('筛选搜索完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '搜索失败')
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
    toast('推荐列表获取完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function runCombinedSearch(): Promise<void> {
  const paging = resolveSearchPaging()
  if (!paging) return
  const priceRange = resolvePriceRange()
  if (!priceRange) return
  const categoryId = parseOptionalNumber(searchCategoryId.value, '分类 ID')
  if (categoryId === null) return
  const brandId = parseOptionalNumber(searchBrandId.value, '品牌 ID')
  if (brandId === null) return
  const shopId = parseOptionalNumber(searchShopId.value, '店铺 ID')
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
    toast('组合搜索完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '搜索失败')
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
    toast('智能搜索完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '搜索失败')
  }
}

async function runSearchSuggest(): Promise<void> {
  try {
    searchResult.value = await getProductFilters({
      keyword: searchKeyword.value || undefined
    })
    toast('搜索筛选已获取', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function runSearchProducts(): Promise<void> {
  if (!searchKeyword.value.trim()) {
    toast('请输入关键词')
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
    toast('搜索完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '搜索失败')
  }
}

// 店铺搜索
const shopId = ref('')
const shopLocation = ref('')
const shopSuggestKeyword = ref('')
const shopSearchJson = ref('')
const shopResult = ref<unknown>(null)

async function runShopSearch(): Promise<void> {
  const payload = parseJson<ShopSearchRequest>(shopSearchJson.value, '店铺搜索')
  if (!payload) return
  try {
    shopResult.value = await searchShops(payload)
    toast('店铺搜索完成', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '搜索失败')
  }
}

async function loadShopById(): Promise<void> {
  const id = Number(shopId.value)
  if (!Number.isFinite(id)) {
    toast('请输入店铺 ID')
    return
  }
  try {
    shopResult.value = await getShopById(id)
    toast('已获取店铺详情', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadShopSuggestions(): Promise<void> {
  if (!shopSuggestKeyword.value.trim()) {
    toast('请输入关键词')
    return
  }
  try {
    shopResult.value = await listShopSuggestions(shopSuggestKeyword.value.trim(), 10)
    toast('已获取联想词', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadShopFilters(): Promise<void> {
  const payload = parseJson<ShopSearchRequest>(shopSearchJson.value, '店铺过滤')
  if (!payload) return
  try {
    shopResult.value = await getShopFilters(payload)
    toast('已获取筛选信息', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadHotShops(): Promise<void> {
  try {
    shopResult.value = await listHotShops(10)
    toast('已获取热门店铺', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadRecommendedShops(): Promise<void> {
  try {
    shopResult.value = await listRecommendedShops(0, 20)
    toast('已获取推荐店铺', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function searchShopsByGeo(): Promise<void> {
  if (!shopLocation.value.trim()) {
    toast('请输入地址关键字')
    return
  }
  try {
    shopResult.value = await searchShopsByLocation(shopLocation.value.trim(), 0, 20)
    toast('已按位置搜索', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '搜索失败')
  }
}
// 批量运维
const userFindUsername = ref('')
const userBatchJson = ref('')
const userBatchResult = ref<unknown>(null)
const userFindResult = ref<unknown>(null)
const merchantBatchIds = ref('')
const merchantBatchStatus = ref('')
const merchantBatchRemark = ref('')
const merchantAuthBatchIds = ref('')
const merchantAuthBatchStatus = ref('')
const merchantAuthBatchRemark = ref('')
const batchResult = ref<unknown>(null)

async function findUser(): Promise<void> {
  if (!userFindUsername.value.trim()) {
    toast('请输入用户名')
    return
  }
  try {
    userFindResult.value = await findUserByUsername(userFindUsername.value.trim())
    toast('已查询用户', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '查询失败')
  }
}

async function updateUsersBatchAction(): Promise<void> {
  const payload = parseJson<unknown[]>(userBatchJson.value, '用户批量')
  if (!payload) return
  try {
    userBatchResult.value = await updateUsersBatch(payload as never[])
    toast('批量更新已提交', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
  }
}

async function approveMerchantsBatchAction(): Promise<void> {
  const ids = parseNumberList(merchantBatchIds.value)
  if (ids.length === 0) {
    toast('请输入商家 ID 列表')
    return
  }
  try {
    batchResult.value = await approveMerchantsBatch(ids, merchantBatchRemark.value || undefined)
    toast('批量审核通过', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '操作失败')
  }
}

async function deleteMerchantsBatchAction(): Promise<void> {
  const ids = parseNumberList(merchantBatchIds.value)
  if (ids.length === 0) {
    toast('请输入商家 ID 列表')
    return
  }
  const ok = await confirm('确认批量删除商家？')
  if (!ok) return
  try {
    batchResult.value = await deleteMerchantsBatch(ids)
    toast('批量删除已提交', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '删除失败')
  }
}

async function updateMerchantStatusBatchAction(): Promise<void> {
  const ids = parseNumberList(merchantBatchIds.value)
  const status = Number(merchantBatchStatus.value)
  if (ids.length === 0 || !Number.isFinite(status)) {
    toast('请输入商家 ID 和状态')
    return
  }
  try {
    batchResult.value = await updateMerchantStatusBatch(ids, status)
    toast('批量状态已更新', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '更新失败')
  }
}

async function reviewMerchantAuthBatchAction(): Promise<void> {
  const ids = parseNumberList(merchantAuthBatchIds.value)
  const status = Number(merchantAuthBatchStatus.value)
  if (ids.length === 0 || !Number.isFinite(status)) {
    toast('请输入商家 ID 和审核状态')
    return
  }
  try {
    batchResult.value = await reviewMerchantAuthBatch(ids, status, merchantAuthBatchRemark.value || undefined)
    toast('批量审核已提交', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '操作失败')
  }
}

// 支付运维
const paymentOrderJson = ref('')
const paymentRefundJson = ref('')
const paymentCallbackJson = ref('')
const paymentResult = ref<unknown>(null)

async function createPaymentOrderAction(): Promise<void> {
  const payload = parseJson<PaymentOrderCommand>(paymentOrderJson.value, '支付单')
  if (!payload) return
  try {
    paymentResult.value = await createPaymentOrder(payload)
    toast('支付单已创建', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '创建失败')
  }
}

async function createPaymentRefundAction(): Promise<void> {
  const payload = parseJson<PaymentRefundCommand>(paymentRefundJson.value, '退款单')
  if (!payload) return
  try {
    paymentResult.value = await createPaymentRefund(payload)
    toast('退款单已创建', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '创建失败')
  }
}

async function handlePaymentCallbackAction(): Promise<void> {
  const payload = parseJson<PaymentCallbackCommand>(paymentCallbackJson.value, '回调')
  if (!payload) return
  try {
    paymentResult.value = await handlePaymentCallback(payload)
    toast('回调已处理', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '处理失败')
  }
}

// 统计运维
const statsStartDate = ref('')
const statsEndDate = ref('')
const statsOverviewAsync = ref<unknown>(null)
const statsTrendRange = ref<unknown>(null)
const statsRefreshResult = ref<unknown>(null)

async function loadStatsOverviewAsync(): Promise<void> {
  try {
    statsOverviewAsync.value = await getStatisticsOverviewAsync()
    toast('已获取异步统计', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function loadStatsTrendRange(): Promise<void> {
  const start = normalizeDateInput(statsStartDate.value, '开始日期')
  if (!start) return
  const end = normalizeDateInput(statsEndDate.value, '结束日期')
  if (!end) return
  if (new Date(start).getTime() > new Date(end).getTime()) {
    toast('开始日期不能晚于结束日期')
    return
  }
  try {
    statsTrendRange.value = await getRegistrationTrendRange(start, end)
    toast('已获取趋势', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '获取失败')
  }
}

async function refreshStatsCache(): Promise<void> {
  try {
    statsRefreshResult.value = await refreshStatisticsCache()
    toast('缓存刷新已触发', 'success')
  } catch (error) {
    toast(error instanceof Error ? error.message : '刷新失败')
  }
}
</script>
<template>
  <AppShell title="Ops Center">
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

    <view v-if="activeTab === 'tokens'" class="panel glass-card">
      <text class="section-title">Token 统计</text>
      <view class="row-inline">
        <button class="btn-outline" @click="loadTokenStats">获取统计</button>
        <button class="btn-outline" @click="loadStorageStructure">存储结构</button>
        <button class="btn-outline" @click="runTokenCleanup">清理过期 Token</button>
        <button class="btn-outline" @click="validateCurrentToken">校验当前 Token</button>
      </view>
      <text class="code-block">{{ formatJson(tokenStats) }}</text>
      <text class="code-block">{{ formatJson(storageStructure) }}</text>
      <text class="code-block">{{ formatJson(cleanupResult) }}</text>
      <text class="code-block">{{ tokenValidationMessage || '--' }}</text>

      <view class="section">
        <text class="section-title">授权详情</text>
        <input v-model="authorizationId" class="input" placeholder="授权 ID" />
        <view class="row-inline">
          <button class="btn-outline" @click="loadAuthorizationDetail">查询</button>
          <button class="btn-outline" @click="revokeAuthorizationRow">撤销</button>
        </view>
        <text class="code-block">{{ formatJson(authorizationDetail) }}</text>
      </view>

      <view class="section">
        <text class="section-title">黑名单</text>
        <input v-model="blacklistToken" class="input" placeholder="Token" />
        <input v-model="blacklistReason" class="input" placeholder="原因(可选)" />
        <view class="row-inline">
          <button class="btn-outline" @click="addBlacklist">加入黑名单</button>
          <button class="btn-outline" @click="checkBlacklistStatus">查询状态</button>
          <button class="btn-outline" @click="cleanupBlacklistEntries">清理黑名单</button>
          <button class="btn-outline" @click="loadBlacklistStats">黑名单统计</button>
        </view>
        <text class="code-block">{{ formatJson(blacklistStats) }}</text>
        <text class="code-block">{{ formatJson(blacklistCheckResult) }}</text>
        <text class="code-block">{{ formatJson(blacklistCleanupResult) }}</text>
      </view>

      <view class="section">
        <text class="section-title">GitHub 授权</text>
        <view class="row-inline">
          <button class="btn-outline" @click="loadGitHubStatus">授权状态</button>
          <button class="btn-outline" @click="loadGitHubUser">用户信息</button>
        </view>
        <text class="code-block">{{ formatJson(gitHubStatus) }}</text>
        <text class="code-block">{{ formatJson(gitHubUserInfo) }}</text>
      </view>

      <view class="section">
        <text class="section-title">全量下线</text>
        <input v-model="logoutUsername" class="input" placeholder="用户名" />
        <button class="btn-outline" @click="logoutAll">触发下线</button>
      </view>
    </view>
    <view v-if="activeTab === 'system'" class="panel glass-card">
      <text class="section-title">线程池</text>
      <view class="row-inline">
        <button class="btn-outline" @click="loadThreadPools">获取线程池</button>
      </view>
      <text class="code-block">{{ formatJson(threadPools) }}</text>

      <view class="section">
        <text class="section-title">线程池详情</text>
        <input v-model="threadPoolName" class="input" placeholder="线程池名称" />
        <button class="btn-outline" @click="loadThreadPoolDetail">查询详情</button>
        <text class="code-block">{{ formatJson(threadPoolDetail) }}</text>
      </view>
    </view>

    <view v-if="activeTab === 'category'" class="panel glass-card">
      <text class="section-title">分类运维</text>
      <input v-model="categoryIdInput" class="input" placeholder="分类 ID" />
      <input v-model="categorySortInput" class="input" placeholder="排序值" />
      <input v-model="categoryMoveParent" class="input" placeholder="新父级 ID" />
      <view class="row-inline">
        <button class="btn-outline" @click="loadCategoryTree">分类树</button>
        <button class="btn-outline" @click="loadCategoryChildren">子分类</button>
        <button class="btn-outline" @click="loadCategoryById">分类详情</button>
        <button class="btn-outline" @click="updateCategorySortAction">更新排序</button>
        <button class="btn-outline" @click="moveCategoryAction">移动分类</button>
      </view>
      <text class="code-block">{{ formatJson(categoryTree) }}</text>
      <text class="code-block">{{ formatJson(categoryChildren) }}</text>
      <text class="code-block">{{ formatJson(categoryById) }}</text>

      <view class="section">
        <text class="section-title">批量操作</text>
        <input v-model="categoryBatchIds" class="input" placeholder="ID 列表(逗号分隔)" />
        <input v-model="categoryBatchStatus" class="input" placeholder="状态" />
        <textarea v-model="categoryBatchPayload" class="textarea" placeholder="批量创建 JSON" />
        <view class="row-inline">
          <button class="btn-outline" @click="updateCategoryStatusBatchAction">批量状态</button>
          <button class="btn-outline" @click="createCategoriesBatchAction">批量创建</button>
          <button class="btn-outline" @click="deleteCategoriesBatchAction">批量删除</button>
        </view>
        <text class="code-block">{{ formatJson(categoryBatchResult) }}</text>
      </view>
    </view>
    <view v-if="activeTab === 'catalog'" class="panel glass-card">
      <text class="section-title">商品目录</text>
      <input v-model="spuId" class="input" placeholder="SPU ID" />
      <input v-model="spuStatus" class="input" placeholder="状态" />
      <input v-model="spuCategoryId" class="input" placeholder="分类 ID" />
      <input v-model="skuIds" class="input" placeholder="SKU ID 列表" />
      <textarea v-model="spuPayloadJson" class="textarea" placeholder="SPU 创建/更新 JSON" />
      <view class="row-inline">
        <button class="btn-outline" @click="createSpuAction">创建 SPU</button>
        <button class="btn-outline" @click="updateSpuAction">更新 SPU</button>
        <button class="btn-outline" @click="loadSpuDetail">查询 SPU</button>
        <button class="btn-outline" @click="loadSpuByCategory">分类 SPU</button>
        <button class="btn-outline" @click="loadSkuList">SKU 列表</button>
        <button class="btn-outline" @click="updateSpuStatusAction">更新状态</button>
      </view>
      <text class="code-block">{{ formatJson(spuActionResult) }}</text>
      <text class="code-block">{{ formatJson(spuDetail) }}</text>
      <text class="code-block">{{ formatJson(spuList) }}</text>
      <text class="code-block">{{ formatJson(skuList) }}</text>
    </view>

    <view v-if="activeTab === 'search'" class="panel glass-card">
      <text class="section-title">搜索运维</text>
      <input v-model="searchKeyword" class="input" placeholder="关键词" />
      <input v-model="searchPage" class="input" placeholder="页码" />
      <input v-model="searchSize" class="input" placeholder="分页大小" />
      <input v-model="searchCategoryId" class="input" placeholder="分类 ID" />
      <input v-model="searchShopId" class="input" placeholder="店铺 ID" />
      <input v-model="searchBrandId" class="input" placeholder="品牌 ID" />
      <input v-model="searchMinPrice" class="input" placeholder="最低价" />
      <input v-model="searchMaxPrice" class="input" placeholder="最高价" />
      <textarea v-model="searchComplexJson" class="textarea" placeholder="复杂搜索 JSON" />
      <textarea v-model="searchFilterJson" class="textarea" placeholder="筛选搜索 JSON" />
      <view class="row-inline">
        <button class="btn-outline" @click="runBasicSearch">基础搜索</button>
        <button class="btn-outline" @click="runAdvancedSearch">高级搜索</button>
        <button class="btn-outline" @click="runSearchByCategory">分类搜索</button>
        <button class="btn-outline" @click="runSearchByShop">店铺搜索</button>
        <button class="btn-outline" @click="runFilterByCategory">分类筛选</button>
        <button class="btn-outline" @click="runFilterByBrand">品牌筛选</button>
        <button class="btn-outline" @click="runFilterByPrice">价格筛选</button>
        <button class="btn-outline" @click="runFilterByShop">店铺筛选</button>
        <button class="btn-outline" @click="runSearchProducts">搜索商品</button>
        <button class="btn-outline" @click="runSearchSuggest">筛选信息</button>
        <button class="btn-outline" @click="runComplexSearch">复杂搜索</button>
        <button class="btn-outline" @click="runFilterSearch">筛选搜索</button>
        <button class="btn-outline" @click="runCombinedSearch">组合搜索</button>
        <button class="btn-outline" @click="runSmartSearch">智能搜索</button>
        <button class="btn-outline" @click="runSearchRecommend('hot')">热门</button>
        <button class="btn-outline" @click="runSearchRecommend('new')">新品</button>
        <button class="btn-outline" @click="runSearchRecommend('recommended')">推荐</button>
      </view>
      <text class="code-block">{{ formatJson(searchResult) }}</text>
    </view>

    <view v-if="activeTab === 'shops'" class="panel glass-card">
      <text class="section-title">店铺搜索</text>
      <input v-model="shopId" class="input" placeholder="店铺 ID" />
      <input v-model="shopLocation" class="input" placeholder="位置关键词" />
      <input v-model="shopSuggestKeyword" class="input" placeholder="联想词关键词" />
      <textarea v-model="shopSearchJson" class="textarea" placeholder="店铺搜索 JSON" />
      <view class="row-inline">
        <button class="btn-outline" @click="runShopSearch">复杂搜索</button>
        <button class="btn-outline" @click="loadShopFilters">筛选</button>
        <button class="btn-outline" @click="loadShopById">查询店铺</button>
        <button class="btn-outline" @click="loadShopSuggestions">联想词</button>
        <button class="btn-outline" @click="loadHotShops">热门店铺</button>
        <button class="btn-outline" @click="loadRecommendedShops">推荐店铺</button>
        <button class="btn-outline" @click="searchShopsByGeo">位置搜索</button>
      </view>
      <text class="code-block">{{ formatJson(shopResult) }}</text>
    </view>
    <view v-if="activeTab === 'batch'" class="panel glass-card">
      <text class="section-title">批量运维</text>
      <input v-model="userFindUsername" class="input" placeholder="用户名" />
      <button class="btn-outline" @click="findUser">查询用户</button>
      <text class="code-block">{{ formatJson(userFindResult) }}</text>

      <textarea v-model="userBatchJson" class="textarea" placeholder="用户批量更新 JSON" />
      <button class="btn-outline" @click="updateUsersBatchAction">批量更新用户</button>
      <text class="code-block">{{ formatJson(userBatchResult) }}</text>

      <input v-model="merchantBatchIds" class="input" placeholder="商家 ID 列表" />
      <input v-model="merchantBatchStatus" class="input" placeholder="状态" />
      <input v-model="merchantBatchRemark" class="input" placeholder="备注" />
      <view class="row-inline">
        <button class="btn-outline" @click="approveMerchantsBatchAction">批量通过</button>
        <button class="btn-outline" @click="updateMerchantStatusBatchAction">批量更新状态</button>
        <button class="btn-outline" @click="deleteMerchantsBatchAction">批量删除</button>
      </view>

      <input v-model="merchantAuthBatchIds" class="input" placeholder="认证商家 ID 列表" />
      <input v-model="merchantAuthBatchStatus" class="input" placeholder="认证状态" />
      <input v-model="merchantAuthBatchRemark" class="input" placeholder="认证备注" />
      <button class="btn-outline" @click="reviewMerchantAuthBatchAction">批量认证审核</button>
      <text class="code-block">{{ formatJson(batchResult) }}</text>
    </view>

    <view v-if="activeTab === 'payments'" class="panel glass-card">
      <text class="section-title">支付运维</text>
      <textarea v-model="paymentOrderJson" class="textarea" placeholder="支付单 JSON" />
      <button class="btn-outline" @click="createPaymentOrderAction">创建支付单</button>
      <textarea v-model="paymentRefundJson" class="textarea" placeholder="退款单 JSON" />
      <button class="btn-outline" @click="createPaymentRefundAction">创建退款单</button>
      <textarea v-model="paymentCallbackJson" class="textarea" placeholder="回调 JSON" />
      <button class="btn-outline" @click="handlePaymentCallbackAction">处理回调</button>
      <text class="code-block">{{ formatJson(paymentResult) }}</text>
    </view>

    <view v-if="activeTab === 'stats'" class="panel glass-card">
      <text class="section-title">统计运维</text>
      <view class="row-inline">
        <button class="btn-outline" @click="loadStatsOverviewAsync">异步概览</button>
        <button class="btn-outline" @click="refreshStatsCache">刷新缓存</button>
      </view>
      <input v-model="statsStartDate" class="input" placeholder="开始日期 YYYY-MM-DD" />
      <input v-model="statsEndDate" class="input" placeholder="结束日期 YYYY-MM-DD" />
      <button class="btn-outline" @click="loadStatsTrendRange">查询趋势</button>
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
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  margin-bottom: 12px;
}

.tab {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.tab.active {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 18px rgba(32, 40, 53, 0.08);
}

.panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
}

.row-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.input {
  background: #fff;
  border-radius: 10px;
  padding: 6px 10px;
  font-size: 12px;
}

.textarea {
  background: #fff;
  border-radius: 10px;
  padding: 8px 10px;
  font-size: 12px;
  min-height: 90px;
}

.code-block {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 11px;
  white-space: pre-wrap;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  padding: 10px;
}
</style>


