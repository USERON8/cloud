import { defineStore } from 'pinia'
import type { OAuthTokenResponse, UserInfo } from '../types/domain'
import { pinia } from '../stores/pinia'
import { getStorage, removeStorage, setStorage } from '../utils/storage'

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

type SessionChangeListener = (user: UserInfo | null) => void

const sessionChangeListeners = new Set<SessionChangeListener>()

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padding = normalized.length % 4 === 0 ? '' : '='.repeat(4 - (normalized.length % 4))
  const input = normalized + padding
  if (typeof globalThis.atob === 'function') {
    return globalThis.atob(input)
  }
  return ''
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
    roles
  }
}

export const useSessionStore = defineStore('session', {
  state: (): SessionState => ({
    accessToken: '',
    tokenType: 'Bearer',
    expiresAt: 0,
    scope: '',
    user: null
  }),
  actions: {
    persist(): void {
      const payload: StoredSession = {
        accessToken: this.accessToken,
        tokenType: this.tokenType,
        expiresAt: this.expiresAt,
        scope: this.scope,
        user: this.user
      }
      setStorage(SESSION_KEY, payload)
    },
    notifySessionChange(): void {
      sessionChangeListeners.forEach((listener) => {
        listener(this.user)
      })
    },
    hydrateFromStorage(): void {
      if (this.accessToken) {
        return
      }

      const payload = getStorage<StoredSession>(SESSION_KEY)
      if (!payload) {
        return
      }

      if (!payload.accessToken || !payload.expiresAt || payload.expiresAt <= Date.now()) {
        removeStorage(SESSION_KEY)
        return
      }

      this.accessToken = payload.accessToken
      this.tokenType = payload.tokenType || 'Bearer'
      this.expiresAt = payload.expiresAt
      this.scope = payload.scope || ''
      this.user = payload.user || null
      this.notifySessionChange()
    },
    setSessionFromTokenResponse(payload: OAuthTokenResponse): void {
      const accessClaims = parseJwtClaims(payload.access_token)
      const idClaims = parseJwtClaims(payload.id_token)

      this.accessToken = payload.access_token
      this.tokenType = payload.token_type || 'Bearer'
      this.expiresAt = Date.now() + (payload.expires_in || 0) * 1000
      this.scope = payload.scope || ''
      this.user = buildUserInfo(accessClaims, idClaims)
      this.persist()
      this.notifySessionChange()
    },
    clear(): void {
      this.accessToken = ''
      this.tokenType = 'Bearer'
      this.expiresAt = 0
      this.scope = ''
      this.user = null
      removeStorage(SESSION_KEY)
      this.notifySessionChange()
    },
    patchUser(patch: Partial<UserInfo>): void {
      this.user = {
        ...(this.user || {}),
        ...patch
      }
      this.persist()
      this.notifySessionChange()
    },
    readAccessToken(): string {
      if (this.expiresAt > 0 && this.expiresAt <= Date.now()) {
        this.clear()
        return ''
      }
      return this.accessToken
    }
  }
})

export const sessionState = useSessionStore(pinia)

export function hydrateSessionFromStorage(): void {
  sessionState.hydrateFromStorage()
}

export function setSessionFromTokenResponse(payload: OAuthTokenResponse): void {
  sessionState.setSessionFromTokenResponse(payload)
}

export function clearSession(): void {
  sessionState.clear()
}

export function patchSessionUser(patch: Partial<UserInfo>): void {
  sessionState.patchUser(patch)
}

export function getAccessToken(): string {
  return sessionState.readAccessToken()
}

export function isAuthenticated(): boolean {
  return getAccessToken().length > 0
}

export function subscribeSessionChange(listener: SessionChangeListener): () => void {
  sessionChangeListeners.add(listener)
  return () => {
    sessionChangeListeners.delete(listener)
  }
}
