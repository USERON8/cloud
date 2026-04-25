# Order Service
Version: 1.1.0

Order domain service for cart checkout, order lifecycle changes, after-sale handling, and timeout processing.

- Service name: `order-service`
- Port: `8083`
- Database bootstrap: `db/init/order-service/init.sql`
- Test data: `db/test/order-service/test.sql`

## Responsibilities

- Owns cart snapshots and order creation.
- Manages cancellation, shipment, completion, and after-sale actions.
- Coordinates timeout cancellation and receipt-related jobs.
- Publishes reliable order-side events to stock and payment flows.

## HTTP Surface

- Cart: `/api/users/me/cart/**`
- Orders: `/api/orders/**`
- After-sale: `/api/after-sales/**`

## Runtime Notes

- Cross-service consistency follows local transaction + `outbox_event` + RocketMQ + consumer idempotency.
- Order timeout cancellation uses delayed RocketMQ delivery.
- `POST /api/orders` requires both `Idempotency-Key` and `clientOrderId`.
- Completed main-order aggregates are cached through a narrow read-optimization path; cache is not the source of truth.
- Stock reservation, confirmation, and release are coordinated with `stock-service`; payment state changes are consumed from `payment-service`.

## Local Run

```bash
mvn -pl order-service spring-boot:run
```
