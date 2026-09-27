import http from './http'
import type {
  CreateCartOrderPayload,
  AfterSaleInfo,
  CreateOrderPayload,
  OrderAggregateResponse,
  OrderSummaryDTO,
  OrderPage,
  OrderQuery,
  EntityId
} from '../types/domain'
import { getCurrentUserId } from '../auth/session'

function buildRequestKeys(
  providedClientOrderId: string | undefined,
  fallbackClientOrderId: string
): { clientOrderId: string; idempotencyKey: string } {
  const clientOrderId = providedClientOrderId?.trim() || fallbackClientOrderId
  return {
    clientOrderId,
    idempotencyKey: clientOrderId
  }
}

function buildClientOrderId(userId: string, payload: CreateOrderPayload): string {
  if (payload.clientOrderId?.trim()) {
    return payload.clientOrderId.trim()
  }
  return `cli-${userId}-${payload.spuId}-${payload.skuId}-${Date.now()}`
}

export function createOrder(payload: CreateOrderPayload): Promise<OrderAggregateResponse> {
  const userId = getCurrentUserId()
  if (!userId) {
    return Promise.reject(new Error('User session is required'))
  }
  if (!payload.skuId.trim()) {
    return Promise.reject(new Error('skuId is required'))
  }
  if (!payload.receiverName.trim() || !payload.receiverPhone.trim() || !payload.receiverAddress.trim()) {
    return Promise.reject(new Error('Receiver details are required'))
  }
  const totalAmount = Number((payload.price * payload.quantity).toFixed(2))
  const requestKeys = buildRequestKeys(
    payload.clientOrderId,
    buildClientOrderId(userId, payload)
  )
  const body = {
    spuId: payload.spuId,
    skuId: payload.skuId,
    quantity: payload.quantity,
    totalAmount,
    payableAmount: totalAmount,
    clientOrderId: requestKeys.clientOrderId,
    receiverName: payload.receiverName.trim(),
    receiverPhone: payload.receiverPhone.trim(),
    receiverAddress: payload.receiverAddress.trim()
  }
  return http.post<OrderAggregateResponse, OrderAggregateResponse>('/api/orders', body, {
    headers: {
      'Idempotency-Key': requestKeys.idempotencyKey
    }
  })
}

export function createCartOrder(payload: CreateCartOrderPayload): Promise<OrderAggregateResponse> {
  const userId = getCurrentUserId()
  if (!userId) {
    return Promise.reject(new Error('User session is required'))
  }
  const cartId = payload.cartId.trim()
  if (!cartId) {
    return Promise.reject(new Error('cartId is required'))
  }
  if (!payload.receiverName.trim() || !payload.receiverPhone.trim() || !payload.receiverAddress.trim()) {
    return Promise.reject(new Error('Receiver details are required'))
  }
  const requestKeys = buildRequestKeys(
    payload.clientOrderId,
    `cart-${userId}-${cartId}-${Date.now()}`
  )
  const body = {
    cartId,
    clientOrderId: requestKeys.clientOrderId,
    receiverName: payload.receiverName.trim(),
    receiverPhone: payload.receiverPhone.trim(),
    receiverAddress: payload.receiverAddress.trim()
  }
  return http.post<OrderAggregateResponse, OrderAggregateResponse>('/api/orders', body, {
    headers: {
      'Idempotency-Key': requestKeys.idempotencyKey
    }
  })
}

export function listOrders(params: OrderQuery = {}): Promise<OrderPage> {
  return http.get<OrderPage, OrderPage>('/api/orders', {
    params
  })
}

export function getOrderById(id: EntityId): Promise<OrderSummaryDTO> {
  return http.get<OrderSummaryDTO, OrderSummaryDTO>(`/api/orders/${id}`)
}

export function cancelOrder(id: EntityId, reason?: string): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/orders/${id}/cancellation`, null, {
    params: { cancelReason: reason }
  })
}

export function shipOrder(id: EntityId, shippingCompany: string, trackingNumber: string): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/orders/${id}/shipments`, null, {
    params: { shippingCompany, trackingNumber }
  })
}

export function completeOrder(id: EntityId): Promise<boolean> {
  return http.post<boolean, boolean>(`/api/orders/${id}/completion`)
}

export function batchCancelOrders(ids: EntityId[], reason?: string): Promise<number> {
  return http.post<number, number>('/api/orders/bulk/cancellations', ids, {
    params: { cancelReason: reason }
  })
}

export function batchShipOrders(ids: EntityId[], shippingCompany: string, trackingNumber: string): Promise<number> {
  return http.post<number, number>('/api/orders/bulk/shipments', ids, {
    params: { shippingCompany, trackingNumber }
  })
}

export function batchCompleteOrders(ids: EntityId[]): Promise<number> {
  return http.post<number, number>('/api/orders/bulk/completions', ids)
}

export function applyAfterSale(payload: AfterSaleInfo): Promise<AfterSaleInfo> {
  return http.post<AfterSaleInfo, AfterSaleInfo>('/api/after-sales', payload)
}

export function advanceAfterSaleStatus(afterSaleId: EntityId, action: string, remark?: string): Promise<AfterSaleInfo> {
  return http.post<AfterSaleInfo, AfterSaleInfo>(`/api/after-sales/${afterSaleId}/events`, null, {
    params: { action, remark }
  })
}
