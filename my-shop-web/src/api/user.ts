import http from './http'
import type { UserInfo } from '../types/domain'

export function getCurrentProfile(): Promise<UserInfo> {
  return http.get<UserInfo, UserInfo>('/api/user/profile/current')
}

export function updateCurrentProfile(payload: Partial<UserInfo>): Promise<boolean> {
  return http.put<boolean, boolean>('/api/user/profile/current', payload)
}
