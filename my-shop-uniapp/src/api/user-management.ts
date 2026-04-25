import http from './http'
import type { PageResult } from '../types/api'
import type { UserSummary, UserUpsertPayload } from '../types/domain'

export interface UserSearchParams {
  page?: number
  size?: number
  username?: string
  email?: string
  phone?: string
  nickname?: string
  status?: number
  roleCode?: string
}

export function findUserByUsername(username: string): Promise<UserSummary | null> {
  return http
    .get<PageResult<UserSummary>, PageResult<UserSummary>>('/api/admin/users', {
      params: { username, page: 1, size: 1 }
    })
    .then((result) => result.records?.[0] ?? null)
}

export function searchUsers(params: UserSearchParams): Promise<PageResult<UserSummary>> {
  return http.get<PageResult<UserSummary>, PageResult<UserSummary>>('/api/admin/users', { params })
}

export function updateUser(id: number, payload: UserUpsertPayload): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/admin/users/${id}`, payload)
}

export function deleteUser(id: number): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/admin/users/${id}`)
}

export function deleteUsersBatch(ids: number[]): Promise<boolean> {
  return http.delete<boolean, boolean>('/api/admin/users/batch', { data: ids })
}

export function updateUsersBatch(payload: UserUpsertPayload[]): Promise<boolean> {
  return http.put<boolean, boolean>('/api/admin/users/batch', payload)
}

export function updateUserStatusBatch(ids: number[], status: number): Promise<boolean> {
  return http.patch<boolean, boolean>('/api/admin/users/status/batch', null, {
    params: {
      ids: ids.join(','),
      status
    }
  })
}
