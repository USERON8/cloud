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

- Produced events: `ORDER_CREATED`, `ORDER_CANCELLED`, `ORDER_TIMEOUT`, `STOCK_RESERVE_REQUEST`, `STOCK_CONFIRM_REQUEST`, `STOCK_RELEASE_REQUEST`, `STOCK_RESTORE`
- Reliable delivery: outbound domain events are persisted to `outbox_event`, dispatched immediately after commit, and retried by `OrderOutboxRelay`
- Delayed delivery: `ORDER_TIMEOUT` is relayed with RocketMQ delay levels after it is written to the outbox
- Consumer idempotency: `MessageIdempotencyService` (Redis) prevents replay side effects
- Transaction model: local transaction + outbox + RocketMQ reliable delivery + consumer idempotency; timeout cancellation uses delayed RocketMQ messages

## Idempotency And Remote Calls

- Order creation now requires both `Idempotency-Key` and request-body `clientOrderId`
- `clientOrderId` is the strict business idempotency key scoped to the user and used to safely return the existing active main order on retries
- Remote payment and stock interactions use shared `RemoteCallSupport`
- Query-style remote calls may degrade explicitly, while command-style remote calls fail fast and keep business semantics explicit

## Cross-Service Interactions

- `stock-service`
  - Inventory pre-check through Dubbo and reserve/confirm/release through MQ commands.
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
- Cache invalidation is already wired into sub-order status transitions, shipping updates, after-sale changes, refund updates, and stock command flows through `OrderAggregateCacheService.evict(...)`.
- The current README reflects that order placement now uses eventual consistency instead of Seata-based distributed transactions.
- Duplicate order submission handling now combines header idempotency with `clientOrderId` business deduplication.
- Shared remote-call handling has replaced scattered Dubbo exception translation in order-side support services.
- This cache remains read-optimization only. Order state truth is still owned by MySQL plus transactional workflows, not by cache.

## Local Run

```bash
mvn -pl order-service spring-boot:run
```
