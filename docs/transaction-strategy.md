# Transaction Strategy

Updated on: 2026-03-30

## Goal

Define the eventual-consistency contract across order, inventory, payment, refund, search sync, and other critical flows. The strategy removes Seata coordination and keeps event delivery reliable through local transactions, Outbox, RocketMQ, and idempotent consumers.

## Design Principles

- Local transaction first: each service commits its own state and persists outbound intent in `outbox_event`.
- No distributed transaction coordinator: cross-service changes converge through MQ commands, delayed messages, and idempotent consumers.
- Asynchronous decoupling: payment success notifications use RocketMQ transactional messages to avoid mismatches between local transactions and outgoing messages.
- Business compensation is explicit: timeout release, refund rollback, and payment retry use domain commands instead of Seata rollback hooks.
- Delayed messages are sent only after the local transaction commits to avoid stray timeout events after rollback.
- End-to-end idempotency: MQ consumers, delayed handlers, and remote command receivers all provide replay-safe processing.
- No compatibility path retention: refund flows no longer use the legacy `refund-process` MQ channel.

## Scenario Mapping

| Business Scenario | Strategy | Key Implementation |
| --- | --- | --- |
| Order placement inventory deduction | Local transaction + Outbox + MQ command | `OrderServiceImpl` + `OrderOutboxService` + stock command consumers |
| Payment success notification | RocketMQ transactional message | `PaymentSuccessTxProducer` + `PaymentSuccessTransactionListener` |
| Order timeout cancellation | RocketMQ delayed message | `OrderTimeoutMessageProducer` + `OrderTimeoutConsumer` |
| Refund creation and completion | Local transaction + remote command + Outbox | `OrderServiceImpl` + `PaymentOrderRemoteService` + `PaymentOutboxRelay` |
| Intra-service state changes | Local transaction | `@Transactional` on write operations |

## Key Implementation Notes

### Order Placement And Stock Reservation

- Order creation commits local order records and writes stock command events into `outbox_event`
- Stock reservation is consumed asynchronously by `stock-service` and updates `available`, `locked`, and `sold`
- Reservation confirmation and release are also modeled as MQ commands, so order and stock stay loosely coupled
- Timeout release uses a delayed RocketMQ message and re-checks order state before moving `locked -> available`

### RocketMQ Transactional Message (Payment Success Notification)

- Producer: `PaymentSuccessTxProducer#send` uses `sendMessageInTransaction`
- Transaction check: `PaymentSuccessTransactionListener` confirms or checks the local transaction state
- Purpose: keep payment success delivery reliable while decoupling downstream order processing

### RocketMQ Delayed Message (Order Timeout Cancellation)

- Producer: `OrderTimeoutMessageProducer#sendAfterCommit` sends after the local transaction commits
- Delay level: controlled by `order.timeout.delay-level` so timeout messages are not published for rolled-back orders

### Refund Flow

- Refund initiation stays inside the order domain transaction and validates after-sale ownership plus payable amount
- Payment refund creation uses explicit remote commands with shared timeout and exception translation rules
- Refund completion is published through `PaymentOutboxRelay` so downstream order status updates stay replay-safe
- Compensation is domain-specific and no longer depends on a Seata state machine

### Outbox (Reliable Delivery)

- Orders: `ORDER_CREATED`, `ORDER_CANCELLED`, and `STOCK_RESTORE` are published by `OrderOutboxRelay`
- Payment: `REFUND_COMPLETED` is published by `PaymentOutboxRelay`
- Payment success notifications do not use Outbox and continue to use RocketMQ transactional messaging
- Role: guarantees MQ delivery after the local transaction succeeds

### Consumer Idempotency

- `MessageIdempotencyService` (Redis) centralizes event deduplication and retry safety
- If an event does not carry `eventId`, a derived business key is used to preserve idempotency

## Configuration Locations

- RocketMQ bindings (`order-service`): `services/order-service/src/main/resources/application-rocketmq.yml`
- RocketMQ bindings (`payment-service`): `services/payment-service/src/main/resources/application-rocketmq.yml`
- RocketMQ bindings (`stock-service`): `services/stock-service/src/main/resources/application-rocketmq.yml`

## Self-Check List

- The `refund-process` MQ channel has been removed and refund handling follows the unified eventual-consistency path
- Order placement inventory deduction does not depend on Seata and uses Outbox plus MQ commands
- Payment success notifications use RocketMQ transactional messages
- No service depends on Seata coordinator runtime or `undo_log`
- Delayed messages are sent only after the local transaction commits
- MQ consumers are idempotent and replay-safe
