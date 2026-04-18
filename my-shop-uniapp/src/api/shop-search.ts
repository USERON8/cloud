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
  return http.post<SearchResult<ShopDocument>, SearchResult<ShopDocument>>('/api/search/shops/searches', request)
}

export function getShopFilters(request: ShopSearchRequest): Promise<SearchResult<ShopDocument>> {
  return http.post<SearchResult<ShopDocument>, SearchResult<ShopDocument>>('/api/search/shops/filter-groups', request)
}

export function listShopSuggestions(keyword: string, size = 10): Promise<string[]> {
  return http.get<string[], string[]>('/api/search/shops/suggestions', { params: { keyword, size } })
}

export function listHotShops(size = 10): Promise<ShopDocument[]> {
  return http.get<ShopDocument[], ShopDocument[]>('/api/search/shops/popular', { params: { size } })
}

export function getShopById(shopId: number): Promise<ShopDocument> {
  return http.get<ShopDocument, ShopDocument>(`/api/search/shops/${shopId}`)
}

export function listRecommendedShops(page = 0, size = 20): Promise<SearchResult<ShopDocument>> {
  return http.get<SearchResult<ShopDocument>, SearchResult<ShopDocument>>('/api/search/shops/recommendations', {
    params: { page, size }
  })
}

export function searchShopsByLocation(location: string, page = 0, size = 20): Promise<SearchResult<ShopDocument>> {
  return http.get<SearchResult<ShopDocument>, SearchResult<ShopDocument>>('/api/search/shops/nearby', {
    params: { location, page, size }
  })
}
