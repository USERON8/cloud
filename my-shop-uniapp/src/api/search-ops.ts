import http from './http'
import type { ProductDocument, ProductFilterRequest, ProductSearchRequest, SearchResult, SpringPage } from '../types/domain'

export function complexSearch(request: ProductSearchRequest): Promise<SearchResult<ProductDocument>> {
  return http.post<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/complex-search', request)
}

export function getProductFilters(request: ProductSearchRequest): Promise<SearchResult<ProductDocument>> {
  return http.post<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/filters', request)
}

export function basicSearch(params: { keyword?: string; page?: number; size?: number }): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/basic', { params })
}

export function searchProducts(params: {
  keyword: string
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
}): Promise<SpringPage<ProductDocument>> {
  return http.get<SpringPage<ProductDocument>, SpringPage<ProductDocument>>('/api/search/search', { params })
}

export function searchByCategory(
  categoryId: number,
  params: { keyword?: string; page?: number; size?: number } = {}
): Promise<SpringPage<ProductDocument>> {
  return http.get<SpringPage<ProductDocument>, SpringPage<ProductDocument>>(`/api/search/search/category/${categoryId}`, {
    params
  })
}

export function searchByShop(
  shopId: number,
  params: { keyword?: string; page?: number; size?: number } = {}
): Promise<SpringPage<ProductDocument>> {
  return http.get<SpringPage<ProductDocument>, SpringPage<ProductDocument>>(`/api/search/search/shop/${shopId}`, {
    params
  })
}

export function advancedSearch(params: {
  keyword: string
  minPrice?: number
  maxPrice?: number
  page?: number
  size?: number
}): Promise<SpringPage<ProductDocument>> {
  return http.get<SpringPage<ProductDocument>, SpringPage<ProductDocument>>('/api/search/search/advanced', { params })
}

export function listRecommendedProducts(page = 0, size = 20): Promise<SpringPage<ProductDocument>> {
  return http.get<SpringPage<ProductDocument>, SpringPage<ProductDocument>>('/api/search/recommended', {
    params: { page, size }
  })
}

export function listNewProducts(page = 0, size = 20): Promise<SpringPage<ProductDocument>> {
  return http.get<SpringPage<ProductDocument>, SpringPage<ProductDocument>>('/api/search/new', {
    params: { page, size }
  })
}

export function listHotProducts(page = 0, size = 20): Promise<SpringPage<ProductDocument>> {
  return http.get<SpringPage<ProductDocument>, SpringPage<ProductDocument>>('/api/search/hot', {
    params: { page, size }
  })
}

export function filterSearch(request: ProductFilterRequest): Promise<SearchResult<ProductDocument>> {
  return http.post<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/filter', request)
}

export function filterByCategory(
  categoryId: number,
  params: { page?: number; size?: number } = {}
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>(`/api/search/filter/category/${categoryId}`, {
    params
  })
}

export function filterByBrand(
  brandId: number,
  params: { page?: number; size?: number } = {}
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
}): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/filter/price', { params })
}

export function filterByShop(
  shopId: number,
  params: { page?: number; size?: number } = {}
): Promise<SearchResult<ProductDocument>> {
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>(`/api/search/filter/shop/${shopId}`, {
    params
  })
}
