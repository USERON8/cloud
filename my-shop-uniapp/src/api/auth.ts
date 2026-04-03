import http, { requestRaw } from './http'
import type { RegisterRequest, RegisterResponse, OAuthTokenResponse, UserInfo } from '../types/domain'
import { getStorage, removeStorage, setStorage } from '../utils/storage'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''
const oauthClientId = import.meta.env.VITE_OAUTH_CLIENT_ID || 'web-client'
const oauthScope = import.meta.env.VITE_OAUTH_SCOPE || 'openid user.read order.write'
const defaultRedirectUri =
  typeof window !== 'undefined' && window.location
    ? `${window.location.origin}/#/pages/oauth/success`
    : ''
const oauthRedirectUri = import.meta.env.VITE_OAUTH_REDIRECT_URI || defaultRedirectUri

const PENDING_AUTH_KEY = 'shop.oauth.pending'

interface PendingAuthorizationRequest {
  redirectPath: string
  state: string
  codeVerifier: string
}

interface AuthorizationRequestPayload {
  clientId: string
  redirectUri: string
  scope: string
  state: string
  codeChallenge: string
  codeChallengeMethod: string
}

function buildApiUrl(path: string): string {
  if (!apiBaseUrl) {
    return path
  }
  return `${apiBaseUrl.replace(/\/+$/, '')}${path}`
}

function randomString(length: number): string {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~'
  const bytes = new Uint8Array(length)
  const cryptoRef = globalThis.crypto
  if (cryptoRef?.getRandomValues) {
    cryptoRef.getRandomValues(bytes)
  }
  return Array.from(bytes, (byte) => alphabet[byte % alphabet.length]).join('')
}

function toBase64Url(bytes: Uint8Array): string {
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })
  if (typeof globalThis.btoa !== 'function') {
    return ''
  }
  return globalThis
    .btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '')
}

async function createAuthorizationRequest(redirectPath: string): Promise<AuthorizationRequestPayload> {
  const codeVerifier = randomString(64)
  const state = randomString(32)
  const digest = await globalThis.crypto.subtle.digest('SHA-256', new TextEncoder().encode(codeVerifier))
  const codeChallenge = toBase64Url(new Uint8Array(digest))

  const pendingAuthorization: PendingAuthorizationRequest = {
    redirectPath,
    state,
    codeVerifier
  }
  setStorage(PENDING_AUTH_KEY, pendingAuthorization)

  return {
    clientId: oauthClientId,
    redirectUri: oauthRedirectUri,
    scope: oauthScope,
    state,
    codeChallenge,
    codeChallengeMethod: 'S256'
  }
}

function readPendingAuthorization(): PendingAuthorizationRequest | null {
  return getStorage<PendingAuthorizationRequest>(PENDING_AUTH_KEY)
}

function clearPendingAuthorization(): void {
  removeStorage(PENDING_AUTH_KEY)
}

export function consumePendingRedirectPath(): string {
  const pending = readPendingAuthorization()
  clearPendingAuthorization()
  return pending?.redirectPath || '/pages/app/home/index'
}

function redirectExternal(target: string): void {
  if (typeof window !== 'undefined' && window.location) {
    window.location.href = target
    return
  }
  const encoded = encodeURIComponent(target)
  uni.navigateTo({ url: `/pages/webview/index?url=${encoded}` })
}

export async function startAuthorization(redirectPath = '/app/home'): Promise<void> {
  const payload = await createAuthorizationRequest(redirectPath)
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: payload.clientId,
    redirect_uri: payload.redirectUri,
    scope: payload.scope,
    state: payload.state,
    code_challenge: payload.codeChallenge,
    code_challenge_method: payload.codeChallengeMethod
  })
  redirectExternal(`${buildApiUrl('/oauth2/authorize')}?${params.toString()}`)
}

export async function startGitHubAuthorization(redirectPath = '/app/home'): Promise<void> {
  const payload = await createAuthorizationRequest(redirectPath)
  const response = await http.get<string, string>('/auth/oauth2/github/login-url', {
    params: {
      clientId: payload.clientId,
      redirectUri: payload.redirectUri,
      scope: payload.scope,
      state: payload.state,
      codeChallenge: payload.codeChallenge,
      codeChallengeMethod: payload.codeChallengeMethod
    }
  })

  const redirectTarget = response.startsWith('http') ? response : buildApiUrl(response)
  redirectExternal(redirectTarget)
}

export async function exchangeAuthorizationCode(code: string, state: string): Promise<OAuthTokenResponse> {
  const pending = readPendingAuthorization()
  if (!pending || pending.state !== state) {
    clearPendingAuthorization()
    throw new Error('OAuth state validation failed')
  }

  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: oauthClientId,
    code,
    redirect_uri: oauthRedirectUri,
    code_verifier: pending.codeVerifier
  })

  const payload = await requestRaw<OAuthTokenResponse>('POST', '/oauth2/token', {
    data: body.toString(),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    skipAuth: true
  })

  if (!payload?.access_token) {
    throw new Error('Token exchange failed')
  }

  return payload
}

export function clearPendingAuthorizationState(): void {
  clearPendingAuthorization()
}

export function register(payload: RegisterRequest): Promise<RegisterResponse> {
  return http.post<RegisterResponse, RegisterResponse>('/auth/users/register', payload)
}

export function logout(): Promise<void> {
  return http.delete<never, void>('/auth/sessions')
}

export function logoutAllSessions(username: string): Promise<string> {
  return http.delete<string, string>(`/auth/users/${username}/sessions`)
}

export function validateToken(): Promise<string> {
  return http.get<string, string>('/auth/tokens/validate')
}

export function getGitHubAuthStatus(): Promise<boolean> {
  return http.get<boolean, boolean>('/auth/oauth2/github/status')
}

export function getGitHubUserInfo(): Promise<UserInfo> {
  return http.get<UserInfo, UserInfo>('/auth/oauth2/github/user-info')
}
