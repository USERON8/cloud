import http from './http'
import type {
  ProductItem,
  ProductPage,
  ProductQuery,
  SearchResult,
  ProductDocument
} from '../types/domain'

export function listProducts(params: ProductQuery = {}): Promise<ProductPage> {
  return http.get<ProductPage, ProductPage>('/api/product', { params })
}

export function listManageProducts(params: ProductQuery = {}): Promise<ProductPage> {
  return http.get<ProductPage, ProductPage>('/api/product/manage', { params })
}

export function searchProducts(name: string): Promise<ProductItem[]> {
  // 调用搜索服务接口，返回的是 SearchResultDTO，需要提取 list 字段
  return http.get<SearchResult<ProductDocument>, SearchResult<ProductDocument>>('/api/search/search', {
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
  return http.patch<boolean, boolean>(`/api/product/${spuId}/status`, null, { params: { status } })
}
