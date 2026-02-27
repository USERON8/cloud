# Implementation Progress (2026-02-27)

## Completed

### Batch A
- Enforced guardrails to prevent `testenv` security bypass in protected profiles (`prod`, `staging`):
  - `common-module` base resource security config
  - `gateway` resource security config
  - `auth-service` testenv filter chain
- Fixed `payment-service` Alipay client initialization to match current SDK API.
- Removed sensitive fallback defaults and required explicit env injection for:
  - auth client secrets / JWT keypair generation
  - docker compose credentials (MySQL/Nacos/MinIO/Grafana/Logstash)

### Batch B
- Upgraded idempotency behavior in `MessageIdempotencyService`:
  - processing state + success state
  - added `markSuccess(namespace, eventId)`
- Refactored trading consumers to:
  - mark success after terminal handling
  - remove `release()` in exception paths to avoid duplicate side effects after partial success
- Updated:
  - `order-service` consumers (`OrderMessageConsumer`, `RefundNotificationConsumer`)
  - `payment-service` consumer (`PaymentMessageConsumer`)
  - `stock-service` consumer (`StockMessageConsumer`)

## Verification
- Command executed: `mvn -T 1C -DskipITs test`
- Result: `BUILD SUCCESS` across all modules.

## Completed (Batch C - backend side)
- Actuator exposure is now configurable and disabled by default:
  - `app.security.public-actuator-enabled=false`
- Common CORS policy switched from wildcard to configurable allowlist patterns.
- Gateway blacklist validation changed from fail-open to fail-closed on Redis/validation errors.

## Completed (Batch C - frontend + auth session hardening)
- Migrated frontend session handling away from `localStorage` token persistence:
  - token data is now in-memory only (`my-shop-web/src/auth/session.ts`)
  - only non-sensitive user profile remains in `localStorage`
- Enabled cross-site credential flow for SPA requests:
  - axios clients now use `withCredentials: true` (`my-shop-web/src/api/http.ts`)
- Added silent session restore path for protected routes:
  - router guard calls `ensureAuthenticatedSession()` before redirect (`my-shop-web/src/router/index.ts`)
- Added refresh-token HttpOnly cookie support in `auth-service`:
  - set cookie on register/login/refresh
  - refresh endpoint can read refresh token from cookie when request param is absent
  - logout clears refresh cookie
  - cookie attributes configurable via `app.security.session-cookie.*`
- Unified auth-service CORS allowlist to configurable origin patterns:
  - `app.security.cors.allowed-origin-patterns`

## Additional Verification (Batch C)
- Command executed: `mvn -T 1C -DskipITs -pl auth-service -am test`
- Result: `BUILD SUCCESS`
- Command executed: `pnpm build` (in `my-shop-web`)
- Result: `vue-tsc` + `vite build` success
