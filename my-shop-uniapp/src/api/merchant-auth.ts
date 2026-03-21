import { getAccessToken } from '../auth/session'
import http from './http'
import { BusinessError, SUCCESS_CODE, type ResultEnvelope } from '../types/api'
import type { MerchantAuthInfo, MerchantAuthPayload, MerchantAuthUploadResult } from '../types/domain'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

function buildApiUrl(path: string): string {
  if (!apiBaseUrl) {
    return path
  }
  return `${apiBaseUrl.replace(/\/+$/, '')}${path}`
}

function parseUploadResponse(payload: string): MerchantAuthUploadResult {
  const parsed = JSON.parse(payload) as ResultEnvelope<MerchantAuthUploadResult> | MerchantAuthUploadResult
  if (
    parsed &&
    typeof parsed === 'object' &&
    'code' in parsed &&
    typeof parsed.code === 'number' &&
    parsed.code !== SUCCESS_CODE
  ) {
    throw new BusinessError(parsed.message || 'Upload failed', parsed.code)
  }
  if (parsed && typeof parsed === 'object' && 'code' in parsed && typeof parsed.code === 'number') {
    return parsed.data
  }
  return parsed as MerchantAuthUploadResult
}

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

export function uploadMerchantBusinessLicense(
  merchantId: number,
  filePath: string
): Promise<MerchantAuthUploadResult> {
  const token = getAccessToken()
  return new Promise<MerchantAuthUploadResult>((resolve, reject) => {
    uni.uploadFile({
      url: buildApiUrl(`/api/merchant/auth/upload/license/${merchantId}`),
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (result) => {
        try {
          if (result.statusCode && result.statusCode >= 400) {
            reject(new Error('Upload failed'))
            return
          }
          resolve(parseUploadResponse(result.data))
        } catch (error) {
          reject(error)
        }
      },
      fail: reject
    })
  })
}
