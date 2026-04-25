import http from './http'
import type { TokenBlacklistStats } from '../types/domain'

export function getTokenStats(): Promise<Record<string, unknown>> {
  return http.get<Record<string, unknown>, Record<string, unknown>>('/auth/authorizations/statistics')
}

export function getAuthorizationDetails(id: string): Promise<Record<string, unknown>> {
  return http.get<Record<string, unknown>, Record<string, unknown>>(`/auth/authorizations/${id}`)
}

export function revokeAuthorization(id: string): Promise<void> {
  return http.delete<never, void>(`/auth/authorizations/${id}`)
}

export function cleanupExpiredTokens(): Promise<Record<string, unknown>> {
  return http.post<Record<string, unknown>, Record<string, unknown>>('/auth/cleanups/authorizations')
}

export function getStorageStructure(): Promise<Record<string, unknown>> {
  return http.get<Record<string, unknown>, Record<string, unknown>>('/auth/authorizations/storage-structure')
}

export function getBlacklistStats(): Promise<TokenBlacklistStats> {
  return http.get<TokenBlacklistStats, TokenBlacklistStats>('/auth/blacklist-entries/statistics')
}

export function addTokenToBlacklist(tokenValue: string, reason?: string): Promise<void> {
  return http.post<never, void>('/auth/blacklist-entries', null, {
    params: {
      tokenValue,
      reason
    }
  })
}

export function checkBlacklist(tokenValue: string): Promise<Record<string, unknown>> {
  return http.get<Record<string, unknown>, Record<string, unknown>>('/auth/blacklist-entries/check', {
    params: { tokenValue }
  })
}

export function cleanupBlacklist(): Promise<Record<string, unknown>> {
  return http.post<Record<string, unknown>, Record<string, unknown>>('/auth/cleanups/blacklist-entries')
}
