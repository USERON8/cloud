import http from './http'
import type { CartSyncPayload, RemoteCart } from '../types/domain'

export function getCurrentCart(): Promise<RemoteCart> {
  return http.get<RemoteCart, RemoteCart>('/api/users/me/cart', {
    responseType: 'text'
  })
}

export function syncCurrentCart(payload: CartSyncPayload): Promise<RemoteCart> {
  return http.put<RemoteCart, RemoteCart>('/api/users/me/cart/items', payload, {
    responseType: 'text'
  })
}
