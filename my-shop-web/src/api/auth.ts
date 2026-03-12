import http from './http'
import type { RegisterRequest, OAuthTokenResponse, UserInfo } from '../types/domain'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''
const oauthClientId = import.meta.env.VITE_OAUTH_CLIENT_ID || 'web-client'
const oauthScope =
  import.meta.env.VITE_OAUTH_SCOPE || 'openid profile read write user:read user:write order:read order:write'
const oauthRedirectUri =
  import.meta.env.VITE_OAUTH_REDIRECT_URI || `${window.location.origin}/callback`

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
  window.crypto.getRandomValues(bytes)
  return Array.from(bytes, (byte) => alphabet[byte % alphabet.length]).join('')
}

function toBase64Url(bytes: Uint8Array): string {
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })
  return window
    .btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '')
}

async function createAuthorizationRequest(redirectPath: string): Promise<AuthorizationRequestPayload> {
  const codeVerifier = randomString(64)
  const state = randomString(32)
  const digest = await window.crypto.subtle.digest('SHA-256', new TextEncoder().encode(codeVerifier))
  const codeChallenge = toBase64Url(new Uint8Array(digest))

  const pendingAuthorization: PendingAuthorizationRequest = {
    redirectPath,
    state,
    codeVerifier
  }
  sessionStorage.setItem(PENDING_AUTH_KEY, JSON.stringify(pendingAuthorization))

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
  const raw = sessionStorage.getItem(PENDING_AUTH_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as PendingAuthorizationRequest
  } catch {
    sessionStorage.removeItem(PENDING_AUTH_KEY)
    return null
  }
}

function clearPendingAuthorization(): void {
  sessionStorage.removeItem(PENDING_AUTH_KEY)
}

export function consumePendingRedirectPath(): string {
  const pending = readPendingAuthorization()
  clearPendingAuthorization()
  return pending?.redirectPath || '/app/home'
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
  window.location.href = `${buildApiUrl('/oauth2/authorize')}?${params.toString()}`
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
  window.location.href = redirectTarget
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

  const response = await fetch(buildApiUrl('/oauth2/token'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body,
    credentials: 'include'
  })

  const payload = (await response.json()) as Record<string, unknown>
  if (!response.ok) {
    const description =
      (typeof payload.error_description === 'string' && payload.error_description) ||
      (typeof payload.error === 'string' && payload.error) ||
      'Token exchange failed'
    throw new Error(description)
  }

  return payload as unknown as OAuthTokenResponse
}

export function clearPendingAuthorizationState(): void {
  clearPendingAuthorization()
}

export function register(payload: RegisterRequest): Promise<unknown> {
  return http.post<unknown, unknown>('/auth/users/register', payload)
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
