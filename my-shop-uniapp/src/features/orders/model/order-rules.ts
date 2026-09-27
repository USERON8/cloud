import type { OrderSummaryDTO, OrderSummaryItem } from '../../../types/domain'

export function actionKey(order: OrderSummaryDTO): string {
  return `${order.id ?? 'main'}:${order.subOrderId ?? 'sub'}`
}

export function orderItems(order: OrderSummaryDTO): OrderSummaryItem[] {
  return Array.isArray(order.items) ? order.items : []
}

export function buildSubOrderActionTarget(
  order: OrderSummaryDTO,
  subOrder: NonNullable<OrderSummaryDTO['subOrders']>[number]
): OrderSummaryDTO {
  const items = orderItems(order).filter(
    (item) => typeof subOrder.subOrderId !== 'string' || item.subOrderId === subOrder.subOrderId
  )
  return {
    id: order.id,
    orderNo: order.orderNo,
    userId: order.userId,
    subOrderId: subOrder.subOrderId,
    subOrderNo: subOrder.subOrderNo,
    merchantId: subOrder.merchantId,
    afterSaleId: subOrder.afterSaleId,
    afterSaleNo: subOrder.afterSaleNo,
    afterSaleType: subOrder.afterSaleType,
    refundNo: subOrder.refundNo,
    totalAmount: subOrder.payAmount ?? order.totalAmount,
    payAmount: subOrder.payAmount ?? order.payAmount,
    status: subOrder.status,
    orderStatusRaw: subOrder.orderStatusRaw,
    afterSaleStatus: subOrder.afterSaleStatus,
    createdAt: order.createdAt,
    items
  }
}

export function orderSubActionTargets(order: OrderSummaryDTO): OrderSummaryDTO[] {
  return Array.isArray(order.subOrders) && order.subOrders.length > 0
    ? order.subOrders.map((subOrder) => buildSubOrderActionTarget(order, subOrder))
    : [order]
}

export function hasMultipleSubOrders(order: OrderSummaryDTO): boolean {
  return orderSubActionTargets(order).length > 1
}

export function canApplyAfterSale(order: OrderSummaryDTO): boolean {
  return (
    typeof order.id === 'string' &&
    typeof order.subOrderId === 'string' &&
    typeof order.merchantId === 'string' &&
    [1, 2, 3].includes(order.status ?? -1) &&
    (!order.afterSaleStatus || order.afterSaleStatus === 'NONE')
  )
}

export function canCancelAfterSale(order: OrderSummaryDTO): boolean {
  return (
    typeof order.afterSaleId === 'string' &&
    ['APPLIED', 'WAIT_RETURN'].includes(order.afterSaleStatus ?? '')
  )
}

export function canPay(order: OrderSummaryDTO): boolean {
  return (
    order.status === 0 &&
    typeof order.userId === 'string' &&
    Boolean(order.orderNo) &&
    Boolean(order.subOrderNo)
  )
}

export function canComplete(order: OrderSummaryDTO): boolean {
  const subOrders = orderSubActionTargets(order)
  return (
    typeof order.id === 'string' &&
    subOrders.length > 0 &&
    subOrders.every((subOrder) => subOrder.status === 2)
  )
}

export function canCancel(order: OrderSummaryDTO): boolean {
  const subOrders = orderSubActionTargets(order)
  return (
    typeof order.id === 'string' &&
    subOrders.length > 0 &&
    subOrders.every((subOrder) => subOrder.status === 0)
  )
}

export function canViewRefund(order: OrderSummaryDTO): boolean {
  return (
    Boolean(order.refundNo) &&
    ['REFUNDING', 'REFUNDED'].includes(order.afterSaleStatus ?? '')
  )
}

export function createPaymentAttemptToken(): string {
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`
}

export function buildPaymentIdempotencyKey(
  order: OrderSummaryDTO,
  attemptToken: string
): string {
  return `payment:${order.orderNo}:${order.subOrderNo ?? order.id}:${attemptToken}`
}

export function buildPaymentNo(order: OrderSummaryDTO, attemptToken: string): string {
  const subOrderNo = order.subOrderNo?.replace(/[^A-Za-z0-9_-]/g, '') || String(order.id)
  return `PAY-${subOrderNo}-${attemptToken}`
}

function readSnapshotText(
  snapshot: Record<string, unknown> | undefined,
  key: string
): string {
  const value = snapshot?.[key]
  return typeof value === 'string' ? value.trim() : ''
}

export function formatSpecText(specJson?: string): string {
  if (!specJson?.trim()) {
    return ''
  }
  try {
    const parsed = JSON.parse(specJson) as Record<string, unknown>
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return specJson
    }
    return Object.entries(parsed)
      .filter(([, value]) => value !== null && value !== undefined && String(value).trim())
      .map(([key, value]) => `${key}: ${value}`)
      .join(' / ')
  } catch {
    return specJson
  }
}

export function resolveOrderItemName(item: OrderSummaryItem): string {
  return (
    item.latestProduct?.skuName ||
    item.skuName ||
    readSnapshotText(item.skuSnapshot, 'skuName') ||
    readSnapshotText(item.skuSnapshot, 'spuName') ||
    `SKU ${item.skuId ?? '--'}`
  )
}

export function resolveOrderItemSpec(item: OrderSummaryItem): string {
  return formatSpecText(
    item.latestProduct?.specJson || readSnapshotText(item.skuSnapshot, 'specJson')
  )
}
