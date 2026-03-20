import { clearSession, getAccessToken } from '../auth/session'
import { BusinessError, SUCCESS_CODE, type ResultEnvelope } from '../types/api'

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export interface RequestConfig {
  params?: object
  data?: unknown
  headers?: Record<string, string>
  raw?: boolean
  skipAuth?: boolean
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''
const apiTimeout = Number(import.meta.env.VITE_API_TIMEOUT || 10000)

function buildApiUrl(path: string): string {
  if (!apiBaseUrl) {
    return path
  }
  return `${apiBaseUrl.replace(/\/+$/, '')}${path}`
}

function buildUrl(path: string, params?: object): string {
  const base = buildApiUrl(path)
  if (!params) {
    return base
  }
  const search = new URLSearchParams()
  Object.entries(params as Record<string, unknown>).forEach(([key, value]) => {
    if (value == null) {
      return
    }
    if (Array.isArray(value)) {
      value.forEach((item) => {
        if (item != null) {
          search.append(key, String(item))
        }
      })
      return
    }
    search.append(key, String(value))
  })
  const query = search.toString()
  if (!query) {
    return base
  }
  return base.includes('?') ? `${base}&${query}` : `${base}?${query}`
}

function isResultEnvelope(payload: unknown): payload is ResultEnvelope<unknown> {
  if (typeof payload !== 'object' || payload === null) {
    return false
  }
  const candidate = payload as Record<string, unknown>
  return typeof candidate.code === 'number' && 'data' in candidate
}

function unwrapPayload<T>(payload: unknown): T {
  if (!isResultEnvelope(payload)) {
    return payload as T
  }

  if (payload.code !== SUCCESS_CODE) {
    throw new BusinessError(payload.message || 'Request failed', payload.code)
  }

  return payload.data as T
}

function normalizeError(payload: unknown, fallbackMessage: string): Error {
  if (payload instanceof BusinessError) {
    return payload
  }
  if (payload instanceof Error) {
    return payload
  }
  if (isResultEnvelope(payload)) {
    return new BusinessError(payload.message || fallbackMessage, payload.code)
  }
  if (typeof payload === 'string' && payload.trim().length > 0) {
    return new Error(payload)
  }
  return new Error(fallbackMessage)
}

async function request<T>(method: HttpMethod, url: string, config: RequestConfig = {}): Promise<T> {
  const headers: Record<string, string> = { ...(config.headers || {}) }
  if (!config.skipAuth) {
    const token = getAccessToken()
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  }

  const isGet = method === 'GET'
  const targetUrl = buildUrl(url, config.params)
  const payload = isGet ? config.params : config.data

  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: targetUrl,
      method: method as any,
      data: payload as any,
      header: headers,
      timeout: apiTimeout,
      withCredentials: true,
      success: (res) => {
        if (res.statusCode === 401) {
          clearSession()
        }
        if (res.statusCode && res.statusCode >= 400) {
          reject(normalizeError(res.data, 'Network request failed'))
          return
        }
        try {
          const result = config.raw ? (res.data as T) : unwrapPayload<T>(res.data)
          resolve(result)
        } catch (error) {
          reject(normalizeError(error, 'Request failed'))
        }
      },
      fail: (error) => {
        reject(normalizeError(error, 'Network request failed'))
      }
    })
  })
}

const http = {
  get<T, R = T>(url: string, config?: RequestConfig): Promise<R> {
    return request<R>('GET', url, config)
  },
  post<T, R = T>(url: string, data?: unknown, config?: RequestConfig): Promise<R> {
    return request<R>('POST', url, { ...config, data })
  },
  put<T, R = T>(url: string, data?: unknown, config?: RequestConfig): Promise<R> {
    return request<R>('PUT', url, { ...config, data })
  },
  patch<T, R = T>(url: string, data?: unknown, config?: RequestConfig): Promise<R> {
    return request<R>('PATCH', url, { ...config, data })
  },
  delete<T, R = T>(url: string, config?: RequestConfig): Promise<R> {
    return request<R>('DELETE', url, config)
  }
}

export function requestRaw<T>(method: HttpMethod, url: string, config?: RequestConfig): Promise<T> {
  return request<T>(method, url, { ...config, raw: true })
}

export default http
