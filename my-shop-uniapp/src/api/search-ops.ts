import http from './http'
import { searchProducts as searchProductsByName } from './product'
import type {
  ProductDocument,
  ProductFilterRequest,
  ProductSearchRequest,
  SearchProductDocument,
  SearchResult,
  SmartSearchResult
} from '../types/domain'

const SEARCH_FALLBACK_TIMEOUT_MS = Number(import.meta.env.VITE_SEARCH_FALLBACK_TIMEOUT || 5000)
const SUGGESTION_CACHE_TTL = 30_000
const HOT_KEYWORD_CACHE_TTL = 60_000
const RECOMMENDATION_CACHE_TTL = 45_000
const SEARCH_RESULT_CACHE_TTL = 30_000

type CacheEntry<T> = {
  expiresAt: number
  value: T
}

const suggestionCache = new Map<string, CacheEntry<string[]>>()
const hotKeywordCache = new Map<string, CacheEntry<string[]>>()
const recommendationCache = new Map<string, CacheEntry<string[]>>()
const searchResultCache = new Map<string, CacheEntry<SmartSearchResult>>()

function getCachedValue<T>(cache: Map<string, CacheEntry<T>>, key: string): T | null {
  const entry = cache.get(key)
  if (!entry) {
    return null
  }
  if (Date.now() > entry.expiresAt) {
    cache.delete(key)
    return null
  }
  return entry.value
}

function setCachedValue<T>(cache: Map<string, CacheEntry<T>>, key: string, value: T, ttl: number): void {
  cache.set(key, { value, expiresAt: Date.now() + ttl })
}

function buildSmartSearchCacheKey(params: {
  keyword?: string
  page?: number
  size?: number
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}): string {
  return JSON.stringify({
    keyword: params.keyword || '',
    page: params.page || 1,
    size: params.size || 20,
    sortField: params.sortField || '',
    sortOrder: params.sortOrder || ''
  })
}

function buildEmptySmartSearchResult(params: {
  page?: number
  size?: number
}): SmartSearchResult {
  const page = Math.max(params.page || 1, 1)
  const size = Math.max(params.size || 20, 1)
  return {
    documents: [],
    total: 0,
    from: (page - 1) * size,
    size,
    aggregations: {}
  }
}

function withTimeout<T>(promise: Promise<T>, timeoutMs = SEARCH_FALLBACK_TIMEOUT_MS): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => {
      reject(new Error(`Search service timeout after ${timeoutMs}ms`))
    }, timeoutMs)
    promise
      .then((result) => resolve(result))
      .catch((error) => reject(error))
      .finally(() => {
        clearTimeout(timer)
      })
  })
}

function toSearchDocumentFromProductDocument(item: ProductDocument): SearchProductDocument {
  return {
    productId: item.productId,
    shopId: item.shopId,
    shopName: item.shopName,
    productName: item.productName,
    price: item.price,
    stockQuantity: item.stockQuantity,
    categoryId: item.categoryId,
    brandId: item.brandId,
    status: item.status,
    description: item.description,
    imageUrl: item.imageUrl
  }
}

export function complexSearch(
  request: ProductSearchRequest,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.post<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/products', request, {
    params: { searchAfter }
  })
}

export function getProductFilters(
  request: ProductSearchRequest,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.post<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/products/filters', request, {
    params: { searchAfter }
  })
}

export function basicSearch(params: {
  keyword?: string
  page?: number
  size?: number
  searchAfter?: string
}): Promise<SearchResult<ProductDocument>> {
  return complexSearch(
    {
      keyword: params.keyword,
      page: params.page,
      size: params.size
    },
    params.searchAfter
  )
}

