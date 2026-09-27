import { ref, type ComputedRef } from 'vue'
import { resolveApiUrl } from '../../../api/http'
import {
  createPaymentCheckoutSession,
  createPaymentOrder,
  getPaymentOrderByOrderNo
} from '../../../api/payment'
import { navigateTo, type GuardOptions } from '../../../router/navigation'
import { Routes } from '../../../router/routes'
import type { OrderSummaryDTO } from '../../../types/domain'
import { openExternalPage } from '../../../utils/external-navigation'
import { toast } from '../../../utils/ui'
import {
  actionKey,
  buildPaymentIdempotencyKey,
  buildPaymentNo,
  canPay,
  createPaymentAttemptToken
} from '../model/order-rules'

interface OrderPaymentMessages {
  payInfoMissing: string
  amountUnavailable: string
  openCheckoutFailed: string
}

function openCheckout(url: string, paymentNo: string): void {
  openExternalPage<GuardOptions>(url, {
    query: { paymentNo },
    guard: {
      requiresAuth: true,
      roles: ['USER', 'MERCHANT', 'ADMIN']
    },
    openWebview(target) {
      navigateTo(Routes.webview, { url: target.url, ...target.query }, target.guard)
    }
  })
}

export function useOrderPayment(copy: ComputedRef<OrderPaymentMessages>) {
  const payingOrderKey = ref<string | null>(null)

  async function onPay(order: OrderSummaryDTO): Promise<void> {
    const orderNo = order.orderNo
    const subOrderNo = order.subOrderNo
    if (
      !canPay(order) ||
      typeof order.userId !== 'string' ||
      !orderNo ||
      !subOrderNo
    ) {
      toast(copy.value.payInfoMissing)
      return
    }
    const amount = Number(order.payAmount ?? order.totalAmount ?? Number.NaN)
    if (!Number.isFinite(amount) || amount <= 0) {
      toast(copy.value.amountUnavailable)
      return
    }

    payingOrderKey.value = actionKey(order)
    let paymentNo = ''
    try {
      const existingOrder = await getPaymentOrderByOrderNo(orderNo, subOrderNo)
      const reusablePayment = existingOrder && existingOrder.status !== 'FAILED' ? existingOrder : null
      if (reusablePayment?.paymentNo) {
        paymentNo = reusablePayment.paymentNo
      } else {
        const attemptToken = createPaymentAttemptToken()
        paymentNo = buildPaymentNo(order, attemptToken)
        await createPaymentOrder({
          paymentNo,
          mainOrderNo: orderNo,
          subOrderNo,
          userId: order.userId,
          amount: Number(amount.toFixed(2)),
          channel: 'ALIPAY',
          idempotencyKey: buildPaymentIdempotencyKey(order, attemptToken)
        })
      }
      const session = await createPaymentCheckoutSession(paymentNo)
      if (!session.checkoutPath) {
        throw new Error('Checkout session is missing checkoutPath')
      }
      openCheckout(resolveApiUrl(session.checkoutPath), paymentNo)
    } catch (error) {
      toast(error instanceof Error ? error.message : copy.value.openCheckoutFailed)
      if (paymentNo) {
        navigateTo(
          Routes.appPayments,
          { paymentNo, autoPoll: 1 },
          { requiresAuth: true, roles: ['USER', 'ADMIN'] }
        )
      }
    } finally {
      payingOrderKey.value = null
    }
  }

  return { onPay, payingOrderKey }
}
