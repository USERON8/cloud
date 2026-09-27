import http from './http'
import type { PageResult } from '../types/api'
import type { EntityId, UserAddress, UserAddressRequestPayload } from '../types/domain'

export interface UserAddressPageQuery {
  userId?: EntityId
  current?: number
  size?: number
  receiverName?: string
}

export function listUserAddresses(userId: EntityId): Promise<UserAddress[]> {
  return http.get<UserAddress[], UserAddress[]>(`/api/users/${userId}/addresses`, {
    responseType: 'text'
  })
}

export function getDefaultAddress(userId: EntityId): Promise<UserAddress | null> {
  return http.get<UserAddress | null, UserAddress | null>(`/api/users/${userId}/addresses/default`, {
    responseType: 'text'
  })
}

export function addUserAddress(userId: EntityId, payload: UserAddressRequestPayload): Promise<UserAddress> {
  return http.post<UserAddress, UserAddress>(`/api/users/${userId}/addresses`, payload, {
    responseType: 'text'
  })
}

export function updateUserAddress(addressId: EntityId, payload: UserAddressRequestPayload): Promise<UserAddress> {
  return http.put<UserAddress, UserAddress>(`/api/addresses/${addressId}`, payload, {
    responseType: 'text'
  })
}

export function deleteUserAddress(addressId: EntityId): Promise<boolean> {
  return http.delete<boolean, boolean>(`/api/addresses/${addressId}`)
}

export function pageUserAddresses(payload: UserAddressPageQuery): Promise<PageResult<UserAddress>> {
  return http.get<PageResult<UserAddress>, PageResult<UserAddress>>('/api/addresses', { params: payload })
}

export function deleteUserAddressesBatch(ids: EntityId[]): Promise<boolean> {
  return http.delete<boolean, boolean>('/api/addresses/bulk', { data: ids })
}

export function updateUserAddressesBatch(payload: UserAddressRequestPayload[]): Promise<boolean> {
  return http.patch<boolean, boolean>('/api/addresses/bulk', payload)
}
