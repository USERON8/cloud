# Seata Order Transaction
Version: 1.1.0

## Scope

The strong-consistency scope is intentionally limited to order creation and stock reservation:

- `order-service` starts the global transaction
- `stock-service` joins as an AT branch
- `payment-service` remains eventual-consistency because external payment providers cannot participate in the same ACID transaction

## Covered Flow

`POST /api/orders` now executes the following steps inside one Seata global transaction:

1. Create the main order and sub-orders in `order_db`
2. Reserve stock in `stock_db` through Dubbo `StockDubboApi.reserve`
3. Advance created sub-orders to `STOCK_RESERVED`

If any stock reservation fails, Seata rolls back both the order writes and the stock ledger changes.

## Required Infrastructure

- `db/init/infra/seata/init.sql` must be applied so the `seata` database contains the server-side tables required by DB store, including `global_table`, `branch_table`, `lock_table`, `distributed_lock`, and `vgroup_table`
- business databases must keep their `undo_log` tables
- local startup should include the Seata server:

```bash
bash scripts/dev/start-platform.sh --with-monitoring
```

The unified startup entrypoint now exports `SEATA_SERVER_ADDR` from `docker/.env`, so service processes follow the host port mapping automatically.

## Verification

Recommended checks after startup:

1. create an order that reserves stock successfully
2. confirm the sub-order status becomes `STOCK_RESERVED`
3. force an insufficient-stock request and confirm the order data is rolled back
4. inspect `seata.global_table`, `seata.branch_table`, `seata.distributed_lock`, and the service `undo_log` tables while testing

## Non-Goals

- payment callback processing
- third-party payment side effects
- cross-service eventual-consistency flows that are already driven by MQ
