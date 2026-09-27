import http from './http'
import type { EntityId, StockLedger } from '../types/domain'

export function getStockLedger(skuId: EntityId): Promise<StockLedger> {
  return http.get<StockLedger, StockLedger>(`/api/admin/stocks/ledger/${skuId}`)
}
