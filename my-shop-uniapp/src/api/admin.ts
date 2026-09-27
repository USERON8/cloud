import http from './http'
import type { PageResult } from '../types/api'
import type { AdminInfo, AdminUpsertPayload, EntityId } from '../types/domain'

export function getAdmins(params: { page?: number; size?: number } = {}): Promise<PageResult<AdminInfo>> {
  return http.get<PageResult<AdminInfo>, PageResult<AdminInfo>>('/api/admins', { params })
}

export function getAdminById(id: EntityId): Promise<AdminInfo> {
  return http.get<AdminInfo, AdminInfo>(`/api/admins/${id}`)
}

export function createAdmin(payload: AdminUpsertPayload): Promise<AdminInfo> {
  return http.post<AdminInfo, AdminInfo>('/api/admins', payload)
}

export function updateAdmin(id: EntityId, payload: AdminUpsertPayload): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/admins/${id}`, payload)
}

export function deleteAdmin(id: EntityId): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/admins/${id}`)
}

export function updateAdminStatus(id: EntityId, status: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/admins/${id}/status`, null, { params: { status } })
}

export function resetAdminPassword(id: EntityId): Promise<string> {
  return http.post<string, string>(`/api/admins/${id}/password-resets`)
}
