import { getAccessToken } from '../auth/session'
import http from './http'
import { BusinessError, SUCCESS_CODE, type PageResult, type ResultEnvelope } from '../types/api'
import type { MerchantAuthInfo, MerchantAuthPayload, MerchantAuthUploadResult } from '../types/domain'
import { buildApiUrl } from './runtime-base'

const merchantAuthUploadPathMap = {
  businessLicenseUrl: '/api/merchants/{merchantId}/authentication/license-files',
  idCardFrontUrl: '/api/merchants/{merchantId}/authentication/id-card-front-files',
  idCardBackUrl: '/api/merchants/{merchantId}/authentication/id-card-back-files'
} as const

export type MerchantAuthUploadField = keyof typeof merchantAuthUploadPathMap

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
  return http.put<MerchantAuthInfo, MerchantAuthInfo>(`/api/merchants/${merchantId}/authentication`, payload)
}

export function getMerchantAuth(merchantId: number): Promise<MerchantAuthInfo | null> {
  return http.get<MerchantAuthInfo | null, MerchantAuthInfo | null>(`/api/merchants/${merchantId}/authentication`)
}

export function revokeMerchantAuth(merchantId: number): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/merchants/${merchantId}/authentication`)
}

export function reviewMerchantAuth(merchantId: number, authStatus: number, remark?: string): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/merchants/${merchantId}/authentication/reviews`, null, {
    params: { authStatus, remark }
  })
}

export function listMerchantAuthByStatus(
  authStatus: number,
  params: { page?: number; size?: number } = {}
): Promise<PageResult<MerchantAuthInfo>> {
  return http.get<PageResult<MerchantAuthInfo>, PageResult<MerchantAuthInfo>>('/api/merchant-authentications', {
    params: {
      authStatus,
      ...params
    }
  })
}

export function reviewMerchantAuthBatch(merchantIds: number[], authStatus: number, remark?: string): Promise<boolean> {
  return http.post<boolean, boolean>('/api/merchant-authentications/bulk/reviews', merchantIds, {
    params: { authStatus, remark }
  })
}

export function uploadMerchantBusinessLicense(
  merchantId: number,
  filePath: string
): Promise<MerchantAuthUploadResult> {
  return uploadMerchantAuthFile(merchantId, 'businessLicenseUrl', filePath)
}

export function uploadMerchantAuthFile(
  merchantId: number,
  field: MerchantAuthUploadField,
  filePath: string
): Promise<MerchantAuthUploadResult> {
  const token = getAccessToken()
  const uploadPath = merchantAuthUploadPathMap[field].replace('{merchantId}', String(merchantId))
  return new Promise<MerchantAuthUploadResult>((resolve, reject) => {
    uni.uploadFile({
      url: buildApiUrl(uploadPath),
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
