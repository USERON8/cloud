import http from './http'
import type { UserInfo } from '../types/domain'

export function getCurrentProfile(): Promise<UserInfo> {
  return http.get<UserInfo, UserInfo>('/api/user/profile/current')
}

export function updateCurrentProfile(payload: Partial<UserInfo>): Promise<boolean> {
  return http.put<boolean, boolean>('/api/user/profile/current', payload)
}

export function changeCurrentPassword(payload: { oldPassword: string; newPassword: string }): Promise<boolean> {
  return http.put<boolean, boolean>('/api/user/profile/current/password', payload)
}

export function uploadCurrentAvatar(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<string, string>('/api/user/profile/current/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
