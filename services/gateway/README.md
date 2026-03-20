# Gateway
Version: 1.1.0

Unified entry gateway responsible for routing, JWT resource server validation, and shared CORS handling.

- Service name: `gateway`
- Port: `8080`
- Dependencies: Nacos, Redis, `auth-service`

## Current Route Prefixes

- `/auth/**`, `/oauth2/**`, `/.well-known/**` -> `auth-service`
- `/api/manage/users/**`, `/api/query/users/**`, `/api/user/**`, `/api/merchant/**`, `/api/admin/**`, `/api/statistics/**` -> `user-service`
- `/api/product/**`, `/api/category/**` -> `product-service`
- `/api/orders/**` -> `order-service`
- `/api/payments/**`, `/api/v1/payment/alipay/**` -> `payment-service`
- `/api/stocks/**` -> `stock-service`
- `/api/search/**` -> `search-service`

## CORS Notes

The gateway enables `DedupeResponseHeader` to prevent duplicate `Access-Control-Allow-Origin` headers from being rejected by browsers.

## Rate Limiting And Cache

- Sentinel is enabled and protects order routes by default (`80 QPS / 1s`, overrideable through environment variables)
- Search fallback cache: `SearchFallbackCache` stores fallback responses with route- and parameter-aware TTLs
  - Configuration prefix: `app.search.fallback.cache`

## Local Run

```bash
mvn -pl gateway spring-boot:run
```
