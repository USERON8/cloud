import http from './http'
import { BusinessError } from '../types/api'
import type {
  PaymentCheckoutSession,
  PaymentOrderCommand,
  PaymentOrderInfo,
  PaymentStatusInfo,
  PaymentRefundCommand,
  PaymentRefundInfo
} from '../types/domain'

export function getPaymentOrderByNo(paymentNo: string): Promise<PaymentOrderInfo> {
  return http.get<PaymentOrderInfo, PaymentOrderInfo>(`/api/payment-orders/${paymentNo}`)
}

export async function getPaymentOrderByOrderNo(
  mainOrderNo: string,
  subOrderNo: string
): Promise<PaymentOrderInfo | null> {
  try {
    return await http.get<PaymentOrderInfo, PaymentOrderInfo>('/api/payment-orders', {
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
  return http.get<PaymentRefundInfo, PaymentRefundInfo>(`/api/payment-refunds/${refundNo}`)
}

export function createPaymentOrder(payload: PaymentOrderCommand): Promise<number> {
  return http.post<number, number>('/api/payment-orders', payload)
}

export function createPaymentCheckoutSession(paymentNo: string): Promise<PaymentCheckoutSession> {
  return http.post<PaymentCheckoutSession, PaymentCheckoutSession>(
    `/api/payment-orders/${paymentNo}/checkout-sessions`
  )
}

export function getPaymentStatus(paymentNo: string): Promise<PaymentStatusInfo> {
  return http.get<PaymentStatusInfo, PaymentStatusInfo>(`/api/payment-orders/${paymentNo}/status`)
}

export function createPaymentRefund(payload: PaymentRefundCommand): Promise<number> {
  return http.post<number, number>('/api/payment-refunds', payload)
}
