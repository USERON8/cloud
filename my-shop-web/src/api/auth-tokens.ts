import http from './http'
import type { TokenBlacklistStats } from '../types/domain'

export function getTokenStats(): Promise<Record<string, unknown>> {
  return http.get<Record<string, unknown>, Record<string, unknown>>('/auth/tokens/stats')
}

export function getAuthorizationDetails(id: string): Promise<Record<string, unknown>> {
  return http.get<Record<string, unknown>, Record<string, unknown>>(`/auth/tokens/authorization/${id}`)
}

export function revokeAuthorization(id: string): Promise<void> {
  return http.delete<never, void>(`/auth/tokens/authorization/${id}`)
}

export function cleanupExpiredTokens(): Promise<Record<string, unknown>> {
  return http.post<Record<string, unknown>, Record<string, unknown>>('/auth/tokens/cleanup')
}

export function getStorageStructure(): Promise<Record<string, unknown>> {
  return http.get<Record<string, unknown>, Record<string, unknown>>('/auth/tokens/storage-structure')
}

export function getBlacklistStats(): Promise<TokenBlacklistStats> {
  return http.get<TokenBlacklistStats, TokenBlacklistStats>('/auth/tokens/blacklist/stats')
}

export function addTokenToBlacklist(tokenValue: string, reason?: string): Promise<void> {
  return http.post<never, void>('/auth/tokens/blacklist/add', null, {
    params: {
      tokenValue,
      reason
    }
  })
}

export function checkBlacklist(tokenValue: string): Promise<Record<string, unknown>> {
  return http.get<Record<string, unknown>, Record<string, unknown>>('/auth/tokens/blacklist/check', {
    params: { tokenValue }
  })
}

export function cleanupBlacklist(): Promise<Record<string, unknown>> {
  return http.post<Record<string, unknown>, Record<string, unknown>>('/auth/tokens/blacklist/cleanup')
}
