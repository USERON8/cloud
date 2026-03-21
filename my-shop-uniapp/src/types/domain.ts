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
  skuId?: number
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
  searchAfter?: unknown[]
}

export interface OrderItem {
  id: number
  orderNo: string
  userId: number
  subOrderId?: number
  subOrderNo?: string
  merchantId?: number
  afterSaleId?: number
  afterSaleNo?: string
  shopId?: number
  totalAmount?: number
  payAmount?: number
  status?: number
  afterSaleStatus?: string
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
  merchantId?: number
  shopId?: number
  status?: number
}

export interface CreateOrderPayload {
  shopId: number
  spuId: number
  skuId: number
  quantity: number
  price: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
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
  roles?: string[]
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

export interface MerchantAuthUploadResult {
  fileKey: string
  previewUrl: string
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
  searchAfter?: unknown[]
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

export interface ProductDocument {
  productId?: number
  shopId?: number
  shopName?: string
  productName?: string
  price?: number
  stockQuantity?: number
  categoryId?: number
  categoryName?: string
  brandId?: number
  brandName?: string
  status?: number
  description?: string
  imageUrl?: string
  detailImages?: string
  tags?: string
  weight?: number
  sku?: string
  salesCount?: number
  sortOrder?: number
  rating?: number
  reviewCount?: number
  createdAt?: string
  updatedAt?: string
  searchWeight?: number
  hotScore?: number
  recommended?: boolean
  isNew?: boolean
  isHot?: boolean
  merchantId?: number
  merchantName?: string
  remark?: string
}

export interface PaymentOrderInfo {
  id?: number
  paymentNo?: string
  mainOrderNo?: string
  subOrderNo?: string
  userId?: number
  amount?: number
  channel?: string
  status?: string
  providerTxnNo?: string
  idempotencyKey?: string
  paidAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface PaymentRefundInfo {
  id?: number
  refundNo?: string
  paymentNo?: string
  afterSaleNo?: string
  refundAmount?: number
  status?: string
  reason?: string
  idempotencyKey?: string
  refundedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface PaymentOrderCommand {
  paymentNo: string
  mainOrderNo: string
  subOrderNo: string
  userId: number
  amount: number
  channel: string
  idempotencyKey: string
}

export interface PaymentRefundCommand {
  refundNo: string
  paymentNo: string
  afterSaleNo: string
  refundAmount: number
  reason: string
  idempotencyKey: string
}

export interface AfterSaleInfo {
  id?: number
  afterSaleNo?: string
  mainOrderId: number
  subOrderId: number
  userId?: number
  merchantId?: number
  afterSaleType: string
  status?: string
  reason: string
  description?: string
  applyAmount: number
  approvedAmount?: number
  returnLogisticsCompany?: string
  returnLogisticsNo?: string
  refundChannel?: string
  refundedAt?: string
  closedAt?: string
  closeReason?: string
  createdAt?: string
  updatedAt?: string
}

export interface PaymentCallbackCommand {
  paymentNo: string
  callbackNo: string
  callbackStatus: string
  providerTxnNo?: string
  idempotencyKey: string
  payload?: string
}

export interface StockLedger {
  id?: number
  skuId?: number
  onHandQty?: number
  reservedQty?: number
  salableQty?: number
  alertThreshold?: number
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface StockOperatePayload {
  subOrderNo: string
  skuId: number
  quantity: number
  reason?: string
}

export type ProductPage = PageResult<ProductItem>
export type OrderPage = PageResult<OrderItem>

export interface ThreadPoolInfo {
  name: string
  corePoolSize: number
  maxPoolSize: number
  activeCount: number
  poolSize: number
  queueSize: number
  completedTaskCount: number
  taskCount: number
  queueRemainingCapacity: number
}

export interface TokenBlacklistStats {
  totalBlacklisted: number
  activeBlacklisted: number
  lastUpdated: string
}

export interface ProductSearchRequest {
  keyword?: string
  shopId?: number
  shopName?: string
  categoryId?: number
  categoryName?: string
  brandId?: number
  brandName?: string
  minPrice?: number
  maxPrice?: number
  status?: number
  stockStatus?: number
  recommended?: boolean
  isNew?: boolean
  isHot?: boolean
  tags?: string[]
  minSalesCount?: number
  minRating?: number
  page?: number
  size?: number
  sortBy?: string
  sortOrder?: string
  highlight?: boolean
  includeAggregations?: boolean
}

export interface ProductFilterRequest {
  keyword?: string
  categoryId?: number
  brandId?: number
  shopId?: number
  minPrice?: number
  maxPrice?: number
  minSalesCount?: number
  recommended?: boolean
  isNew?: boolean
  isHot?: boolean
  sortBy?: string
  sortOrder?: string
  page?: number
  size?: number
}

export interface SpringPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  numberOfElements?: number
  first?: boolean
  last?: boolean
  empty?: boolean
  sort?: unknown
}

export interface SpuDto {
  spuId?: number
  spuName: string
  subtitle?: string
  categoryId: number
  brandId?: number
  merchantId: number
  status?: number
  description?: string
  mainImage?: string
}

export interface SkuDto {
  skuId?: number
  skuCode: string
  skuName: string
  specJson?: string
  salePrice: number
  marketPrice?: number
  costPrice?: number
  status?: number
  imageUrl?: string
}

export interface SpuCreateRequest {
  spu: SpuDto
  skus: SkuDto[]
}

export interface SkuDetail {
  skuId?: number
  spuId?: number
  skuCode?: string
  skuName?: string
  specJson?: string
  salePrice?: number
  marketPrice?: number
  costPrice?: number
  status?: number
  imageUrl?: string
  createdAt?: string
  updatedAt?: string
}

export interface SpuDetail {
  spuId?: number
  spuName?: string
  subtitle?: string
  categoryId?: number
  brandId?: number
  merchantId?: number
  status?: number
  description?: string
  mainImage?: string
  createdAt?: string
  updatedAt?: string
  skus?: SkuDetail[]
}
