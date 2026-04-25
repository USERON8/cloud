import axios, { AxiosHeaders, type AxiosRequestConfig, type AxiosResponse, isAxiosError } from 'axios'
import { clearSession, getAccessToken } from '../auth/session'
import { Routes } from '../router/routes'
import {
  BusinessError,
  SUCCESS_CODE,
  type ApiErrorCategory,
  type ResultEnvelope
} from '../types/api'
import { buildApiUrl } from './runtime-base'

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export interface RequestConfig {
  params?: object
  data?: unknown
  headers?: Record<string, string>
  raw?: boolean
  skipAuth?: boolean
  responseType?: 'json' | 'text'
}

interface InternalRequestConfig extends AxiosRequestConfig {
  raw?: boolean
  skipAuth?: boolean
  responseType?: 'json' | 'text'
}

const apiTimeout = Number(import.meta.env.VITE_API_TIMEOUT || 10000)
const inflightGetRequests = new Map<string, Promise<unknown>>()
let lastErrorPageAt = 0

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

function parseJsonTextWithLongIntegers(payload: string): unknown {
  const normalized = payload.replace(/([:\[,]\s*)(-?\d{16,})(?=\s*[,}\]])/g, '$1"$2"')
  return JSON.parse(normalized)
}

function normalizeResponseData(data: unknown, responseType?: 'json' | 'text'): unknown {
  if (responseType !== 'text' || typeof data !== 'string') {
    return data
  }
  const trimmed = data.trim()
  if (!trimmed) {
    return data
  }
  if (
    (trimmed.startsWith('{') && trimmed.endsWith('}')) ||
    (trimmed.startsWith('[') && trimmed.endsWith(']'))
  ) {
    try {
      return parseJsonTextWithLongIntegers(trimmed)
    } catch {
      return data
    }
  }
  return data
}

function isResultEnvelope(payload: unknown): payload is ResultEnvelope<unknown> {
  if (typeof payload !== 'object' || payload === null) {
    return false
  }
  const candidate = payload as Record<string, unknown>
  return typeof candidate.code === 'number' && 'data' in candidate
}

function resolveErrorCategory(code: number, httpStatus?: number): ApiErrorCategory {
  if (httpStatus === 401 || (code >= 17011 && code <= 17055)) {
    return 'auth'
  }
  if (httpStatus === 403 || code === 2001 || code === 2002) {
    return 'permission'
  }
  if (httpStatus === 404 || code === 4001 || code === 7004 || code === 9001 || code === 10001 || code === 11001 || code === 12001 || code === 13001) {
    return 'notFound'
  }
  if (httpStatus === 409 || code === 4002 || code === 5001 || code === 5002 || code === 7002 || code === 9008) {
    return 'conflict'
  }
  if (httpStatus === 429 || code === 429 || code === 18003) {
    return 'rateLimit'
  }
  if ((code >= 18001 && code <= 18004) || httpStatus === 503 || httpStatus === 504) {
    return 'remote'
  }
  if (
    (httpStatus && httpStatus >= 500) ||
    (code >= 1001 && code <= 1004) ||
    code === 500 ||
    code === 7001 ||
    code === 14002
  ) {
    return 'system'
  }
  if (httpStatus === 400 || code === 400 || code === 501 || (code >= 3001 && code <= 3003) || code === 8007) {
    return 'validation'
  }
  return 'business'
}

function unwrapPayload<T>(payload: unknown, httpStatus?: number): T {
  if (!isResultEnvelope(payload)) {
    return payload as T
  }

  if (payload.code !== SUCCESS_CODE) {
    throw new BusinessError(payload.message || 'Request failed', payload.code, {
      httpStatus,
      traceId: payload.traceId,
      category: resolveErrorCategory(payload.code, httpStatus)
    })
  }

  return payload.data as T
}

function normalizeError(payload: unknown, fallbackMessage: string, httpStatus?: number): Error {
  if (payload instanceof BusinessError) {
    return payload
  }
  if (payload instanceof Error) {
    return payload
  }
  if (isResultEnvelope(payload)) {
    return new BusinessError(payload.message || fallbackMessage, payload.code, {
      httpStatus,
      traceId: payload.traceId,
      category: resolveErrorCategory(payload.code, httpStatus)
    })
  }
  if (typeof payload === 'string' && payload.trim().length > 0) {
    if (httpStatus) {
      return new BusinessError(payload, httpStatus, {
        httpStatus,
        category: resolveErrorCategory(httpStatus, httpStatus)
      })
    }
    return new Error(payload)
  }
  if (httpStatus) {
    return new BusinessError(fallbackMessage, httpStatus, {
      httpStatus,
      category: resolveErrorCategory(httpStatus, httpStatus)
    })
  }
  return new Error(fallbackMessage)
}

function currentRoutePath(): string {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  return current?.route ? `/${current.route}` : ''
}

function normalizeErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }
  if (typeof error === 'object' && error !== null && 'errMsg' in error) {
    return String((error as { errMsg?: unknown }).errMsg || '')
  }
  if (typeof error === 'string') {
    return error
  }
  return ''
}

function openErrorPage(
  kind: 'network' | 'timeout' | 'server' | 'not-found',
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

function handlePageLevelRequestFailure(status: number, payload: unknown): void {
  const error = normalizeError(payload, 'Network request failed', status)
  if (error instanceof BusinessError) {
    if (error.category === 'remote') {
      openErrorPage(status === 504 || error.code === 18002 ? 'timeout' : 'server', {
        status,
        message: error.message
      })
    }
    if (error.category === 'system') {
      openErrorPage('server', {
        status,
        message: error.message
      })
    }
    return
  }

  if (status >= 500) {
    openErrorPage('server', {
      status,
      message: error.message
    })
  }
}

function handleNetworkFailure(error: unknown): void {
  const message = normalizeErrorMessage(error)
  const normalized = message.toLowerCase()
  openErrorPage(normalized.includes('timeout') ? 'timeout' : 'network', {
    message: message || 'Please check the network connection and try again'
  })
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
        dataType: (config as InternalRequestConfig).responseType === 'text' ? 'text' : 'json',
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
      skipAuth: config.skipAuth,
      responseType: config.responseType
    } as InternalRequestConfig)
    .then((response) => {
      const responseData = config.raw
        ? response.data
        : normalizeResponseData(response.data, config.responseType)
        if (response.status === 401) {
          clearSession()
        }
        if (response.status >= 400) {
          handlePageLevelRequestFailure(response.status, responseData)
          throw normalizeError(responseData, 'Network request failed', response.status)
        }
      return config.raw ? (response.data as T) : unwrapPayload<T>(responseData, response.status)
    })
    .catch((error) => {
      if (error instanceof BusinessError) {
        throw error
      }
      if (isAxiosError(error) && error.response) {
        handlePageLevelRequestFailure(error.response.status, error.response.data)
        throw normalizeError(
          normalizeResponseData(error.response.data, config.responseType),
          'Network request failed',
          error.response.status
        )
      }
      handleNetworkFailure(error)
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
