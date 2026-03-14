import http from './http'
import type { PageResult } from '../types/api'
import type { UserAddress } from '../types/domain'

export interface UserAddressPageQuery {
  userId?: number
  current?: number
  size?: number
  consignee?: string
}

export function listUserAddresses(userId: number): Promise<UserAddress[]> {
  return http.get<UserAddress[], UserAddress[]>(`/api/user/address/list/${userId}`)
}

export function getDefaultAddress(userId: number): Promise<UserAddress | null> {
  return http.get<UserAddress | null, UserAddress | null>(`/api/user/address/default/${userId}`)
}

export function addUserAddress(userId: number, payload: UserAddress): Promise<UserAddress> {
  return http.post<UserAddress, UserAddress>(`/api/user/address/add/${userId}`, payload)
}

export function updateUserAddress(addressId: number, payload: UserAddress): Promise<UserAddress> {
  return http.put<UserAddress, UserAddress>(`/api/user/address/update/${addressId}`, payload)
}

export function deleteUserAddress(addressId: number): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/user/address/delete/${addressId}`)
}

export function pageUserAddresses(payload: UserAddressPageQuery): Promise<PageResult<UserAddress>> {
  return http.post<PageResult<UserAddress>, PageResult<UserAddress>>('/api/user/address/page', payload)
}

export function deleteUserAddressesBatch(ids: number[]): Promise<boolean> {
  return http.delete<boolean, boolean>('/api/user/address/deleteBatch', { data: ids })
}

export function updateUserAddressesBatch(payload: UserAddress[]): Promise<boolean> {
  return http.put<boolean, boolean>('/api/user/address/updateBatch', payload)
}
