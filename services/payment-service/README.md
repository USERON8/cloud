# Payment Service
Version: 1.1.0

支付服务，包含支付单管理、退款处理和支付通道补偿能力。

- 服务名：`payment-service`
- 端口：`8086`
- 数据库脚本：`db/init/payment-service/init.sql`
- 测试数据：`db/test/payment-service/test.sql`

## 核心接口

- 支付单创建：`POST /api/payments/orders`
- 支付单查询：`GET /api/payments/orders/{paymentNo}`
- 支付状态查询：`GET /api/payments/orders/{paymentNo}/status`
- 内部回调处理：`POST /api/payments/callbacks`
- 外部回调兼容入口：`POST /api/v1/payment/alipay/notify`
- 退款创建：`POST /api/payments/refunds`
- 退款查询：`GET /api/payments/refunds/{refundNo}`
- 支付通道接入：由 `PaymentProviderGateway` 实现，目前已接入支付宝查询/退款补偿链路

## 消息与一致性

- 生产消息：`PAYMENT_SUCCESS`（RocketMQ 事务消息） / `REFUND_COMPLETED`（Outbox）
- 可靠投递：`REFUND_COMPLETED` 写入 `outbox_event`，由 `PaymentOutboxRelay` 定时发布到 RocketMQ
- 事务模式：支付成功使用 RocketMQ 事务消息；退款使用本地事务 + Outbox

## 调度任务

- 业务补偿任务通过 `XXL-JOB` 执行
- 当前已接入 handler：`paymentOrderReconcileJob`
- 当前已接入 handler：`paymentRefundRetryJob`

## 本地启动

```bash
mvn -pl payment-service spring-boot:run
```

