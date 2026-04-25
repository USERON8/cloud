import http from './http'
import type { StockLedger } from '../types/domain'

export function getStockLedger(skuId: number): Promise<StockLedger> {
  return http.get<StockLedger, StockLedger>(`/api/admin/stocks/ledger/${skuId}`)
}
