# Order Service
Version: 1.1.0

订单与退款服务，覆盖订单生命周期和批量状态操作。

- 服务名：`order-service`
- 端口：`8083`
- 数据库脚本：`db/init/order-service/init.sql`
- 测试数据：`db/test/order-service/test.sql`

## 核心接口

- 订单与售后：`/api/orders/**`

## 消息与一致性

- 生产消息：`ORDER_CREATED` / `ORDER_CANCELLED` / `STOCK_RESTORE` / `ORDER_TIMEOUT`
- 可靠投递：`ORDER_CREATED` / `ORDER_CANCELLED` / `STOCK_RESTORE` 写入 `outbox_event`，由 `OrderOutboxRelay` 定时发布到 RocketMQ
- 延迟消息：`ORDER_TIMEOUT` 通过 RocketMQ 延迟等级直接发送
- 消费幂等：使用 `MessageIdempotencyService`（Redis）防重
- 事务模式：下单扣库存使用 Seata TCC；退款走 Seata SAGA（补偿）；超时取消使用 RocketMQ 延迟消息

## 库存交互

- Dubbo 调用 `stock-service`：`reserve` / `confirm` / `release` / `rollback`
- 下单时通过 TCC 预占库存，支付成功后确认，取消/退款时释放或回滚

## 调度任务

- 业务定时任务通过 `XXL-JOB` 执行
- 订单超时取消通过 RocketMQ 延迟消息处理
- 当前已接入 handler：`orderAutoConfirmReceiptJob`
- 当前已接入 handler：`afterSaleAutoApproveJob`

## 本地启动

```bash
mvn -pl order-service spring-boot:run
```
