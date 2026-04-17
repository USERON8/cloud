# Cloud Shop API Guide

Generated on: 2026-04-02
Source of truth: `services/**/controller/*.java`, gateway security rules, and current frontend integration under `my-shop-uniapp/src/api/*.ts`.

## Base URLs

- Gateway: `http://127.0.0.1:18080`
- Auth server endpoints: `/auth/**`, `/oauth2/**`
- Business endpoints: `/api/**`
- Public checkout HTML: `/api/payments/checkout/{ticket}`
- External Alipay callback: `/api/v1/payment/alipay/notify`

## Authorization Model

- Public traffic is authenticated and normalized at `gateway` first.
- For authenticated routed traffic, gateway strips the public bearer token before forwarding, injects trusted internal identity headers, and signs them with shared HMAC.
- Downstream servlet services restore trusted identity from those gateway-signed internal headers.
- `Public`: no explicit controller restriction.
- `Authenticated`: `isAuthenticated()`.
- `Admin`: `hasAuthority('admin:all')` or `ROLE_ADMIN` where noted.
- `Merchant`: `hasAuthority('merchant:manage')` or `hasAuthority('merchant:audit')`.
- `Order`: `order:create`, `order:query`, `order:cancel`, `order:refund`.
- `Product`: `product:create`, `product:edit`, `product:delete`.
- `Internal`: `hasAuthority('SCOPE_internal')`.

## Internal Header Contract

The following headers are internal-only and must never be produced by public clients:

- `X-Internal-Request`
- `X-Internal-Subject`
- `X-Internal-User-Id`
- `X-Internal-Username`
- `X-Internal-Client-Id`
- `X-Internal-Roles`
- `X-Internal-Permissions`
- `X-Internal-Scopes`
- `X-Internal-Timestamp`
- `X-Internal-Signature`

Rules:

- Downstream services accept these headers only when HMAC verification succeeds.
- Bearer token requests bypass internal HMAC verification and follow the normal public JWT path.
- Public clients should continue sending only bearer tokens or the existing public request signature headers where applicable.

## End-to-End Chains

### 1. Public Browse Chain

1. Categories
   - `GET /api/category`
   - `GET /api/category/{id}`
   - `GET /api/category/tree`
   - `GET /api/category/{id}/children`
2. Product discovery
   - `GET /api/product`
   - `GET /api/product/search`
   - `GET /api/product/spu/{spuId}`
   - `GET /api/product/spu/category/{categoryId}`
   - `GET /api/product/sku/batch`
3. Product management discovery
   - `GET /api/product/manage`
4. Search discovery
   - `GET /api/search/search`
   - `GET /api/search/smart-search`
   - `GET /api/search/basic`
   - `GET /api/search/suggestions`
   - `GET /api/search/hot-keywords`
   - `GET /api/search/keyword-recommendations`
   - `GET /api/search/recommended`
   - `GET /api/search/new`
   - `GET /api/search/hot`
   - `GET /api/search/hot/today`
   - `POST /api/search/complex-search`
   - `POST /api/search/filters`
   - `POST /api/search/filter`
4. Shop discovery
   - `POST /api/search/shops/complex-search`
   - `POST /api/search/shops/filters`
   - `GET /api/search/shops/suggestions`
   - `GET /api/search/shops/hot-shops`
   - `GET /api/search/shops/{shopId}`
   - `GET /api/search/shops/recommended`
   - `GET /api/search/shops/by-location`

Notes:
- Public product and shop search only supports active records.
- Public product list and SPU detail return only active SPUs with active SKUs.

### 2. User Authentication And Session Chain

1. Register user
   - `POST /auth/users/register`
2. Start OAuth2 authorization
   - `GET /oauth2/authorize`
   - `POST /oauth2/token`
3. GitHub OAuth helper endpoints
   - `GET /auth/oauth2/github/login-url`
   - `GET /auth/oauth2/github/status`
   - `GET /auth/oauth2/github/user-info`
   - `GET /auth/oauth2/github/callback`
4. Validate or terminate session
   - `GET /auth/tokens/validate`
   - `DELETE /auth/sessions`
   - `DELETE /auth/users/{username}/sessions` (admin only)
5. Admin token management
   - `GET /auth/tokens/stats`
   - `GET /auth/tokens/authorization/{id}`
   - `DELETE /auth/tokens/authorization/{id}`
   - `POST /auth/tokens/cleanup`
   - `GET /auth/tokens/storage-structure`
   - `GET /auth/tokens/blacklist/stats`
   - `POST /auth/tokens/blacklist/add`
   - `GET /auth/tokens/blacklist/check`
   - `POST /auth/tokens/blacklist/cleanup`
