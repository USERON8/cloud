import type { PageResult } from './api'

export interface UserInfo {
  id?: number
  username?: string
  nickname?: string
  avatarUrl?: string
  email?: string
  phone?: string
  userType?: string
}

export interface LoginRequest {
  username: string
  password: string
  userType: 'USER' | 'MERCHANT' | 'ADMIN'
}

export interface RegisterRequest {
  username: string
  password: string
  phone: string
  nickname: string
  userType?: 'USER' | 'MERCHANT' | 'ADMIN'
}

export interface LoginResponse {
  access_token: string
  token_type: string
  expires_in: number
  refresh_token: string
  scope?: string
  userType?: string
  nickname?: string
  user?: UserInfo
}

export interface ProductItem {
  id: number
  shopId?: number
  name: string
  price?: number
  stockQuantity?: number
  categoryId?: number
  brandId?: number
  status?: number
  description?: string
  imageUrl?: string
}

export interface ProductUpsertPayload {
  shopId: number
  name: string
  price: number
  stockQuantity: number
  categoryId: number
  brandId?: number
  status?: number
  description?: string
  imageUrl?: string
}

export interface OrderItem {
  id: number
  orderNo: string
  userId: number
  shopId?: number
  totalAmount?: number
  payAmount?: number
  status?: number
  addressId?: number
  createdAt?: string
}

export interface ProductQuery {
  page?: number
  size?: number
  name?: string
  categoryId?: number
  brandId?: number
  status?: number
}

export interface OrderQuery {
  page?: number
  size?: number
  userId?: number
  shopId?: number
  status?: number
}

export type ProductPage = PageResult<ProductItem>
export type OrderPage = PageResult<OrderItem>
