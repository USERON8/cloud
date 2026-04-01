# Gateway
Version: 1.1.0

Unified entry gateway responsible for routing, public-token validation, internal identity forwarding, rate limiting, and shared CORS handling.

- Service name: `gateway`
- Port: `8080`
- Primary dependencies: Nacos, Redis, `auth-service`

## Responsibilities

- Provides the single public HTTP entrance for browser and app clients.
- Routes requests to the correct downstream service.
- Performs JWT validation on public protected routes and forwards trusted internal identity headers to downstream services.
- Applies shared CORS handling, trace propagation, and response-header normalization.
- Carries search fallback cache logic for degraded scenarios.
- Owns gateway-level degradation endpoints for payment and user downstream failures.

## Current Route Prefixes

- `/auth/**`, `/oauth2/**`, `/.well-known/**` -> `auth-service`
- `/api/manage/users/**`, `/api/query/users/**`, `/api/user/**`, `/api/merchant/**`, `/api/admin/**`, `/api/statistics/**` -> `user-service`
- `/api/product/**`, `/api/category/**` -> `product-service`
- `/api/orders/**` -> `order-service`
- `/api/payments/**`, `/api/v1/payment/alipay/**` -> `payment-service`
- `/api/stocks/**` -> `stock-service`
- `/api/search/**` -> `search-service`

## Identity And Downstream Trust

- `JwtTokenForwardFilter` signs and forwards gateway-trusted identity headers.
- Downstream services restore `Authentication` through `GatewayInternalAuthenticationFilter`.
- Shared secret is configured through `GATEWAY_INTERNAL_IDENTITY_SECRET` and falls back to `GATEWAY_SIGNATURE_SECRET` in local dev.
- Public JWT validation and internal identity validation are intentionally separated concerns.

## Rate Limiting And Fallback Cache

- Sentinel is enabled at both route level and user-header level.
- Route rules protect main API groups by default, with a dedicated lower threshold for search traffic.
- User-level rules parse `X-User-Id` and apply per-API limits to reduce single-user abuse.
- Search fallback cache: `SearchFallbackCache` stores fallback responses with route- and parameter-aware TTLs.
- Cache keys are normalized from route-effective params only, so irrelevant query params do not fragment degraded-cache hit rate.
- Configuration prefix: `app.search.fallback.cache`

Gateway degradation endpoints:

- `/gateway/fallback/search`
- `/gateway/fallback/payment`
- `/gateway/fallback/user`

## Operational Notes

- The gateway is the place where public path conventions are effectively defined.
- If downstream service endpoints change but gateway route definitions do not, external behavior breaks even if the target service still works internally.
- The startup scripts and Nginx host/cluster linked flows assume gateway remains the public entry service.

## CORS Notes

The gateway enables `DedupeResponseHeader` to prevent duplicate `Access-Control-Allow-Origin` headers from being rejected by browsers.

## Known Findings In This Sync

- JWT blacklist Redis failures now reject tokens by default in gateway fail-closed mode.
- Gateway JWT validation now assumes short-lived access tokens from `auth-service`; otherwise a strict blacklist outage policy becomes operationally expensive.
- Gateway fallback cache should be treated separately from business caches because its role is degradation control, not domain read acceleration.
- Payment and user downstream fallbacks now return explicit degraded responses instead of generic proxy failures.
- Trace ids are included in blocked and fallback responses so gateway degradation remains debuggable.

## Local Run

```bash
mvn -pl gateway spring-boot:run
```
