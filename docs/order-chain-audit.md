# Order Chain Audit

Audit date: `2026-04-06`

Scope:
- UniApp frontend order, payment, and after-sale modules
- Backend order and payment controllers/services
- Existing docs and Postman collection

## Closed Contracts

- `POST /api/orders` requires `Idempotency-Key` and `clientOrderId`.
- `GET /api/orders` and `GET /api/orders/{orderId}` now return `OrderSummaryDTO` with `items[]`, `items[].skuSnapshot`, and optional `items[].latestProduct`.
- Payment creation and checkout must go through `/api/payments/orders` and `/api/payments/orders/{paymentNo}/checkout-session`.
- `/api/orders/{orderId}/pay` and `/api/orders/batch/pay` still exist but are compatibility routes only. Current business flow should not rely on them.

## High-Signal Gaps

### 1. Cart page does not use backend cart checkout

- Frontend cart submission loops through cart items and calls direct-buy order creation once per item.
- Backend order service supports `cartId` checkout, but the frontend does not use it.

Files:
- `my-shop-uniapp/src/pages/app/cart/index.vue`
- `my-shop-uniapp/src/api/order.ts`
- `services/order-service/src/main/java/com/cloud/order/dto/CreateMainOrderRequest.java`

Impact:
- Cart aggregation is not closed end-to-end.
- One cart with multiple items becomes multiple main orders.
- Backend cart checkout semantics and cart status transitions are bypassed by the current frontend.

### 2. Frontend order pages still render summary-only data

- Backend order query already returns item snapshots and optional latest product data.
- Current order pages still bind to legacy summary-shaped rows and do not render `items[]`.

Files:
- `my-shop-uniapp/src/pages/app/orders/index.vue`
- `my-shop-uniapp/src/pages/app/orders-manage/index.vue`
- `services/order-service/src/main/java/com/cloud/order/dto/OrderSummaryDTO.java`

Impact:
- Transaction snapshot data is available but invisible to users and merchants.
- The new query contract is only partially consumed by the frontend.

### 3. Frontend order pages still use the legacy `OrderItem` view model for list APIs

- `listOrders()` returns `OrderSummaryDTO` records.
- User and merchant order pages still type rows as `OrderItem[]`.
- The shape overlaps enough to run, but the declared contract is stale.

Files:
- `my-shop-uniapp/src/pages/app/orders/index.vue`
- `my-shop-uniapp/src/pages/app/orders-manage/index.vue`
- `my-shop-uniapp/src/types/domain.ts`

Impact:
- New fields are easy to miss in page code.
- Type-level closure between frontend and backend is incomplete.

### 4. Compatibility pay endpoints are exposed in frontend API but should not be treated as the main path

- Frontend still exports `payOrder()` and `batchPayOrders()`.
- Backend controller still exposes the routes.
- Actual payment chain in the user order page already goes through the payment service APIs instead.

Files:
- `my-shop-uniapp/src/api/order.ts`
- `my-shop-uniapp/src/pages/app/orders/index.vue`
- `services/order-service/src/main/java/com/cloud/order/controller/OrderController.java`

Impact:
- The public API surface suggests two payment paths.
- Documentation must continue to mark `/api/orders/**/pay` as compatibility-only.

### 5. User-side after-sale cancellation capability is narrower than backend capability

- Backend allows `CANCEL` from `APPLIED` and `WAIT_RETURN`.
- The user order page only allows cancel when `afterSaleStatus === "APPLIED"`.

Files:
- `my-shop-uniapp/src/pages/app/orders/index.vue`
- `services/order-service/src/main/java/com/cloud/order/service/impl/OrderServiceImpl.java`

Impact:
- The frontend hides a valid backend transition.
- User-side after-sale flow is not fully closed.

### 6. `shopId` in frontend create-order payload is client-only metadata

- `CreateOrderPayload` requires `shopId`.
- `src/api/order.ts` does not send `shopId` to the backend order endpoint.
- Merchant routing is derived by backend product/cart data instead.

Files:
- `my-shop-uniapp/src/types/domain.ts`
- `my-shop-uniapp/src/api/order.ts`

Impact:
- The payload type implies a backend contract that does not exist.
- This is harmless for runtime but misleading for client developers.

### 7. Refund chain is not closed by order-only docs and Postman requests

- The backend refund endpoint requires a paid payment order.
- The order and after-sale requests in the published collection do not on their own produce a paid payment state.
- `POST /api/payments/callbacks` still exists, but the service treats it as a legacy compatibility path rather than an authoritative mutation lane.

Files:
- `docs/backend-api.md`
- `docs/postman/cloud-shop.postman_collection.json`
- `services/payment-service/src/main/java/com/cloud/payment/controller/PaymentOrderController.java`

Impact:
- The documented endpoint exists, but the published smoke-test sequence does not close the refund path end-to-end.
- Readers can overestimate how executable the refund chain is from repository assets alone.

## Synced Docs In This Round

- `docs/backend-api.md`
- `docs/frontend-api.md`
- `docs/postman/cloud-shop.postman_collection.json`

The docs now reflect:
- required `clientOrderId`
- order query response with `items[]`, `skuSnapshot`, and optional `latestProduct`
- compatibility-only order pay endpoints
- current frontend limitation that cart page still uses direct-buy creation instead of `cartId`

## Suggested Next Closure Work

1. Convert the cart page to call backend cart checkout with `cartId`.
2. Switch order pages from `OrderItem` to `OrderSummaryDTO`.
3. Render `items[]` and snapshot data in user and merchant order pages.
4. Either remove `payOrder()` and `batchPayOrders()` from the frontend API surface or label them as internal compatibility helpers.
5. Expand user-side after-sale cancel UI to cover `WAIT_RETURN` when business wants that path exposed.
