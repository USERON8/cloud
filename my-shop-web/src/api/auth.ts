import http from './http'
import type { LoginRequest, LoginResponse, RegisterRequest } from '../types/domain'

export function login(payload: LoginRequest): Promise<LoginResponse> {
  return http.post<LoginResponse, LoginResponse>('/auth/sessions', payload)
}

export function register(payload: RegisterRequest): Promise<LoginResponse> {
  return http.post<LoginResponse, LoginResponse>('/auth/users/register', payload)
}

export function logout(): Promise<void> {
  return http.delete<never, void>('/auth/sessions')
}

export function validateToken(): Promise<string> {
  return http.get<string, string>('/auth/tokens/validate')
}
