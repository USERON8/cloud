import http from './http'
import { getAccessToken } from '../auth/session'
import { resolveApiUrl } from './http'
import type { UserInfo, UserProfileUpdatePayload } from '../types/domain'

export function getCurrentProfile(): Promise<UserInfo> {
  return http.get<UserInfo, UserInfo>('/api/users/me/profile')
}

export function updateCurrentProfile(payload: UserProfileUpdatePayload): Promise<boolean> {
  return http.put<boolean, boolean>('/api/users/me/profile', payload)
}

export function changeCurrentPassword(payload: { oldPassword: string; newPassword: string }): Promise<boolean> {
  return http.put<boolean, boolean>('/api/users/me/password', payload)
}

export function uploadCurrentAvatar(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<string, string>('/api/users/me/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function uploadCurrentAvatarByPath(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const accessToken = getAccessToken()
    uni.uploadFile({
      url: resolveApiUrl('/api/users/me/avatar'),
      filePath,
      name: 'file',
      header: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
      success: (result) => {
        if (result.statusCode && result.statusCode >= 400) {
          reject(new Error('Avatar upload failed'))
          return
        }
        try {
          const payload = JSON.parse(result.data) as {
            code?: number
            message?: string
            data?: string
          }
          if (payload.code !== 200 || typeof payload.data !== 'string') {
            reject(new Error(payload.message || 'Avatar upload failed'))
            return
          }
          resolve(payload.data)
        } catch {
          reject(new Error('Avatar upload failed'))
        }
      },
      fail: (error) => {
        reject(error instanceof Error ? error : new Error('Avatar upload failed'))
      }
    })
  })
}
