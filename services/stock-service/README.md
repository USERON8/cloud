# Stock Service
Version: 1.1.0

Inventory service supporting segmented reservation, release, confirmation, stocktaking, and ledger queries.

- Service name: `stock-service`
- Port: `8085`
- Database bootstrap: `db/init/stock-service/init.sql`
- Test data: `db/test/stock-service/test.sql`

## Responsibilities

- Owns segmented inventory quantity, reservation, release, and final confirmation.
- Maintains explicit `available`, `locked`, and `sold` quantities per stock segment.
- Executes reservation commands from MQ with local transactions and consumer idempotency.
- Publishes stock failure or alert events when required.

## Core Endpoints

- Unified entry: `/api/stocks/**`
- Internal access: `/internal/stock/**`

## Messaging And Consistency

- Produced events: `STOCK_FREEZE_FAILED`, `STOCK_ALERT`
- Reliable delivery: produced events are persisted to `outbox_event`, then dispatched after commit and polled by `StockOutboxRelay` as fallback
- Consumed events: `STOCK_RESERVE_REQUEST`, `STOCK_CONFIRM_REQUEST`, `STOCK_RELEASE_REQUEST`, `STOCK_RESTORE`
- Transaction model: local transaction + outbox + RocketMQ reliable delivery + consumer idempotency

## Current Design Notes

- `stock_segment` splits one SKU into multiple rows to reduce hot-row contention.
- `StockRedisCacheService` keeps a Redis summary per SKU and uses Lua for fast availability pre-check.
- Reservation, confirmation, release, and rollback reconcile MySQL state first and then evict Redis summary state after commit with delayed second eviction.
- Stock changes also trigger product-search sync for affected SKUs.

## Known Findings In This Sync

- The ledger query path uses Redis summary cache backed by aggregated `stock_segment` queries.
- Redis Lua currently guards entry-side availability checks before database allocation runs.
- Cross-node cache freshness now follows post-commit eviction plus delayed double delete instead of in-transaction cache writes.
- Delayed RocketMQ timeout handling is part of the normal stock release path for unpaid orders.

## Local Run

```bash
mvn -pl stock-service spring-boot:run
```