6. Internal governance token operations
   - `GET /internal/governance/auth/tokens/stats`
   - `GET /internal/governance/auth/tokens/authorization/{id}`
   - `POST /internal/governance/auth/tokens/authorization/{id}/revoke`
   - `GET /internal/governance/auth/tokens/blacklist/stats`
   - `POST /internal/governance/auth/tokens/blacklist/add`
   - `GET /internal/governance/auth/tokens/blacklist/check`
   - `POST /internal/governance/auth/tokens/blacklist/cleanup`

### 3. User Profile And Address Chain

1. Current profile
   - `GET /api/user/profile/current`
   - `PUT /api/user/profile/current`
   - `PUT /api/user/profile/current/password`
   - `POST /api/user/profile/current/avatar`
2. Address book
   - `POST /api/user/address/add/{userId}`
   - `PUT /api/user/address/update/{addressId}`
   - `DELETE /api/user/address/delete/{addressId}`
   - `GET /api/user/address/list/{userId}`
   - `GET /api/user/address/default/{userId}`
   - `POST /api/user/address/page`
   - `DELETE /api/user/address/deleteBatch`
   - `PUT /api/user/address/updateBatch`

Notes:
- Address controller is authenticated at class level and still enforces owner or admin checks in each operation.
- The frontend profile page now reads and updates the current user through the profile endpoints instead of relying on session claims only.

### 4. Cart And Order Preparation Chain

1. Current cart
   - `GET /api/cart`
2. Sync cart snapshot
   - `POST /api/cart/sync`

Notes:
- `GET /api/cart` returns the current authenticated user's active cart and creates one if it does not exist yet.
- `POST /api/cart/sync` replaces the current active cart with a full item snapshot. It is not a patch-style update.
- The response contains the server-issued `cartId`, which is the identifier used by cart checkout on the order side.

### 5. Order Creation, Fulfillment, And After-Sale Chain

1. Create order
   - `POST /api/orders`
   - Header: `Idempotency-Key`
   - Body field: `clientOrderId`
   - Regular users can only create orders for themselves.
2. Query order
   - `GET /api/orders`
   - `GET /api/orders/{orderId}`
3. Order actions
   - `POST /api/orders/{orderId}/cancel`
   - `POST /api/orders/{orderId}/ship`
   - `POST /api/orders/{orderId}/complete`
   - Batch variants under `/api/orders/batch/*`
4. After-sale
   - `POST /api/orders/after-sales`
   - `POST /api/orders/after-sales/{afterSaleId}/actions/{action}`

Important behavior:
- `POST /api/orders/{orderId}/pay` and `POST /api/orders/batch/pay` still exist for compatibility but direct pay transitions are disabled in business logic. Verified payment confirmation must come from the payment chain.
- Shipping now requires explicit `shippingCompany` and `trackingNumber`.
- Merchant users cannot force `complete`; completion is limited to the order owner or admin.

### 6. Payment And Checkout Chain

1. Create or locate payment order
   - `POST /api/payments/orders`
2. Query payment order
   - `GET /api/payments/orders/{paymentNo}`
   - `GET /api/payments/orders/by-order?mainOrderNo=...&subOrderNo=...`
   - `GET /api/payments/orders/{paymentNo}/status`
3. Create checkout session
   - `POST /api/payments/orders/{paymentNo}/checkout-session`
4. Render checkout page
   - `GET /api/payments/checkout/{ticket}`
5. External payment callback
   - `POST /api/v1/payment/alipay/notify`
6. Refund chain
   - `POST /api/payments/refunds`
   - `GET /api/payments/refunds/{refundNo}`

Important behavior:
- Regular users can create payment orders only for themselves; the controller now binds request `userId` to the authenticated owner.
- Internal callback mutation endpoint `/api/payments/callbacks` still exists for controlled internal callers, but business logic rejects state mutation through the old direct internal callback path.
- Checkout is public only at the ticketed HTML endpoint; all order and refund reads remain owner/admin restricted.

### 7. Merchant And Admin Operations Chain

1. Merchant management
   - `GET /api/merchant`
   - `GET /api/merchant/{id}`
   - `POST /api/merchant`
   - `PUT /api/merchant/{id}`
   - `DELETE /api/merchant/{id}`
   - `POST /api/merchant/{id}/approve`
   - `POST /api/merchant/{id}/reject`
   - `PATCH /api/merchant/{id}/status`
   - `GET /api/merchant/{id}/statistics`
   - Batch endpoints under `/api/merchant/batch/*`
