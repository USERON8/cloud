# Transaction Strategy

Updated on: 2026-03-13

## Goal

Define the boundary between strong consistency and eventual consistency across order, inventory, payment, refund, and other critical flows. The strategy reduces distributed transaction complexity while keeping critical event delivery reliable and consumer handling idempotent.

## Design Principles

- Strong consistency first where required: order placement inventory deduction uses Seata TCC so failures can roll back cleanly.
- Limited strong-consistency boundary: only order placement inventory deduction is inside the strict global transaction boundary; payment success and refund completion rely on MQ-driven eventual consistency, and `payment-service` does not participate in Seata.
- Asynchronous decoupling: payment success notifications use RocketMQ transactional messages to avoid mismatches between local transactions and outgoing messages.
- Long-running compensation: the full refund flow uses Seata SAGA and compensation actions to converge state.
- Delayed messages are sent only after the local transaction commits to avoid stray timeout events after rollback.
- End-to-end idempotency: MQ consumers, TCC, and SAGA all provide idempotent handling, empty rollback protection, and replay prevention.
- No compatibility path retention: refund flows no longer use the legacy `refund-process` MQ channel.

## Scenario Mapping

| Business Scenario | Strategy | Key Implementation |
| --- | --- | --- |
| Order placement inventory deduction | Seata TCC | `OrderPlacementServiceImpl` + `OrderCreateTccAction` + `StockReserveTccService` |
| Payment success notification | RocketMQ transactional message | `PaymentSuccessTxProducer` + `PaymentSuccessTransactionListener` |
| Order timeout cancellation | RocketMQ delayed message | `OrderTimeoutMessageProducer` + `OrderTimeoutConsumer` |
| Full refund flow | Seata SAGA | `refund-saga.json` + `OrderRefundSagaCoordinator` + `OrderRefundSagaService` |
| Intra-service state changes | Local transaction | `@Transactional` on write operations |

## Key Implementation Notes

### Seata TCC (Order Placement Inventory Deduction)

- Entry point: `OrderPlacementServiceImpl#createOrder` uses `@GlobalTransactional`
- Try: `OrderCreateTccAction#prepare` creates the order aggregate and writes `order_tcc_log`
- Confirm: `OrderCreateTccAction#commit` advances the order to `STOCK_RESERVED` and sends the `ORDER_TIMEOUT` delayed message after commit
- Cancel: `OrderCreateTccAction#rollback` rolls back the order and order item states
- Empty rollback / hanging prevention: `order_tcc_log` state and idempotency checks guard order-side rollback; the stock side uses reservation tokens and rollback markers
- Stock TCC: `StockReserveTccService` implements Try/Confirm/Cancel and delegates persistence to `StockLedgerService`

### RocketMQ Transactional Message (Payment Success Notification)

- Producer: `PaymentSuccessTxProducer#send` uses `sendMessageInTransaction`
- Transaction check: `PaymentSuccessTransactionListener` confirms or checks the local transaction state
- Purpose: keep payment success delivery reliable while decoupling downstream order processing

### RocketMQ Delayed Message (Order Timeout Cancellation)

- Producer: `OrderTimeoutMessageProducer#sendAfterCommit` sends after the local transaction commits
- Delay level: controlled by `order.timeout.delay-level` so timeout messages are not published for rolled-back orders

### Seata SAGA (Full Refund Flow)

- Definition: `services/order-service/src/main/resources/saga/refund-saga.json`
- Entry point: `OrderRefundSagaCoordinator#startRefundSaga` uses `afterSaleNo` as the business key
- Main flow: `orderRefundSagaService.applyRefund` -> `orderRefundSagaService.createRefund`
- Compensation: `orderRefundSagaService.cancelRefund` restores after-sale status; if the payment-side refund already exists, it calls `cancelRefund` to mark the cancellation
- Refund amount: must be greater than zero or the refund flow is rejected
- Idempotency key: `after-sale:refund:{afterSaleNo}` is passed into the payment refund creation path

### Outbox (Reliable Delivery)

- Orders: `ORDER_CREATED`, `ORDER_CANCELLED`, and `STOCK_RESTORE` are published by `OrderOutboxRelay`
- Payment: `REFUND_COMPLETED` is published by `PaymentOutboxRelay`
- Payment success notifications do not use Outbox and continue to use RocketMQ transactional messaging
- Role: guarantees MQ delivery after the local transaction succeeds

### Consumer Idempotency

- `MessageIdempotencyService` (Redis) centralizes event deduplication and retry safety
- If an event does not carry `eventId`, a derived business key is used to preserve idempotency

## Configuration Locations

- SAGA configuration: `services/order-service/src/main/resources/application.yml`
- SAGA definition: `services/order-service/src/main/resources/saga/refund-saga.json`
- RocketMQ bindings (`order-service`): `services/order-service/src/main/resources/application-rocketmq.yml`
- RocketMQ bindings (`payment-service`): `services/payment-service/src/main/resources/application-rocketmq.yml`
- RocketMQ bindings (`stock-service`): `services/stock-service/src/main/resources/application-rocketmq.yml`

## Self-Check List

- The `refund-process` MQ channel has been removed and refund handling goes through SAGA only
- Order placement inventory deduction uses TCC, and `order_tcc_log` exists for traceability
- Payment success notifications use RocketMQ transactional messages
- `payment-service` keeps Seata disabled; only order and stock participate in global transactions
- Delayed messages are sent only after the local transaction commits
- MQ consumers are idempotent and replay-safe
