# System Maintenance Scope

Updated: 2026-07-02

This document freezes the intended scope of Cloud Shop as a learning and portfolio system.
The goal is maintainability, not adding more infrastructure.

## Scope Rule

Do not add new platform components unless an existing business flow cannot be made reliable with
the current stack. The current stack is enough:

- `gateway` for public entry and trust restoration
- Dubbo through `common-api` for normal service-to-service calls
- RocketMQ + `outbox_event` for cross-service eventual consistency
- Redis for bounded caches, idempotency, rate limiting, and hot search data
- Elasticsearch for search read models
- `governance-service` for operations, compensation, and observability entrypoints

## Core State Machines

### Order

Source of truth: `order-service`.

| State | Meaning | Allowed next states | Terminal |
| --- | --- | --- | --- |
| `PENDING_PAYMENT` | Main order is created and waiting for payment | `PAID`, `CANCELLED` | No |
| `PAID` | Payment is confirmed and stock can be finalized or order can be shipped | `SHIPPED` | No |
| `SHIPPED` | Merchant has shipped the order | `COMPLETED` | No |
| `COMPLETED` | User receipt/order completion is finished | None | Yes |
| `CANCELLED` | Order is cancelled before completion | None | Yes |

Timeout cancellation must only target sub-orders that are still waiting for payment or stock
reservation. Before cancelling, `order-service` must ask `payment-service` whether a payment is
already confirmed.

### Stock

Source of truth: `stock-service`.

| Transition | Trigger | Expected behavior |
| --- | --- | --- |
| reserve | order creation event | pre-check availability, reserve each SKU, publish stock-reserved or stock-freeze-failed |
| confirm | payment success/order confirmation | convert reserved stock into sold stock |
| release | order cancellation, timeout, or payment failure | return reserved stock to available inventory |

Stock operations must stay idempotent at the command/event level. Redis pre-checks are only fast
guards; MySQL stock ledger remains authoritative.

### Payment

Source of truth: `payment-service`.

| State | Meaning | Allowed next states | Terminal |
| --- | --- | --- | --- |
| `CREATED` | Payment order exists and awaits provider result | `PAID`, `FAILED` | No |
| `PAID` | Provider callback or query confirms payment | None | Yes |
| `FAILED` | Provider callback or query confirms failure | None | Yes |

Provider callback handling must reject transitions from terminal states. Payment cache may store
idempotency data, checkout tickets, and short-lived status helpers, but final payment truth stays
in MySQL.

### Refund

Source of truth: `order-service` for after-sale state, `payment-service` for provider refund truth.

| State | Meaning | Allowed next states |
| --- | --- | --- |
| `PENDING_AUDIT` | User submitted after-sale request | `AUDIT_PASSED`, `AUDIT_REJECTED`, `CANCELLED` |
| `AUDIT_PASSED` | Merchant approved after-sale | `RETURNING`, `REFUNDING`, `CANCELLED` |
| `RETURNING` | User is returning goods | `GOODS_RECEIVED`, `CLOSED` |
| `GOODS_RECEIVED` | Merchant confirmed returned goods | `REFUNDING` |
| `REFUNDING` | Provider refund is in progress | `COMPLETED`, `CLOSED` |
| `AUDIT_REJECTED`, `COMPLETED`, `CANCELLED`, `CLOSED` | Finished states | None |

Refund provider callbacks and order after-sale updates must be reconciled through compensation when
one side succeeds and the other side fails.

## Failure Handling Table

| Chain | Failure point | Required behavior | Owner |
| --- | --- | --- | --- |
| order creation -> stock reserve | stock pre-check fails | publish stock-freeze-failed, keep order cancellable | `stock-service` + `order-service` |
| order creation -> stock reserve | reserved event cannot be queued | rollback local stock transaction and retry through MQ/outbox | `stock-service` |
| stock reserved -> order update | order consumer fails remotely | retry remote/system failures; business-invalid events are ACKed and recorded | `order-service` |
| payment success -> order paid | order update fails after provider confirms payment | retry through outbox; expose compensation entry in governance | `payment-service` + `order-service` |
| payment success -> stock confirm | stock confirm fails | retry stock command; do not mark inventory final from cache | `stock-service` |
| timeout cancellation | payment already confirmed remotely | skip cancellation and keep order for payment-success reconciliation | `order-service` |
| timeout cancellation | payment lookup unavailable | retry later; do not cancel blindly | `order-service` |
| refund success -> after-sale completed | order update fails | retry through outbox and expose compensation entry | `payment-service` + `order-service` |
| product/category/stock change -> search index | index update fails | retry event or scheduled rebuild; search remains read-model only | `search-service` |

## Common Module Boundaries

`common-*` modules are closed for speculative abstractions. New shared code must satisfy both:

1. At least two services need the same behavior now.
2. The behavior has no product-specific workflow, persistence rule, or controller behavior.

When in doubt, keep code inside the owning service. Prefer duplication over a shared abstraction
that hides business ownership.

## Governance Boundary

`governance-service` is for operations, not for normal business workflows.

Allowed:

- MQ/outbox inspection, requeue, and compensation triggers
- token governance aggregation
- observability redirects
- thread-pool/statistics/admin aggregation reads
- stock ledger/admin reads through explicit contracts
- admin-user operational views through user-domain Dubbo contracts

Not allowed:

- product, order, payment, stock, user, or merchant CRUD as the primary business owner
- exposing a parallel `/api/admins/**` administrator-account proxy
- replacing domain services with controller-to-controller proxy routes
- storing business truth that belongs to a domain service

## Testing Contract

Minimum test coverage for future changes:

- State transition changes require focused unit tests for allowed, rejected, and terminal states.
- Cross-service chains require tests for success, business failure, remote/system failure, and idempotent replay.
- Cache changes require post-commit behavior tests when the cache is invalidated after transaction commit.
- Governance changes require tests that prove they aggregate or trigger compensation without taking ownership of business truth.

Before adding new features, run:

```powershell
mvn -pl services/order-service,services/payment-service,services/stock-service,services/search-service,services/user-service,services/product-service -am test
```
