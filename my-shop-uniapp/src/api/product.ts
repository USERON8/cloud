import http from './http'
import type {
  ProductItem,
  ProductPage,
  ProductQuery
} from '../types/domain'

export function listProducts(params: ProductQuery = {}): Promise<ProductPage> {
  return http.get<ProductPage, ProductPage>('/api/product', { params })
}

export function searchProducts(name: string): Promise<ProductItem[]> {
  return http.get<ProductItem[], ProductItem[]>('/api/product/search', { params: { name } })
}

export function updateProductStatus(id: number, status: 0 | 1): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/product/${id}/status`, null, { params: { status } })
}
