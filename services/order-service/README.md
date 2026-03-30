# Order Service
Version: 1.1.0

Order and refund service covering the order lifecycle and batch status operations.

- Service name: `order-service`
- Port: `8083`
- Database bootstrap: `db/init/order-service/init.sql`
- Test data: `db/test/order-service/test.sql`

## Responsibilities

- Creates and manages orders.
- Coordinates checkout, cancellation, timeout handling, receipt confirmation, and refund initiation.
- Owns after-sale entry points and refund orchestration.
- Publishes reliable domain events to downstream services.

## Core Endpoints

- Orders and after-sale flows: `/api/orders/**`

## Messaging And Consistency

- Produced events: `ORDER_CREATED`, `ORDER_CANCELLED`, `STOCK_RESTORE`, `ORDER_TIMEOUT`
- Reliable delivery: `ORDER_CREATED`, `ORDER_CANCELLED`, `STOCK_RESTORE`, and `ORDER_TIMEOUT` are persisted to `outbox_event` and published by `OrderOutboxRelay`
- Delayed delivery: `ORDER_TIMEOUT` is relayed with RocketMQ delay levels after it is written to the outbox
- Consumer idempotency: `MessageIdempotencyService` (Redis) prevents replay side effects
- Transaction model: Seata TCC is used for order placement inventory reservation; refunds use Seata SAGA compensation; timeout cancellation uses delayed RocketMQ messages

## Cross-Service Interactions

- `stock-service`
  - Inventory reserve/confirm/release/rollback through Dubbo.
- `payment-service`
  - Payment completion drives final order state transitions.
- `user-service`
  - User and merchant related context is consumed indirectly through upstream flows.

## Current Cache Model

- `OrderAggregateCacheService` accelerates completed-order aggregate reads with short-lived local L1 plus Redis.
- Cache scope is intentionally narrow: only completed main-order aggregates are cached.
- Write and status-transition paths evict aggregate cache explicitly instead of relying on annotation-driven cache behavior.

## Scheduled Jobs

- Business jobs run through `XXL-JOB`
- Order timeout cancellation is handled through delayed RocketMQ messages
- Registered handler: `orderAutoConfirmReceiptJob`
- Registered handler: `afterSaleAutoApproveJob`

## Known Findings In This Sync

- Order aggregate query now uses a pragmatic multi-level cache: short-lived local L1 plus Redis aggregate cache, and only completed main-order aggregates are cached.
- Cache invalidation is already wired into sub-order status transitions, shipping updates, after-sale changes, refund saga updates, and TCC reserve/cancel flows through `OrderAggregateCacheService.evict(...)`.
- The current README now explicitly reflects that order consistency is centered on TCC + SAGA + Outbox, not on synchronous distributed database transactions.
- This cache remains read-optimization only. Order state truth is still owned by MySQL plus transactional workflows, not by cache.

## Local Run

```bash
mvn -pl order-service spring-boot:run
```