2. Merchant authentication
   - `POST /api/merchant/auth/apply/{merchantId}`
   - `POST /api/merchant/auth/upload/license/{merchantId}`
   - `GET /api/merchant/auth/get/{merchantId}`
   - `DELETE /api/merchant/auth/revoke/{merchantId}`
   - `POST /api/merchant/auth/review/{merchantId}`
   - `GET /api/merchant/auth/list`
   - `POST /api/merchant/auth/review/batch`
3. Admin and user management
   - `GET /api/admin`
   - `GET /api/admin/{id}`
   - `POST /api/admin`
   - `PUT /api/admin/{id}`
   - `DELETE /api/admin/{id}`
   - `PATCH /api/admin/{id}/status`
   - `POST /api/admin/{id}/reset-password`
   - `GET /api/admin/query/users`
   - `GET /api/admin/query/users/search`
   - `PUT /api/admin/manage/users/{id}`
   - `POST /api/admin/manage/users/delete`
   - `POST /api/admin/manage/users/deleteBatch`
   - `POST /api/admin/manage/users/updateBatch`
   - `POST /api/admin/manage/users/updateStatusBatch`
4. Notifications, statistics, and ops
   - `/api/app/user/notification/*`
   - `/auth/tokens/*`
   - `/api/admin/mq/*`
   - `/api/admin/outbox/*`
   - `/api/admin/observability/*`
5. Internal governance aliases
   - `/api/admin/thread-pool/internal/*`
   - `/api/admin/statistics/internal/*`
   - `/api/admin/stocks/internal/ledger/*`
6. Admin governance proxy
   - `/api/admin/governance/admins`
   - `/api/admin/governance/admins/{id}`
   - `/api/admin/governance/admins/{id}/status`
   - `/api/admin/governance/admins/{id}/reset-password`
   - `/api/admin/governance/auth/tokens/*`
   - `/api/admin/governance/thread-pools`
   - `/api/admin/governance/thread-pools/{name}`
   - `/api/admin/governance/users/statistics/*`
   - `/api/admin/governance/stocks/ledger/{skuId}`
7. Governance aggregation
   - `/internal/governance/admins`
   - `/internal/governance/admins/{id}`
   - `/internal/governance/admins/{id}/status`
   - `/internal/governance/admins/{id}/reset-password`
   - `/internal/governance/users/query`
   - `/internal/governance/users/search`
   - `/internal/governance/users/{id}`
   - `/internal/governance/users/delete-batch`
   - `/internal/governance/users/update-batch`
   - `/internal/governance/users/status-batch`
   - `/internal/governance/auth/tokens/*`
   - `/internal/governance/mq/consumers`
   - `/internal/governance/mq/dead-letters/pending`
   - `/internal/governance/mq/dead-letters/handle`
    - `/internal/governance/outbox/stats`
    - `/internal/governance/outbox/pending`
    - `/internal/governance/outbox/dead`
    - `/internal/governance/outbox/requeue`
    - `/internal/governance/outbox/requeue-batch`
    - `/internal/governance/observability/grafana`
    - `/internal/governance/observability/grafana/open`
    - `/internal/governance/notifications/welcome/{userId}`
    - `/internal/governance/notifications/status-change/{userId}`
    - `/internal/governance/notifications/batch`
    - `/internal/governance/notifications/system`
   - `/internal/governance/thread-pools`
   - `/internal/governance/thread-pools/{name}`
   - `/internal/governance/users/statistics/*`
   - `/internal/governance/stocks/ledger/{skuId}`

Notes:
- Merchant approval in the current frontend workflow is driven by `/api/merchant/auth/review/{merchantId}` because the UI reads `authStatus` from merchant auth records. `/api/merchant/{id}/approve|reject` still exists as a backend management API but is not the frontend source of truth for auth review state.
- `/api/admin/statistics/**`, `/api/admin/thread-pool/**`, `/api/admin/manage/users/**`, `/api/admin/query/users/**`, `/api/admin/mq/**`, `/api/admin/outbox/**`, `/api/admin/observability/**`, `/api/app/user/notification/**`, `/auth/tokens/**`, and `GET /api/admin/stocks/ledger/{skuId}` are now governance-owned admin paths routed to `governance-service`.
- `/api/admin/governance/**` remains the explicit governance aggregation prefix for new admin-side callers, while the older admin-shaped paths above are now formal governance entries rather than business-service endpoints.
- Internal callers should still prefer `/internal/governance/**` or the dedicated internal aliases when they need `SCOPE_internal` access.
- Grafana open endpoints only allow dashboards declared in `governance-service` observability configuration. Unknown dashboard UIDs are rejected instead of being forwarded.
- Merchant admin and merchant-auth paths remain intentionally routed to `user-service` in this phase and are not governance-owned paths.

### 8. Internal Inventory And Gateway Utility Chain

