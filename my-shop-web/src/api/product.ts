import http from './http'
import type { ProductItem, ProductPage, ProductQuery, ProductUpsertPayload } from '../types/domain'

export function listProducts(params: ProductQuery = {}): Promise<ProductPage> {
  return http.get<ProductPage, ProductPage>('/api/product', { params })
}

export function searchProducts(name: string): Promise<ProductItem[]> {
  return http.get<ProductItem[], ProductItem[]>('/api/product/search', { params: { name } })
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
