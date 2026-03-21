import http from './http'
import type { PageResult } from '../types/api'
import type { AdminInfo, AdminUpsertPayload } from '../types/domain'

export function getAdmins(params: { page?: number; size?: number } = {}): Promise<PageResult<AdminInfo>> {
  return http.get<PageResult<AdminInfo>, PageResult<AdminInfo>>('/api/admin', { params })
}

export function getAdminById(id: number): Promise<AdminInfo> {
  return http.get<AdminInfo, AdminInfo>(`/api/admin/${id}`)
}

export function createAdmin(payload: AdminUpsertPayload): Promise<AdminInfo> {
  return http.post<AdminInfo, AdminInfo>('/api/admin', payload)
}

export function updateAdmin(id: number, payload: AdminUpsertPayload): Promise<boolean> {
  return http.put<boolean, boolean>(`/api/admin/${id}`, payload)
}

export function deleteAdmin(id: number): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/admin/${id}`)
}

export function updateAdminStatus(id: number, status: number): Promise<boolean> {
  return http.patch<boolean, boolean>(`/api/admin/${id}/status`, null, { params: { status } })
}

export function resetAdminPassword(id: number): Promise<string> {
  return http.post<string, string>(`/api/admin/${id}/reset-password`)
}
