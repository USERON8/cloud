import http from './http'
import type { EntityId, SpuCreateRequest, SpuDetail, SkuDetail } from '../types/domain'

export function createSpu(payload: SpuCreateRequest): Promise<EntityId> {
  return http.post<number | string, number | string>('/api/spus', payload).then(String)
}

export function updateSpu(spuId: EntityId, payload: SpuCreateRequest): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/spus/${spuId}`, payload)
}

export function getSpu(spuId: EntityId): Promise<SpuDetail> {
  return http.get<SpuDetail, SpuDetail>(`/api/spus/${spuId}`)
}

export function listSpuByCategory(categoryId: EntityId, status?: number): Promise<SpuDetail[]> {
  return http.get<SpuDetail[], SpuDetail[]>(`/api/categories/${categoryId}/spus`, { params: { status } })
}

export function listSkuByIds(skuIds: EntityId[]): Promise<SkuDetail[]> {
  return http.get<SkuDetail[], SkuDetail[]>('/api/skus', {
    params: { ids: skuIds }
  })
}

export function updateSpuStatus(spuId: EntityId, status: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/spus/${spuId}/status`, null, { params: { status } })
}
