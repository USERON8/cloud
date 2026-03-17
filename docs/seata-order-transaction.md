# Seata Order Transaction
Version: 1.1.0

## Current Status

Seata is enabled for order placement (TCC) and refund (SAGA) in `order-service`.
`payment-service` keeps Seata disabled. Global transactions are limited to the order placement flow; other cross-service
consistency relies on RocketMQ events (Outbox/transactional messages) and idempotent consumers.

## What Is Implemented Today

- Order placement uses `@GlobalTransactional` + Seata TCC: `OrderPlacementServiceImpl` + `OrderCreateTccAction` + `StockReserveTccService`.
- TCC Try writes `order_tcc_log`, Confirm advances orders to `STOCK_RESERVED` and emits `ORDER_TIMEOUT` after commit; Cancel rolls back orders/sub-orders.
- Stock TCC uses reservation tokens and rollback markers to handle empty rollback/hanging.
- Refund flow uses Seata SAGA state machine (`refund-saga.json`) via `OrderRefundSagaCoordinator`.
- Payment success uses RocketMQ transactional message (`PaymentSuccessTxProducer` + `PaymentSuccessTransactionListener`).
- Outbox relays publish `ORDER_CREATED`/`ORDER_CANCELLED`/`STOCK_RESTORE` and `REFUND_COMPLETED`.

## If You Want To Extend Seata Coverage

Prerequisites (already prepared in this repo):

- Seata server SQL: `db/init/infra/seata/init.sql`
- `undo_log` tables in business databases (already included in init scripts)
- Seata config blocks in service `application.yml`

Required code changes (as needed):

- Add `@GlobalTransactional` on additional entrypoints you want to protect
- Ensure the remote calls are within the same global transaction
- Validate rollback behavior and idempotency under retries/timeouts

## Non-Goals (Current State)

- Global transaction coverage for payment callbacks (handled by RocketMQ transactional messages).
- Cross-service ACID on third-party payment side effects.
