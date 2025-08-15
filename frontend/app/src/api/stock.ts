import apiClient from './request';
import { AxiosPromise } from 'axios';

// 库存信息类型
export interface StockInfo {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  price: number;
}

// 库存查询响应类型
export interface StockResponse {
  code: number;
  message: string;
  data: StockInfo | null;
}

// 分页查询请求参数类型
export interface StockPageRequest {
  page: number;
  size: number;
  productName?: string;
}

// 库存添加请求参数类型
export interface StockAddRequest {
  productId: number;
  productName: string;
  quantity: number;
  price: number;
}

// 分页查询响应类型
export interface StockPageResponse {
  code: number;
  message: string;
  data: {
    records: StockInfo[];
    total: number;
    size: number;
    current: number;
  } | null;
}

// 库存统计信息类型
export interface StockStatistics {
  totalProducts: number;
  totalStock: number;
  totalValue: number;
}

// 库存统计响应类型
export interface StockStatisticsResponse {
  code: number;
  message: string;
  data: StockStatistics | null;
}

/**
 * 根据商品ID获取库存信息
 * @param productId 商品ID
 */
export const getStockByProductId = (productId: number): AxiosPromise<StockResponse> => {
  return apiClient.get(`/stock/query/product/${productId}`);
};

/**
 * 分页查询库存
 * @param data 分页查询参数
 */
export const getStockPage = (data: StockPageRequest): AxiosPromise<StockPageResponse> => {
  return apiClient.post('/stock/query/page', data);
};

/**
 * 增加库存
 * @param data 库存信息
 */
export const addStock = (data: StockAddRequest): AxiosPromise<StockResponse> => {
  return apiClient.post('/stock/add', data);
};

/**
 * 批量增加库存
 * @param data 库存信息数组
 */
export const addStockBatch = (data: StockAddRequest[]): AxiosPromise<StockResponse> => {
  return apiClient.post('/stock/add/batch', data);
};

/**
 * 扣减库存
 * @param id 库存ID
 */
export const reduceStock = (id: number): AxiosPromise<StockResponse> => {
  return apiClient.delete(`/stock/reduce/${id}`);
};

/**
 * 批量扣减库存
 * @param ids 库存ID数组
 */
export const reduceStockBatch = (ids: number[]): AxiosPromise<StockResponse> => {
  return apiClient.delete('/stock/reduce/batch', {
    data: ids,
  });
};

/**
 * 更新库存
 * @param data 库存信息
 */
export const updateStock = (data: StockAddRequest): AxiosPromise<StockResponse> => {
  return apiClient.post('/stock/update', data);
};

/**
 * 异步根据商品ID查询库存
 * @param productId 商品ID
 */
export const getStockByProductIdAsync = (productId: number): AxiosPromise<StockResponse> => {
  return apiClient.get(`/stock/async/product/${productId}`);
};

/**
 * 异步分页查询库存
 * @param data 分页查询参数
 */
export const getStockPageAsync = (data: StockPageRequest): AxiosPromise<StockPageResponse> => {
  return apiClient.post('/stock/async/page', data);
};

/**
 * 异步批量查询库存
 * @param productIds 商品ID数组
 */
export const getStockBatchAsync = (productIds: number[]): AxiosPromise<StockResponse> => {
  return apiClient.post('/stock/async/batch', productIds);
};

/**
 * 异步查询库存统计信息
 */
export const getStockStatisticsAsync = (): AxiosPromise<StockStatisticsResponse> => {
  return apiClient.get('/stock/async/statistics');
};

/**
 * 并发查询多个商品库存
 * @param productIds 商品ID数组
 */
export const getStockConcurrent = (productIds: number[]): AxiosPromise<StockResponse> => {
  return apiClient.post('/stock/async/concurrent', productIds);
};