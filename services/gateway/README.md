# Gateway
Version: 1.1.0

Unified public entry for routing, JWT validation, internal identity forwarding, rate limiting, and degraded fallback responses.

- Service name: `gateway`
- Port: `8080`
- Primary dependencies: Nacos, Redis, `auth-service`

## Responsibilities

- Defines the public route map for browser and app traffic.
- Validates public JWTs and restores trusted downstream identity.
- Verifies gateway-signature headers for non-bearer mutating `/api/**` traffic.
- Applies route-level and user-level rate limiting.
- Hosts degraded fallback endpoints for search, payment, and user routes.

## Current Route Map

- `/auth/**`, `/oauth2/**`, `/.well-known/**` -> `auth-service`
- `/api/users/**`, `/api/addresses/**`, `/api/merchants/**`, `/api/merchant-authentications/**`, `/api/admins/**` -> `user-service`
- `/api/products/**`, `/api/categories/**`, `/api/spus/**`, `/api/skus/**` -> `product-service`
- `/api/orders/**`, `/api/users/me/cart`, `/api/users/me/cart/**`, `/api/after-sales/**` -> `order-service`
- `/api/payment-orders/**`, `/api/payment-refunds/**`, `/api/payment-checkouts/**`, `/api/v1/payment/alipay/**` -> `payment-service`
- `/api/search/**`, `/api/shops/**` -> `search-service`
- `/api/admin/stocks/internal/**` -> `stock-service`
- `/api/admin/stocks/ledger/**`, `/api/admin/governance/**`, `/api/admin/thread-pools/**`, `/api/admin/statistics/**`, `/api/admin/users/**`, `/api/admin/mq/**`, `/api/admin/outbox/**`, `/api/admin/observability/**`, `/api/admin/notifications/**`, `/auth/authorizations/**`, `/auth/cleanups/**`, `/auth/blacklist-entries/**` -> `governance-service`

## Runtime Notes

- Public JWT validation and downstream HMAC trust restoration are separate concerns and both live here.
- Search uses a dedicated fallback cache path through `SearchFallbackCache`.
- Payment and user fallbacks are exposed at:
  - `/gateway/fallback/payment`
  - `/gateway/fallback/user`
- Search fallback is exposed at:
  - `/gateway/fallback/search`
- If a downstream route changes, `application-route.yml`, `docs/backend-api.md`, and the corresponding service README should be updated together.

## Local Run

```bash
mvn -pl gateway spring-boot:run
```
