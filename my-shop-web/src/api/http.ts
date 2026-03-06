import axios, { AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { clearSession, getAccessToken } from '../auth/session'
import { BusinessError, SUCCESS_CODE, type ResultEnvelope } from '../types/api'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''
const apiTimeout = Number(import.meta.env.VITE_API_TIMEOUT || 10000)

const http = axios.create({
  baseURL: apiBaseUrl,
  timeout: apiTimeout,
  withCredentials: true
})

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
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      clearSession()
    }
    return Promise.reject(normalizeError(error))
  }
)

export default http