import http from './http'
import type { SpuCreateRequest, SpuDetail, SkuDetail } from '../types/domain'

export function createSpu(payload: SpuCreateRequest): Promise<number> {
  return http.post<number, number>('/api/spus', payload)
}

export function updateSpu(spuId: number | string, payload: SpuCreateRequest): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/spus/${spuId}`, payload)
}

export function getSpu(spuId: number | string): Promise<SpuDetail> {
  return http.get<SpuDetail, SpuDetail>(`/api/spus/${spuId}`)
}

export function listSpuByCategory(categoryId: number | string, status?: number): Promise<SpuDetail[]> {
  return http.get<SpuDetail[], SpuDetail[]>(`/api/categories/${categoryId}/spus`, { params: { status } })
}

export function listSkuByIds(skuIds: (number | string)[]): Promise<SkuDetail[]> {
  return http.get<SkuDetail[], SkuDetail[]>('/api/skus', {
    params: { ids: skuIds }
  })
}

export function updateSpuStatus(spuId: number | string, status: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/spus/${spuId}/status`, null, { params: { status } })
}
