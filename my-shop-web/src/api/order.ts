import http from './http'
import type { OrderItem, OrderPage, OrderQuery } from '../types/domain'

export function listOrders(params: OrderQuery = {}): Promise<OrderPage> {
  return http.get<OrderPage, OrderPage>('/api/orders', { params })
}

export function getOrderById(id: number): Promise<OrderItem> {
  return http.get<OrderItem, OrderItem>(`/api/orders/${id}`)
}

export function payOrder(id: number): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/orders/${id}/pay`)
}

export function cancelOrder(id: number, reason?: string): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/orders/${id}/cancel`, null, { params: { cancelReason: reason } })
}

export function shipOrder(id: number): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/orders/${id}/ship`)
}

export function completeOrder(id: number): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/orders/${id}/complete`)
}

export function batchPayOrders(ids: number[]): Promise<number> {
  return http.post<number, number>('/api/orders/batch/pay', ids)
}

export function batchCancelOrders(ids: number[], reason?: string): Promise<number> {
  return http.post<number, number>('/api/orders/batch/cancel', ids, { params: { cancelReason: reason } })
}

export function batchShipOrders(ids: number[]): Promise<number> {
  return http.post<number, number>('/api/orders/batch/ship', ids)
}

export function batchCompleteOrders(ids: number[]): Promise<number> {
  return http.post<number, number>('/api/orders/batch/complete', ids)
}
