import http from './http'
import type {
  ProductDocument,
  ProductItem,
  ProductPage,
  ProductQuery,
  SearchResult
} from '../types/domain'

export function listProducts(params: ProductQuery = {}): Promise<ProductPage> {
  return http.get<ProductPage, ProductPage>('/api/products', { params })
}

export function listManageProducts(params: ProductQuery = {}): Promise<ProductPage> {
  return http.get<ProductPage, ProductPage>('/api/spus', { params })
}

export function searchProducts(name: string): Promise<ProductItem[]> {
  // Search service returns a SearchResult payload, so map from its list field.
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/products', {
    params: { keyword: name, page: 0, size: 20 }
  }).then(result => result.list.map(doc => ({
    id: doc.productId || 0,
    name: doc.productName || '',
    price: doc.price,
    stockQuantity: doc.stockQuantity,
    categoryId: doc.categoryId,
    brandId: doc.brandId,
    status: doc.status,
    description: doc.description,
    imageUrl: doc.imageUrl,
    shopId: doc.shopId
  } as ProductItem)))
}

export function updateProductStatus(spuId: number | string, status: 0 | 1): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/spus/${spuId}/status`, null, { params: { status } })
}
