# Cloud Shop API Guide

Updated: 2026-04-22

Source of truth:

- controller mappings under `services/**/src/main/java/**/controller`
- gateway predicates in `services/gateway/src/main/resources/application-route.yml`
- frontend callers under `my-shop-uniapp/src/api`

## Base URLs

- Public local entry: `http://127.0.0.1:18080`
- Auth surface: `/auth/**`, `/oauth2/**`
- Business surface: `/api/**`
- Public checkout page: `GET /api/payment-checkouts/{ticket}`
- External provider callback: `POST /api/v1/payment/alipay/notify`

Raw-response exceptions:

- `GET /api/payment-checkouts/{ticket}` returns HTML instead of the normal `Result<T>` envelope.
- `GET /api/admin/observability/grafana/open` returns a redirect response instead of the normal `Result<T>` envelope.

## Request Trust Model

- Public clients send bearer tokens only. They must not send `X-Internal-*`, `X-Signature`, `X-Timestamp`, or `X-Nonce`.
- `gateway` validates public JWTs and forwards trusted identity through signed `X-Internal-*` headers.
- Downstream services accept two request modes:
  - gateway-forwarded internal headers with HMAC verification
  - direct bearer-token traffic validated by the service resource server
- Non-bearer mutating requests to `/api/**` must provide `X-Signature`, `X-Timestamp`, and `X-Nonce` so `gateway` can reject replay or unsigned traffic.

## Gateway Route Ownership

| Route group | Target service | Notes |
| --- | --- | --- |
| `/auth/**`, `/oauth2/**`, `/.well-known/**` | `auth-service` | Public auth and OAuth2 server routes |
| `/api/users/**`, `/api/addresses/**`, `/api/merchants/**`, `/api/merchant-authentications/**`, `/api/admins/**` | `user-service` | User, merchant, and admin domain routes |
| `/api/products/**`, `/api/categories/**`, `/api/spus/**`, `/api/skus/**` | `product-service` | Product and catalog routes |
| `/api/orders/**`, `/api/users/me/cart`, `/api/users/me/cart/**`, `/api/after-sales/**` | `order-service` | Cart, orders, and after-sale |
| `/api/payment-orders/**`, `/api/payment-refunds/**`, `/api/payment-checkouts/**`, `/api/v1/payment/alipay/**` | `payment-service` | Payment, refund, checkout, callback |
| `/api/search/**`, `/api/shops/**` | `search-service` | Public browse and search |
| `/api/admin/stocks/internal/**` | `stock-service` | Internal stock ledger route |
| `/api/admin/stocks/ledger/**`, `/api/admin/thread-pools/**`, `/api/admin/statistics/**`, `/api/admin/users/**`, `/api/admin/mq/**`, `/api/admin/outbox/**`, `/api/admin/observability/**`, `/api/admin/notifications/**`, `/auth/authorizations/**`, `/auth/cleanups/**`, `/auth/blacklist-entries/**` | `governance-service` | Admin aggregation and governance |
| `/api/admin/governance/**` | `governance-service` | Rewritten to `/internal/governance/**` |

## Business Surface Summary

### Authentication and session

- Public login and registration:
  - `POST /auth/users/register`
  - `GET /oauth2/authorize`
  - `POST /oauth2/token`
  - `GET /auth/oauth2/github/**`
- Current-session operations:
  - `DELETE /auth/sessions`
  - `DELETE /auth/users/{username}/sessions`
  - `GET /auth/tokens/validate`
- Token governance:
  - `/auth/authorizations/**`
  - `/auth/blacklist-entries/**`
  - `/auth/cleanups/**`

### User, merchant, and admin domain

- User self-service: `/api/users/me/**`
- Address book: `/api/users/{userId}/addresses/**`, `/api/addresses/**`
- Merchant domain: `/api/merchants/**`
- Merchant-auth domain: `/api/merchants/{merchantId}/authentication/**`, `/api/merchant-authentications/**`
- Admin accounts: `/api/admins/**`
- Owner or admin checks still happen inside service logic. A path parameter alone is not an authorization grant.

### Catalog and search

- Catalog maintenance: `/api/categories/**`, `/api/spus/**`, `/api/skus`
- Product browse and query: `/api/products`, `/api/spus`
- Public discovery: `/api/search/**`, `/api/shops/**`
- Search routes are the main anonymous browse surface. Product and category management routes remain permissioned even for `GET` requests.

### Cart, order, and after-sale

- Cart: `GET /api/users/me/cart`, `PUT /api/users/me/cart/items`
- Orders: `/api/orders/**`
- After-sale: `/api/after-sales/**`
- `POST /api/orders` currently requires:
  - header `Idempotency-Key`
  - body field `clientOrderId`
  - server-issued `cartId` for cart checkout

### Payment and refund

- Payment orders: `/api/payment-orders/**`
- Refunds: `/api/payment-refunds/**`
- Checkout page: `GET /api/payment-checkouts/{ticket}`
- Frontend flow is `create payment order -> create checkout session -> open checkoutPath -> poll status`.
- `GET /api/payment-checkouts/{ticket}` is an HTML response endpoint and is intentionally not wrapped in `Result<T>`.
- `payment-service` cache is intentionally limited to idempotency, duplicate-result reuse, short-lived non-final status helpers, checkout tickets, and rate limiting.

### Admin and governance

- Admin user management: `/api/admin/users/**`
- Statistics and thread pools: `/api/admin/statistics/**`, `/api/admin/thread-pools/**`
- Notifications: `/api/admin/notifications/**`
- MQ and Outbox governance: `/api/admin/mq/**`, `/api/admin/outbox/**`
- Observability redirects: `/api/admin/observability/**`
- `GET /api/admin/observability/grafana/open` is a redirect response endpoint and is intentionally not wrapped in `Result<T>`.
- Stock ledger: `/api/admin/stocks/ledger/{skuId}`
- Governance compatibility proxy: `/api/admin/governance/**`

## Internal-Only Surfaces

- `/internal/governance/**`
- `/api/admin/stocks/internal/**`
- `/api/admin/thread-pool/internal/**`
- `/api/admin/statistics/internal/**`

These routes are operational or internal and are not part of the normal frontend flow.

## Consistency Notes

- `order-service`, `payment-service`, and `stock-service` use local transactions, `outbox_event`, RocketMQ delivery, and consumer idempotency.
- Order timeout cancellation uses delayed RocketMQ messages.
- `stock-service` reduces hot-row contention through `stock_segment`.
- `search-service` updates Elasticsearch from upstream product, category, and stock signals.

## Exact Endpoint References

- Backend controllers under `services/**/src/main/java/**/controller`
- Gateway routes in `services/gateway/src/main/resources/application-route.yml`
- Postman collection in `docs/postman/cloud-shop.postman_collection.json`
- Related docs:
  - `docs/frontend-api.md`
  - `docs/backend-runtime.md`
