import { configureHttpFailureHandler } from '../platform/http/failure-events'
import { Routes } from '../router/routes'
import { BusinessError } from '../types/api'

type ErrorPageKind = 'network' | 'timeout' | 'server' | 'not-found'

let lastErrorPageAt = 0

function currentRoutePath(): string {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  return current?.route ? `/${current.route}` : ''
}

function describeUnknownError(value: unknown): string {
  if (value instanceof Error) {
    return value.message
  }
  if (typeof value === 'object' && value !== null && 'errMsg' in value) {
    return String((value as { errMsg?: unknown }).errMsg || '')
  }
  return typeof value === 'string' ? value : ''
}

function openErrorPage(
  kind: ErrorPageKind,
  options: { status?: number; message?: string } = {}
): void {
  const current = currentRoutePath()
  if (current === Routes.error) {
    return
  }

  const now = Date.now()
  if (now - lastErrorPageAt < 1200) {
    return
  }
  lastErrorPageAt = now

  const query = new URLSearchParams()
  query.set('kind', kind)
  if (options.status) {
    query.set('status', String(options.status))
  }
  if (options.message) {
    query.set('message', options.message.slice(0, 180))
  }
  if (current) {
    query.set('redirect', current)
  }

  uni.redirectTo({ url: `${Routes.error}?${query.toString()}` })
}

export function installRequestFailureNavigation(): void {
  configureHttpFailureHandler(({ kind, error, cause, status }) => {
    if (kind === 'network') {
      const message = describeUnknownError(cause) || error.message
      openErrorPage(message.toLowerCase().includes('timeout') ? 'timeout' : 'network', {
        message: message || 'Please check the network connection and try again'
      })
      return
    }

    if (error instanceof BusinessError) {
      if (error.category === 'remote') {
        openErrorPage(status === 504 || error.code === 18002 ? 'timeout' : 'server', {
          status,
          message: error.message
        })
      } else if (error.category === 'system') {
        openErrorPage('server', { status, message: error.message })
      }
      return
    }

    if (status && status >= 500) {
      openErrorPage('server', { status, message: error.message })
    }
  })
}
