import http from './http'
import type {
  PaymentCallbackCommand,
  PaymentOrderCommand,
  PaymentOrderInfo,
  PaymentRefundCommand,
  PaymentRefundInfo
} from '../types/domain'

export function getPaymentOrderByNo(paymentNo: string): Promise<PaymentOrderInfo> {
  return http.get<PaymentOrderInfo, PaymentOrderInfo>(`/api/payments/orders/${paymentNo}`)
}

export function getRefundByNo(refundNo: string): Promise<PaymentRefundInfo> {
  return http.get<PaymentRefundInfo, PaymentRefundInfo>(`/api/payments/refunds/${refundNo}`)
}

export function createPaymentOrder(payload: PaymentOrderCommand): Promise<number> {
  return http.post<number, number>('/api/payments/orders', payload)
}

export function createPaymentRefund(payload: PaymentRefundCommand): Promise<number> {
  return http.post<number, number>('/api/payments/refunds', payload)
}

export function handlePaymentCallback(payload: PaymentCallbackCommand): Promise<boolean> {
  return http.post<boolean, boolean>('/api/payments/callbacks', payload)
}
