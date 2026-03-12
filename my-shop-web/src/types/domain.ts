import type { PageResult } from './api'

export interface UserInfo {
  id?: number
  username?: string
  nickname?: string
  avatarUrl?: string
  email?: string
  phone?: string
  roles?: string[]
}

export interface OAuthTokenResponse {
  access_token: string
  token_type: string
  expires_in: number
  refresh_token?: string
  scope?: string
  id_token?: string
}

export interface RegisterRequest {
  username: string
  password: string
  phone: string
  nickname: string
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

export interface SearchProductDocument {
  productId?: number
  shopId?: number
  shopName?: string
  productName?: string
  price?: number
  stockQuantity?: number
  categoryId?: number
  brandId?: number
  status?: number
  description?: string
  imageUrl?: string
}

export interface SmartSearchResult {
  documents: SearchProductDocument[]
  total: number
  from: number
  size: number
  aggregations?: Record<string, unknown>
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

export interface CreateOrderPayload {
  shopId: number
  items: Array<{
    productId: number
    quantity: number
    price: number
  }>
}

export interface UserAddress {
  id?: number
  userId?: number
  consignee: string
  phone: string
  province: string
  city: string
  district: string
  street: string
  detailAddress: string
  isDefault: number
  createdAt?: string
  updatedAt?: string
}

export interface UserSummary {
  id: number
  username: string
  phone?: string
  nickname?: string
  avatarUrl?: string
  email?: string
  status?: number
  roles?: string[]
  createdAt?: string
  updatedAt?: string
  lastLoginAt?: string
}

export interface UserUpsertPayload {
  id?: number
  username?: string
  phone?: string
  nickname?: string
  avatarUrl?: string
  email?: string
  status?: number
  password?: string
}

export interface AdminInfo {
  id: number
  username: string
  realName?: string
  phone?: string
  role?: string
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface AdminUpsertPayload {
  username?: string
  password?: string
  phone?: string
  status?: number
  realName?: string
  role?: string
}

export interface MerchantInfo {
  id: number
  username?: string
  merchantName?: string
  email?: string
  phone?: string
  roles?: string[]
  status?: number
  authStatus?: number
  createdAt?: string
  updatedAt?: string
}

export interface MerchantUpsertPayload {
  username?: string
  password?: string
  phone?: string
  status?: number
  merchantName?: string
  email?: string
}

export interface MerchantAuthInfo {
  id?: number
  merchantId?: number
  businessLicenseNumber?: string
  businessLicenseUrl?: string
  idCardFrontUrl?: string
  idCardBackUrl?: string
  contactPhone?: string
  contactAddress?: string
  authStatus?: number
  authRemark?: string
  createdAt?: string
  updatedAt?: string
}

export interface MerchantAuthPayload {
  businessLicenseNumber: string
  businessLicenseUrl: string
  idCardFrontUrl: string
  idCardBackUrl: string
  contactPhone: string
  contactAddress: string
}

export interface CategoryItem {
  id?: number
  parentId?: number
  name: string
  description?: string
  iconUrl?: string
  imageUrl?: string
  sortOrder?: number
  level?: number
  path?: string
  status?: number
  isVisible?: number
  createdAt?: string
  updatedAt?: string
  deleted?: boolean
  children?: CategoryItem[]
}

export interface UserStatisticsOverview {
  totalUsers?: number
  todayNewUsers?: number
  monthNewUsers?: number
  activeUsers?: number
  roleDistribution?: Record<string, number>
  userStatusDistribution?: Record<string, number>
  growthRate?: number
  averageActivity?: number
}

export interface SearchResult<T> {
  list: T[]
  total: number
  page: number
  size: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
  took?: number
  aggregations?: Record<string, unknown>
  highlights?: Record<string, string[]>
}

export interface ShopDocument {
  shopId?: number
  merchantId?: number
  shopName?: string
  avatarUrl?: string
  description?: string
  contactPhone?: string
  address?: string
  status?: number
  productCount?: number
  rating?: number
  reviewCount?: number
  followCount?: number
  createdAt?: string
  updatedAt?: string
  searchWeight?: number
  hotScore?: number
  recommended?: boolean
}

export type ProductPage = PageResult<ProductItem>
export type OrderPage = PageResult<OrderItem>
