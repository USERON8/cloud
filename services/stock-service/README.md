# Stock Service
Version: 1.1.0

Inventory service supporting stock increments and decrements, reservation and release, stocktaking, and ledger queries.

- Service name: `stock-service`
- Port: `8085`
- Database bootstrap: `db/init/stock-service/init.sql`
- Test data: `db/test/stock-service/test.sql`

## Core Endpoints

- Unified entry: `/api/stocks/**`
- Internal access: `/internal/stock/**`

## Messaging And Consistency

- Produced event: `STOCK_FREEZE_FAILED` when reservation fails because inventory is insufficient
- Reliable delivery: events are persisted to `outbox_event` and published by `StockOutboxRelay`
- Consumed event: `STOCK_RESTORE` (inventory rollback after refund completion)
- Transaction model: participates in Seata TCC for order reservation Try/Confirm/Cancel; other writes use local transactions + Outbox

## Local Run

```bash
mvn -pl stock-service spring-boot:run
```
