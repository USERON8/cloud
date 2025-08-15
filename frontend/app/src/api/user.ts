import apiClient from './request';
import type {  AxiosPromise } from 'axios';

// 用户注册请求参数类型
export interface UserRegisterRequest {
  username: string;
  password: string;
  email: string;
  nickname: string;
}

// 用户信息响应类型
export interface UserInfoResponse {
  code: number;
  message: string;
  data: {
    username: string;
  } | null;
}

/**
 * 用户注册
 * @param data 用户注册信息
 */
export const userRegister = (data: UserRegisterRequest): AxiosPromise<UserInfoResponse> => {
  return apiClient.post('/users/register', data);
};

/**
 * 获取当前用户信息
 */
export const getCurrentUserInfo = (): AxiosPromise<UserInfoResponse> => {
  return apiClient.get('/users/info');
};