import http from './http'
import type { MerchantAuthInfo, MerchantAuthPayload } from '../types/domain'

export function applyMerchantAuth(merchantId: number, payload: MerchantAuthPayload): Promise<MerchantAuthInfo> {
  return http.post<MerchantAuthInfo, MerchantAuthInfo>(`/api/merchant/auth/apply/${merchantId}`, payload)
}

export function getMerchantAuth(merchantId: number): Promise<MerchantAuthInfo | null> {
  return http.get<MerchantAuthInfo | null, MerchantAuthInfo | null>(`/api/merchant/auth/get/${merchantId}`)
}

export function revokeMerchantAuth(merchantId: number): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/merchant/auth/revoke/${merchantId}`)
}

export function reviewMerchantAuth(merchantId: number, authStatus: number, remark?: string): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/merchant/auth/review/${merchantId}`, null, {
    params: { authStatus, remark }
  })
}

export function listMerchantAuthByStatus(authStatus: number): Promise<MerchantAuthInfo[]> {
  return http.get<MerchantAuthInfo[], MerchantAuthInfo[]>('/api/merchant/auth/list', { params: { authStatus } })
}

export function reviewMerchantAuthBatch(merchantIds: number[], authStatus: number, remark?: string): Promise<boolean> {
  return http.post<boolean, boolean>('/api/merchant/auth/review/batch', merchantIds, {
    params: { authStatus, remark }
  })
}
