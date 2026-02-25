import http from './http'
import type {
  ProductItem,
  ProductPage,
  ProductQuery,
  ProductUpsertPayload,
  SearchProductDocument,
  SmartSearchResult
} from '../types/domain'

const SEARCH_FALLBACK_TIMEOUT_MS = Number(import.meta.env.VITE_SEARCH_FALLBACK_TIMEOUT || 1200)

export function listProducts(params: ProductQuery = {}): Promise<ProductPage> {
  return http.get<ProductPage, ProductPage>('/api/product', { params })
}

export function searchProducts(name: string): Promise<ProductItem[]> {
  return http.get<ProductItem[], ProductItem[]>('/api/product/search', { params: { name } })
}

export function smartSearchProducts(params: {
  keyword?: string
  page?: number
  size?: number
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}): Promise<SmartSearchResult> {
  return http.get<SmartSearchResult, SmartSearchResult>('/api/search/smart-search', { params })
}

export function listSearchSuggestions(keyword: string, size = 10): Promise<string[]> {
  return http.get<string[], string[]>('/api/search/suggestions', { params: { keyword, size } })
}

export function listSearchHotKeywords(size = 10): Promise<string[]> {
  return http.get<string[], string[]>('/api/search/hot-keywords', { params: { size } })
}

export function listSearchKeywordRecommendations(keyword = '', size = 10): Promise<string[]> {
  return http.get<string[], string[]>('/api/search/keyword-recommendations', { params: { keyword, size } })
}

function withTimeout<T>(promise: Promise<T>, timeoutMs = SEARCH_FALLBACK_TIMEOUT_MS): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timer = window.setTimeout(() => {
      reject(new Error(`Search service timeout after ${timeoutMs}ms`))
    }, timeoutMs)
    promise
      .then((result) => resolve(result))
      .catch((error) => reject(error))
      .finally(() => {
        window.clearTimeout(timer)
      })
  })
}

function toSearchDocument(item: ProductItem): SearchProductDocument {
  return {
    productId: item.id,
    shopId: item.shopId,
    productName: item.name,
    price: item.price,
    stockQuantity: item.stockQuantity,
    categoryId: item.categoryId,
    brandId: item.brandId,
    status: item.status,
    description: item.description,
    imageUrl: item.imageUrl
  }
}

export async function smartSearchProductsWithFallback(params: {
  keyword?: string
  page?: number
  size?: number
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}): Promise<SmartSearchResult> {
  try {
    return await withTimeout(smartSearchProducts(params))
  } catch {
    const fallback = await listProducts({
      page: params.page,
      size: params.size,
      name: params.keyword || undefined
    })
    return {
      documents: fallback.records.map(toSearchDocument),
      total: fallback.total,
      from: ((params.page || 1) - 1) * (params.size || 20),
      size: params.size || 20,
      aggregations: {}
    }
  }
}

export async function listSearchSuggestionsWithFallback(keyword: string, size = 10): Promise<string[]> {
  try {
    return await withTimeout(listSearchSuggestions(keyword, size))
  } catch {
    const products = await searchProducts(keyword)
    return products
      .map((item) => item.name)
      .filter((name): name is string => typeof name === 'string' && name.trim().length > 0)
      .slice(0, size)
  }
}

export async function listSearchHotKeywordsWithFallback(size = 10): Promise<string[]> {
  try {
    return await withTimeout(listSearchHotKeywords(size))
  } catch {
    return []
  }
}

export async function listSearchKeywordRecommendationsWithFallback(keyword = '', size = 10): Promise<string[]> {
  try {
    return await withTimeout(listSearchKeywordRecommendations(keyword, size))
  } catch {
    return listSearchSuggestionsWithFallback(keyword, size)
  }
}

export function updateProductStatus(id: number, status: 0 | 1): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/product/${id}/status`, null, { params: { status } })
}

export function createProduct(payload: ProductUpsertPayload): Promise<number> {
  return http.post<number, number>('/api/product', payload)
}

export function updateProduct(id: number, payload: ProductUpsertPayload): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/product/${id}`, payload)
}
