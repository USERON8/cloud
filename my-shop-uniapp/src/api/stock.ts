import http from './http'
import type { StockLedger, StockOperatePayload } from '../types/domain'

export function getStockLedger(skuId: number): Promise<StockLedger> {
  return http.get<StockLedger, StockLedger>(`/api/stocks/ledger/${skuId}`)
}

export function reserveStock(payload: StockOperatePayload): Promise<boolean> {
  return http.post<boolean, boolean>('/api/stocks/reserve', payload)
}

export function confirmStock(payload: StockOperatePayload): Promise<boolean> {
  return http.post<boolean, boolean>('/api/stocks/confirm', payload)
}

export function releaseStock(payload: StockOperatePayload): Promise<boolean> {
  return http.post<boolean, boolean>('/api/stocks/release', payload)
}

export function rollbackStock(payload: StockOperatePayload): Promise<boolean> {
  return http.post<boolean, boolean>('/api/stocks/rollback', payload)
}
