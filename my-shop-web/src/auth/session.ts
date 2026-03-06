import { reactive } from 'vue'
import type { OAuthTokenResponse, UserInfo } from '../types/domain'

const SESSION_KEY = 'shop.session'

interface SessionState {
  accessToken: string
  tokenType: string
  expiresAt: number
  scope: string
  user: UserInfo | null
}

interface StoredSession {
  accessToken: string
  tokenType: string
  expiresAt: number
  scope: string
  user: UserInfo | null
}

export const sessionState = reactive<SessionState>({
  accessToken: '',
  tokenType: 'Bearer',
  expiresAt: 0,
  scope: '',
  user: null
})

function persist(): void {
  const payload: StoredSession = {
    accessToken: sessionState.accessToken,
    tokenType: sessionState.tokenType,
    expiresAt: sessionState.expiresAt,
    scope: sessionState.scope,
    user: sessionState.user
  }
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(payload))
}

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padding = normalized.length % 4 === 0 ? '' : '='.repeat(4 - (normalized.length % 4))
  return atob(normalized + padding)
}

function parseJwtClaims(token?: string): Record<string, unknown> {
  if (!token) {
    return {}
  }
  const parts = token.split('.')
  if (parts.length < 2 || !parts[1]) {
    return {}
  }

  try {
    const json = decodeBase64Url(parts[1])
    return JSON.parse(json) as Record<string, unknown>
  } catch {
    return {}
  }
}

function readStringClaim(claims: Record<string, unknown>, key: string): string {
  const value = claims[key]
  return typeof value === 'string' ? value : ''
}

function readNumberClaim(claims: Record<string, unknown>, key: string): number | undefined {
  const value = claims[key]
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (typeof value === 'string' && /^\d+$/.test(value)) {
    return Number(value)
  }
  return undefined
}

function readRoles(claims: Record<string, unknown>): string[] {
  const value = claims.roles
  if (Array.isArray(value)) {
    return value.filter((item): item is string => typeof item === 'string' && item.length > 0)
  }
  if (typeof value === 'string' && value.trim().length > 0) {
    return value.trim().split(/\s+/)
  }
  return []
}

function deriveUserType(roles: string[]): string {
  if (roles.includes('ADMIN')) {
    return 'ADMIN'
  }
  if (roles.includes('MERCHANT')) {
    return 'MERCHANT'
  }
  if (roles.includes('USER')) {
    return 'USER'
  }
  return 'USER'
}

function buildUserInfo(accessClaims: Record<string, unknown>, idClaims: Record<string, unknown>): UserInfo {
  const roles = readRoles(accessClaims)
  const username =
    readStringClaim(accessClaims, 'username') ||
    readStringClaim(idClaims, 'preferred_username') ||
    readStringClaim(idClaims, 'sub') ||
    readStringClaim(accessClaims, 'sub')
  const nickname =
    readStringClaim(accessClaims, 'nickname') ||
    readStringClaim(idClaims, 'nickname') ||
    username

  return {
    id: readNumberClaim(accessClaims, 'user_id'),
    username,
    nickname,
    email: readStringClaim(idClaims, 'email') || undefined,
    userType: deriveUserType(roles),
    roles
  }
}

export function hydrateSessionFromStorage(): void {
  if (sessionState.accessToken) {
    return
  }

  const serialized = sessionStorage.getItem(SESSION_KEY)
  if (!serialized) {
    return
  }

  try {
    const payload = JSON.parse(serialized) as StoredSession
    if (!payload.accessToken || !payload.expiresAt || payload.expiresAt <= Date.now()) {
      sessionStorage.removeItem(SESSION_KEY)
      return
    }

    sessionState.accessToken = payload.accessToken
    sessionState.tokenType = payload.tokenType || 'Bearer'
    sessionState.expiresAt = payload.expiresAt
    sessionState.scope = payload.scope || ''
    sessionState.user = payload.user || null
  } catch {
    sessionStorage.removeItem(SESSION_KEY)
  }
}

export function setSessionFromTokenResponse(payload: OAuthTokenResponse): void {
  const accessClaims = parseJwtClaims(payload.access_token)
  const idClaims = parseJwtClaims(payload.id_token)

  sessionState.accessToken = payload.access_token
  sessionState.tokenType = payload.token_type || 'Bearer'
  sessionState.expiresAt = Date.now() + (payload.expires_in || 0) * 1000
  sessionState.scope = payload.scope || ''
  sessionState.user = buildUserInfo(accessClaims, idClaims)
  persist()
}

export function clearSession(): void {
  sessionState.accessToken = ''
  sessionState.tokenType = 'Bearer'
  sessionState.expiresAt = 0
  sessionState.scope = ''
  sessionState.user = null
  sessionStorage.removeItem(SESSION_KEY)
}

export function patchSessionUser(patch: Partial<UserInfo>): void {
  if (!sessionState.user) {
    sessionState.user = {}
  }
  sessionState.user = {
    ...sessionState.user,
    ...patch
  }
  persist()
}

export function getAccessToken(): string {
  if (sessionState.expiresAt > 0 && sessionState.expiresAt <= Date.now()) {
    clearSession()
    return ''
  }
  return sessionState.accessToken
}

export function isAuthenticated(): boolean {
  return getAccessToken().length > 0
}