export function smartSearchProducts(params: {
  keyword?: string
  page?: number
  size?: number
  sortField?: string
  sortOrder?: 'asc' | 'desc'
  searchAfter?: string
}): Promise<SmartSearchResult> {
  const page = params.page == null ? 0 : Math.max(params.page - 1, 0)
  return http
    .get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/products', {
      params: {
        keyword: params.keyword,
        page,
        size: params.size,
        sortBy: params.sortField,
        sortDir: params.sortOrder,
        searchAfter: params.searchAfter
      }
    })
    .then((result) => ({
      documents: result.list.map(toSearchDocumentFromProductDocument),
      total: result.total,
      from: result.page * result.size,
      size: result.size,
      aggregations: result.aggregations,
      searchAfter: result.searchAfter
    }))
}

export function searchProducts(params: {
  keyword: string
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
  searchAfter?: string
}): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/products', { params })
}

export function searchByCategory(
  categoryId: number,
  params: { keyword?: string; page?: number; size?: number; searchAfter?: string } = {}
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>(`/api/search/categories/${categoryId}/products`, {
    params
  })
}

export function searchByShop(
  shopId: number,
  params: { keyword?: string; page?: number; size?: number; searchAfter?: string } = {}
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>(`/api/search/shops/${shopId}/products`, {
    params
  })
}

export function advancedSearch(params: {
  keyword: string
  minPrice?: number
  maxPrice?: number
  page?: number
  size?: number
  searchAfter?: string
}): Promise<SearchResult<ProductDocument>> {
  return complexSearch(
    {
      keyword: params.keyword,
      minPrice: params.minPrice,
      maxPrice: params.maxPrice,
      page: params.page,
      size: params.size
    },
    params.searchAfter
  )
}

export function listSearchSuggestions(keyword: string, size = 10): Promise<string[]> {
  return http.get<string[], string[]>('/api/search/products/suggestions', { params: { keyword, size } })
}

export function listSearchHotKeywords(size = 10): Promise<string[]> {
  return http.get<string[], string[]>('/api/search/products/keywords/hot', { params: { size } })
}

export function listSearchKeywordRecommendations(keyword = '', size = 10): Promise<string[]> {
  return http.get<string[], string[]>('/api/search/products/keywords/recommendations', { params: { keyword, size } })
}

export function listRecommendedProducts(
  page = 0,
  size = 20,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/products/recommendations', {
    params: { page, size, searchAfter }
  })
}

export function listNewProducts(
  page = 0,
  size = 20,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/products/latest', {
    params: { page, size, searchAfter }
  })
}

export function listHotProducts(
  page = 0,
  size = 20,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/products/popular', {
    params: { page, size, searchAfter }
  })
}

export function listTodayHotSellingProducts(
  page = 0,
  size = 20
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/products/popular/today', {
    params: { page, size }
  })
}

export async function smartSearchProductsWithFallback(params: {
  keyword?: string
  page?: number
  size?: number
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}): Promise<SmartSearchResult> {
  const cacheKey = buildSmartSearchCacheKey(params)
  try {
    const result = await withTimeout(smartSearchProducts(params))
    setCachedValue(searchResultCache, cacheKey, result, SEARCH_RESULT_CACHE_TTL)
    return result
  } catch {
    const cached = getCachedValue(searchResultCache, cacheKey)
    if (cached) {
      return cached
    }
    return buildEmptySmartSearchResult(params)
  }
}

export async function listSearchSuggestionsWithFallback(keyword: string, size = 10): Promise<string[]> {
  const cacheKey = `${keyword.toLowerCase()}::${size}`
  const cached = getCachedValue(suggestionCache, cacheKey)
  if (cached) {
    return cached
  }
  try {
    const result = await withTimeout(listSearchSuggestions(keyword, size))
    setCachedValue(suggestionCache, cacheKey, result, SUGGESTION_CACHE_TTL)
    return result
  } catch {
    const products = await searchProductsByName(keyword)
    const result = products
      .map((item) => item.name)
      .filter((name): name is string => typeof name === 'string' && name.trim().length > 0)
      .slice(0, size)
    setCachedValue(suggestionCache, cacheKey, result, SUGGESTION_CACHE_TTL)
    return result
  }
}

