import { isAuthenticated } from '../auth/session'
import { hasAnyRole, type UserRole } from '../auth/permission'
import { Routes, type RoutePath } from './routes'

export interface GuardOptions {
  requiresAuth?: boolean
  roles?: UserRole[]
}

function buildQuery(params?: Record<string, unknown>): string {
  if (!params) {
    return ''
  }
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value == null) {
      return
    }
    search.append(key, String(value))
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

function ensureAuth(targetUrl: string, guard?: GuardOptions): boolean {
  if (!guard?.requiresAuth) {
    return true
  }
  if (!isAuthenticated()) {
    const redirect = encodeURIComponent(targetUrl)
    uni.redirectTo({ url: `${Routes.login}?redirect=${redirect}` })
    return false
  }
  if (guard.roles && guard.roles.length > 0 && !hasAnyRole(...guard.roles)) {
    uni.redirectTo({ url: Routes.forbidden })
    return false
  }
  return true
}

export function navigateTo(path: RoutePath, query?: Record<string, unknown>, guard?: GuardOptions): void {
  const url = `${path}${buildQuery(query)}`
  if (!ensureAuth(url, guard)) {
    return
  }
  uni.navigateTo({ url })
}

export function redirectTo(path: RoutePath, query?: Record<string, unknown>, guard?: GuardOptions): void {
  const url = `${path}${buildQuery(query)}`
  if (!ensureAuth(url, guard)) {
    return
  }
  uni.redirectTo({ url })
}

export function reLaunch(path: RoutePath, query?: Record<string, unknown>, guard?: GuardOptions): void {
  const url = `${path}${buildQuery(query)}`
  if (!ensureAuth(url, guard)) {
    return
  }
  uni.reLaunch({ url })
}

export function back(delta = 1): void {
  uni.navigateBack({ delta })
}

export function currentRoutePath(): string {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  return current?.route ? `/${current.route}` : ''
}
