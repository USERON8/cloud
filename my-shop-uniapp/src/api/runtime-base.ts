const configuredApiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').trim()
const configuredDevProxyTarget = (import.meta.env.VITE_DEV_PROXY_TARGET || '').trim()

function normalizeOrigin(value: string): string {
  if (!value) {
    return ''
  }
  try {
    return new URL(value, typeof window !== 'undefined' ? window.location.origin : undefined).origin
  } catch {
    return ''
  }
}

function shouldUseSameOriginProxy(): boolean {
  if (typeof window === 'undefined' || !window.location) {
    return false
  }

  const currentOrigin = normalizeOrigin(window.location.origin)
  if (!currentOrigin) {
    return false
  }

  const apiOrigin = normalizeOrigin(configuredApiBaseUrl)
  if (apiOrigin && apiOrigin === currentOrigin) {
    return true
  }

  const proxyOrigin = normalizeOrigin(configuredDevProxyTarget)
  return proxyOrigin.length > 0 && proxyOrigin === currentOrigin
}

export function resolveApiBaseUrl(): string {
  if (!configuredApiBaseUrl || shouldUseSameOriginProxy()) {
    return ''
  }
  return configuredApiBaseUrl.replace(/\/+$/, '')
}

export function buildApiUrl(path: string): string {
  const baseUrl = resolveApiBaseUrl()
  return baseUrl ? `${baseUrl}${path}` : path
}
