# Cloud Shop API 文档（源码校对版）

生成时间：2026-03-14
来源：
services/**/controller 的接口定义、common-parent/common-core 的返回结构与结果码、common-parent/common-domain 的 DTO/VO、docs/postman/cloud-shop.postman_collection.json 的示例说明。

## 基础信息

| 项 | 说明 |
| --- | --- |
| 网关默认地址 | http://127.0.0.1:18080 |
| 网关路由 | /api/**, /auth/** |
| 网关文档入口 | /doc.html |
| 备注 | 以上信息见 README-zh.md |

## 前端 API 清单

前端调用清单见 my-shop-uniapp/docs/api.md（由 my-shop-uniapp/src/api/*.ts 生成）。

## 统一返回结构

Result<T> 定义见 common-parent/common-core/src/main/java/com/cloud/common/result/Result.java。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | Integer | 业务码 |
| message | String | 提示信息 |
| data | T | 业务数据 |
| timestamp | Long | 服务器时间戳（System.currentTimeMillis） |

## 结果码（ResultCode）

枚举定义见 common-parent/common-core/src/main/java/com/cloud/common/enums/ResultCode.java。

| code | message | enum |
| --- | --- | --- |
| 200 | Success | SUCCESS |
| 400 | Bad request | BAD_REQUEST |
| 401 | Unauthorized | UNAUTHORIZED |
| 403 | Forbidden | FORBIDDEN |
| 404 | Not found | NOT_FOUND |
| 405 | Method not allowed | METHOD_NOT_ALLOWED |
| 409 | Conflict | CONFLICT |
| 500 | Internal error | ERROR |
| 501 | Parameter error | PARAM_ERROR |
| 502 | Business error | BUSINESS_ERROR |
| 1001 | System error | SYSTEM_ERROR |
| 1002 | System busy | SYSTEM_BUSY |
| 1003 | System timeout | SYSTEM_TIMEOUT |
| 1004 | Not implemented | SYSTEM_NOT_IMPLEMENTED |
| 2001 | Permission denied | PERMISSION_DENIED |
| 2002 | Access denied | ACCESS_DENIED |
| 2003 | Role not found | ROLE_NOT_FOUND |
| 3001 | Validation error | VALIDATION_ERROR |
| 3002 | Missing parameter | MISSING_PARAMETER |
| 3003 | Invalid parameter | INVALID_PARAMETER |
| 4001 | Resource not found | RESOURCE_NOT_FOUND |
| 4002 | Resource already exists | RESOURCE_ALREADY_EXISTS |
| 4003 | Resource locked | RESOURCE_LOCKED |
| 5001 | Concurrent modification | CONCURRENT_MODIFICATION |
| 5002 | Optimistic lock error | OPTIMISTIC_LOCK_ERROR |
| 6001 | Stock not found | STOCK_NOT_FOUND |
| 6002 | Insufficient stock | STOCK_INSUFFICIENT |
| 6003 | Stock deduction failed | STOCK_DEDUCT_FAILED |
| 6004 | Stock increment failed | STOCK_ADD_FAILED |
| 6005 | Stock query failed | STOCK_QUERY_FAILED |
| 7001 | Database error | DB_ERROR |
| 7002 | Duplicate data | DB_DUPLICATE_KEY |
| 7003 | Constraint violation | DB_CONSTRAINT_VIOLATION |
| 7004 | User not found | USER_NOT_FOUND |
| 7005 | Username or password error | USERNAME_OR_PASSWORD_ERROR |
| 8001 | User already exists | USER_ALREADY_EXISTS |
| 8002 | User creation failed | USER_CREATE_FAILED |
| 8003 | User update failed | USER_UPDATE_FAILED |
| 8004 | User deletion failed | USER_DELETE_FAILED |
| 8005 | User query failed | USER_QUERY_FAILED |
| 8006 | User is not a merchant | USER_NOT_MERCHANT |
| 8007 | Parameter validation failed | PARAM_VALIDATION_FAILED |
| 8008 | Role mismatch | ROLE_MISMATCH |
| 8009 | Password error | PASSWORD_ERROR |
| 8010 | User is disabled | USER_DISABLED |
| 9001 | Product not found | PRODUCT_NOT_FOUND |
| 9002 | Product creation failed | PRODUCT_CREATE_FAILED |
| 9003 | Product update failed | PRODUCT_UPDATE_FAILED |
| 9004 | Product deletion failed | PRODUCT_DELETE_FAILED |
| 9005 | Product category not found | PRODUCT_CATEGORY_NOT_FOUND |
| 9006 | Product status error | PRODUCT_STATUS_ERROR |
| 9007 | Product query failed | PRODUCT_QUERY_FAILED |
| 9008 | Product already exists | PRODUCT_ALREADY_EXISTS |
| 9009 | Category not found | CATEGORY_NOT_FOUND |
| 10001 | Order not found | ORDER_NOT_FOUND |
| 10002 | Order creation failed | ORDER_CREATE_FAILED |
| 10003 | Order update failed | ORDER_UPDATE_FAILED |
| 10004 | Order deletion failed | ORDER_DELETE_FAILED |
| 10005 | Order status error | ORDER_STATUS_ERROR |
| 10006 | Order query failed | ORDER_QUERY_FAILED |
| 11001 | Payment record not found | PAYMENT_NOT_FOUND |
| 11002 | Payment record creation failed | PAYMENT_CREATE_FAILED |
| 11003 | Payment record update failed | PAYMENT_UPDATE_FAILED |
| 11004 | Payment record deletion failed | PAYMENT_DELETE_FAILED |
| 11005 | Payment status error | PAYMENT_STATUS_ERROR |
| 11006 | Refund failed | PAYMENT_REFUND_FAILED |
| 11007 | Payment query failed | PAYMENT_QUERY_FAILED |
| 12001 | Merchant not found | MERCHANT_NOT_FOUND |
| 12002 | Merchant creation failed | MERCHANT_CREATE_FAILED |
| 12003 | Merchant update failed | MERCHANT_UPDATE_FAILED |
| 12004 | Merchant deletion failed | MERCHANT_DELETE_FAILED |
| 12005 | Merchant status error | MERCHANT_STATUS_ERROR |
| 12006 | Merchant query failed | MERCHANT_QUERY_FAILED |
| 13001 | Admin not found | ADMIN_NOT_FOUND |
| 13002 | Admin creation failed | ADMIN_CREATE_FAILED |
| 13003 | Admin update failed | ADMIN_UPDATE_FAILED |
| 13004 | Admin deletion failed | ADMIN_DELETE_FAILED |
| 13005 | Admin status error | ADMIN_STATUS_ERROR |
| 13006 | Admin query failed | ADMIN_QUERY_FAILED |
| 14001 | Search failed | SEARCH_FAILED |
| 14002 | Search index error | SEARCH_INDEX_ERROR |
| 15001 | File is empty | FILE_IS_EMPTY |
| 15002 | File size exceeded | FILE_SIZE_EXCEEDED |
| 15003 | Upload failed | UPLOAD_FAILED |
| 16001 | Log creation failed | LOG_CREATE_FAILED |
| 16002 | Log update failed | LOG_UPDATE_FAILED |
| 16003 | Log deletion failed | LOG_DELETE_FAILED |
| 16004 | Log query failed | LOG_QUERY_FAILED |
| 17001 | OAuth2 invalid request | OAUTH2_INVALID_REQUEST |
| 17002 | OAuth2 invalid client | OAUTH2_INVALID_CLIENT |
| 17003 | OAuth2 invalid grant | OAUTH2_INVALID_GRANT |
| 17004 | OAuth2 unauthorized client | OAUTH2_UNAUTHORIZED_CLIENT |
| 17005 | OAuth2 unsupported grant type | OAUTH2_UNSUPPORTED_GRANT_TYPE |
| 17006 | OAuth2 invalid scope | OAUTH2_INVALID_SCOPE |
| 17007 | OAuth2 access denied | OAUTH2_ACCESS_DENIED |
| 17008 | OAuth2 server error | OAUTH2_SERVER_ERROR |
| 17011 | JWT token invalid | JWT_TOKEN_INVALID |
| 17012 | JWT token expired | JWT_TOKEN_EXPIRED |
| 17013 | JWT token malformed | JWT_TOKEN_MALFORMED |
| 17014 | JWT signature invalid | JWT_SIGNATURE_INVALID |
| 17015 | JWT token not found | JWT_TOKEN_NOT_FOUND |
| 17016 | JWT generation failed | JWT_GENERATION_FAILED |
| 17021 | Authentication failed | AUTHENTICATION_FAILED |
| 17022 | Bad credentials | BAD_CREDENTIALS |
| 17023 | Account locked | ACCOUNT_LOCKED |
| 17024 | Account expired | ACCOUNT_EXPIRED |
| 17025 | Credentials expired | CREDENTIALS_EXPIRED |
| 17026 | Token generation failed | TOKEN_GENERATION_FAILED |
| 17027 | Token revocation failed | TOKEN_REVOCATION_FAILED |
| 17031 | PKCE challenge missing | PKCE_CHALLENGE_MISSING |
| 17032 | PKCE verifier invalid | PKCE_VERIFIER_INVALID |
| 17033 | PKCE method unsupported | PKCE_METHOD_UNSUPPORTED |
| 17041 | Client registration failed | CLIENT_REGISTRATION_FAILED |
| 17042 | Client not found | CLIENT_NOT_FOUND |
| 17043 | Client authentication failed | CLIENT_AUTHENTICATION_FAILED |
| 17051 | Authorization code invalid | AUTHORIZATION_CODE_INVALID |
| 17052 | Authorization code expired | AUTHORIZATION_CODE_EXPIRED |
| 17053 | Authorization code already used | AUTHORIZATION_CODE_USED |
| 17054 | Refresh token invalid | REFRESH_TOKEN_INVALID |
| 17055 | Refresh token expired | REFRESH_TOKEN_EXPIRED |

## 权限说明

权限表达式以 @PreAuthorize 为准；未标注表示控制器未显式限制。常见标识示例：

| 类型 | 示例 |
| --- | --- |
| 角色 | hasRole('ADMIN'), hasRole('MERCHANT') |
| 权限 | hasAuthority('admin:all'), hasAuthority('merchant:manage'), hasAuthority('merchant:audit'), hasAuthority('order:create'), hasAuthority('order:query'), hasAuthority('order:cancel'), hasAuthority('order:refund'), hasAuthority('product:view'), hasAuthority('product:create'), hasAuthority('product:edit'), hasAuthority('product:delete'), hasAuthority('user:profile'), hasAuthority('user:address') |
| Scope | hasAuthority('SCOPE_openid'), hasAuthority('SCOPE_profile'), hasAuthority('SCOPE_read'), hasAuthority('SCOPE_internal') |
| 通用 | isAuthenticated() |

## 接口清单

### auth-service

#### AuthController（/auth）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| POST | /auth/users/register | Register user | 未标注 | body=RegisterRequestDTO | Result<RegisterResponseDTO> |
| DELETE | /auth/sessions | Logout current session | 未标注 | header=Authorization(Bearer) | Result<Void> |
| DELETE | /auth/users/{username}/sessions | Logout all user sessions | hasAuthority('admin:all') | path=username | Result<String> |
| GET | /auth/tokens/validate | Validate access token | isAuthenticated() | header=Authorization(Bearer) | Result<String> |

#### OAuth21AuthorizationServerConfig（AuthorizationServerSettings）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /oauth2/authorize | OAuth2 authorize | 由 Spring Authorization Server 处理 | query=response_type,client_id,redirect_uri,scope?,state?,code_challenge,code_challenge_method,nonce? | 302 Redirect |
| POST | /oauth2/token | OAuth2 token | 由 Spring Authorization Server 处理 | body=application/x-www-form-urlencoded(grant_type,code,redirect_uri,code_verifier 或 refresh_token/client_credentials) | OAuthTokenResponse |

#### GitHubOAuth2Controller（/auth/oauth2/github）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /auth/oauth2/github/user-info | Get GitHub user info | 未标注 | 无 | Result<UserDTO> |
| GET | /auth/oauth2/github/status | Check GitHub authorization status | 未标注 | 无 | Result<Boolean> |
| GET | /auth/oauth2/github/login-url | Get GitHub OAuth2 login URL | 未标注 | query=AuthorizationRequestDTO | Result<String> |

#### OAuth2TokenManageController（/auth/tokens）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /auth/tokens/stats | Get token storage statistics | hasAuthority('admin:all') | 无 | Result<Map<String,Object>> |
| GET | /auth/tokens/authorization/{id} | Get authorization details | hasAuthority('admin:all') | path=id | Result<Map<String,Object>> |
| DELETE | /auth/tokens/authorization/{id} | Revoke authorization | hasAuthority('admin:all') | path=id | Result<Void> |
| POST | /auth/tokens/cleanup | Cleanup expired tokens | hasAuthority('admin:all') | 无 | Result<Map<String,Object>> |
| GET | /auth/tokens/storage-structure | Get Redis storage structure | hasAuthority('admin:all') | 无 | Result<Map<String,Object>> |
| GET | /auth/tokens/blacklist/stats | Get blacklist statistics | hasAuthority('admin:all') | 无 | Result<TokenBlacklistService.BlacklistStats> |
| POST | /auth/tokens/blacklist/add | Add token to blacklist | hasAuthority('admin:all') | query=tokenValue, reason | Result<Void> |
| GET | /auth/tokens/blacklist/check | Check blacklist status | hasAuthority('admin:all') | query=tokenValue | Result<Map<String,Object>> |
| POST | /auth/tokens/blacklist/cleanup | Cleanup blacklist entries | hasAuthority('admin:all') | 无 | Result<Map<String,Object>> |

### order-service

#### OrderController（/api/orders）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| POST | /api/orders | Create main order | hasAuthority('order:create') | header=Idempotency-Key, body=CreateMainOrderRequest | Result<OrderAggregateResponse> |
| GET | /api/orders | List orders | hasAuthority('order:query') | query=page,size,userId?,shopId?,status? | Result<PageResult<OrderSummaryDTO>> |
| GET | /api/orders/{orderId} | Get order detail | hasAuthority('order:query') | path=orderId | Result<OrderSummaryDTO> |
| POST | /api/orders/{orderId}/pay | Pay order | hasAuthority('order:create') | path=orderId | Result<Boolean> |
| POST | /api/orders/{orderId}/cancel | Cancel order | hasAuthority('order:cancel') | path=orderId, query=cancelReason? | Result<Boolean> |
| POST | /api/orders/{orderId}/ship | Ship order | hasAuthority('order:query') | path=orderId, query=shippingCompany?,trackingNumber? | Result<Boolean> |
| POST | /api/orders/{orderId}/complete | Complete order | hasAuthority('order:query') | path=orderId | Result<Boolean> |
| POST | /api/orders/batch/pay | Batch pay orders | hasAuthority('order:create') | body=List<Long> | Result<Integer> |
| POST | /api/orders/batch/cancel | Batch cancel orders | hasAuthority('order:cancel') | body=List<Long>, query=cancelReason? | Result<Integer> |
| POST | /api/orders/batch/ship | Batch ship orders | hasAuthority('order:query') | body=List<Long>, query=shippingCompany?,trackingNumber? | Result<Integer> |
| POST | /api/orders/batch/complete | Batch complete orders | hasAuthority('order:query') | body=List<Long> | Result<Integer> |
| POST | /api/orders/after-sales | Apply after-sale | hasAuthority('order:refund') | body=AfterSale | Result<AfterSale> |
| POST | /api/orders/after-sales/{afterSaleId}/actions/{action} | Advance after-sale status | hasAuthority('order:refund') | path=afterSaleId,action, query=remark? | Result<AfterSale> |

### payment-service

#### PaymentOrderController（/api/payments）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| POST | /api/payments/orders | Create payment order | hasAuthority('order:create') | body=PaymentOrderCommandDTO | Result<Long> |
| GET | /api/payments/orders/{paymentNo} | Get payment order by number | isAuthenticated() | path=paymentNo | Result<PaymentOrderVO> |
| GET | /api/payments/orders/{paymentNo}/status | Get payment order status | isAuthenticated() | path=paymentNo | Result<Map<String,Object>> |
| POST | /api/payments/callbacks | Handle payment callback | hasAuthority('order:refund') | body=PaymentCallbackCommandDTO | Result<Boolean> |
| POST | /api/payments/refunds | Create payment refund | hasAuthority('order:refund') | body=PaymentRefundCommandDTO | Result<Long> |
| GET | /api/payments/refunds/{refundNo} | Get refund by number | hasAuthority('order:refund') | path=refundNo | Result<PaymentRefundVO> |

### product-service

#### CategoryController（/api/category）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /api/category | Get categories | hasAuthority('product:view') | query=page,size,parentId,level | Result<PageResult<CategoryDTO>> |
| GET | /api/category/{id} | Get category | hasAuthority('product:view') | path=id | Result<CategoryDTO> |
| GET | /api/category/tree | Get category tree | hasAuthority('product:view') | query=enabledOnly | Result<List<CategoryDTO>> |
| GET | /api/category/{id}/children | Get children | hasAuthority('product:view') | path=id, query=enabledOnly | Result<List<CategoryDTO>> |
| POST | /api/category | Create category | hasAuthority('product:create') | body=CategoryDTO | Result<CategoryDTO> |
| PUT | /api/category/{id} | Update category | hasAuthority('product:edit') | path=id, body=CategoryDTO | Result<Boolean> |
| DELETE | /api/category/{id} | Delete category | hasAuthority('product:delete') | path=id, query=cascade | Result<Boolean> |
| PATCH | /api/category/{id}/status | Update status | hasAuthority('product:edit') | path=id, query=status | Result<Boolean> |
| PATCH | /api/category/{id}/sort | Update sort | hasAuthority('product:edit') | path=id, query=sort | Result<Boolean> |
| PATCH | /api/category/{id}/move | Move category | hasAuthority('product:edit') | path=id, query=newParentId | Result<Boolean> |
| DELETE | /api/category/batch | Batch delete | hasAuthority('product:delete') | body=List<Long> | Result<Boolean> |
| PATCH | /api/category/batch/status | Batch update status | hasAuthority('product:edit') | query=ids,status | Result<Integer> |
| POST | /api/category/batch | Batch create | hasAuthority('product:create') | body=List<CategoryDTO> | Result<Integer> |

#### ProductCatalogController（/api/product）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| POST | /api/product/spu | Create SPU | hasAuthority('product:create') | body=SpuCreateRequestDTO | Result<Long> |
| PUT | /api/product/spu/{spuId} | Update SPU | hasAuthority('product:edit') | path=spuId, body=SpuCreateRequestDTO | Result<Boolean> |
| GET | /api/product/spu/{spuId} | Get SPU detail | hasAuthority('product:view') | path=spuId | Result<SpuDetailVO> |
| GET | /api/product/spu/category/{categoryId} | List SPU by category | hasAuthority('product:view') | path=categoryId, query=status | Result<List<SpuDetailVO>> |
| GET | /api/product/sku/batch | Batch query SKU details | hasAuthority('product:view') | query=skuIds | Result<List<SkuDetailVO>> |
| PATCH | /api/product/spu/{spuId}/status | Update SPU status | hasAuthority('product:edit') | path=spuId, query=status | Result<Boolean> |

#### ProductQueryController（/api/product）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /api/product | List products | hasAuthority('product:view') | query=page,size,name?,categoryId?,brandId?,status? | Result<PageResult<ProductItemDTO>> |
| GET | /api/product/search | Search products | permitAll | query=name,size? | Result<List<ProductItemDTO>> |
| PATCH | /api/product/{spuId}/status | Update SPU status | hasAuthority('product:edit') | path=spuId, query=status | Result<Boolean> |

### search-service

#### ProductSearchController（/api/search）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| POST | /api/search/complex-search | Complex search | 未标注 | body=ProductSearchRequest, query=searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| POST | /api/search/filters | Get filter data | 未标注 | body=ProductSearchRequest, query=searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/suggestions | Search suggestions | 未标注 | query=keyword,size | Result<List<String>> |
| GET | /api/search/hot-keywords | Hot keywords | 未标注 | query=size | Result<List<String>> |
| GET | /api/search/keyword-recommendations | Keyword recommendations | 未标注 | query=keyword,size | Result<List<String>> |
| GET | /api/search/search | Basic search | 未标注 | query=keyword,page,size,sortBy,sortDir,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/search/category/{categoryId} | Search by category | 未标注 | path=categoryId, query=keyword,page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/search/shop/{shopId} | Search by shop | 未标注 | path=shopId, query=keyword,page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/search/advanced | Advanced search | 未标注 | query=keyword,minPrice,maxPrice,page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/smart-search | Smart search | 未标注 | query=keyword,categoryId,minPrice,maxPrice,sortField,sortOrder,page,size,searchAfter? | Result<ElasticsearchOptimizedService.SearchResultDTO> |
| GET | /api/search/recommended | Recommended products | 未标注 | query=page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/new | New products | 未标注 | query=page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/hot | Hot products | 未标注 | query=page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/basic | Basic API search | 未标注 | query=keyword,page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| POST | /api/search/filter | Filter search | 未标注 | body=ProductFilterRequest, query=searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/filter/category/{categoryId} | Filter by category | 未标注 | path=categoryId, query=page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/filter/brand/{brandId} | Filter by brand | 未标注 | path=brandId, query=page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/filter/price | Filter by price | 未标注 | query=minPrice,maxPrice,page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/filter/shop/{shopId} | Filter by shop | 未标注 | path=shopId, query=page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |
| GET | /api/search/filter/combined | Combined filter | 未标注 | query=keyword,categoryId,brandId,minPrice,maxPrice,shopId,sortBy,sortOrder,page,size,searchAfter? | Result<SearchResultDTO<ProductDocument>> |

#### ShopSearchController（/api/search/shops）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| POST | /api/search/shops/complex-search | Complex shop search | 未标注 | body=ShopSearchRequest | Result<SearchResult<ShopDocument>> |
| POST | /api/search/shops/filters | Shop filter data | 未标注 | body=ShopSearchRequest | Result<SearchResult<ShopDocument>> |
| GET | /api/search/shops/suggestions | Shop suggestions | 未标注 | query=keyword,size | Result<List<String>> |
| GET | /api/search/shops/hot-shops | Hot shops | 未标注 | query=size | Result<List<ShopDocument>> |
| GET | /api/search/shops/{shopId} | Get shop by id | 未标注 | path=shopId | Result<ShopDocument> |
| GET | /api/search/shops/recommended | Recommended shops | 未标注 | query=page,size | Result<SearchResult<ShopDocument>> |
| GET | /api/search/shops/by-location | Search shops by location | 未标注 | query=location,page,size | Result<SearchResult<ShopDocument>> |

### stock-service

#### StockLedgerController（/api/stocks）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /api/stocks/ledger/{skuId} | Get stock ledger by sku | 未标注 | path=skuId | Result<StockLedgerVO> |
| POST | /api/stocks/reserve | Reserve stock | 未标注 | body=StockOperateCommandDTO | Result<Boolean> |
| POST | /api/stocks/confirm | Confirm stock reservation | 未标注 | body=StockOperateCommandDTO | Result<Boolean> |
| POST | /api/stocks/release | Release reserved stock | 未标注 | body=StockOperateCommandDTO | Result<Boolean> |
| POST | /api/stocks/rollback | Rollback stock reservation | 未标注 | body=StockOperateCommandDTO | Result<Boolean> |

### user-service

#### AdminController（/api/admin）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /api/admin | Get admins with pagination | hasAuthority('admin:all') | query=page,size | Result<PageResult<AdminDTO>> |
| GET | /api/admin/{id} | Get admin details | hasAuthority('admin:all') | path=id | Result<AdminDTO> |
| POST | /api/admin | Create admin | hasAuthority('admin:all') | body=AdminUpsertRequestDTO | Result<AdminDTO> |
| PUT | /api/admin/{id} | Update admin | hasAuthority('admin:all') | path=id, body=AdminUpsertRequestDTO | Result<Boolean> |
| DELETE | /api/admin/{id} | Delete admin | hasAuthority('admin:all') | path=id | Result<Boolean> |
| PATCH | /api/admin/{id}/status | Update admin status | hasAuthority('admin:all') | path=id, query=status | Result<Boolean> |
| POST | /api/admin/{id}/reset-password | Reset admin password | hasAuthority('admin:all') | path=id | Result<Boolean> |

#### ThreadPoolMonitorController（/api/thread-pool）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /api/thread-pool/info | Get all thread pool metrics | hasAuthority('admin:all') | 无 | Result<List<Map<String,Object>>> |
| GET | /api/thread-pool/info/detail | Get thread pool metrics by bean name | hasAuthority('admin:all') | query=name | Result<Map<String,Object>> |

#### MerchantAuthController（/api/merchant/auth）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| POST | /api/merchant/auth/apply/{merchantId} | Apply merchant auth | hasAuthority('merchant:manage') and @permissionManager.isMerchantOwner(...) | path=merchantId, body=MerchantAuthRequestDTO | Result<MerchantAuthDTO> |
| POST | /api/merchant/auth/upload/license/{merchantId} | Upload business license | hasAuthority('merchant:manage') and @permissionManager.isMerchantOwner(...) | path=merchantId, multipart file | Result<MerchantAuthFileUploadDTO> |
| GET | /api/merchant/auth/get/{merchantId} | Get merchant auth | hasAuthority('merchant:manage') and @permissionManager.isMerchantOwner(...) | path=merchantId | Result<MerchantAuthDTO> |
| DELETE | /api/merchant/auth/revoke/{merchantId} | Revoke merchant auth | hasAuthority('merchant:manage') and @permissionManager.isMerchantOwner(...) | path=merchantId | Result<Boolean> |
| POST | /api/merchant/auth/review/{merchantId} | Review merchant auth | hasAuthority('merchant:audit') | path=merchantId, query=authStatus,remark | Result<Boolean> |
| GET | /api/merchant/auth/list | List merchant auth by status | hasAuthority('merchant:audit') | query=authStatus | Result<List<MerchantAuthDTO>> |
| POST | /api/merchant/auth/review/batch | Batch review merchant auth | hasAuthority('merchant:audit') | body=List<Long>, query=authStatus,remark | Result<Boolean> |

#### MerchantController（/api/merchant）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /api/merchant | Get merchants | hasAuthority('merchant:manage') | query=page,size,status,auditStatus | Result<PageResult<MerchantDTO>> |
| GET | /api/merchant/{id} | Get merchant by ID | hasAuthority('merchant:manage') and @permissionManager.isMerchantOwner(...) | path=id | Result<MerchantDTO> |
| POST | /api/merchant | Create merchant | hasAuthority('admin:all') | body=MerchantUpsertRequestDTO | Result<MerchantDTO> |
| PUT | /api/merchant/{id} | Update merchant | hasAuthority('merchant:manage') and @permissionManager.isMerchantOwner(...) | path=id, body=MerchantUpsertRequestDTO | Result<Boolean> |
| DELETE | /api/merchant/{id} | Delete merchant | hasAuthority('admin:all') | path=id | Result<Boolean> |
| POST | /api/merchant/{id}/approve | Approve merchant | hasAuthority('merchant:audit') | path=id, query=remark | Result<Boolean> |
| POST | /api/merchant/{id}/reject | Reject merchant | hasAuthority('merchant:audit') | path=id, query=reason | Result<Boolean> |
| PATCH | /api/merchant/{id}/status | Update merchant status | hasAuthority('admin:all') | path=id, query=status | Result<Boolean> |
| GET | /api/merchant/{id}/statistics | Get merchant statistics | hasAuthority('merchant:manage') and @permissionManager.isMerchantOwner(...) | path=id | Result<Object> |
| DELETE | /api/merchant/batch | Batch delete merchants | hasAuthority('admin:all') | body=List<Long> | Result<Boolean> |
| PATCH | /api/merchant/batch/status | Batch update merchant status | hasAuthority('admin:all') | query=ids,status | Result<Boolean> |
| POST | /api/merchant/batch/approve | Batch approve merchants | hasAuthority('merchant:audit') | body=List<Long>, query=remark | Result<Boolean> |

#### UserProfileController（/api/user/profile）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /api/user/profile/current | Get current profile | isAuthenticated() | 无 | Result<UserDTO> |
| PUT | /api/user/profile/current | Update current profile | isAuthenticated() | body=UserProfileUpdateDTO | Result<Boolean> |
| PUT | /api/user/profile/current/password | Change current password | isAuthenticated() | body=UserProfilePasswordChangeDTO | Result<Boolean> |
| POST | /api/user/profile/current/avatar | Upload current avatar | isAuthenticated() | multipart/form-data file | Result<String> |

#### UserAddressController（/api/user/address）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| POST | /api/user/address/add/{userId} | Add user address | 未标注 | path=userId, body=UserAddressRequestDTO | Result<UserAddressDTO> |
| PUT | /api/user/address/update/{addressId} | Update user address | 未标注 | path=addressId, body=UserAddressRequestDTO | Result<UserAddressDTO> |
| DELETE | /api/user/address/delete/{addressId} | Delete user address | 未标注 | path=addressId | Result<Boolean> |
| GET | /api/user/address/list/{userId} | List user addresses | 未标注 | path=userId | Result<List<UserAddressVO>> |
| GET | /api/user/address/default/{userId} | Get default address | 未标注 | path=userId | Result<UserAddressVO> |
| POST | /api/user/address/page | Page user addresses | 未标注 | body=UserAddressPageDTO | Result<PageResult<UserAddressVO>> |
| DELETE | /api/user/address/deleteBatch | Batch delete addresses | 未标注 | body=List<Long> | Result<Boolean> |
| PUT | /api/user/address/updateBatch | Batch update addresses | 未标注 | body=List<UserAddressRequestDTO> | Result<Boolean> |

#### UserManageController（/api/manage/users）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| PUT | /api/manage/users/{id} | Update user | hasAuthority('admin:all') | path=id, body=UserUpsertRequestDTO | Result<Boolean> |
| POST | /api/manage/users/delete | Delete user | hasAuthority('admin:all') | body=Long | Result<Boolean> |
| POST | /api/manage/users/deleteBatch | Batch delete users | hasAuthority('admin:all') | body=Long[] | Result<Boolean> |
| POST | /api/manage/users/updateBatch | Batch update users | hasAuthority('admin:all') | body=List<UserUpsertRequestDTO> | Result<Boolean> |
| POST | /api/manage/users/updateStatusBatch | Batch update user status | hasAuthority('admin:all') | query=ids,status | Result<Boolean> |

#### UserQueryController（/api/query/users）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /api/query/users | Find user by username | hasAuthority('admin:all') | query=username | Result<UserDTO> |
| GET | /api/query/users/search | Search users | hasAuthority('admin:all') | query=page,size,username,email,roleCode | Result<PageResult<UserVO>> |

#### UserNotificationController（/api/user/notification）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| POST | /api/user/notification/welcome/{userId} | Send welcome notification | hasAuthority('admin:all') | path=userId | Result<Boolean> |
| POST | /api/user/notification/status-change/{userId} | Send status change notification | hasAuthority('admin:all') | path=userId, body=UserNotificationStatusChangeRequestDTO | Result<Boolean> |
| POST | /api/user/notification/batch | Send batch notification | hasAuthority('admin:all') | body=UserNotificationBatchRequestDTO | Result<Boolean> |
| POST | /api/user/notification/system | Send system announcement | hasAuthority('admin:all') | body=UserSystemAnnouncementRequestDTO | Result<Boolean> |

#### UserStatisticsController（/api/statistics）

| 方法 | 路径 | 说明 | 权限 | 参数/Body | 返回 |
| --- | --- | --- | --- | --- | --- |
| GET | /api/statistics/overview | Get overview | hasAuthority('admin:all') | 无 | Result<UserStatisticsVO> |
| GET | /api/statistics/overview/async | Get overview async | hasAuthority('admin:all') | 无 | CompletableFuture<Result<UserStatisticsVO>> |
| GET | /api/statistics/registration-trend | Get registration trend | hasAuthority('admin:all') | query=startDate,endDate | Result<Map<LocalDate,Long>> |
| GET | /api/statistics/registration-trend/async | Get registration trend async | hasAuthority('admin:all') | query=days | CompletableFuture<Result<Map<LocalDate,Long>>> |
| GET | /api/statistics/role-distribution | Get role distribution | hasAuthority('admin:all') | 无 | Result<Map<String,Long>> |
| GET | /api/statistics/status-distribution | Get status distribution | hasAuthority('admin:all') | 无 | Result<Map<String,Long>> |
| GET | /api/statistics/active-users | Count active users | hasAuthority('admin:all') | query=days | Result<Long> |
| GET | /api/statistics/growth-rate | Calculate growth rate | hasAuthority('admin:all') | query=days | Result<Double> |
| GET | /api/statistics/activity-ranking | Get activity ranking | hasAuthority('admin:all') | query=limit,days | CompletableFuture<Result<Map<Long,Long>>> |
| POST | /api/statistics/refresh-cache | Refresh statistics cache | hasAuthority('admin:all') | 无 | CompletableFuture<Result<Boolean>> |

## 请求体 DTO 字段（仅列出接口入参涉及的类型）

### RegisterRequestDTO

| 字段 | 类型 |
| --- | --- |
| username | String |
| password | String |
| phone | String |
| nickname | String |

### AuthorizationRequestDTO（query 模型）

| 字段 | 类型 |
| --- | --- |
| clientId | String |
| redirectUri | String |
| scope | String |
| state | String |
| codeChallenge | String |
| codeChallengeMethod | String（默认 S256） |
| nonce | String |

### CreateMainOrderRequest

| 字段 | 类型 |
| --- | --- |
| userId | Long（普通用户可省略，管理员创建时必填） |
| cartId | Long |
| spuId | Long |
| skuId | Long |
| quantity | Integer |
| totalAmount | BigDecimal |
| payableAmount | BigDecimal |
| remark | String |
| idempotencyKey | String |
| receiverName | String |
| receiverPhone | String |
| receiverAddress | String |

说明：cartId 与 (spuId, skuId, quantity) 二选一；单品下单必须同时提供 spuId 和 skuId，不接受只传 spuId。

### AfterSale

| 字段 | 类型 |
| --- | --- |
| afterSaleNo | String |
| mainOrderId | Long |
| subOrderId | Long |
| userId | Long（普通用户可省略，管理员创建时必填） |
| merchantId | Long |
| afterSaleType | String |
| status | String |
| reason | String |
| description | String |
| applyAmount | BigDecimal |
| approvedAmount | BigDecimal |
| returnLogisticsCompany | String |
| returnLogisticsNo | String |
| refundChannel | String |
| refundedAt | LocalDateTime |
| closedAt | LocalDateTime |
| closeReason | String |
| id | Long（BaseEntity） |
| createdAt | LocalDateTime（BaseEntity） |
| updatedAt | LocalDateTime（BaseEntity） |
| deleted | Integer（BaseEntity） |
| version | Integer（BaseEntity） |

### PaymentOrderCommandDTO

| 字段 | 类型 |
| --- | --- |
| paymentNo | String |
| mainOrderNo | String |
| subOrderNo | String |
| userId | Long（普通用户可省略，管理员创建时必填） |
| amount | BigDecimal |
| channel | String |
| idempotencyKey | String |

### PaymentCallbackCommandDTO

| 字段 | 类型 |
| --- | --- |
| paymentNo | String |
| callbackNo | String |
| callbackStatus | String |
| providerTxnNo | String |
| idempotencyKey | String |
| payload | String |

### PaymentRefundCommandDTO

| 字段 | 类型 |
| --- | --- |
| refundNo | String |
| paymentNo | String |
| afterSaleNo | String |
| refundAmount | BigDecimal |
| reason | String |
| idempotencyKey | String |

### AdminUpsertRequestDTO（继承 BaseAccountUpsertRequestDTO）

| 字段 | 类型 |
| --- | --- |
| username | String |
| password | String |
| phone | String |
| status | Integer |
| realName | String |
| role | String |

### MerchantUpsertRequestDTO（继承 BaseAccountUpsertRequestDTO）

| 字段 | 类型 |
| --- | --- |
| username | String |
| password | String |
| phone | String |
| status | Integer |
| merchantName | String |
| email | String |

### MerchantAuthRequestDTO

| 字段 | 类型 |
| --- | --- |
| businessLicenseNumber | String |
| businessLicenseUrl | String |
| idCardFrontUrl | String |
| idCardBackUrl | String |
| contactPhone | String |
| contactAddress | String |

### UserAddressRequestDTO（继承 BaseEntity）

| 字段 | 类型 |
| --- | --- |
| consignee | String |
| phone | String |
| province | String |
| city | String |
| district | String |
| street | String |
| detailAddress | String |
| isDefault | Integer |
| id | Long（BaseEntity） |
| createdAt | LocalDateTime（BaseEntity） |
| updatedAt | LocalDateTime（BaseEntity） |
| deleted | Integer（BaseEntity） |
| version | Integer（BaseEntity） |

### UserAddressPageDTO（继承 PageQuery）

| 字段 | 类型 |
| --- | --- |
| current | Long（PageQuery） |
| size | Long（PageQuery） |
| orderBy | String（PageQuery） |
| orderDirection | String（PageQuery） |
| userId | Long（普通用户可省略，管理员创建时必填） |
| consignee | String |

### UserUpsertRequestDTO（继承 BaseAccountUpsertRequestDTO）

| 字段 | 类型 |
| --- | --- |
| id | Long |
| username | String |
| password | String |
| phone | String |
| status | Integer |
| nickname | String |
| avatarUrl | String |
| email | String |
| roles | List<String> |

### UserNotificationBatchRequestDTO

| 字段 | 类型 |
| --- | --- |
| userIds | List<Long> |
| title | String |
| content | String |

### UserNotificationStatusChangeRequestDTO

| 字段 | 类型 |
| --- | --- |
| newStatus | Integer |
| reason | String |

### UserSystemAnnouncementRequestDTO

| 字段 | 类型 |
| --- | --- |
| title | String |
| content | String |

### UserProfileUpdateDTO

| 字段 | 类型 |
| --- | --- |
| nickname | String |
| avatarUrl | String |
| email | String |
| phone | String |

### UserProfilePasswordChangeDTO

| 字段 | 类型 |
| --- | --- |
| oldPassword | String |
| newPassword | String |

## 响应与查询对象参考（源码路径）

| 类型 | 路径 |
| --- | --- |
| RegisterResponseDTO | common-parent/common-domain/src/main/java/com/cloud/common/domain/dto/auth/RegisterResponseDTO.java |
| UserDTO | common-parent/common-domain/src/main/java/com/cloud/common/domain/dto/user/UserDTO.java |
| AdminDTO | common-parent/common-domain/src/main/java/com/cloud/common/domain/dto/user/AdminDTO.java |
| MerchantDTO | common-parent/common-domain/src/main/java/com/cloud/common/domain/dto/user/MerchantDTO.java |
| MerchantAuthDTO | common-parent/common-domain/src/main/java/com/cloud/common/domain/dto/user/MerchantAuthDTO.java |
| UserAddressDTO | common-parent/common-domain/src/main/java/com/cloud/common/domain/dto/user/UserAddressDTO.java |
| UserAddressVO | common-parent/common-domain/src/main/java/com/cloud/common/domain/vo/UserAddressVO.java |
| UserVO | common-parent/common-domain/src/main/java/com/cloud/common/domain/vo/user/UserVO.java |
| UserStatisticsVO | common-parent/common-domain/src/main/java/com/cloud/common/domain/vo/user/UserStatisticsVO.java |
| OrderAggregateResponse | services/order-service/src/main/java/com/cloud/order/dto/OrderAggregateResponse.java |
| OrderMain | services/order-service/src/main/java/com/cloud/order/entity/OrderMain.java |
| OrderSub | services/order-service/src/main/java/com/cloud/order/entity/OrderSub.java |
| OrderItem | services/order-service/src/main/java/com/cloud/order/entity/OrderItem.java |
| AfterSale | services/order-service/src/main/java/com/cloud/order/entity/AfterSale.java |
| PaymentOrderVO | common-parent/common-domain/src/main/java/com/cloud/common/domain/vo/payment/PaymentOrderVO.java |
| PaymentRefundVO | common-parent/common-domain/src/main/java/com/cloud/common/domain/vo/payment/PaymentRefundVO.java |
| SpuDetailVO | common-parent/common-domain/src/main/java/com/cloud/common/domain/vo/product/SpuDetailVO.java |
| SkuDetailVO | common-parent/common-domain/src/main/java/com/cloud/common/domain/vo/product/SkuDetailVO.java |
| StockLedgerVO | common-parent/common-domain/src/main/java/com/cloud/common/domain/vo/stock/StockLedgerVO.java |
| SearchResult | services/search-service/src/main/java/com/cloud/search/dto/SearchResult.java |
| ProductDocument | services/search-service/src/main/java/com/cloud/search/document/ProductDocument.java |
| ShopDocument | services/search-service/src/main/java/com/cloud/search/document/ShopDocument.java |

## 测试数据与示例说明

Postman 集合：docs/postman/cloud-shop.postman_collection.json。
默认测试数据来源于 db/test/*/test.sql（userId=20001, merchantId=30001, categoryId=300, spuId=50001, skuId=51001）。














