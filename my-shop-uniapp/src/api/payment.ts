import http from './http'
import { BusinessError } from '../types/api'
import type {
  PaymentCallbackCommand,
  PaymentCheckoutSession,
  PaymentOrderCommand,
  PaymentOrderInfo,
  PaymentRefundCommand,
  PaymentRefundInfo
} from '../types/domain'

export function getPaymentOrderByNo(paymentNo: string): Promise<PaymentOrderInfo> {
  return http.get<PaymentOrderInfo, PaymentOrderInfo>(`/api/payments/orders/${paymentNo}`)
}

export async function getPaymentOrderByOrderNo(
  mainOrderNo: string,
  subOrderNo: string
): Promise<PaymentOrderInfo | null> {
  try {
    return await http.get<PaymentOrderInfo, PaymentOrderInfo>('/api/payments/orders/by-order', {
      params: { mainOrderNo, subOrderNo }
    })
  } catch (error) {
    if (error instanceof BusinessError && error.code === 404) {
      return null
    }
    throw error
  }
}

export function getRefundByNo(refundNo: string): Promise<PaymentRefundInfo> {
  return http.get<PaymentRefundInfo, PaymentRefundInfo>(`/api/payments/refunds/${refundNo}`)
}

export function createPaymentOrder(payload: PaymentOrderCommand): Promise<number> {
  return http.post<number, number>('/api/payments/orders', payload)
}

export function createPaymentCheckoutSession(paymentNo: string): Promise<PaymentCheckoutSession> {
  return http.post<PaymentCheckoutSession, PaymentCheckoutSession>(
    `/api/payments/orders/${paymentNo}/checkout-session`
  )
}

export function createPaymentRefund(payload: PaymentRefundCommand): Promise<number> {
  return http.post<number, number>('/api/payments/refunds', payload)
}

export function handlePaymentCallback(payload: PaymentCallbackCommand): Promise<boolean> {
  return http.post<boolean, boolean>('/api/payments/callbacks', payload)
}
