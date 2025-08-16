import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';
import router from '@/router';

// 创建axios实例
const apiClient: AxiosInstance = axios.create({
  baseURL: '/api', // 基础URL，实际项目中可能需要根据环境配置
  timeout: 10000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 在发送请求之前做些什么
    const token = localStorage.getItem('token');
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    // 对响应数据做点什么
    return response.data;
  },
  (error) => {
    // 对响应错误做点什么
    if (error.response?.status === 401) {
      // token过期或未授权，清除本地存储并跳转到登录页
      localStorage.removeItem('token');
      // 如果有路由实例，跳转到登录页
      if (router) {
        router.push('/login');
      }
    }
    return Promise.reject(error);
  }
);

// OAuth2专用客户端（不使用Bearer前缀）
export const oauth2Client: AxiosInstance = axios.create({
  baseURL: '/api', // 基础URL，实际项目中可能需要根据环境配置
  timeout: 10000, // 请求超时时间
});

// OAuth2请求拦截器
oauth2Client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // OAuth2请求直接使用access_token参数
    return config;
  },
  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error);
  }
);

// OAuth2响应拦截器
oauth2Client.interceptors.response.use(
  (response: AxiosResponse) => {
    // 对响应数据做点什么
    return response.data;
  },
  (error) => {
    // 对响应错误做点什么
    if (error.response?.status === 401) {
      // token过期或未授权，清除本地存储并跳转到登录页
      localStorage.removeItem('token');
      // 如果有路由实例，跳转到登录页
      if (router) {
        router.push('/login');
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;