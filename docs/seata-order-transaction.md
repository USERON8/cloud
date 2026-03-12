# Seata Order Transaction
Version: 1.1.0

## Current Status

Seata dependencies and configuration are present in the codebase, but **no `@GlobalTransactional` flow is enabled** at the moment.
The order/stock workflow currently relies on **local transactions + Outbox** for reliable messaging, and `payment-service` keeps Seata disabled in `application.yml`.

## What Is Implemented Today

- Order creation and stock reservation run as local DB transactions.
- Cross-service consistency is achieved via RocketMQ events written to `outbox_event` and relayed by scheduled jobs.
- Consumers are idempotent via `MessageIdempotencyService` (Redis).

## If You Want To Enable Seata Later

Prerequisites (already prepared in this repo):

- Seata server SQL: `db/init/infra/seata/init.sql`
- `undo_log` tables in business databases (already included in init scripts)
- Seata config blocks in service `application.yml`

Required code changes (not present today):

- Add `@GlobalTransactional` on the order entrypoint you want to protect
- Ensure the stock reservation call is within the same global transaction
- Validate rollback behavior under insufficient stock

## Non-Goals (Current State)

- Global transaction coverage for payment callbacks
- Cross-service ACID on third-party payment side effects
- SAGA/TCC flows (not implemented yet)
