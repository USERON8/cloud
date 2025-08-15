import apiClient from './request';
import type { AxiosPromise } from 'axios';

// 商家认证申请请求参数类型
export interface MerchantAuthApplyRequest {
  shopName: string;
  businessLicenseNumber: string;
  businessLicenseFile: File;
  idCardFrontFile: File;
  idCardBackFile: File;
  contactPhone: string;
  contactAddress: string;
}

// 商家认证信息响应类型
export interface MerchantAuthInfo {
  id: number;
  userId: number;
  shopName: string;
  businessLicenseNumber: string;
  businessLicenseUrl: string;
  idCardFrontUrl: string;
  idCardBackUrl: string;
  contactPhone: string;
  contactAddress: string;
  status: number;
  auditRemark: string;
  createdAt: string;
  updatedAt: string;
}

// 商家认证信息响应类型
export interface MerchantAuthInfoResponse {
  code: number;
  message: string;
  data: MerchantAuthInfo | null;
}

// 分页查询待审核商家认证申请响应类型
export interface PendingMerchantAuthResponse {
  code: number;
  message: string;
  data: {
    records: MerchantAuthInfo[];
    total: number;
    size: number;
    current: number;
    pages: number;
  } | null;
}

/**
 * 提交商家认证申请
 * @param data 认证信息
 */
export const applyMerchantAuth = (data: FormData): AxiosPromise<MerchantAuthInfoResponse> => {
  return apiClient.post('/merchant/auth/apply', data, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

/**
 * 查询当前用户的认证信息
 */
export const getMerchantAuthInfo = (): AxiosPromise<MerchantAuthInfoResponse> => {
  return apiClient.get('/merchant/auth/info');
};

/**
 * 分页查询待审核的商家认证申请
 * @param page 页码
 * @param size 每页数量
 */
export const getPendingMerchantAuth = (
  page: number = 1,
  size: number = 10
): AxiosPromise<PendingMerchantAuthResponse> => {
  return apiClient.get('/merchant/auth/pending', {
    params: {
      page,
      size,
    },
  });
};

/**
 * 审核商家认证申请
 * @param authId 认证ID
 * @param status 审核状态 (1-通过, 2-拒绝)
 * @param remark 审核备注
 */
export const auditMerchantAuth = (
  authId: number,
  status: number,
  remark?: string
): AxiosPromise<MerchantAuthInfoResponse> => {
  return apiClient.put(`/merchant/auth/audit/${authId}`, null, {
    params: {
      status,
      remark,
    },
  });
};