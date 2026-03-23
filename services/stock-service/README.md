# Stock Service
Version: 1.1.0

Inventory service supporting stock increments and decrements, reservation and release, stocktaking, and ledger queries.

- Service name: `stock-service`
- Port: `8085`
- Database bootstrap: `db/init/stock-service/init.sql`
- Test data: `db/test/stock-service/test.sql`

## Responsibilities

- Owns inventory quantity, reservation, release, and final confirmation.
- Participates in checkout reservation flow through TCC.
- Maintains inventory movement and stock ledger style state transitions.
- Publishes stock failure or rollback-related domain events when required.

## Core Endpoints

- Unified entry: `/api/stocks/**`
- Internal access: `/internal/stock/**`

## Messaging And Consistency

- Produced event: `STOCK_FREEZE_FAILED` when reservation fails because inventory is insufficient
- Reliable delivery: events are persisted to `outbox_event` and published by `StockOutboxRelay`
- Consumed event: `STOCK_RESTORE` (inventory rollback after refund completion)
- Transaction model: participates in Seata TCC for order reservation Try/Confirm/Cancel; other writes use local transactions + Outbox

## Current Design Notes

- `stock-service` is a high-frequency read/write boundary and a strong candidate for explicit multi-level cache review.
- The repository already contains `StockRedisCacheService`, which indicates inventory read caching is an active design concern.
- Because stock consistency requirements are tighter than user/search hot-data paths, cache choices here must be validated against reservation semantics first.

## Known Findings In This Sync

- This module was not changed in the current round.
- Existing stock cache implementation was not re-audited here, only identified as one of the next places that needs a dedicated review if cache cleanup continues.

## Local Run

```bash
mvn -pl stock-service spring-boot:run
```