1. Inventory
   - `GET /api/stocks/ledger/{skuId}`
   - `POST /api/stocks/reserve`
   - `POST /api/stocks/pre-check`
   - `POST /api/stocks/confirm`
   - `POST /api/stocks/release`
   - `POST /api/stocks/rollback`
2. Gateway fallback
   - `GET /gateway/fallback/search`
   - `/gateway/fallback/payment`
   - `/gateway/fallback/user`
3. MQ governance and dead-letter utilities
   - `GET /internal/governance/mq/consumers`
   - `GET /internal/governance/mq/dead-letters/pending`
   - `POST /internal/governance/mq/dead-letters/handle`
   - `GET /api/admin/mq/consumers`
   - `GET /api/admin/mq/dead-letters/pending`
   - `POST /api/admin/mq/dead-letters/handle`
4. Outbox governance and observability entry
   - `GET /internal/governance/outbox/stats`
   - `GET /internal/governance/outbox/pending`
   - `GET /internal/governance/outbox/dead`
   - `POST /internal/governance/outbox/requeue`
   - `POST /internal/governance/outbox/requeue-batch`
   - `GET /internal/governance/observability/grafana`
   - `GET /internal/governance/observability/grafana/open`
   - `GET /api/admin/outbox/stats`
   - `GET /api/admin/outbox/pending`
   - `GET /api/admin/outbox/dead`
   - `POST /api/admin/outbox/requeue`
   - `POST /api/admin/outbox/requeue-batch`
   - `GET /api/admin/observability/grafana`
   - `GET /api/admin/observability/grafana/open`

Notes:
- Stock ledger reads require admin.
- Admin stock ledger reads now terminate at `governance-service`, which queries `stock-service` over Dubbo.
- Stock ledger internal governance reads are also available under `/api/admin/stocks/internal/ledger/*` for trusted internal callers.
- Stock mutation endpoints require internal scope.
- Stock ledger `status` is the raw integer segment status aggregated from active rows. Current live ledger reads return active inventory only, so callers should treat `1` as `Active` and derive low-stock warnings from `availableQty` versus `alertThreshold`.
- Gateway fallback is a degradation utility endpoint, not a primary client search API.
- MQ governance aggregation now terminates at `governance-service`.
- Service-local `/internal/mq/**` endpoints still exist behind each business service as infrastructure support endpoints, but callers should prefer the governance aggregation paths.
- Service-local `/internal/outbox/governance/**` endpoints now exist behind each business service as infrastructure support endpoints, but callers should prefer the governance aggregation paths.
- Grafana remains the observability system of record. `governance-service` only exposes governed entry metadata and dashboard deeplinks.
- User statistics, thread-pool monitor, and token management endpoints are admin-only operational APIs.
- Thread-pool monitor and user statistics now also expose internal governance aliases under `/api/admin/thread-pool/internal/**` and `/api/admin/statistics/internal/**` to decouple future governance callers from the public admin API surface.
- Gateway now authorizes `/api/admin/thread-pool/internal/**`, `/api/admin/statistics/internal/**`, and `/api/admin/stocks/internal/**` with `SCOPE_internal` before the broader admin route match.
- Gateway now also exposes `/api/admin/governance/**` as the admin-facing governance proxy and rewrites it to `/internal/governance/**`.
- Gateway now routes statistics, thread-pool, user-management, user-query, MQ-governance, and token-governance admin paths to `governance-service` with higher priority than the business-service routes.
- These internal governance aliases are now served by dedicated internal controllers instead of being mixed into the public admin controllers.
- `governance-service` now aggregates stable internal governance capabilities behind `/internal/governance/**` and should be preferred by trusted callers over reaching into business services directly.

## Endpoint Index

### Authentication

| Method | Path | Access | Notes |
| --- | --- | --- | --- |
| POST | `/auth/users/register` | Public | Register a new user. |
| DELETE | `/auth/sessions` | Authenticated | Logout current session and revoke tokens when available. |
| DELETE | `/auth/users/{username}/sessions` | Admin | Revoke all sessions for one user. |
| GET | `/auth/tokens/validate` | Authenticated | Validate current bearer token. |
| GET | `/auth/oauth2/github/user-info` | Public controller, principal-aware | Returns GitHub-linked user info if session exists. |
| GET | `/auth/oauth2/github/status` | Public controller, principal-aware | Checks GitHub OAuth status. |
| GET | `/auth/oauth2/github/callback` | Public | Informational callback endpoint. |
| GET | `/auth/oauth2/github/login-url` | Public | Stores authorization request in session and returns `/oauth2/authorization/github`. |
| GET | `/oauth2/authorize` | Spring Authorization Server | OAuth2 authorization endpoint. |
| POST | `/oauth2/token` | Spring Authorization Server | OAuth2 token endpoint. |

