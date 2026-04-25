import http from './http'
import type { PageResult } from '../types/api'
import type { MerchantInfo, MerchantUpsertPayload } from '../types/domain'

export function getMerchants(params: { page?: number; size?: number; status?: number; auditStatus?: number } = {}): Promise<PageResult<MerchantInfo>> {
  return http.get<PageResult<MerchantInfo>, PageResult<MerchantInfo>>('/api/merchants', { params })
}

export function getMerchantById(id: number): Promise<MerchantInfo> {
  return http.get<MerchantInfo, MerchantInfo>(`/api/merchants/${id}`)
}

export function createMerchant(payload: MerchantUpsertPayload): Promise<MerchantInfo> {
  return http.post<MerchantInfo, MerchantInfo>('/api/merchants', payload)
}

export function updateMerchant(id: number, payload: MerchantUpsertPayload): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/merchants/${id}`, payload)
}

export function deleteMerchant(id: number): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/merchants/${id}`)
}

export function reviewMerchant(id: number, approved: boolean, remark?: string, reason?: string): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/merchants/${id}/reviews`, null, {
    params: { approved, remark, reason }
  })
}

export function approveMerchant(id: number, remark?: string): Promise<boolean> {
  return reviewMerchant(id, true, remark)
}

export function rejectMerchant(id: number, reason: string): Promise<boolean> {
  return reviewMerchant(id, false, undefined, reason)
}

export function updateMerchantStatus(id: number, status: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/merchants/${id}/status`, null, { params: { status } })
}

export function getMerchantStatistics(id: number): Promise<Record<string, unknown>> {
  return http.get<Record<string, unknown>, Record<string, unknown>>(`/api/merchants/${id}/statistics`)
}

export function deleteMerchantsBatch(ids: number[]): Promise<boolean> {
  return http.delete<boolean, boolean>('/api/merchants/batch', { data: ids })
}

export function updateMerchantStatusBatch(ids: number[], status: number): Promise<boolean> {
  return http.patch<boolean, boolean>('/api/merchants/batch/status', null, {
    params: {
      ids,
      status
    }
  })
}

export function approveMerchantsBatch(ids: number[], remark?: string): Promise<boolean> {
  return http.post<boolean, boolean>('/api/merchants/bulk/reviews', ids, {
    params: { approved: true, remark }
  })
}

export function reviewMerchantsBatch(ids: number[], approved: boolean, remark?: string, reason?: string): Promise<boolean> {
  return http.post<boolean, boolean>('/api/merchants/bulk/reviews', ids, {
    params: { approved, remark, reason }
  })
}

export function rejectMerchantsBatch(ids: number[], reason: string): Promise<boolean> {
  return reviewMerchantsBatch(ids, false, undefined, reason)
}
