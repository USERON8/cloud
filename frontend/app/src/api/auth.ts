import apiClient from './request';
import type { AxiosPromise } from 'axios';
import type { OAuth2TokenResponse } from './oauth2';

// 用户注册请求参数类型
export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  phone: string;
  nickname: string;
  userType: string;
}

// 商家注册请求参数类型
export interface MerchantRegisterRequest {
  username: string;
  password: string;
  email: string;
  phone: string;
  nickname: string;
  businessLicenseNumber: string;
  businessLicenseFile: File;
  idCardFrontFile: File;
  idCardBackFile: File;
  contactPhone: string;
  contactAddress: string;
}

// 注册并登录请求参数类型
export interface RegisterAndLoginRequest {
  username: string;
  password: string;
  email: string;
  nickname: string;
}

// 登录请求参数类型
export interface LoginRequest {
  username: string;
  password: string;
}

// OAuth2回调参数类型
export interface OAuth2CallbackParams {
  code: string;
  state?: string;
}

// 响应数据类型
export interface AuthResponse {
  code: number;
  message: string;
  data: {
    token: string;
    expiresIn: number;
    userType: string;
    nickname: string;
  } | null;
}

/**
 * 用户注册
 * @param data 注册信息
 */
export const register = (data: RegisterRequest): AxiosPromise<AuthResponse> => {
  return apiClient.post('/auth/register', data);
};

/**
 * 商家注册
 * @param data 商家注册信息
 */
export const merchantRegister = (data: FormData): AxiosPromise<AuthResponse> => {
  return apiClient.post('/auth/register-merchant', data, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

/**
 * 用户注册并登录
 * @param data 注册信息
 */
export const registerAndLogin = (data: RegisterAndLoginRequest): AxiosPromise<AuthResponse> => {
  return apiClient.post('/auth/register-and-login', data);
};

/**
 * 用户登录
 * @param data 登录信息
 */
export const login = (data: LoginRequest): AxiosPromise<AuthResponse> => {
  return apiClient.post('/auth/login', data);
};

/**
 * 用户登出
 */
export const logout = (): AxiosPromise<AuthResponse> => {
  return apiClient.post('/auth/logout');
};

/**
 * 刷新令牌
 */
export const refreshToken = (): AxiosPromise<AuthResponse> => {
  return apiClient.post('/auth/refresh');
};

/**
 * OAuth2回调处理
 * @param params 回调参数
 */
export const oauth2Callback = (params: OAuth2CallbackParams): AxiosPromise<AuthResponse> => {
  return apiClient.post('/auth/oauth2/callback', params);
};

/**
 * 使用OAuth2令牌登录
 * @param token OAuth2访问令牌
 */
export const loginWithOAuth2Token = (token: string): AxiosPromise<AuthResponse> => {
  return apiClient.post('/auth/oauth2/login', { token });
};