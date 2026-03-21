import axios, { AxiosHeaders, type AxiosRequestConfig, type AxiosResponse, isAxiosError } from 'axios'
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

interface InternalRequestConfig extends AxiosRequestConfig {
  raw?: boolean
  skipAuth?: boolean
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''
const apiTimeout = Number(import.meta.env.VITE_API_TIMEOUT || 10000)
const inflightGetRequests = new Map<string, Promise<unknown>>()

function buildApiUrl(path: string): string {
  if (!apiBaseUrl) {
    return path
  }
  return `${apiBaseUrl.replace(/\/+$/, '')}${path}`
}

export function resolveApiUrl(path: string): string {
  return buildApiUrl(path)
}

function buildSearchParams(params?: object): string {
  if (!params) {
    return ''
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
  return search.toString()
}

function buildUrl(path: string, params?: object): string {
  const base = buildApiUrl(path)
  const query = buildSearchParams(params)
  if (!query) {
    return base
  }
  return base.includes('?') ? `${base}&${query}` : `${base}?${query}`
}

function normalizeHeaders(headers?: AxiosRequestConfig['headers']): Record<string, string> {
  if (!headers) {
    return {}
  }
  const normalized: Record<string, string> = {}
  if (headers instanceof AxiosHeaders) {
    Object.entries(headers.toJSON()).forEach(([key, value]) => {
      normalized[key] = value == null ? '' : String(value)
    })
    return normalized
  }
  Object.entries(headers as Record<string, unknown>).forEach(([key, value]) => {
    normalized[key] = value == null ? '' : String(value)
  })
  return normalized
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

const httpClient = axios.create({
  timeout: apiTimeout,
  validateStatus: () => true,
  adapter: async (config) => {
    const targetUrl = buildUrl(config.url || '', config.params as object | undefined)
    const payload = config.method?.toUpperCase() === 'GET' ? undefined : config.data
    const headers = normalizeHeaders(config.headers)

    return new Promise<AxiosResponse>((resolve, reject) => {
      uni.request({
        url: targetUrl,
        method: (config.method || 'GET').toUpperCase() as any,
        data: payload as any,
        header: headers,
        timeout: config.timeout,
        withCredentials: true,
        success: (res) => {
          resolve({
            data: res.data,
            status: res.statusCode || 0,
            statusText: String(res.statusCode || ''),
            headers: res.header as Record<string, string>,
            config,
            request: null
          })
        },
        fail: (error) => {
          reject(error)
        }
      })
    })
  }
})

httpClient.interceptors.request.use((config) => {
  const nextConfig = config as InternalRequestConfig & { headers?: any }
  if (!nextConfig.skipAuth) {
    const token = getAccessToken()
    if (token) {
      const headers = AxiosHeaders.from(nextConfig.headers)
      headers.set('Authorization', `Bearer ${token}`)
      nextConfig.headers = headers
    }
  }
  return nextConfig as any
})

async function request<T>(method: HttpMethod, url: string, config: RequestConfig = {}): Promise<T> {
  const requestUrl = buildUrl(url, config.params)
  const accessToken = config.skipAuth ? '' : getAccessToken()
  const requestKey = method === 'GET' ? `${requestUrl}::${accessToken}` : ''

  if (method === 'GET') {
    const inflightRequest = inflightGetRequests.get(requestKey)
    if (inflightRequest) {
      return inflightRequest as Promise<T>
    }
  }

  const requestPromise = httpClient
    .request<unknown>({
      url,
      method,
      params: config.params,
      data: config.data,
      headers: config.headers,
      raw: config.raw,
      skipAuth: config.skipAuth
    } as InternalRequestConfig)
    .then((response) => {
      if (response.status === 401) {
        clearSession()
      }
      if (response.status >= 400) {
        throw normalizeError(response.data, 'Network request failed')
      }
      return config.raw ? (response.data as T) : unwrapPayload<T>(response.data)
    })
    .catch((error) => {
      if (isAxiosError(error) && error.response) {
        throw normalizeError(error.response.data, 'Network request failed')
      }
      throw normalizeError(error, 'Network request failed')
    })

  if (method !== 'GET') {
    return requestPromise
  }

  inflightGetRequests.set(requestKey, requestPromise)
  return requestPromise.finally(() => {
    inflightGetRequests.delete(requestKey)
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
