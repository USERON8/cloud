import http from './http'
import type { SpuCreateRequest, SpuDetail, SkuDetail } from '../types/domain'

export function createSpu(payload: SpuCreateRequest): Promise<number> {
  return http.post<number, number>('/api/product/spu', payload)
}

export function updateSpu(spuId: number, payload: SpuCreateRequest): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/product/spu/${spuId}`, payload)
}

export function getSpu(spuId: number): Promise<SpuDetail> {
  return http.get<SpuDetail, SpuDetail>(`/api/product/spu/${spuId}`)
}

export function listSpuByCategory(categoryId: number, status?: number): Promise<SpuDetail[]> {
  return http.get<SpuDetail[], SpuDetail[]>(`/api/product/spu/category/${categoryId}`, { params: { status } })
}

export function listSkuByIds(skuIds: number[]): Promise<SkuDetail[]> {
  return http.get<SkuDetail[], SkuDetail[]>('/api/product/sku/batch', {
    params: { skuIds: skuIds.join(',') }
  })
}

export function updateSpuStatus(spuId: number, status: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/product/spu/${spuId}/status`, null, { params: { status } })
}
