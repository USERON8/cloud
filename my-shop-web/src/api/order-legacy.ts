import http from './http'
import type {
  LegacyAfterSale,
  LegacyCreateMainOrderRequest,
  LegacyOrderAggregate,
  LegacyOrderSub
} from '../types/domain'

export function createMainOrder(payload: LegacyCreateMainOrderRequest, idempotencyKey: string): Promise<LegacyOrderAggregate> {
  return http.post<LegacyOrderAggregate, LegacyOrderAggregate>('/api/orders', payload, {
    headers: {
      'Idempotency-Key': idempotencyKey
    }
  })
}

export function getMainOrder(mainOrderId: number): Promise<LegacyOrderAggregate> {
  return http.get<LegacyOrderAggregate, LegacyOrderAggregate>(`/api/orders/main/${mainOrderId}`)
}

export function listSubOrders(mainOrderId: number): Promise<LegacyOrderSub[]> {
  return http.get<LegacyOrderSub[], LegacyOrderSub[]>(`/api/orders/main/${mainOrderId}/sub-orders`)
}

export function advanceSubOrderStatus(subOrderId: number, action: string): Promise<LegacyOrderSub> {
  return http.post<LegacyOrderSub, LegacyOrderSub>(`/api/orders/sub/${subOrderId}/actions/${action}`)
}

export function applyAfterSale(payload: LegacyAfterSale): Promise<LegacyAfterSale> {
  return http.post<LegacyAfterSale, LegacyAfterSale>('/api/orders/after-sales', payload)
}

export function advanceAfterSaleStatus(afterSaleId: number, action: string, remark?: string): Promise<LegacyAfterSale> {
  return http.post<LegacyAfterSale, LegacyAfterSale>(`/api/orders/after-sales/${afterSaleId}/actions/${action}`, null, {
    params: {
      remark
    }
  })
}
