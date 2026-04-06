# Order Chain Audit

Audit date: `2026-04-06`

Scope:
- UniApp frontend order, payment, and after-sale modules
- Backend order and payment controllers/services
- Existing docs and Postman collection

## Closed Contracts

- `GET /api/cart` returns the current user's active cart and creates one when missing.
- `POST /api/cart/sync` replaces the current active cart with a full snapshot and returns the updated cart, including `cartId`.
- The cart page now synchronizes local cart state to `/api/cart/sync` and submits cart checkout through backend `cartId`.
- `POST /api/orders` requires `Idempotency-Key` and `clientOrderId`.
- `GET /api/orders` and `GET /api/orders/{orderId}` now return `OrderSummaryDTO` with `items[]`, `items[].skuSnapshot`, and optional `items[].latestProduct`.
- User and merchant order pages now consume `OrderSummaryDTO` and render item-level snapshot data.
- Payment creation and checkout must go through `/api/payments/orders` and `/api/payments/orders/{paymentNo}/checkout-session`.
- `/api/orders/{orderId}/pay` and `/api/orders/batch/pay` still exist but are compatibility routes only. Current business flow should not rely on them.

## High-Signal Gaps

### 1. Refund chain is not closed by order-only docs and Postman requests

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
- the new cart lookup and full-sync endpoints
- required `clientOrderId`
- order query response with `items[]`, `skuSnapshot`, and optional `latestProduct`
- compatibility-only order pay endpoints
- the current frontend cart checkout and order summary consumption behavior

## Suggested Next Closure Work

1. Add a fully executable refund smoke chain once payment callback simulation is available.
