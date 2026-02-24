import { reactive } from 'vue'
import type { LoginResponse, UserInfo } from '../types/domain'

const ACCESS_TOKEN_KEY = 'shop.access_token'
const REFRESH_TOKEN_KEY = 'shop.refresh_token'
const TOKEN_TYPE_KEY = 'shop.token_type'
const EXPIRES_AT_KEY = 'shop.expires_at'
const SCOPE_KEY = 'shop.scope'
const USER_KEY = 'shop.user'

interface SessionState {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresAt: number
  scope: string
  user: UserInfo | null
}

export const sessionState = reactive<SessionState>({
  accessToken: '',
  refreshToken: '',
  tokenType: 'Bearer',
  expiresAt: 0,
  scope: '',
  user: null
})

function persist(): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, sessionState.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, sessionState.refreshToken)
  localStorage.setItem(TOKEN_TYPE_KEY, sessionState.tokenType || 'Bearer')
  localStorage.setItem(EXPIRES_AT_KEY, String(sessionState.expiresAt || 0))
  localStorage.setItem(SCOPE_KEY, sessionState.scope || '')
  localStorage.setItem(USER_KEY, sessionState.user ? JSON.stringify(sessionState.user) : '')
}

export function hydrateSessionFromStorage(): void {
  if (sessionState.accessToken) {
    return
  }
  sessionState.accessToken = localStorage.getItem(ACCESS_TOKEN_KEY) || ''
  sessionState.refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY) || ''
  sessionState.tokenType = localStorage.getItem(TOKEN_TYPE_KEY) || 'Bearer'
  sessionState.expiresAt = Number(localStorage.getItem(EXPIRES_AT_KEY) || 0)
  sessionState.scope = localStorage.getItem(SCOPE_KEY) || ''

  const serializedUser = localStorage.getItem(USER_KEY)
  if (serializedUser) {
    try {
      sessionState.user = JSON.parse(serializedUser) as UserInfo
    } catch {
      sessionState.user = null
    }
  }
}

export function setSessionFromLogin(payload: LoginResponse): void {
  sessionState.accessToken = payload.access_token
  sessionState.refreshToken = payload.refresh_token
  sessionState.tokenType = payload.token_type || 'Bearer'
  sessionState.expiresAt = Date.now() + (payload.expires_in || 0) * 1000
  sessionState.scope = payload.scope || ''
  sessionState.user = payload.user
    ? { ...payload.user, userType: payload.userType || payload.user.userType }
    : payload.nickname || payload.userType
      ? {
          nickname: payload.nickname,
          userType: payload.userType
        }
      : null
  persist()
}

export function clearSession(): void {
  sessionState.accessToken = ''
  sessionState.refreshToken = ''
  sessionState.tokenType = 'Bearer'
  sessionState.expiresAt = 0
  sessionState.scope = ''
  sessionState.user = null
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(TOKEN_TYPE_KEY)
  localStorage.removeItem(EXPIRES_AT_KEY)
  localStorage.removeItem(SCOPE_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getAccessToken(): string {
  return sessionState.accessToken
}

export function getRefreshToken(): string {
  return sessionState.refreshToken
}

export function isAuthenticated(): boolean {
  return sessionState.accessToken.length > 0
}
