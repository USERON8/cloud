# UniApp Frontend API List

Generated on: 2026-03-15 18:47
Source: my-shop-uniapp/src/api/*.ts

Notes:
- Methods and paths are extracted from frontend call sites.
- The parameter column follows the function signature; field names come from code.
- The request column indicates whether query/body/headers are used; `-` means no usage was detected.

## address

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /api/user/address/list/{param} | listUserAddresses | userId: number | - |
| GET | /api/user/address/default/{param} | getDefaultAddress | userId: number | - |
| POST | /api/user/address/add/{param} | addUserAddress | userId: number, payload: UserAddress | body |
| PUT | /api/user/address/update/{param} | updateUserAddress | addressId: number, payload: UserAddress | body |
| DELETE | /api/user/address/delete/{param} | deleteUserAddress | addressId: number | - |
| POST | /api/user/address/page | pageUserAddresses | payload: UserAddressPageQuery | body |
| DELETE | /api/user/address/deleteBatch | deleteUserAddressesBatch | ids: number[] | body |
| PUT | /api/user/address/updateBatch | updateUserAddressesBatch | payload: UserAddress[] | body |

## admin

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /api/admin | getAdmins | params: { page?: number; size?: number } = {} | query |
| GET | /api/admin/{param} | getAdminById | id: number | - |
| POST | /api/admin | createAdmin | payload: AdminUpsertPayload | body |
| PUT | /api/admin/{param} | updateAdmin | id: number, payload: AdminUpsertPayload | body |
| DELETE | /api/admin/{param} | deleteAdmin | id: number | - |
| PATCH | /api/admin/{param}/status | updateAdminStatus | id: number, status: number | query |
| POST | /api/admin/{param}/reset-password | resetAdminPassword | id: number | response: string |

## auth-tokens

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /auth/tokens/stats | getTokenStats | - | - |
| GET | /auth/tokens/authorization/{param} | getAuthorizationDetails | id: string | - |
| DELETE | /auth/tokens/authorization/{param} | revokeAuthorization | id: string | - |
| POST | /auth/tokens/cleanup | cleanupExpiredTokens | - | - |
| GET | /auth/tokens/storage-structure | getStorageStructure | - | - |
| GET | /auth/tokens/blacklist/stats | getBlacklistStats | - | - |
| POST | /auth/tokens/blacklist/add | addTokenToBlacklist | tokenValue: string, reason?: string | query |
| GET | /auth/tokens/blacklist/check | checkBlacklist | tokenValue: string | query |
| POST | /auth/tokens/blacklist/cleanup | cleanupBlacklist | - | - |

## auth

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /auth/oauth2/github/login-url | startGitHubAuthorization | redirectPath = '/app/home' | query |
| GET | /oauth2/authorize | startAuthorization | redirectPath = '/app/home' | query |
| POST | /oauth2/token | exchangeAuthorizationCode | code: string, state: string | headers, body |
| POST | /auth/users/register | register | payload: RegisterRequest | body |
| DELETE | /auth/sessions | logout | - | - |
| DELETE | /auth/users/{param}/sessions | logoutAllSessions | username: string | - |
| GET | /auth/tokens/validate | validateToken | - | - |
| GET | /auth/oauth2/github/status | getGitHubAuthStatus | - | - |
| GET | /auth/oauth2/github/user-info | getGitHubUserInfo | - | - |

## category

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /api/category | getCategories | params: CategoryQuery = {} | query |
| GET | /api/category/{param} | getCategoryById | id: number | - |
| GET | /api/category/tree | getCategoryTree | enabledOnly = false | query |
| GET | /api/category/{param}/children | getCategoryChildren | id: number, enabledOnly = false | query |
| POST | /api/category | createCategory | payload: CategoryItem | body |
| PUT | /api/category/{param} | updateCategory | id: number, payload: CategoryItem | body |
| DELETE | /api/category/{param} | deleteCategory | id: number, cascade = false | query |
| PATCH | /api/category/{param}/status | updateCategoryStatus | id: number, status: number | query |
| PATCH | /api/category/{param}/sort | updateCategorySort | id: number, sort: number | query |
| PATCH | /api/category/{param}/move | moveCategory | id: number, newParentId: number | query |
| DELETE | /api/category/batch | deleteCategoriesBatch | ids: number[] | body |
| PATCH | /api/category/batch/status | updateCategoryStatusBatch | ids: number[], status: number | query |
| POST | /api/category/batch | createCategoriesBatch | payload: CategoryItem[] | body |

## merchant-auth

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| POST | /api/merchant/auth/apply/{param} | applyMerchantAuth | merchantId: number, payload: MerchantAuthPayload | body |
| POST | /api/merchant/auth/upload/license/{param} | uploadMerchantBusinessLicense | merchantId: number, filePath: string | multipart |
| GET | /api/merchant/auth/get/{param} | getMerchantAuth | merchantId: number | - |
| DELETE | /api/merchant/auth/revoke/{param} | revokeMerchantAuth | merchantId: number | - |
| POST | /api/merchant/auth/review/{param} | reviewMerchantAuth | merchantId: number, authStatus: number, remark?: string | query |
| GET | /api/merchant/auth/list | listMerchantAuthByStatus | authStatus: number | query |
| POST | /api/merchant/auth/review/batch | reviewMerchantAuthBatch | merchantIds: number[], authStatus: number, remark?: string | query, body |

## merchant

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /api/merchant | getMerchants | params: { page?: number; size?: number; status?: number; auditStatus?: number } = {} | query |
| GET | /api/merchant/{param} | getMerchantById | id: number | - |
| POST | /api/merchant | createMerchant | payload: MerchantUpsertPayload | body |
| PUT | /api/merchant/{param} | updateMerchant | id: number, payload: MerchantUpsertPayload | body |
| DELETE | /api/merchant/{param} | deleteMerchant | id: number | - |
| POST | /api/merchant/{param}/approve | approveMerchant | id: number, remark?: string | query |
| POST | /api/merchant/{param}/reject | rejectMerchant | id: number, reason: string | query |
| PATCH | /api/merchant/{param}/status | updateMerchantStatus | id: number, status: number | query |
| GET | /api/merchant/{param}/statistics | getMerchantStatistics | id: number | - |
| DELETE | /api/merchant/batch | deleteMerchantsBatch | ids: number[] | body |
| PATCH | /api/merchant/batch/status | updateMerchantStatusBatch | ids: number[], status: number | query |
| POST | /api/merchant/batch/approve | approveMerchantsBatch | ids: number[], remark?: string | query, body |

## notification

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| POST | /api/user/notification/welcome/{param} | sendWelcomeNotification | userId: number | - |
| POST | /api/user/notification/status-change/{param} | sendStatusChangeNotification | userId: number,
  payload: { newStatus: number; reason?: string } | body |
| POST | /api/user/notification/batch | sendBatchNotification | payload: { userIds: number[]; title: string; content: string } | body |
| POST | /api/user/notification/system | sendSystemAnnouncement | payload: { title: string; content: string } | body |

## order

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| POST | /api/orders | createOrder | payload: CreateOrderPayload (must include `skuId`, `receiverName`, `receiverPhone`, `receiverAddress`), idempotencyKey: string | headers, body |
| GET | /api/orders | listOrders | params: OrderQuery = {} | query |
| GET | /api/orders/{param} | getOrderById | id: number | - |
| POST | /api/orders/{param}/pay | payOrder | id: number | - |
| POST | /api/orders/{param}/cancel | cancelOrder | id: number, cancelReason?: string | query |
| POST | /api/orders/{param}/ship | shipOrder | id: number, shippingCompany: string, trackingNumber: string | query |
| POST | /api/orders/{param}/complete | completeOrder | id: number | - |
| POST | /api/orders/batch/pay | batchPayOrders | ids: number[] | body |
| POST | /api/orders/batch/cancel | batchCancelOrders | ids: number[], cancelReason?: string | query, body |
| POST | /api/orders/batch/ship | batchShipOrders | ids: number[], shippingCompany: string, trackingNumber: string | query, body |
| POST | /api/orders/batch/complete | batchCompleteOrders | ids: number[] | body |
| POST | /api/orders/after-sales | applyAfterSale | payload: AfterSaleInfo | body |
| POST | /api/orders/after-sales/{param}/actions/{param} | advanceAfterSaleStatus | afterSaleId: number, action: string, remark?: string | query |

## payment

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /api/payments/orders/{param} | getPaymentOrderByNo | paymentNo: string | - |
| GET | /api/payments/orders/by-order | getPaymentOrderByOrderNo | mainOrderNo: string, subOrderNo: string | query |
| POST | /api/payments/orders/{param}/checkout-session | createPaymentCheckoutSession | paymentNo: string | - |
| GET | /api/payments/refunds/{param} | getRefundByNo | refundNo: string | - |
| POST | /api/payments/orders | createPaymentOrder | payload: PaymentOrderCommand | body |
| POST | /api/payments/refunds | createPaymentRefund | payload: PaymentRefundCommand | body |
| POST | /api/payments/callbacks | handlePaymentCallback | payload: PaymentCallbackCommand | body |

## product-catalog

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| POST | /api/product/spu | createSpu | payload: SpuCreateRequest | body |
| PUT | /api/product/spu/{param} | updateSpu | spuId: number, payload: SpuCreateRequest | body |
| GET | /api/product/spu/{param} | getSpu | spuId: number | - |
| GET | /api/product/spu/category/{param} | listSpuByCategory | categoryId: number, status?: number | query |
| GET | /api/product/sku/batch | listSkuByIds | skuIds: number[] | query |
| PATCH | /api/product/spu/{param}/status | updateSpuStatus | spuId: number, status: number | query |

## product

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /api/product | listProducts | params: ProductQuery = {} | query |
| GET | /api/product/search | searchProducts | name: string | query |
| GET | /api/search/smart-search | smartSearchProducts | params: {
  keyword?: string
  page?: number
  size?: number
  sortField?: string
  sortOrder?: 'asc' \| 'desc'
  searchAfter?: string
} | query |
| GET | /api/search/suggestions | listSearchSuggestions | keyword: string, size = 10 | query |
| GET | /api/search/hot-keywords | listSearchHotKeywords | size = 10 | query |
| GET | /api/search/keyword-recommendations | listSearchKeywordRecommendations | keyword = '', size = 10 | query |
| GET | /api/search/hot/today | listTodayHotSellingProducts | page = 0, size = 20 | query |
| GET | /api/search/filter/combined | combinedSearchProducts | params: CombinedSearchParams (includes `searchAfter?: string`) | query |
| PATCH | /api/product/{param}/status | updateProductStatus | id: number, status: 0 \| 1 | query |

## search-ops

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| POST | /api/search/complex-search | complexSearch | request: ProductSearchRequest, searchAfter?: string | body, query |
| POST | /api/search/filters | getProductFilters | request: ProductSearchRequest, searchAfter?: string | body, query |
| GET | /api/search/basic | basicSearch | params: { keyword?: string; page?: number; size?: number; searchAfter?: string } | query |
| GET | /api/search/search | searchProducts | params: {
  keyword: string
  page?: number
  size?: number
  sortBy?: string
  sortDir?: string
  searchAfter?: string
} | query |
| GET | /api/search/search/category/{param} | searchByCategory | categoryId: number,
  params: { keyword?: string; page?: number; size?: number; searchAfter?: string } = {} | query |
| GET | /api/search/search/shop/{param} | searchByShop | shopId: number,
  params: { keyword?: string; page?: number; size?: number; searchAfter?: string } = {} | query |
| GET | /api/search/search/advanced | advancedSearch | params: {
  keyword: string
  minPrice?: number
  maxPrice?: number
  page?: number
  size?: number
  searchAfter?: string
} | query |
| GET | /api/search/recommended | listRecommendedProducts | page = 0, size = 20, searchAfter?: string | query |
| GET | /api/search/new | listNewProducts | page = 0, size = 20, searchAfter?: string | query |
| GET | /api/search/hot | listHotProducts | page = 0, size = 20, searchAfter?: string | query |
| GET | /api/search/hot/today | listTodayHotSellingProducts | page = 0, size = 20 | query |
| POST | /api/search/filter | filterSearch | request: ProductFilterRequest, searchAfter?: string | body, query |
| GET | /api/search/filter/category/{param} | filterByCategory | categoryId: number,
  params: { page?: number; size?: number; searchAfter?: string } = {} | query |
| GET | /api/search/filter/brand/{param} | filterByBrand | brandId: number,
  params: { page?: number; size?: number; searchAfter?: string } = {} | query |
| GET | /api/search/filter/price | filterByPrice | params: {
  minPrice?: number
  maxPrice?: number
  page?: number
  size?: number
  searchAfter?: string
} | query |
| GET | /api/search/filter/shop/{param} | filterByShop | shopId: number,
  params: { page?: number; size?: number; searchAfter?: string } = {} | query |

## shop-search

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| POST | /api/search/shops/complex-search | searchShops | request: ShopSearchRequest | body |
| POST | /api/search/shops/filters | getShopFilters | request: ShopSearchRequest | body |
| GET | /api/search/shops/suggestions | listShopSuggestions | keyword: string, size = 10 | query |
| GET | /api/search/shops/hot-shops | listHotShops | size = 10 | query |
| GET | /api/search/shops/{param} | getShopById | shopId: number | - |
| GET | /api/search/shops/recommended | listRecommendedShops | page = 0, size = 20 | query |
| GET | /api/search/shops/by-location | searchShopsByLocation | location: string, page = 0, size = 20 | query |

## statistics

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /api/statistics/overview | getStatisticsOverview | - | - |
| GET | /api/statistics/overview/async | getStatisticsOverviewAsync | - | - |
| GET | /api/statistics/registration-trend | getRegistrationTrendRange | startDate: string, endDate: string | query |
| GET | /api/statistics/registration-trend/async | getRegistrationTrend | days = 30 | query |
| GET | /api/statistics/role-distribution | getRoleDistribution | - | - |
| GET | /api/statistics/status-distribution | getStatusDistribution | - | - |
| GET | /api/statistics/active-users | getActiveUsers | days = 7 | query |
| GET | /api/statistics/growth-rate | getGrowthRate | days = 7 | query |
| GET | /api/statistics/activity-ranking | getActivityRanking | limit = 10, days = 30 | query |
| POST | /api/statistics/refresh-cache | refreshStatisticsCache | - | - |

## stock

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /api/stocks/ledger/{param} | getStockLedger | skuId: number | - |
| POST | /api/stocks/reserve | reserveStock | payload: StockOperatePayload | body |
| POST | /api/stocks/confirm | confirmStock | payload: StockOperatePayload | body |
| POST | /api/stocks/release | releaseStock | payload: StockOperatePayload | body |
| POST | /api/stocks/rollback | rollbackStock | payload: StockOperatePayload | body |

## thread-pool

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /api/thread-pool/info | getThreadPools | - | - |
| GET | /api/thread-pool/info/detail | getThreadPoolDetail | name: string | query |

## user-management

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /api/query/users | findUserByUsername | username: string | query |
| GET | /api/query/users/search | searchUsers | params: UserSearchParams (`page`, `size`, `username`, `email`, `phone`, `nickname`, `status`, `roleCode`) | query |
| PUT | /api/manage/users/{param} | updateUser | id: number, payload: UserUpsertPayload | body |
| POST | /api/manage/users/delete | deleteUser | id: number | body |
| POST | /api/manage/users/deleteBatch | deleteUsersBatch | ids: number[] | body |
| POST | /api/manage/users/updateBatch | updateUsersBatch | payload: UserUpsertPayload[] | body |
| POST | /api/manage/users/updateStatusBatch | updateUserStatusBatch | ids: number[], status: number | query |

## user

| Method | Path | Function | Parameters | Request |
| --- | --- | --- | --- | --- |
| GET | /api/user/profile/current | getCurrentProfile | - | - |
| PUT | /api/user/profile/current | updateCurrentProfile | payload: Partial<UserInfo> | body |
| PUT | /api/user/profile/current/password | changeCurrentPassword | payload: { oldPassword: string; newPassword: string } | body |
| POST | /api/user/profile/current/avatar | uploadCurrentAvatar | file: File | headers, body |
