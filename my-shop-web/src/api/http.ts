import axios, { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import {
  clearSession,
  getAccessToken,
  setSessionFromLogin
} from '../auth/session'
import { BusinessError, SUCCESS_CODE, type ResultEnvelope } from '../types/api'
import type { LoginResponse } from '../types/domain'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''
const apiTimeout = Number(import.meta.env.VITE_API_TIMEOUT || 10000)

const http = axios.create({
  baseURL: apiBaseUrl,
  timeout: apiTimeout,
  withCredentials: true
})

const refreshClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: apiTimeout,
  withCredentials: true
})

let refreshPromise: Promise<string | null> | null = null

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

function attachAuthHeader(config: InternalAxiosRequestConfig, token: string): void {
  const headers = config.headers instanceof AxiosHeaders ? config.headers : AxiosHeaders.from(config.headers)
  headers.set('Authorization', `Bearer ${token}`)
  config.headers = headers
}

function shouldSkipRefresh(url?: string): boolean {
  if (!url) {
    return false
  }
  return url.includes('/auth/sessions') || url.includes('/auth/tokens/refresh')
}

async function performTokenRefresh(): Promise<string | null> {
  const response = await refreshClient.post<unknown>('/auth/tokens/refresh')
  const loginPayload = unwrapPayload<LoginResponse>(response.data)
  if (!loginPayload?.access_token) {
    return null
  }

  setSessionFromLogin(loginPayload)
  return loginPayload.access_token
}

async function getOrCreateRefreshPromise(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = performTokenRefresh()
      .catch(() => null)
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

export async function ensureAuthenticatedSession(): Promise<boolean> {
  if (getAccessToken()) {
    return true
  }
  const token = await getOrCreateRefreshPromise()
  return Boolean(token)
}

function normalizeError(error: unknown): Error {
  if (error instanceof BusinessError) {
    return error
  }

  if (axios.isAxiosError(error)) {
    const payload = error.response?.data
    if (isResultEnvelope(payload)) {
      return new BusinessError(payload.message || 'Request failed', payload.code)
    }
    if (typeof payload === 'string' && payload.trim().length > 0) {
      return new Error(payload)
    }
    return new Error(error.message || 'Network request failed')
  }

  if (error instanceof Error) {
    return error
  }
  return new Error('Unexpected request error')
}

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    attachAuthHeader(config, token)
  }
  return config
})

http.interceptors.response.use(
  (response) => unwrapPayload(response.data),
  async (error: AxiosError) => {
    const requestConfig = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    const status = error.response?.status

    if (
      requestConfig &&
      status === 401 &&
      !requestConfig._retry &&
      !shouldSkipRefresh(requestConfig.url)
    ) {
      requestConfig._retry = true
      const token = await getOrCreateRefreshPromise()
      if (token) {
        attachAuthHeader(requestConfig, token)
        return http(requestConfig)
      }
      clearSession()
    }

    return Promise.reject(normalizeError(error))
  }
)

export default http
