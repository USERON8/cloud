import http from './http'
import type { CartSyncPayload, RemoteCart } from '../types/domain'

export function getCurrentCart(): Promise<RemoteCart> {
  return http.get<RemoteCart, RemoteCart>('/api/cart')
}

export function syncCurrentCart(payload: CartSyncPayload): Promise<RemoteCart> {
  return http.post<RemoteCart, RemoteCart>('/api/cart/sync', payload)
}
