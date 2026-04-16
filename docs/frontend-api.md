# UniApp API Guide

Generated on: 2026-04-02
Source of truth: `my-shop-uniapp/src/api/*.ts`

## Runtime Notes

- Default gateway base URL: `http://127.0.0.1:18080`
- Frontend uses OAuth2 for login and JWT bearer tokens for protected APIs.
- Payment flow is no longer based on direct order pay transitions. The app creates a payment order, creates a checkout session, opens the checkout webview, and polls payment status.
- The cart UI now synchronizes local cart state to the backend cart API and uses backend `cartId` checkout when submitting the cart page.
- The profile page now refreshes and updates data through `/api/user/profile/current` instead of rendering JWT claims only.

## Frontend Chains

### 1. Authentication

Modules:
- `src/api/auth.ts`
- `src/api/auth-tokens.ts`

Primary APIs:
- `POST /auth/users/register`
- `GET /oauth2/authorize`
- `POST /oauth2/token`
- `GET /auth/oauth2/github/login-url`
- `GET /auth/oauth2/github/status`
- `GET /auth/oauth2/github/user-info`
- `DELETE /auth/sessions`
- `DELETE /auth/users/{username}/sessions`
- `GET /auth/tokens/validate`

### 2. Profile And Address

Modules:
- `src/api/user.ts`
- `src/api/address.ts`

Primary APIs:
- `GET /api/user/profile/current`
- `PUT /api/user/profile/current`
- `PUT /api/user/profile/current/password`
- `POST /api/user/profile/current/avatar`
- `GET /api/user/address/list/{userId}`
- `GET /api/user/address/default/{userId}`
- `POST /api/user/address/add/{userId}`
- `PUT /api/user/address/update/{addressId}`
- `DELETE /api/user/address/delete/{addressId}`
- `POST /api/user/address/page`
- `DELETE /api/user/address/deleteBatch`
- `PUT /api/user/address/updateBatch`

Notes:
- The profile page now supports backend-backed refresh, profile update, password change, and avatar upload.
- Avatar upload on the page uses `uni.uploadFile` against `POST /api/user/profile/current/avatar` with the current bearer token.

### 3. Catalog And Search

Modules:
- `src/api/category.ts`
- `src/api/product.ts`
- `src/api/product-catalog.ts`
- `src/api/search-ops.ts`
- `src/api/shop-search.ts`

Primary APIs:
- `GET /api/category`
- `GET /api/category/{id}`
- `GET /api/category/tree`
- `GET /api/category/{id}/children`
- `GET /api/product`
- `GET /api/product/manage`
- `GET /api/product/search`
- `GET /api/product/spu/{spuId}`
- `GET /api/product/spu/category/{categoryId}`
- `GET /api/product/sku/batch`
- `GET /api/search/search`
- `GET /api/search/smart-search`
- `GET /api/search/basic`
- `POST /api/search/complex-search`
- `POST /api/search/filter`
- `POST /api/search/filters`
- `GET /api/search/recommended`
- `GET /api/search/new`
- `GET /api/search/hot`
- `GET /api/search/hot/today`
- `GET /api/search/filter/category/{categoryId}`
- `GET /api/search/filter/brand/{brandId}`
- `GET /api/search/filter/price`
- `GET /api/search/filter/shop/{shopId}`
- `GET /api/search/filter/combined`
- `GET /api/search/shops/recommended`
- `GET /api/search/shops/suggestions`
- `GET /api/search/shops/hot-shops`
- `GET /api/search/shops/{shopId}`
- `GET /api/search/shops/by-location`
- `POST /api/search/shops/complex-search`
- `POST /api/search/shops/filters`

Notes:
- Empty-keyword market landing now prefers `GET /api/search/hot/today`.
- Search APIs are split between lightweight aliases and rich request-body operations.
- Merchant catalog management now uses `GET /api/product/manage` so unpublished products remain visible to the merchant owner and can be published again.
- `src/api/product-catalog.ts` is the frontend entry for SPU and SKU maintenance APIs.
- `src/api/shop-search.ts` is the frontend entry for shop discovery and recommendation APIs.
- The operations workspace now imports and executes its token, category, catalog, search, and thread-pool helpers through the current frontend API modules.

### 4. Order And After-Sale

Module:
- `src/api/order.ts`

