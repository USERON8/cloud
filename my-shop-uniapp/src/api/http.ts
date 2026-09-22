import axios, { AxiosHeaders, type AxiosRequestConfig, type AxiosResponse, isAxiosError } from 'axios'
import { clearSession, getAccessToken } from '../auth/session'
import { resolveApiErrorCategory } from '../platform/http/error-category'
import { reportHttpFailure } from '../platform/http/failure-events'
import { BusinessError, SUCCESS_CODE, type ResultEnvelope } from '../types/api'
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

function unwrapPayload<T>(payload: unknown, httpStatus?: number): T {
  if (!isResultEnvelope(payload)) {
    return payload as T
  }

  if (payload.code !== SUCCESS_CODE) {
    throw new BusinessError(payload.message || 'Request failed', payload.code, {
      httpStatus,
      traceId: payload.traceId,
      category: resolveApiErrorCategory(payload.code, httpStatus)
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
      category: resolveApiErrorCategory(payload.code, httpStatus)
    })
  }
  if (typeof payload === 'string' && payload.trim().length > 0) {
    if (httpStatus) {
      return new BusinessError(payload, httpStatus, {
        httpStatus,
        category: resolveApiErrorCategory(httpStatus, httpStatus)
      })
    }
    return new Error(payload)
  }
  if (httpStatus) {
    return new BusinessError(fallbackMessage, httpStatus, {
      httpStatus,
      category: resolveApiErrorCategory(httpStatus, httpStatus)
    })
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
          const error = normalizeError(responseData, 'Network request failed', response.status)
          reportHttpFailure({ kind: 'response', error, status: response.status })
          throw error
        }
      return config.raw ? (response.data as T) : unwrapPayload<T>(responseData, response.status)
    })
    .catch((error) => {
      if (error instanceof BusinessError) {
        throw error
      }
      if (isAxiosError(error) && error.response) {
        const normalizedError = normalizeError(
          normalizeResponseData(error.response.data, config.responseType),
          'Network request failed',
          error.response.status
        )
        reportHttpFailure({
          kind: 'response',
          error: normalizedError,
          cause: error,
          status: error.response.status
        })
        throw normalizedError
      }
      const normalizedError = normalizeError(error, 'Network request failed')
      reportHttpFailure({ kind: 'network', error: normalizedError, cause: error })
      throw normalizedError
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
