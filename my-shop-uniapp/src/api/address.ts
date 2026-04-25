import http from './http'
import type { PageResult } from '../types/api'
import type { UserAddress, UserAddressRequestPayload } from '../types/domain'

export interface UserAddressPageQuery {
  userId?: number | string
  current?: number
  size?: number
  receiverName?: string
}

export function listUserAddresses(userId: number | string): Promise<UserAddress[]> {
  return http.get<UserAddress[], UserAddress[]>(`/api/users/${userId}/addresses`, {
    responseType: 'text'
  })
}

export function getDefaultAddress(userId: number | string): Promise<UserAddress | null> {
  return http.get<UserAddress | null, UserAddress | null>(`/api/users/${userId}/addresses/default`, {
    responseType: 'text'
  })
}

export function addUserAddress(userId: number | string, payload: UserAddressRequestPayload): Promise<UserAddress> {
  return http.post<UserAddress, UserAddress>(`/api/users/${userId}/addresses`, payload, {
    responseType: 'text'
  })
}

export function updateUserAddress(addressId: number | string, payload: UserAddressRequestPayload): Promise<UserAddress> {
  return http.put<UserAddress, UserAddress>(`/api/addresses/${addressId}`, payload, {
    responseType: 'text'
  })
}

export function deleteUserAddress(addressId: number | string): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/addresses/${addressId}`)
}

export function pageUserAddresses(payload: UserAddressPageQuery): Promise<PageResult<UserAddress>> {
  return http.get<PageResult<UserAddress>, PageResult<UserAddress>>('/api/addresses', { params: payload })
}

export function deleteUserAddressesBatch(ids: number[]): Promise<boolean> {
  return http.delete<boolean, boolean>('/api/addresses/bulk', { data: ids })
}

export function updateUserAddressesBatch(payload: UserAddressRequestPayload[]): Promise<boolean> {
  return http.patch<boolean, boolean>('/api/addresses/bulk', payload)
}
