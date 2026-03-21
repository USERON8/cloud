import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)

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
