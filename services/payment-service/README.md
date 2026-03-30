# Payment Service
Version: 1.1.0

Payment service for payment order management, refund processing, and payment channel compensation.

- Service name: `payment-service`
- Port: `8086`
- Database bootstrap: `db/init/payment-service/init.sql`
- Test data: `db/test/payment-service/test.sql`

## Responsibilities

- Manages payment orders and refund orders.
- Integrates payment provider callbacks and compensation flows.
- Publishes payment success and refund completion events.
- Provides payment-status lookup for order orchestration.

## Core Endpoints

- Create payment order: `POST /api/payments/orders`
- Query payment order: `GET /api/payments/orders/{paymentNo}`
- Query payment status: `GET /api/payments/orders/{paymentNo}/status`
- Internal callback handling: `POST /api/payments/callbacks`
- External compatibility callback: `POST /api/v1/payment/alipay/notify`
- Create refund: `POST /api/payments/refunds`
- Query refund: `GET /api/payments/refunds/{refundNo}`
- Payment provider integration: implemented through `PaymentProviderGateway`, currently with Alipay query and refund compensation paths

## Messaging And Consistency

- Produced events: `PAYMENT_SUCCESS` (RocketMQ transactional message) and `REFUND_COMPLETED` (Outbox)
- Reliable delivery: `REFUND_COMPLETED` is persisted to `outbox_event` and published by `PaymentOutboxRelay`
- Transaction model: payment success uses RocketMQ transactional messages; refunds use local transactions + Outbox

## Current Design Notes

- Payment success is treated as a high-value event and uses RocketMQ transactional messaging instead of only local outbox relay.
- Refund completion remains on the local-transaction-plus-outbox pattern.
- `payment-service` now participates in the unified eventual-consistency model without any Seata dependency.

## Current Cache Model

- `PaymentSecurityCacheService` uses explicit Redis single-level cache for:
  - payment order idempotency keys
  - payment result reuse keys
  - short-lived payment status lookup cache
  - checkout tickets
  - per-user payment rate limiting
- This cache layer is security and anti-duplication oriented, not a business read-acceleration layer.

## Scheduled Jobs

- Compensation jobs run through `XXL-JOB`
- Registered handler: `paymentOrderReconcileJob`
- Registered handler: `paymentRefundRetryJob`

## Known Findings In This Sync

- `payment-service` already uses explicit Redis single-level cache through `PaymentSecurityCacheService` instead of annotation-driven cache behavior.
- The current cache scope is intentionally narrow and focused on idempotency, short-lived status lookup, checkout tickets, and rate limiting.
- The README now makes the split between transactional-message success handling and outbox-based refund handling explicit.
- No extra local L1 cache was added here because payment correctness and anti-duplication semantics matter more than shaving a network hop.

## Local Run

```bash
mvn -pl payment-service spring-boot:run
```