### User Profile And Address

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/user/profile/current` | Authenticated |
| PUT | `/api/user/profile/current` | Authenticated |
| PUT | `/api/user/profile/current/password` | Authenticated |
| POST | `/api/user/profile/current/avatar` | Authenticated |
| POST | `/api/user/address/add/{userId}` | Authenticated + owner/admin guard |
| PUT | `/api/user/address/update/{addressId}` | Authenticated + owner/admin guard |
| DELETE | `/api/user/address/delete/{addressId}` | Authenticated + owner/admin guard |
| GET | `/api/user/address/list/{userId}` | Authenticated + owner/admin guard |
| GET | `/api/user/address/default/{userId}` | Authenticated + owner/admin guard |
| POST | `/api/user/address/page` | Authenticated |
| DELETE | `/api/user/address/deleteBatch` | Authenticated |
| PUT | `/api/user/address/updateBatch` | Authenticated |

### Cart

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/cart` | Authenticated |
| POST | `/api/cart/sync` | Authenticated |

Notes:
- `GET /api/cart` returns the active cart for the current user and creates one if missing.
- `POST /api/cart/sync` accepts a full cart snapshot and replaces the active cart items atomically.

### Product Catalog

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/category` | Public |
| GET | `/api/category/{id}` | Public |
| GET | `/api/category/tree` | Public |
| GET | `/api/category/{id}/children` | Public |
| POST | `/api/category` | `product:create` |
| PUT | `/api/category/{id}` | `product:edit` |
| DELETE | `/api/category/{id}` | `product:delete` |
| PATCH | `/api/category/{id}/status` | `product:edit` |
| PATCH | `/api/category/{id}/sort` | `product:edit` |
| PATCH | `/api/category/{id}/move` | `product:edit` |
| DELETE | `/api/category/batch` | `product:delete` |
| PATCH | `/api/category/batch/status` | `product:edit` |
| POST | `/api/category/batch` | `product:create` |
| POST | `/api/product/spu` | `product:create` |
| PUT | `/api/product/spu/{spuId}` | `product:edit` |
| GET | `/api/product/spu/{spuId}` | Public |
| GET | `/api/product/spu/category/{categoryId}` | Public |
| GET | `/api/product/sku/batch` | Public |
| PATCH | `/api/product/spu/{spuId}/status` | `product:edit` |
| GET | `/api/product` | Public |
| GET | `/api/product/manage` | `product:edit` |
| GET | `/api/product/search` | Public |
| PATCH | `/api/product/{spuId}/status` | `product:edit` |

Notes:
- `GET /api/product` remains a public browse endpoint and only supports active products.
- `GET /api/product/manage` is the merchant/admin management list endpoint and supports non-public status values.
- Merchant callers on `/api/product/manage` are restricted to their own `merchantId`. When `merchantId` is omitted, the backend defaults to the authenticated merchant.

### Search

| Method | Path | Access |
| --- | --- | --- |
| POST | `/api/search/complex-search` | Public |
| POST | `/api/search/filters` | Public |
| GET | `/api/search/suggestions` | Public |
| GET | `/api/search/hot-keywords` | Public |
| GET | `/api/search/keyword-recommendations` | Public |
| GET | `/api/search/search` | Public |
| GET | `/api/search/search/category/{categoryId}` | Public |
| GET | `/api/search/search/shop/{shopId}` | Public |
| GET | `/api/search/search/advanced` | Public |
| GET | `/api/search/smart-search` | Public |
| GET | `/api/search/recommended` | Public |
| GET | `/api/search/new` | Public |
| GET | `/api/search/hot` | Public |
| GET | `/api/search/hot/today` | Public |
| GET | `/api/search/basic` | Public |
| POST | `/api/search/filter` | Public |
| GET | `/api/search/filter/category/{categoryId}` | Public |
| GET | `/api/search/filter/brand/{brandId}` | Public |
| GET | `/api/search/filter/price` | Public |
| GET | `/api/search/filter/shop/{shopId}` | Public |
| GET | `/api/search/filter/combined` | Public |
| POST | `/api/search/shops/complex-search` | Public |
| POST | `/api/search/shops/filters` | Public |
| GET | `/api/search/shops/suggestions` | Public |
| GET | `/api/search/shops/hot-shops` | Public |
| GET | `/api/search/shops/{shopId}` | Public |
| GET | `/api/search/shops/recommended` | Public |
| GET | `/api/search/shops/by-location` | Public |
| GET | `/gateway/fallback/search` | Gateway utility |
| GET | `/internal/governance/mq/consumers` | Internal ops |
| GET | `/internal/governance/mq/dead-letters/pending` | Internal ops |
| POST | `/internal/governance/mq/dead-letters/handle` | Internal ops |
| GET | `/internal/governance/outbox/stats` | Internal ops |
| GET | `/internal/governance/outbox/pending` | Internal ops |
| GET | `/internal/governance/outbox/dead` | Internal ops |
| POST | `/internal/governance/outbox/requeue` | Internal ops |
| POST | `/internal/governance/outbox/requeue-batch` | Internal ops |
| GET | `/internal/governance/observability/grafana` | Internal ops |
| GET | `/internal/governance/observability/grafana/open` | Internal ops |
| POST | `/internal/governance/notifications/welcome/{userId}` | Internal ops |
| POST | `/internal/governance/notifications/status-change/{userId}` | Internal ops |
| POST | `/internal/governance/notifications/batch` | Internal ops |
| POST | `/internal/governance/notifications/system` | Internal ops |

### Orders And After-Sale

| Method | Path | Access | Notes |
| --- | --- | --- | --- |
| POST | `/api/orders` | `order:create` | Requires `Idempotency-Key` header and `clientOrderId` body field. |
| GET | `/api/orders` | `order:query` | `shopId` is a legacy alias of `merchantId`. |
| GET | `/api/orders/{orderId}` | `order:query` |  |
| POST | `/api/orders/{orderId}/pay` | `order:create` | Intentionally blocked in service layer. |
| POST | `/api/orders/{orderId}/cancel` | `order:cancel` |  |
| POST | `/api/orders/{orderId}/ship` | Merchant or admin | Requires `shippingCompany` and `trackingNumber`. |
| POST | `/api/orders/{orderId}/complete` | `order:query` | Merchant callers are blocked in service layer. |
| POST | `/api/orders/batch/pay` | `order:create` | Intentionally blocked in service layer. |
| POST | `/api/orders/batch/cancel` | `order:cancel` |  |
| POST | `/api/orders/batch/ship` | Merchant or admin | Requires `shippingCompany` and `trackingNumber`. |
| POST | `/api/orders/batch/complete` | `order:query` | Merchant callers are blocked in service layer. |
| POST | `/api/orders/after-sales` | `order:refund` | User ownership and amount checks are enforced. |
| POST | `/api/orders/after-sales/{afterSaleId}/actions/{action}` | `order:refund` | User, merchant, and admin action sets differ. |

Order list and order detail now return `OrderSummaryDTO` with these stable fields:
- Top-level summary: `id`, `orderNo`, `userId`, `subOrderId`, `subOrderNo`, `merchantId`, `afterSaleId`, `afterSaleNo`, `afterSaleType`, `refundNo`, `totalAmount`, `payAmount`, `status`, `afterSaleStatus`, `createdAt`.
- Item list: `items[]` contains `id`, `subOrderId`, `spuId`, `skuId`, `skuCode`, `skuName`, `quantity`, `unitPrice`, `totalPrice`.
- Snapshot field: `items[].skuSnapshot` is the immutable transaction snapshot parsed from `order_item.sku_snapshot`.
- Optional latest product view: `items[].latestProduct` is best-effort live product data. It may be `null` when the product service is unavailable or the SKU no longer exists.
- Status mapping: `0` pending, `1` paid, `2` shipped, `3` done, `4` closed/cancelled.

### Payments

| Method | Path | Access | Notes |
| --- | --- | --- | --- |
| POST | `/api/payments/orders` | Authenticated + `order:create` | Owner-bound for regular users. |
| GET | `/api/payments/orders/{paymentNo}` | Authenticated | Owner/admin only. |
| GET | `/api/payments/orders/by-order` | Authenticated | Owner/admin only. |
| POST | `/api/payments/orders/{paymentNo}/checkout-session` | Authenticated | Owner/admin only. |
| GET | `/api/payments/orders/{paymentNo}/status` | Authenticated | Owner/admin only. |
| POST | `/api/payments/callbacks` | `order:refund` | Legacy compatibility endpoint. Business mutation through this path is intentionally rejected. |
| POST | `/api/payments/refunds` | `order:refund` | Refund creation path for already-paid payment orders only. |
| GET | `/api/payments/refunds/{refundNo}` | Authenticated | Owner/admin only. |
| GET | `/api/payments/checkout/{ticket}` | Public | Ticketed checkout HTML. |
| POST | `/api/v1/payment/alipay/notify` | Public | Verified external callback. |

### Merchants, Admin, Notifications, Statistics, Ops

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/merchant` | `admin:all` or `merchant:manage` |
| GET | `/api/merchant/{id}` | Admin or merchant owner |
| POST | `/api/merchant` | `admin:all` |
| PUT | `/api/merchant/{id}` | Admin or merchant owner |
| DELETE | `/api/merchant/{id}` | `admin:all` |
| POST | `/api/merchant/{id}/approve` | `merchant:audit` |
| POST | `/api/merchant/{id}/reject` | `merchant:audit` |
| PATCH | `/api/merchant/{id}/status` | `admin:all` |
| GET | `/api/merchant/{id}/statistics` | Admin or merchant owner |
| DELETE | `/api/merchant/batch` | `admin:all` |
| PATCH | `/api/merchant/batch/status` | `admin:all` |
| POST | `/api/merchant/batch/approve` | `merchant:audit` |
| POST | `/api/merchant/auth/apply/{merchantId}` | Admin or merchant owner |
| POST | `/api/merchant/auth/upload/license/{merchantId}` | Admin or merchant owner |
| GET | `/api/merchant/auth/get/{merchantId}` | Admin or merchant owner |
| DELETE | `/api/merchant/auth/revoke/{merchantId}` | Admin or merchant owner |
| POST | `/api/merchant/auth/review/{merchantId}` | `admin:all` or `merchant:audit` |
| GET | `/api/merchant/auth/list` | `admin:all` or `merchant:audit` |
| POST | `/api/merchant/auth/review/batch` | `admin:all` or `merchant:audit` |
| GET | `/api/admin` | `admin:all` |
| GET | `/api/admin/{id}` | `admin:all` |
| POST | `/api/admin` | `admin:all` |
| PUT | `/api/admin/{id}` | `admin:all` |
| DELETE | `/api/admin/{id}` | `admin:all` |
| PATCH | `/api/admin/{id}/status` | `admin:all` |
| POST | `/api/admin/{id}/reset-password` | `admin:all` |
| GET | `/api/admin/query/users` | `admin:all` |
| GET | `/api/admin/query/users/search` | `admin:all` |
| PUT | `/api/admin/manage/users/{id}` | `admin:all` |
| POST | `/api/admin/manage/users/delete` | `admin:all` |
| POST | `/api/admin/manage/users/deleteBatch` | `admin:all` |
| POST | `/api/admin/manage/users/updateBatch` | `admin:all` |
| POST | `/api/admin/manage/users/updateStatusBatch` | `admin:all` |
| POST | `/api/app/user/notification/welcome/{userId}` | `admin:all` |
| POST | `/api/app/user/notification/status-change/{userId}` | `admin:all` |
| POST | `/api/app/user/notification/batch` | `admin:all` |
| POST | `/api/app/user/notification/system` | `admin:all` |
| GET | `/api/admin/statistics/overview` | `admin:all` |
| GET | `/api/admin/statistics/overview/async` | `admin:all` |
| GET | `/api/admin/statistics/registration-trend` | `admin:all` |
| GET | `/api/admin/statistics/registration-trend/async` | `admin:all` |
| GET | `/api/admin/statistics/role-distribution` | `admin:all` |
| GET | `/api/admin/statistics/status-distribution` | `admin:all` |
| GET | `/api/admin/statistics/active-users` | `admin:all` |
| GET | `/api/admin/statistics/growth-rate` | `admin:all` |
| GET | `/api/admin/statistics/activity-ranking` | `admin:all` |
| POST | `/api/admin/statistics/refresh-cache` | `admin:all` |
| GET | `/api/admin/thread-pool/info` | `admin:all` |
| GET | `/api/admin/thread-pool/info/detail` | `admin:all` |
| GET | `/api/admin/mq/consumers` | `admin:all` |
| GET | `/api/admin/mq/dead-letters/pending` | `admin:all` |
| POST | `/api/admin/mq/dead-letters/handle` | `admin:all` |
| GET | `/api/admin/outbox/stats` | `admin:all` |
| GET | `/api/admin/outbox/pending` | `admin:all` |
| GET | `/api/admin/outbox/dead` | `admin:all` |
| POST | `/api/admin/outbox/requeue` | `admin:all` |
| POST | `/api/admin/outbox/requeue-batch` | `admin:all` |
| GET | `/api/admin/observability/grafana` | `admin:all` |
| GET | `/api/admin/observability/grafana/open` | `admin:all` |
| GET | `/auth/tokens/stats` | `admin:all` |
| GET | `/auth/tokens/authorization/{id}` | `admin:all` |
| DELETE | `/auth/tokens/authorization/{id}` | `admin:all` |
| POST | `/auth/tokens/cleanup` | `admin:all` |
| GET | `/auth/tokens/storage-structure` | `admin:all` |
| GET | `/auth/tokens/blacklist/stats` | `admin:all` |
| POST | `/auth/tokens/blacklist/add` | `admin:all` |
| GET | `/auth/tokens/blacklist/check` | `admin:all` |
| POST | `/auth/tokens/blacklist/cleanup` | `admin:all` |

