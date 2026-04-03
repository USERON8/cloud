import http from './http'
import type { PageResult } from '../types/api'
import type { MerchantInfo, MerchantUpsertPayload } from '../types/domain'

export function getMerchants(params: { page?: number; size?: number; status?: number; auditStatus?: number } = {}): Promise<PageResult<MerchantInfo>> {
  return http.get<PageResult<MerchantInfo>, PageResult<MerchantInfo>>('/api/merchant', { params })
}

export function getMerchantById(id: number): Promise<MerchantInfo> {
  return http.get<MerchantInfo, MerchantInfo>(`/api/merchant/${id}`)
}

export function createMerchant(payload: MerchantUpsertPayload): Promise<MerchantInfo> {
  return http.post<MerchantInfo, MerchantInfo>('/api/merchant', payload)
}

export function updateMerchant(id: number, payload: MerchantUpsertPayload): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/merchant/${id}`, payload)
}

export function deleteMerchant(id: number): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/merchant/${id}`)
}

export function approveMerchant(id: number, remark?: string): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/merchant/${id}/approve`, null, { params: { remark } })
}

export function rejectMerchant(id: number, reason: string): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/merchant/${id}/reject`, null, { params: { reason } })
}

export function updateMerchantStatus(id: number, status: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/merchant/${id}/status`, null, { params: { status } })
}

export function getMerchantStatistics(id: number): Promise<Record<string, unknown>> {
  return http.get<Record<string, unknown>, Record<string, unknown>>(`/api/merchant/${id}/statistics`)
}

export function deleteMerchantsBatch(ids: number[]): Promise<boolean> {
  return http.delete<boolean, boolean>('/api/merchant/batch', { data: ids })
}

export function updateMerchantStatusBatch(ids: number[], status: number): Promise<boolean> {
  return http.patch<boolean, boolean>('/api/merchant/batch/status', null, {
    params: {
      ids,
      status
    }
  })
}

export function approveMerchantsBatch(ids: number[], remark?: string): Promise<boolean> {
  return http.post<boolean, boolean>('/api/merchant/batch/approve', ids, { params: { remark } })
}
