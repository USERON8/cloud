import { reactive } from 'vue'
import type { LoginResponse, UserInfo } from '../types/domain'

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
  localStorage.setItem(USER_KEY, sessionState.user ? JSON.stringify(sessionState.user) : '')
}

export function hydrateSessionFromStorage(): void {
  if (sessionState.user) {
    return
  }

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
  localStorage.removeItem(USER_KEY)
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
  return sessionState.accessToken
}

export function isAuthenticated(): boolean {
  return sessionState.accessToken.length > 0
}
