# Stock Service
Version: 1.1.0

Inventory service for reservation, confirmation, release, restoration, and stock ledger reads.

- Service name: `stock-service`
- Port: `8085`
- Database bootstrap: `db/init/stock-service/init.sql`
- Test data: `db/test/stock-service/test.sql`

## Responsibilities

- Owns segmented stock rows and inventory quantity transitions.
- Consumes stock reservation, confirmation, release, and restore commands.
- Publishes stock failure or alert events when needed.
- Serves stock ledger reads for admin operations and the `StockDubboApi` governance contract.

## HTTP Surface

- Direct service admin ledger: `GET /api/admin/stocks/ledger/{skuId}`
- Gateway-facing stock governance uses `governance-service` and `StockDubboApi`; `/api/admin/stocks/internal/**` is no longer exposed.

## Runtime Notes

- Hot-row contention is reduced through `stock_segment`.
- Redis summary cache and Lua checks are used for fast availability pre-checks.
- Cross-service consistency follows local transaction + `outbox_event` + RocketMQ + consumer idempotency.
- Cache invalidation for stock summaries uses post-commit eviction and delayed second delete.

## Local Run

```bash
mvn -pl stock-service spring-boot:run
```