Notes:
- Merchant auth reads (`/api/merchant/auth/get/{merchantId}` and `/api/merchant/auth/list`) now return presigned certificate URLs for business license and ID card attachments when the stored value is an object key in the cert bucket.
- Admin workspace endpoints now explicitly allow pure `admin:all` callers on merchant listing and merchant-auth review APIs, which matches the current admin page behavior.

### Inventory

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/stocks/ledger/{skuId}` | Admin |
| POST | `/api/stocks/reserve` | Internal |
| POST | `/api/stocks/pre-check` | Internal |
| POST | `/api/stocks/confirm` | Internal |
| POST | `/api/stocks/release` | Internal |
| POST | `/api/stocks/rollback` | Internal |

## Chain-Critical Payload Notes

### Create order request

- Endpoint: `POST /api/orders`
- Header: `Idempotency-Key`
- Field: `clientOrderId`
- Single-item checkout must include `spuId`, `skuId`, `quantity`, `receiverName`, `receiverPhone`, `receiverAddress`.
- Cart checkout uses `cartId` plus the same receiver fields.
- Regular users cannot impersonate another `userId`.
- `clientOrderId` is the strict business idempotency key on the order domain and must stay stable across retries from the same client intent.

Example order detail response shape:

```json
{
  "id": 10001,
  "orderNo": "MO202604060001",
  "userId": 20001,
  "subOrderId": 11001,
  "subOrderNo": "SO202604060001",
  "merchantId": 30001,
  "totalAmount": 199.00,
  "payAmount": 189.00,
  "status": 1,
  "afterSaleStatus": "NONE",
  "createdAt": "2026-04-06T16:30:00",
  "items": [
    {
      "id": 12001,
      "subOrderId": 11001,
      "spuId": 50001,
      "skuId": 51001,
      "skuCode": "SKU-51001",
      "skuName": "Cloud Phone Pro",
      "quantity": 1,
      "unitPrice": 189.00,
      "totalPrice": 189.00,
      "skuSnapshot": {
        "spuId": 50001,
        "spuName": "Cloud Phone",
        "brandName": "Cloud",
        "skuId": 51001,
        "skuCode": "SKU-51001",
        "skuName": "Cloud Phone Pro",
        "specJson": "{\"color\":\"black\"}",
        "unitPrice": 189.00,
        "quantity": 1
      },
      "latestProduct": {
        "spuId": 50001,
        "skuId": 51001,
        "spuName": "Cloud Phone",
        "skuCode": "SKU-51001",
        "skuName": "Cloud Phone Pro",
        "specJson": "{\"color\":\"black\"}",
        "salePrice": 189.00,
        "marketPrice": 219.00,
        "imageUrl": "https://cdn.example.com/products/51001.png",
        "status": 1,
        "brandName": "Cloud",
        "categoryName": "Phone",
        "merchantId": 30001,
        "shopName": "Cloud Shop"
      }
    }
  ]
}
```

### Create payment order request

- Endpoint: `POST /api/payments/orders`
- Fields: `paymentNo`, `mainOrderNo`, `subOrderNo`, `userId`, `amount`, `channel`, `idempotencyKey`
- Regular users can only create a payment order for themselves.
- The requested amount must match the remote order payable amount.

### Create refund request

- Endpoint: `POST /api/payments/refunds`
- Fields: `refundNo`, `paymentNo`, `afterSaleNo`, `refundAmount`, `reason`, `idempotencyKey`
- Refunds are allowed only for paid payment orders.
- Cumulative refund amount cannot exceed the original payment amount.
- This endpoint is downstream of a successful payment chain. The order-only Postman requests do not by themselves produce a refundable payment state.

### After-sale request

- Endpoint: `POST /api/orders/after-sales`
- Fields: `mainOrderId`, `subOrderId`, `userId`, `merchantId`, `afterSaleType`, `reason`, `description`, `applyAmount`
- Active after-sale requests block duplicate submissions for the same sub order.

## Postman Assets

- Collection: `docs/postman/cloud-shop.postman_collection.json`
- Local environment: `docs/postman/cloud-shop.local.postman_environment.json`
- Detailed chain audit: `docs/order-chain-audit.md`

Use the collection for chain-level smoke tests. The collection is intentionally aligned to current gateway routes and removes old order endpoints that no longer exist.
- The collection includes both direct-buy and cart-checkout order creation examples.
- Refund creation still requires a paid payment order and is not closed by the order-only requests alone.
