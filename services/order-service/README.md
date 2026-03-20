# Order Service
Version: 1.1.0

Order and refund service covering the order lifecycle and batch status operations.

- Service name: `order-service`
- Port: `8083`
- Database bootstrap: `db/init/order-service/init.sql`
- Test data: `db/test/order-service/test.sql`

## Core Endpoints

- Orders and after-sale flows: `/api/orders/**`

## Messaging And Consistency

- Produced events: `ORDER_CREATED`, `ORDER_CANCELLED`, `STOCK_RESTORE`, `ORDER_TIMEOUT`
- Reliable delivery: `ORDER_CREATED`, `ORDER_CANCELLED`, and `STOCK_RESTORE` are persisted to `outbox_event` and published by `OrderOutboxRelay`
- Delayed delivery: `ORDER_TIMEOUT` is sent directly through RocketMQ delay levels
- Consumer idempotency: `MessageIdempotencyService` (Redis) prevents replay side effects
- Transaction model: Seata TCC is used for order placement inventory reservation; refunds use Seata SAGA compensation; timeout cancellation uses delayed RocketMQ messages

## Inventory Interaction

- Dubbo calls to `stock-service`: `reserve`, `confirm`, `release`, `rollback`
- Inventory is reserved during checkout with TCC, confirmed after payment success, and released or rolled back during cancellation or refund

## Scheduled Jobs

- Business jobs run through `XXL-JOB`
- Order timeout cancellation is handled through delayed RocketMQ messages
- Registered handler: `orderAutoConfirmReceiptJob`
- Registered handler: `afterSaleAutoApproveJob`

## Local Run

```bash
mvn -pl order-service spring-boot:run
```
