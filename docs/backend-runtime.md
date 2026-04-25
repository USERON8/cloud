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
- `BusinessException`: compatibility type aligned with business exception semantics

Shared handling:

- `common-web`: `GlobalExceptionHandler`
- `common-security`: `GlobalPermissionExceptionHandler`
- service fallback: `ServiceExceptionAspect`

Boundary rules:

- controllers do not swallow exceptions
- services catch only when translating semantics
- DAO and infrastructure code do not catch by default
- MQ consumers ACK business errors and retry remote/system failures

## Related Documents

- `docs/backend-api.md`
- `docs/frontend-api.md`
- `docs/dev-startup.md`
- `docs/observability-stack.md`
- `docs/TEST_SCRIPT_INDEX.md`
