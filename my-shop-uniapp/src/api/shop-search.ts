import http from './http'
import type { SearchResult, ShopDocument } from '../types/domain'

export interface ShopSearchRequest {
  keyword?: string
  merchantId?: number
  status?: number
  minRating?: number
  minProductCount?: number
  minFollowCount?: number
  recommended?: boolean
  addressKeyword?: string
  page?: number
  size?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  highlight?: boolean
  includeAggregations?: boolean
}

export function searchShops(request: ShopSearchRequest): Promise<SearchResult<ShopDocument>> {
  return http.post<SearchResult<ShopDocument>, SearchResult<ShopDocument>>('/api/search/shops/complex-search', request)
}

export function getShopFilters(request: ShopSearchRequest): Promise<SearchResult<ShopDocument>> {
  return http.post<SearchResult<ShopDocument>, SearchResult<ShopDocument>>('/api/search/shops/filters', request)
}

export function listShopSuggestions(keyword: string, size = 10): Promise<string[]> {
  return http.get<string[], string[]>('/api/search/shops/suggestions', { params: { keyword, size } })
}

export function listHotShops(size = 10): Promise<ShopDocument[]> {
  return http.get<ShopDocument[], ShopDocument[]>('/api/search/shops/hot-shops', { params: { size } })
}

export function getShopById(shopId: number): Promise<ShopDocument> {
  return http.get<ShopDocument, ShopDocument>(`/api/search/shops/${shopId}`)
}

export function listRecommendedShops(page = 0, size = 20): Promise<SearchResult<ShopDocument>> {
  return http.get<SearchResult<ShopDocument>, SearchResult<ShopDocument>>('/api/search/shops/recommended', {
    params: { page, size }
  })
}

export function searchShopsByLocation(location: string, page = 0, size = 20): Promise<SearchResult<ShopDocument>> {
  return http.get<SearchResult<ShopDocument>, SearchResult<ShopDocument>>('/api/search/shops/by-location', {
    params: { location, page, size }
  })
}
