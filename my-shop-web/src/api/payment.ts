import http from './http'
import type { PaymentOrderInfo, PaymentRefundInfo } from '../types/domain'

export function getPaymentOrderByNo(paymentNo: string): Promise<PaymentOrderInfo> {
  return http.get<PaymentOrderInfo, PaymentOrderInfo>(`/api/payments/orders/${paymentNo}`)
}

export function getRefundByNo(refundNo: string): Promise<PaymentRefundInfo> {
  return http.get<PaymentRefundInfo, PaymentRefundInfo>(`/api/payments/refunds/${refundNo}`)
}