Primary APIs:
- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{orderId}`
- `POST /api/orders/{orderId}/cancel`
- `POST /api/orders/{orderId}/ship`
- `POST /api/orders/{orderId}/complete`
- `POST /api/orders/batch/cancel`
- `POST /api/orders/batch/ship`
- `POST /api/orders/batch/complete`
- `POST /api/orders/after-sales`
- `POST /api/orders/after-sales/{afterSaleId}/actions/{action}`

Notes:
- Real payment starts from the payment module after order creation.
- The frontend no longer exports direct order pay helpers. Current payment flow must go through the payment module.
- `createOrder(payload)` in `src/api/order.ts` currently implements direct-buy only. It auto-generates `clientOrderId` when the caller does not provide one.
- `createCartOrder(payload)` now implements cart checkout through backend `cartId`.
- `listOrders` and `getOrderById` now return `items[]`. Each item includes immutable `skuSnapshot` data and an optional `latestProduct` view when the backend can still resolve the SKU.
- User and merchant order pages now render `items[]` with snapshot-first product details.

### 5. Payment And Checkout

Module:
- `src/api/payment.ts`

Primary APIs:
- `POST /api/payments/orders`
- `GET /api/payments/orders/{paymentNo}`
- `GET /api/payments/orders/by-order`
- `POST /api/payments/orders/{paymentNo}/checkout-session`
- `GET /api/payments/orders/{paymentNo}/status`
- `GET /api/payments/refunds/{refundNo}`
- `POST /api/payments/refunds`
- `GET /api/payments/checkout/{ticket}`

Notes:
- Checkout opens the webview page with a ticketed checkout URL.
- The payment page and webview both support auto-poll for final payment status.
- Refund query is owner/admin protected and is wired into the order page.
- Refund creation is not exposed from the normal frontend payment flow yet. It remains a downstream backend capability after payment success and after-sale progression.

### 6. Merchant, Admin, Notifications, Statistics, Operations

Modules:
- `src/api/merchant.ts`
- `src/api/merchant-auth.ts`
- `src/api/admin.ts`
- `src/api/user-management.ts`
- `src/api/notification.ts`
- `src/api/statistics.ts`
- `src/api/thread-pool.ts`
- `src/api/stock.ts`

Primary APIs:
- Merchant management: `/api/merchant/**`
- Merchant auth: `/api/merchant/auth/**`
- Admin: `/api/admin/**`
- User management: `/api/admin/query/users/**`, `/api/admin/manage/users/**`
- Notifications: `/api/app/user/notification/**`
- Statistics: `/api/admin/statistics/**`
- Thread pool: `/api/admin/thread-pool/**`
- MQ governance: `/api/admin/mq/**`
- Outbox governance: `/api/admin/outbox/**`
- Observability entry: `/api/admin/observability/**`
- Stock: `/api/stocks/**`
- Auth token ops: `/auth/tokens/**`

Notes:
- Stock ledger view is admin-only in current backend policy.
- Stock mutation APIs are internal-scope APIs and are not part of normal frontend user flows.
- Stock pre-check API (`POST /api/stocks/pre-check`) is available for batch stock validation before order creation.
- Stock ledger responses expose integer `status` values from the backend. The stock page now renders `1` as `Active` and derives low-stock warnings from `availableQty` and `alertThreshold` instead of assuming string enums.
- Admin workspace currently consumes `/api/admin`, `/api/admin/query/users/search`, `/api/merchant/auth/list`, `/api/merchant/auth/review/{merchantId}`, `/api/admin/statistics/overview`, and `/api/admin/thread-pool/info`.
- Admin workspace notification operations should continue using `/api/app/user/notification/**`, which is now governance-owned behind gateway routing.
- App shell navigation now hides `Payments` from `MERCHANT` and hides `Ops` from non-admin users because the current backend access policy only closes those pages for owner/admin payment reads and admin operational APIs.
- Merchant-facing quick actions now avoid linking directly to the standalone payments page, and home quick links only surface `Payments` for roles that can complete the current payment-query flow.
- Merchant review actions in the admin UI are now unified on `/api/merchant/auth/review/{merchantId}`. The merchant list is read-only for audit status and no longer calls `/api/merchant/{id}/approve|reject`.
- Admin workspace merchant list and merchant-auth review queue now align with backend access by allowing pure `ADMIN` accounts to read `/api/merchant` and to use `/api/merchant/auth/list|review/*`.
- Token management utilities exposed in `src/api/auth-tokens.ts` are admin-only operational tools rather than normal user flows.
- Operations workspace now closes the current admin toolchain for `/auth/tokens/**`, `/api/category/**`, `/api/product/spu/**`, `/api/search/**`, `/api/search/shops/**`, `/api/admin/thread-pool/**`, `/api/admin/statistics/**`, `/api/admin/mq/**`, `/api/admin/outbox/**`, `/api/admin/observability/**`, and payment admin helpers already exposed in the frontend API layer.

## Request And Behavior Notes

### Create order payload

`createOrder(payload, idempotencyKey)` must include:
- `skuId` for direct buy
- Receiver fields: `receiverName`, `receiverPhone`, `receiverAddress`
- `spuId` together with `skuId` for direct buy

Notes:
- `createCartOrder(payload)` uses backend cart checkout with `cartId`.
- The cart store synchronizes local cart items to `/api/cart/sync` before cart checkout submission.

### Order summary payload

`OrderSummaryDTO` now includes:
- Summary fields: `id`, `orderNo`, `userId`, `subOrderId`, `subOrderNo`, `merchantId`, `afterSaleId`, `afterSaleNo`, `afterSaleType`, `refundNo`, `totalAmount`, `payAmount`, `status`, `afterSaleStatus`, `createdAt`
- `items[]` with `id`, `subOrderId`, `spuId`, `skuId`, `skuCode`, `skuName`, `quantity`, `unitPrice`, `totalPrice`
- `items[].skuSnapshot` as parsed immutable order-time product snapshot data
- `items[].latestProduct` as optional live product projection for display enhancement

### Create payment order payload

`createPaymentOrder(payload)` sends:
- `paymentNo`
- `mainOrderNo`
- `subOrderNo`
- `userId`
- `amount`
- `channel`
- `idempotencyKey`

Backend now binds this request to the authenticated owner for regular users.

### Address page expectations

The address page now supports:
- create
- update
- delete
- set default address through the default flag in the payload

### Admin console expectations

The admin UI now depends on:
- merchant list query fields `status` and `auditStatus`
- user search fields `username`, `email`, `phone`, `nickname`, `status`, `roleCode`
- admin password reset returning a generated password string

## Postman Alignment

The backend reference is maintained in `docs/backend-api.md`.
The Postman collection under `docs/postman/cloud-shop.postman_collection.json` follows the same chain order used here.
Detailed chain audit findings are maintained in `docs/order-chain-audit.md`.
Broader module audit findings are maintained in `docs/full-project-audit.md`.
