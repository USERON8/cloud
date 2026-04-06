import http from './http'
import type {
  CreateCartOrderPayload,
  AfterSaleInfo,
  CreateOrderPayload,
  OrderAggregateResponse,
  OrderSummaryDTO,
  OrderPage,
  OrderQuery
} from '../types/domain'
import { sessionState } from '../auth/session'

function buildClientOrderId(userId: number, payload: CreateOrderPayload): string {
  if (payload.clientOrderId?.trim()) {
    return payload.clientOrderId.trim()
  }
  return `cli-${userId}-${payload.spuId}-${payload.skuId}-${Date.now()}`
}

export function createOrder(payload: CreateOrderPayload): Promise<OrderAggregateResponse> {
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
    clientOrderId: buildClientOrderId(userId, payload),
    receiverName: payload.receiverName.trim(),
    receiverPhone: payload.receiverPhone.trim(),
    receiverAddress: payload.receiverAddress.trim()
  }
  const idempotencyKey = `${userId}-${payload.spuId}-${payload.skuId}-${Date.now()}`
  return http.post<OrderAggregateResponse, OrderAggregateResponse>('/api/orders', body, {
    headers: {
      'Idempotency-Key': idempotencyKey
    }
  })
}

export function createCartOrder(payload: CreateCartOrderPayload): Promise<OrderAggregateResponse> {
  const userId = sessionState.user?.id
  if (typeof userId !== 'number') {
    return Promise.reject(new Error('User session is required'))
  }
  if (typeof payload.cartId !== 'number') {
    return Promise.reject(new Error('cartId is required'))
  }
  if (!payload.receiverName.trim() || !payload.receiverPhone.trim() || !payload.receiverAddress.trim()) {
    return Promise.reject(new Error('Receiver details are required'))
  }
  const body = {
    userId,
    cartId: payload.cartId,
    clientOrderId: payload.clientOrderId?.trim() || `cart-${userId}-${payload.cartId}-${Date.now()}`,
    receiverName: payload.receiverName.trim(),
    receiverPhone: payload.receiverPhone.trim(),
    receiverAddress: payload.receiverAddress.trim()
  }
  const idempotencyKey = `${userId}-cart-${payload.cartId}-${Date.now()}`
  return http.post<OrderAggregateResponse, OrderAggregateResponse>('/api/orders', body, {
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

export function getOrderById(id: number): Promise<OrderSummaryDTO> {
  return http.get<OrderSummaryDTO, OrderSummaryDTO>(`/api/orders/${id}`)
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

export function advanceAfterSaleStatus(afterSaleId: number, action: string, remark?: string): Promise<AfterSaleInfo> {
  return http.post<AfterSaleInfo, AfterSaleInfo>(`/api/orders/after-sales/${afterSaleId}/actions/${action}`, null, {
    params: { remark }
  })
}
