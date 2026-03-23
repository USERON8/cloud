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
- `payment-service` intentionally keeps Seata disabled while still cooperating with distributed business flows.

## Scheduled Jobs

- Compensation jobs run through `XXL-JOB`
- Registered handler: `paymentOrderReconcileJob`
- Registered handler: `paymentRefundRetryJob`

## Known Findings In This Sync

- This service was not changed in the current documentation-and-cache sync.
- The README now makes the split between transactional-message success handling and outbox-based refund handling explicit.
- Cache and query optimization behavior here was not re-audited in the current round.

## Local Run

```bash
mvn -pl payment-service spring-boot:run
```
