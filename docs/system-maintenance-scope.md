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

Current runtime state names are the string states used by `OrderServiceImpl`; older enum names in
legacy helper classes are not the authority for new changes.

| State | Meaning | Allowed next states | Terminal |
| --- | --- | --- | --- |
| `CREATED` | Main/sub order is created and waiting for stock reservation or payment reconciliation | `STOCK_RESERVED`, `PAID`, `CANCELLED`, `CLOSED` | No |
| `STOCK_RESERVED` | Stock has been frozen and the order is waiting for payment | `PAID`, `CANCELLED`, `CLOSED` | No |
| `PAID` | Payment is confirmed and stock can be finalized or order can be shipped | `SHIPPED`, `CLOSED` | No |
| `SHIPPED` | Merchant has shipped the order | `DONE`, `CLOSED` | No |
| `DONE` | User receipt/order completion is finished | None | Yes |
| `CANCELLED` | Order is cancelled before completion | None | Yes |
| `CLOSED` | Order is force-closed or compensated after an abnormal path | None | Yes |

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
| `APPLIED` | User submitted after-sale request | `AUDITING`, `CANCELLED` |
| `AUDITING` | Merchant/system is auditing the request | `APPROVED`, `REJECTED`, `CLOSED` |
| `APPROVED` | Merchant approved after-sale | `WAIT_RETURN`, `REFUNDING`, `CLOSED` |
| `WAIT_RETURN` | User is returning goods | `RETURNED`, `CANCELLED`, `CLOSED` |
| `RETURNED` | User has submitted return shipment | `RECEIVED`, `CLOSED` |
| `RECEIVED` | Merchant confirmed returned goods | `REFUNDING`, `CLOSED` |
| `REFUNDING` | Provider refund is in progress | `REFUNDED`, `CLOSED` |
| `REJECTED`, `REFUNDED`, `CANCELLED`, `CLOSED` | Finished states | None |

The intentionally simple after-sale refund flow is:

1. `PROCESS` starts a payment refund through `payment-service` and moves after-sale/sub-order status
   to `REFUNDING`.
2. The refund request uses a deterministic refund number and idempotency key based on `afterSaleNo`.
3. `REFUND_COMPLETED` from `payment-service` advances order after-sale state to `REFUNDED`; return
   refunds restore stock at that point.

If creating the remote refund fails, the local order transaction rolls back and merchant processing
can be retried. Refund provider callbacks and order after-sale updates must still be reconciled
through governance compensation when one side succeeds and the other side fails.

## Failure Handling Table

| Chain | Failure point | Required behavior | Owner |
| --- | --- | --- | --- |
| order creation -> stock reserve | stock pre-check fails | publish stock-freeze-failed, keep order cancellable | `stock-service` + `order-service` |
| order creation -> stock reserve | reserved event cannot be queued | rollback local stock transaction and retry through MQ/outbox | `stock-service` |
| stock reserved -> order update | order consumer fails remotely | retry remote/system failures; business-invalid events are ACKed and recorded | `order-service` |
| payment success -> order paid | order update fails after provider confirms payment | retry through outbox; expose compensation entry in governance | `payment-service` + `order-service` |
| payment success -> stock confirm | stock confirm hits a transient DB failure | retry the confirm command a bounded number of times; do not mark inventory final from cache | `stock-service` |
| order cancellation/refund -> stock release | stock release hits a transient DB failure | retry the release command a bounded number of times; keep business errors explicit | `stock-service` |
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
