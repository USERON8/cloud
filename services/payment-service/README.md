# Payment Service
Version: 1.1.0

Payment domain service for payment orders, checkout sessions, refunds, and external payment callbacks.

- Service name: `payment-service`
- Port: `8086`
- Database bootstrap: `db/init/payment-service/init.sql`
- Test data: `db/test/payment-service/test.sql`

## Responsibilities

- Creates and queries payment orders.
- Creates checkout sessions and renders the ticketed checkout page.
- Handles payment provider callbacks and refund processing.
- Publishes payment success and refund-completed events.

## HTTP Surface

- Payment orders: `/api/payment-orders/**`
- Refunds: `/api/payment-refunds/**`
- Checkout page: `GET /api/payment-checkouts/{ticket}`
- Provider callback: `POST /api/v1/payment/alipay/notify`

## Runtime Notes

- Reliable delivery follows local transaction + `outbox_event` + RocketMQ relay.
- The public payment flow is `payment order -> checkout session -> checkoutPath -> status polling`.
- Cache is intentionally limited to:
  - idempotency keys
  - duplicate-result reuse
  - short-lived non-final status helpers
  - checkout tickets
  - rate limiting
- Final payment truth stays in MySQL and transactional workflows. Amounts and terminal states are not cached as business data.

## Local Run

```bash
mvn -pl payment-service spring-boot:run
```
