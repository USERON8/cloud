import type { PageResult } from './api'

export type EntityId = string

export interface UserInfo {
  id?: EntityId
  username?: string
  nickname?: string
  avatarUrl?: string
  email?: string
  phone?: string
  status?: number
  roles?: string[]
  createdAt?: string
  updatedAt?: string
  deleted?: number
}

/**
 * User profile update payload mapped to UserProfileUpdateDTO.
 */
export interface UserProfileUpdatePayload {
  nickname?: string
  avatarUrl?: string
  email?: string
  phone?: string
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

export interface RegisterResponse {
  id: EntityId
  username: string
  phone: string
  nickname: string
  roles: string[]
}

export interface ProductItem {
  id: EntityId
  skuId?: EntityId
  shopId?: EntityId
  name: string
  price?: number
  stockQuantity?: number
  categoryId?: EntityId
  brandId?: EntityId
  status?: number
  description?: string
  imageUrl?: string
}

export interface SearchProductDocument {
  productId?: EntityId
  shopId?: EntityId
  shopName?: string
  productName?: string
  categoryName?: string
  brandName?: string
  price?: number
  stockQuantity?: number
  categoryId?: EntityId
  brandId?: EntityId
  status?: number
  description?: string
  imageUrl?: string
  detailImages?: string
  tags?: string[]
  sku?: string
  salesCount?: number
  reviewCount?: number
  createdAt?: string
  updatedAt?: string
  hotScore?: number
  searchWeight?: number
  recommended?: boolean
  isNew?: boolean
  isHot?: boolean
  merchantId?: EntityId
  merchantName?: string
  remark?: string
}

export interface SmartSearchResult {
  documents: SearchProductDocument[]
  total: number
  from: number
  size: number
  aggregations?: Record<string, unknown>
  searchAfter?: unknown[]
}

export interface OrderSummaryDTO {
  id?: EntityId
  orderNo?: string
  userId?: EntityId
  subOrders?: OrderSubSummaryDTO[]
  subOrderId?: EntityId
  subOrderNo?: string
  merchantId?: EntityId
  afterSaleId?: EntityId
  afterSaleNo?: string
  afterSaleType?: string
  refundNo?: string
  totalAmount?: number
  payAmount?: number
  status?: number
  orderStatusRaw?: string
  afterSaleStatus?: string
  createdAt?: string
  items?: OrderSummaryItem[]
}

export interface OrderSubSummaryDTO {
  subOrderId?: EntityId
  subOrderNo?: string
  merchantId?: EntityId
  afterSaleId?: EntityId
  afterSaleNo?: string
  afterSaleType?: string
  refundNo?: string
  payAmount?: number
  status?: number
  orderStatusRaw?: string
  afterSaleStatus?: string
}

export interface OrderSummaryItem {
  id?: EntityId
  subOrderId?: EntityId
  spuId?: EntityId
  skuId?: EntityId
  skuCode?: string
  skuName?: string
  quantity?: number
  unitPrice?: number
  totalPrice?: number
  skuSnapshot?: Record<string, unknown>
  latestProduct?: LatestOrderProduct | null
}

export interface LatestOrderProduct {
  spuId?: EntityId
  skuId?: EntityId
  spuName?: string
  skuCode?: string
  skuName?: string
  specJson?: string
  salePrice?: number
  marketPrice?: number
  imageUrl?: string
  imageFile?: string
  status?: number
  brandName?: string
  categoryName?: string
  merchantId?: EntityId
  shopName?: string
}

export interface OrderItem {
  id: EntityId
  orderNo: string
  userId: EntityId
  subOrderId?: EntityId
  subOrderNo?: string
  merchantId?: EntityId
  afterSaleId?: EntityId
  afterSaleNo?: string
  afterSaleType?: string
  refundNo?: string
  shopId?: EntityId
  totalAmount?: number
  payAmount?: number
  status?: number
  afterSaleStatus?: string
  addressId?: EntityId
  createdAt?: string
}

export interface ProductQuery {
  page?: number
  size?: number
  name?: string
  categoryId?: EntityId
  brandId?: EntityId
  merchantId?: EntityId
  status?: number
}

export interface OrderQuery {
  page?: number
  size?: number
  userId?: EntityId
  merchantId?: EntityId
  status?: number
}

export interface CreateOrderPayload {
  spuId: EntityId
  skuId: EntityId
  quantity: number
  price: number
  clientOrderId?: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
}

export interface CreateCartOrderPayload {
  cartId: EntityId
  clientOrderId?: string
  receiverName: string
  receiverPhone: string
  receiverAddress: string
}

export interface RemoteCartItem {
  id?: EntityId
  cartId?: EntityId
  spuId: EntityId
  skuId: EntityId
  skuName: string
  unitPrice: number
  quantity: number
  selected?: number
  checkedOut?: number
  shopId?: EntityId
  productName?: string
}

export interface RemoteCart {
  id?: EntityId
  cartNo?: string
  userId?: EntityId
  cartStatus?: string
  selectedCount?: number
  totalAmount?: number
  items?: RemoteCartItem[]
}

export interface CartSyncItemPayload {
  spuId: EntityId
  skuId: EntityId
  skuName: string
  unitPrice: number
  quantity: number
  selected?: number
  shopId?: EntityId
}

export interface CartSyncPayload {
  items: CartSyncItemPayload[]
}

export interface UserAddress {
  id?: EntityId
  userId?: EntityId
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  street: string
  detailAddress: string
  isDefault: number
  addressTag?: string
  country?: string
  postalCode?: string
  longitude?: number
  latitude?: number
  createdAt?: string
  updatedAt?: string
  deleted?: number
}

/**
 * User address create or update payload mapped to UserAddressRequestDTO.
 */
export interface UserAddressRequestPayload {
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  street: string
  detailAddress: string
  isDefault: number
  addressTag?: string
  country?: string
  postalCode?: string
  longitude?: number
  latitude?: number
}

export interface UserSummary {
  id: EntityId
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
  id?: EntityId
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
  id: EntityId
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
  id: EntityId
  ownerUserId?: EntityId
  username?: string
  merchantName?: string
  email?: string
  phone?: string
  roles?: string[]
  status?: number
  authStatus?: number
  auditStatus?: number
  createdAt?: string
  updatedAt?: string
  deleted?: number
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
  id?: EntityId
  merchantId?: EntityId
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
  id?: EntityId
  parentId?: EntityId
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
  shopId?: EntityId
  merchantId?: EntityId
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
  productId?: EntityId
  shopId?: EntityId
  shopName?: string
  productName?: string
  price?: number
  stockQuantity?: number
  categoryId?: EntityId
  categoryName?: string
  brandId?: EntityId
  brandName?: string
  status?: number
  description?: string
  imageUrl?: string
  detailImages?: string
  tags?: string[]
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
  merchantId?: EntityId
  merchantName?: string
  remark?: string
}

export interface PaymentOrderInfo {
  id?: EntityId
  paymentNo?: string
  mainOrderNo?: string
  subOrderNo?: string
  userId?: EntityId
  amount?: number
  channel?: string
  status?: string
  providerTxnNo?: string
  idempotencyKey?: string
  paidAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface PaymentStatusInfo {
  paymentNo?: string
  status?: string
}

export interface PaymentCheckoutSession {
  paymentNo?: string
  checkoutPath?: string
  expiresInSeconds?: number
}

export interface PaymentRefundInfo {
  id?: EntityId
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
  userId: EntityId
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
  id?: EntityId
  afterSaleNo?: string
  mainOrderId: EntityId
  subOrderId: EntityId
  userId?: EntityId
  merchantId?: EntityId
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
  deleted?: number
  version?: number
}

export interface OrderAggregateItem {
  id?: EntityId
  mainOrderId?: EntityId
  subOrderId?: EntityId
  spuId?: EntityId
  skuId?: EntityId
  skuCode?: string
  skuName?: string
  skuSnapshot?: string
  quantity?: number
  unitPrice?: number
  totalPrice?: number
  deleted?: number
  createdAt?: string
  updatedAt?: string
}

export interface OrderAggregateSubOrder {
  id?: EntityId
  mainOrderId?: EntityId
  subOrderNo?: string
  merchantId?: EntityId
  orderStatus?: string
  shippingStatus?: string
  afterSaleStatus?: string
  itemAmount?: number
  shippingFee?: number
  discountAmount?: number
  payableAmount?: number
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  shippingCompany?: string
  trackingNumber?: string
  shippedAt?: string
  estimatedArrival?: string
  receivedAt?: string
  doneAt?: string
  closedAt?: string
  closeReason?: string
  createdAt?: string
  updatedAt?: string
}

export interface OrderAggregateMainOrder {
  id?: EntityId
  mainOrderNo?: string
  userId?: EntityId
  orderStatus?: string
  totalAmount?: number
  payableAmount?: number
  payChannel?: string
  paidAt?: string
  cancelledAt?: string
  cancelReason?: string
  remark?: string
  clientOrderId?: string
  idempotencyKey?: string
  createdAt?: string
  updatedAt?: string
}

export interface OrderAggregateResponse {
  mainOrder?: OrderAggregateMainOrder
  subOrders?: Array<{
    subOrder?: OrderAggregateSubOrder
    items?: OrderAggregateItem[]
  }>
}

export interface StockLedger {
  skuId?: EntityId
  availableQty?: number
  lockedQty?: number
  soldQty?: number
  segmentCount?: number
  alertThreshold?: number
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface StockOperatePayload {
  subOrderNo: string
  skuId: EntityId
  quantity: number
  reason?: string
}

export type ProductPage = PageResult<ProductItem>
export type OrderPage = PageResult<OrderSummaryDTO>

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
  shopId?: EntityId
  shopName?: string
  categoryId?: EntityId
  categoryName?: string
  brandId?: EntityId
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
  categoryId?: EntityId
  brandId?: EntityId
  shopId?: EntityId
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
  spuId?: EntityId
  spuName: string
  subtitle?: string
  categoryId: EntityId
  brandId?: EntityId
  merchantId: EntityId
  status?: number
  description?: string
  mainImage?: string
}

export interface SkuDto {
  skuId?: EntityId
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
  skuId?: EntityId
  spuId?: EntityId
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
  spuId?: EntityId
  spuName?: string
  subtitle?: string
  categoryId?: EntityId
  brandId?: EntityId
  merchantId?: EntityId
  status?: number
  description?: string
  mainImage?: string
  createdAt?: string
  updatedAt?: string
  skus?: SkuDetail[]
}
