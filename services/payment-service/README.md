# Payment Service
Version: 1.1.0

Payment service for payment order management, refund processing, and payment channel compensation.

- Service name: `payment-service`
- Port: `8086`
- Database bootstrap: `db/init/payment-service/init.sql`
- Test data: `db/test/payment-service/test.sql`

## Core Endpoints

- Create payment order: `POST /api/payments/orders`
- Query payment order: `GET /api/payments/orders/{paymentNo}`
- Query payment status: `GET /api/payments/orders/{paymentNo}/status`
- Internal callback handling: `POST /api/payments/callbacks`
- External compatibility callback: `POST /api/v1/payment/alipay/notify`
- Create refund: `POST /api/payments/refunds`
- Query refund: `GET /api/payments/refunds/{refundNo}`
- Payment provider integration: implemented through `PaymentProviderGateway`, currently with Alipay query and refund compensation paths

## Messaging And Consistency

- Produced events: `PAYMENT_SUCCESS` (RocketMQ transactional message) and `REFUND_COMPLETED` (Outbox)
- Reliable delivery: `REFUND_COMPLETED` is persisted to `outbox_event` and published by `PaymentOutboxRelay`
- Transaction model: payment success uses RocketMQ transactional messages; refunds use local transactions + Outbox

## Scheduled Jobs

- Compensation jobs run through `XXL-JOB`
- Registered handler: `paymentOrderReconcileJob`
- Registered handler: `paymentRefundRetryJob`

## Local Run

```bash
mvn -pl payment-service spring-boot:run
```