export async function listSearchHotKeywordsWithFallback(size = 10): Promise<string[]> {
  const cacheKey = `hot::${size}`
  const cached = getCachedValue(hotKeywordCache, cacheKey)
  if (cached) {
    return cached
  }
  try {
    const result = await withTimeout(listSearchHotKeywords(size))
    setCachedValue(hotKeywordCache, cacheKey, result, HOT_KEYWORD_CACHE_TTL)
    return result
  } catch {
    const fallback: string[] = []
    setCachedValue(hotKeywordCache, cacheKey, fallback, HOT_KEYWORD_CACHE_TTL)
    return fallback
  }
}

export async function listSearchKeywordRecommendationsWithFallback(keyword = '', size = 10): Promise<string[]> {
  const cacheKey = `${keyword.toLowerCase()}::${size}`
  const cached = getCachedValue(recommendationCache, cacheKey)
  if (cached) {
    return cached
  }
  try {
    const result = await withTimeout(listSearchKeywordRecommendations(keyword, size))
    setCachedValue(recommendationCache, cacheKey, result, RECOMMENDATION_CACHE_TTL)
    return result
  } catch {
    const result = await listSearchSuggestionsWithFallback(keyword, size)
    setCachedValue(recommendationCache, cacheKey, result, RECOMMENDATION_CACHE_TTL)
    return result
  }
}

export async function listTodayHotSellingProductsWithFallback(
  page = 1,
  size = 20
): Promise<SmartSearchResult> {
  const safePage = Math.max(1, page)
  const safeSize = Math.max(1, size)
  try {
    const result = await withTimeout(listTodayHotSellingProducts(safePage - 1, safeSize))
    return {
      documents: result.list.map(toSearchDocumentFromProductDocument),
      total: result.total,
      from: result.page * result.size,
      size: result.size,
      aggregations: result.aggregations,
      searchAfter: result.searchAfter
    }
  } catch {
    return smartSearchProductsWithFallback({
      page: safePage,
      size: safeSize,
      sortField: 'sales_count',
      sortOrder: 'desc'
    })
  }
}

export interface CombinedSearchParams {
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
  searchAfter?: string
}

export function combinedSearchProducts(params: CombinedSearchParams): Promise<SearchResult<ProductDocument>> {
  return filterSearch(
    {
      keyword: params.keyword,
      categoryId: params.categoryId,
      brandId: params.brandId,
      shopId: params.shopId,
      minPrice: params.minPrice,
      maxPrice: params.maxPrice,
      sortBy: params.sortBy,
      sortOrder: params.sortOrder,
      page: params.page,
      size: params.size
    },
    params.searchAfter
  )
}

export function filterSearch(
  request: ProductFilterRequest,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.post<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/products', request, {
    params: { searchAfter }
  })
}

export function filterByCategory(
  categoryId: number,
  params: { page?: number; size?: number; searchAfter?: string } = {}
): Promise<SearchResult<ProductDocument>> {
  return filterSearch(
    {
      categoryId,
      page: params.page,
      size: params.size
    },
    params.searchAfter
  )
}

export function filterByBrand(
  brandId: number,
  params: { page?: number; size?: number; searchAfter?: string } = {}
): Promise<SearchResult<ProductDocument>> {
  return filterSearch(
    {
      brandId,
      page: params.page,
      size: params.size
    },
    params.searchAfter
  )
}

export function filterByPrice(params: {
  minPrice?: number
  maxPrice?: number
  page?: number
  size?: number
  searchAfter?: string
}): Promise<SearchResult<ProductDocument>> {
  return filterSearch(
    {
      minPrice: params.minPrice,
      maxPrice: params.maxPrice,
      page: params.page,
      size: params.size
    },
    params.searchAfter
  )
}

export function filterByShop(
  shopId: number,
  params: { page?: number; size?: number; searchAfter?: string } = {}
): Promise<SearchResult<ProductDocument>> {
  return filterSearch(
    {
      shopId,
      page: params.page,
      size: params.size
    },
    params.searchAfter
  )
}
