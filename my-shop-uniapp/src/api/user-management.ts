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
  return http.get<UserSummary | null, UserSummary | null>('/api/query/users', { params: { username } })
}

export function searchUsers(params: UserSearchParams): Promise<PageResult<UserSummary>> {
  return http.get<PageResult<UserSummary>, PageResult<UserSummary>>('/api/query/users/search', { params })
}

export function updateUser(id: number, payload: UserUpsertPayload): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/manage/users/${id}`, payload)
}

export function deleteUser(id: number): Promise<boolean> {
  return http.post<boolean, boolean>('/api/manage/users/delete', id)
}

export function deleteUsersBatch(ids: number[]): Promise<boolean> {
  return http.post<boolean, boolean>('/api/manage/users/deleteBatch', ids)
}

export function updateUsersBatch(payload: UserUpsertPayload[]): Promise<boolean> {
  return http.post<boolean, boolean>('/api/manage/users/updateBatch', payload)
}

export function updateUserStatusBatch(ids: number[], status: number): Promise<boolean> {
  return http.post<boolean, boolean>('/api/manage/users/updateStatusBatch', null, {
    params: {
      ids: ids.join(','),
      status
    }
  })
}
