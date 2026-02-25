import http from './http'
import type {
  ProductItem,
  ProductPage,
  ProductQuery,
  ProductUpsertPayload,
  SmartSearchResult
} from '../types/domain'

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

export function updateProductStatus(id: number, status: 0 | 1): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/product/${id}/status`, null, { params: { status } })
}

export function createProduct(payload: ProductUpsertPayload): Promise<number> {
  return http.post<number, number>('/api/product', payload)
}

export function updateProduct(id: number, payload: ProductUpsertPayload): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/product/${id}`, payload)
}
