import http from './http'
import type { ProductDocument, ProductFilterRequest, ProductSearchRequest, SearchResult } from '../types/domain'

export function complexSearch(
  request: ProductSearchRequest,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.post<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/complex-search', request, {
    params: { searchAfter }
  })
}

export function getProductFilters(
  request: ProductSearchRequest,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.post<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/filters', request, {
    params: { searchAfter }
  })
}

export function basicSearch(params: {
  keyword?: string
  page?: number
  size?: number
  searchAfter?: string
}): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/basic', { params })
}

export function searchProducts(params: {
  keyword: string
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
  searchAfter?: string
}): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/search', { params })
}

export function searchByCategory(
  categoryId: number,
  params: { keyword?: string; page?: number; size?: number; searchAfter?: string } = {}
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>(`/api/search/search/category/${categoryId}`, {
    params
  })
}

export function searchByShop(
  shopId: number,
  params: { keyword?: string; page?: number; size?: number; searchAfter?: string } = {}
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>(`/api/search/search/shop/${shopId}`, {
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
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/search/advanced', { params })
}

export function listRecommendedProducts(
  page = 0,
  size = 20,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/recommended', {
    params: { page, size, searchAfter }
  })
}

export function listNewProducts(
  page = 0,
  size = 20,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/new', {
    params: { page, size, searchAfter }
  })
}

export function listHotProducts(
  page = 0,
  size = 20,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/hot', {
    params: { page, size, searchAfter }
  })
}

export function filterSearch(
  request: ProductFilterRequest,
  searchAfter?: string
): Promise<SearchResult<ProductDocument>> {
  return http.post<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/filter', request, {
    params: { searchAfter }
  })
}

export function filterByCategory(
  categoryId: number,
  params: { page?: number; size?: number; searchAfter?: string } = {}
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>(`/api/search/filter/category/${categoryId}`, {
    params
  })
}

export function filterByBrand(
  brandId: number,
  params: { page?: number; size?: number; searchAfter?: string } = {}
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>(`/api/search/filter/brand/${brandId}`, {
    params
  })
}

export function filterByPrice(params: {
  minPrice?: number
  maxPrice?: number
  page?: number
  size?: number
  searchAfter?: string
}): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/filter/price', { params })
}

export function filterByShop(
  shopId: number,
  params: { page?: number; size?: number; searchAfter?: string } = {}
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>(`/api/search/filter/shop/${shopId}`, {
    params
  })
}
