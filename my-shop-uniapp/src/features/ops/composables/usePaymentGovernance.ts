import { ref } from 'vue'
import { resolveApiUrl } from '../../../api/http'
import {
  createPaymentCheckoutSession,
  createPaymentOrder,
  createPaymentRefund,
  getPaymentOrderByNo,
  getPaymentOrderByOrderNo,
  getPaymentStatus
} from '../../../api/payment'
import { navigateTo, type GuardOptions } from '../../../router/navigation'
import { Routes } from '../../../router/routes'
import type {
  PaymentOrderCommand,
  PaymentOrderInfo,
  PaymentRefundCommand,
  PaymentStatusInfo
} from '../../../types/domains/payment'
import { openExternalPage } from '../../../utils/external-navigation'
import { toast } from '../../../utils/ui'
import type { OpsInputTools } from '../model/input-tools'

type PaymentInputTools = Pick<OpsInputTools, 'parseJson'>
type PaymentOperationResult = number | PaymentOrderInfo | PaymentStatusInfo | null

export function usePaymentGovernance(input: PaymentInputTools) {
  const paymentNoInput = ref('')
  const paymentMainOrderNo = ref('')
  const paymentSubOrderNo = ref('')
  const paymentOrderJson = ref('')
  const paymentRefundJson = ref('')
  const paymentResult = ref<PaymentOperationResult>(null)

  async function createPaymentOrderAction(): Promise<void> {
    const payload = input.parseJson<PaymentOrderCommand>(paymentOrderJson.value, 'Payment order')
    if (!payload) return
    try {
      paymentResult.value = await createPaymentOrder(payload)
      toast('Payment order created', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Create failed')
    }
  }

  async function createPaymentRefundAction(): Promise<void> {
    const payload = input.parseJson<PaymentRefundCommand>(paymentRefundJson.value, 'Payment refund')
    if (!payload) return
    try {
      paymentResult.value = await createPaymentRefund(payload)
      toast('Payment refund created', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Create failed')
    }
  }

  async function loadPaymentByNoAction(): Promise<void> {
    const paymentNo = paymentNoInput.value.trim()
    if (!paymentNo) {
      toast('Please enter a payment number')
      return
    }
    try {
      paymentResult.value = await getPaymentOrderByNo(paymentNo)
      toast('Payment order loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  async function loadPaymentByOrderAction(): Promise<void> {
    const mainOrderNo = paymentMainOrderNo.value.trim()
    const subOrderNo = paymentSubOrderNo.value.trim()
    if (!mainOrderNo || !subOrderNo) {
      toast('Please enter both the main order number and sub order number')
      return
    }
    try {
      paymentResult.value = await getPaymentOrderByOrderNo(mainOrderNo, subOrderNo)
      toast('Payment order lookup completed', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Lookup failed')
    }
  }

  async function refreshPaymentStatusAction(): Promise<void> {
    const paymentNo = paymentNoInput.value.trim()
    if (!paymentNo) {
      toast('Please enter a payment number')
      return
    }
    try {
      paymentResult.value = await getPaymentStatus(paymentNo)
      toast('Payment status loaded', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Load failed')
    }
  }

  async function openPaymentCheckoutAction(): Promise<void> {
    const paymentNo = paymentNoInput.value.trim()
    if (!paymentNo) {
      toast('Please enter a payment number')
      return
    }
    try {
      const session = await createPaymentCheckoutSession(paymentNo)
      if (!session.checkoutPath) {
        throw new Error('Checkout session is missing checkoutPath')
      }
      openExternalPage<GuardOptions>(resolveApiUrl(session.checkoutPath), {
        query: { paymentNo },
        guard: {
          requiresAuth: true,
          roles: ['USER', 'MERCHANT', 'ADMIN']
        },
        openWebview(target) {
          navigateTo(Routes.webview, { url: target.url, ...target.query }, target.guard)
        }
      })
      toast('Checkout page opened', 'success')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Open checkout failed')
    }
  }

  return {
    createPaymentOrderAction,
    createPaymentRefundAction,
    loadPaymentByNoAction,
    loadPaymentByOrderAction,
    openPaymentCheckoutAction,
    paymentMainOrderNo,
    paymentNoInput,
    paymentOrderJson,
    paymentRefundJson,
    paymentResult,
    paymentSubOrderNo,
    refreshPaymentStatusAction
  }
}
