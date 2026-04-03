import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)

/** Shared order status label used by orders and orders-manage pages */
export function formatOrderStatus(status?: number): string {
  if (status === 0) return 'Awaiting payment'
  if (status === 1) return 'Paid'
  if (status === 2) return 'Shipped'
  if (status === 3) return 'Completed'
  if (status === 4) return 'Cancelled'
  return status != null ? String(status) : 'Unknown'
}

/** Product publish/unpublish status label */
export function formatProductStatus(status?: number): string {
  if (status === 1) return 'Published'
  if (status === 0) return 'Unpublished'
  return 'Unknown'
}

export function formatPrice(price?: number, currency = 'CNY'): string {
  if (typeof price !== 'number' || !Number.isFinite(price)) {
    return '--'
  }
  return `${currency} ${price.toFixed(2)}`
}

export function parseDate(value?: string): dayjs.Dayjs | null {
  if (!value) {
    return null
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed : null
}

export function formatDate(value?: string): string {
  const parsed = parseDate(value)
  if (!parsed) {
    return value || '--'
  }
  return parsed.format('YYYY-MM-DD HH:mm')
}

export function isDateAfter(start?: string, end?: string): boolean {
  const startDate = parseDate(start)
  const endDate = parseDate(end)
  if (!startDate || !endDate) {
    return false
  }
  return startDate.isAfter(endDate)
}

export function formatDateOnly(value?: string): string {
  const parsed = parseDate(value)
  if (!parsed) {
    return '--'
  }
  return parsed.format('YYYY-MM-DD')
}

export function formatRelativeDate(value?: string): string {
  const parsed = parseDate(value)
  if (!parsed) {
    return '--'
  }
  return parsed.fromNow()
}
