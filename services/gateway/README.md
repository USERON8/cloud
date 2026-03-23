# Gateway
Version: 1.1.0

Unified entry gateway responsible for routing, JWT resource server validation, and shared CORS handling.

- Service name: `gateway`
- Port: `8080`
- Primary dependencies: Nacos, Redis, `auth-service`

## Responsibilities

- Provides the single public HTTP entrance for browser and app clients.
- Routes requests to the correct downstream service.
- Performs JWT resource-server validation on protected routes.
- Applies shared CORS handling and response-header normalization.
- Carries search fallback cache logic for degraded scenarios.

## Current Route Prefixes

- `/auth/**`, `/oauth2/**`, `/.well-known/**` -> `auth-service`
- `/api/manage/users/**`, `/api/query/users/**`, `/api/user/**`, `/api/merchant/**`, `/api/admin/**`, `/api/statistics/**` -> `user-service`
- `/api/product/**`, `/api/category/**` -> `product-service`
- `/api/orders/**` -> `order-service`
- `/api/payments/**`, `/api/v1/payment/alipay/**` -> `payment-service`
- `/api/stocks/**` -> `stock-service`
- `/api/search/**` -> `search-service`

## Operational Notes

- The gateway is the place where public path conventions are effectively defined.
- If downstream service endpoints change but gateway route definitions do not, external behavior breaks even if the target service still works internally.
- The startup scripts and Nginx host/cluster linked flows assume gateway remains the public entry service.

## CORS Notes

The gateway enables `DedupeResponseHeader` to prevent duplicate `Access-Control-Allow-Origin` headers from being rejected by browsers.

## Rate Limiting And Fallback Cache

- Sentinel is enabled and protects order routes by default (`80 QPS / 1s`, overrideable through environment variables)
- Search fallback cache: `SearchFallbackCache` stores fallback responses with route- and parameter-aware TTLs
  - Configuration prefix: `app.search.fallback.cache`

## Known Findings In This Sync

- This service was not code-refactored in the current round.
- The README now makes route ownership and gateway-specific fallback behavior explicit.
- Gateway fallback cache should be treated separately from business caches because its role is degradation control, not domain read acceleration.

## Local Run

```bash
mvn -pl gateway spring-boot:run
```
