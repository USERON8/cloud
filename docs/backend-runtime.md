# Backend Runtime Guide

Updated: 2026-04-22

This document is the compact runtime guide for backend boundaries, consistency, cache behavior, and exception handling.

## Core Rules

- Public HTTP traffic enters through `gateway`.
- Internal business-to-business calls prefer Dubbo RPC.
- Cross-service consistency uses local transactions, `outbox_event`, RocketMQ delivery, and idempotent consumers.
- Business reads use Cache-Aside with post-commit eviction.
- Controllers stay free of `try-catch`; shared global handlers shape the final response.

## Service Boundary Summary

| Area | Current owner | Notes |
| --- | --- | --- |
| Public auth and OAuth2 | `auth-service` via `gateway` | `/auth/**`, `/oauth2/**` |
| Public business APIs | domain services via `gateway` | `/api/**` |
| Admin governance aggregation | `governance-service` | statistics, thread pools, MQ, Outbox, observability, token governance |
| Merchant and merchant-auth admin surfaces | `user-service` | still intentionally left in the business domain |
| Internal identity trust | `gateway` + shared security layer | `gateway` signs `X-Internal-*`, downstream services verify HMAC |

## Shared Module Boundaries

Shared modules should stay boring and dependency-light. If code needs business data access,
service orchestration, or a product-specific rule, keep it inside the owning service instead
of moving it to `common-*`.

| Module | Owns | Must not own |
| --- | --- | --- |
| `common-api` | Dubbo contracts and RPC-facing request/response types | HTTP controllers, service implementations, persistence logic |
| `common-domain` | Shared DTO/VO/entity base types used across service boundaries | Business workflows, repository code, cache or MQ integration |
| `common-core` | Result wrappers, exceptions, small stateless utilities, context helpers | Spring web filters, database access, service-specific validation |
| `common-web` | Web exception handling, web helpers, transaction commit support | Domain rules, direct RPC clients, service orchestration |
| `common-db` | Database, Redis, ID, MyBatis, and cache infrastructure helpers | Domain-specific SQL, service tables, controller behavior |
| `common-security` | Authentication helpers, internal request verification, permission handling | Public route ownership, auth-service token issuance logic |
| `common-messaging` | Outbox, MQ consumer base classes, idempotency helpers | Message payload business decisions or service-specific compensation |
| `common-async` | Generic async execution infrastructure | Business retries, domain scheduling rules |
| `common-minio` | Object storage client wiring and reusable file helpers | Merchant/user/product upload policies |
| `common-log` | Logging integration only | Audit business semantics |

Before adding a new shared helper, check whether at least two services need the same behavior
now. Do not promote code into `common-*` only because it might be reused later.

## Request And Trust Model

- Public clients send bearer tokens only.
- `gateway` validates public JWTs.
- `gateway` forwards trusted internal identity through signed `X-Internal-*` headers.
- Downstream services accept:
  - gateway-signed internal headers
  - direct bearer-token traffic when intentionally invoked that way
- Non-bearer mutating `/api/**` traffic still requires gateway signature headers.

## Consistency Model

- Each service commits its own database change first.
- Outbound intent is persisted in `outbox_event` inside the same local transaction.
- MQ delivery happens after commit.
- Consumers must be replay-safe.
- Delayed messages are only sent after the local transaction commits.
- Compensation is explicit and domain-specific.

Current main chains:

- order creation -> stock reserve/confirm/release through MQ commands
- payment success -> outbox relay
- refund completion -> outbox relay
- order timeout cancellation -> delayed RocketMQ
- search freshness -> upstream event or scheduled rebuild

Main relay classes:

- `OrderOutboxRelay`
- `PaymentOutboxRelay`
- `StockOutboxRelay`

## Cache Rules

- Database remains the source of truth.
- Write path is database first, then cache eviction after commit.
- Redis-backed business reads default to delayed double delete.
- Hot keys may use shorter TTLs or jitter to reduce avalanche risk.
- Do not cache money or terminal payment state.

Service summary:

| Service | Current cache shape |
| --- | --- |
| `product-service` | hot detail uses local L1 + Redis; category and shop paths use Redis |
| `stock-service` | Redis summary cache + Lua pre-check |
| `user-service` | Redis single-level cache |
| `order-service` | Redis cache for completed order aggregates only |
| `search-service` | Redis hot-data and query-result cache |
| `auth-service` | Redis auth and blacklist data |
| `payment-service` | Redis only for idempotency, ticket, short-lived status, and rate limit |

## Exception Rules

- `BizException`: business rule or invalid-state failure
- `SystemException`: database, transaction, or infrastructure failure
- `RemoteException`: RPC, MQ, or remote dependency failure

Shared handling:

- `common-web`: `GlobalExceptionHandler`
- `common-security`: `GlobalPermissionExceptionHandler`
- service fallback: `ServiceExceptionAspect`

Boundary rules:

- controllers do not swallow exceptions
- services catch only when translating semantics
- DAO and infrastructure code do not catch by default
- MQ consumers ACK business errors and retry remote/system failures

## Style And Maintenance Rules

- Do not reintroduce Spotless or another compile-blocking formatter without a separate formatting-only change.
- `.editorconfig` is the source for basic whitespace rules: Java/YAML/JSON use 2 spaces; XML and Maven POM keep 4 spaces.
- Normal `/api/**` controller methods return `Result<T>` unless listed as a raw-response exception in `docs/backend-api.md`.
- Controller code handles HTTP parameters, permission annotations, response wrapping, and orchestration only.
- Business state checks belong in service or support classes.
- Service-to-service synchronous calls prefer `common-api` Dubbo contracts.
- Do not keep two HTTP routes for the same capability unless the comment and docs state the compatibility reason and removal condition.
- Service README files should stay short: responsibility, public surface, runtime notes, local run command.

## Test Priorities

When tightening this repository, add tests in this order:

1. Shared behavior in `common-core`, `common-security`, `common-db`, and `common-messaging`.
2. Cross-service contracts in `common-api` and event payload handling.
3. High-change business services with low coverage: `user-service`, `product-service`, `search-service`, `stock-service`, and `auth-service`.
4. Gateway route and trust-boundary behavior whenever public routing or identity propagation changes.

Recommended local checks:

1. `powershell -ExecutionPolicy Bypass -File scripts/tools/check-api-contract.ps1 -Root .`
2. `mvn -pl <changed-module> -am test`
3. `mvn -DskipTests compile`
4. For gateway route changes, run `GatewayRouteDefinitionTest`.

## Related Documents

- `docs/backend-api.md`
- `docs/frontend-api.md`
- `docs/dev-startup.md`
- `docs/observability-stack.md`
- `docs/TEST_SCRIPT_INDEX.md`
