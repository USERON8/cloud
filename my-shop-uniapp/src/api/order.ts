import http from './http'
import type { AfterSaleInfo, CreateOrderPayload, OrderItem, OrderPage, OrderQuery } from '../types/domain'
import { sessionState } from '../auth/session'

export function createOrder(payload: CreateOrderPayload): Promise<unknown> {
  const userId = sessionState.user?.id
  if (typeof userId !== 'number') {
    return Promise.reject(new Error('User session is required'))
  }
  if (typeof payload.skuId !== 'number') {
    return Promise.reject(new Error('skuId is required'))
  }
  if (!payload.receiverName.trim() || !payload.receiverPhone.trim() || !payload.receiverAddress.trim()) {
    return Promise.reject(new Error('Receiver details are required'))
  }
  const totalAmount = Number((payload.price * payload.quantity).toFixed(2))
  const body = {
    userId,
    spuId: payload.spuId,
    skuId: payload.skuId,
    quantity: payload.quantity,
    totalAmount,
    payableAmount: totalAmount,
    receiverName: payload.receiverName.trim(),
    receiverPhone: payload.receiverPhone.trim(),
    receiverAddress: payload.receiverAddress.trim()
  }
  const idempotencyKey = `${userId}-${payload.spuId}-${payload.skuId}-${Date.now()}`
  return http.post<unknown, unknown>('/api/orders', body, {
    headers: {
      'Idempotency-Key': idempotencyKey
    }
  })
}

export function listOrders(params: OrderQuery = {}): Promise<OrderPage> {
  const { merchantId, shopId, ...rest } = params
  return http.get<OrderPage, OrderPage>('/api/orders', {
    params: {
      ...rest,
      merchantId: merchantId ?? shopId
    }
  })
}

export function getOrderById(id: number): Promise<OrderItem> {
  return http.get<OrderItem, OrderItem>(`/api/orders/${id}`)
}

export function payOrder(id: number): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/orders/${id}/pay`)
}

export function cancelOrder(id: number, reason?: string): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/orders/${id}/cancel`, null, {
    params: { cancelReason: reason }
  })
}

export function shipOrder(id: number, shippingCompany: string, trackingNumber: string): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/orders/${id}/ship`, null, {
    params: { shippingCompany, trackingNumber }
  })
}

export function completeOrder(id: number): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/orders/${id}/complete`)
}

export function batchPayOrders(ids: number[]): Promise<number> {
  return http.post<number, number>('/api/orders/batch/pay', ids)
}

export function batchCancelOrders(ids: number[], reason?: string): Promise<number> {
  return http.post<number, number>('/api/orders/batch/cancel', ids, {
    params: { cancelReason: reason }
  })
}

export function batchShipOrders(ids: number[], shippingCompany: string, trackingNumber: string): Promise<number> {
  return http.post<number, number>('/api/orders/batch/ship', ids, {
    params: { shippingCompany, trackingNumber }
  })
}

export function batchCompleteOrders(ids: number[]): Promise<number> {
  return http.post<number, number>('/api/orders/batch/complete', ids)
}

export function applyAfterSale(payload: AfterSaleInfo): Promise<AfterSaleInfo> {
  return http.post<AfterSaleInfo, AfterSaleInfo>('/api/orders/after-sales', payload)
}
