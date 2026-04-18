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
- Legacy order pay compatibility routes have been removed. Payment confirmation must come from the payment chain.

## Environment Preconditions

- The payment callback chain depends on environment injection through `.env`-backed runtime configuration.
- Refund execution is therefore treated as an environment prerequisite instead of a repository contract gap.
- No smoke or performance test sequence is required in this audit round.

## Synced Docs In This Round

- `docs/backend-api.md`
- `docs/frontend-api.md`
- `docs/postman/cloud-shop.postman_collection.json`

The docs now reflect:
- the new cart lookup and full-sync endpoints
- required `clientOrderId`
- order query response with `items[]`, `skuSnapshot`, and optional `latestProduct`
- removed compatibility-only order pay endpoints
- the current frontend cart checkout and order summary consumption behavior

## Suggested Next Closure Work

1. Keep the deployment/runtime environment aligned with the documented payment callback configuration.
