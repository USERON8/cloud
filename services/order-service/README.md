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

- 生产消息：`ORDER_CREATED` / `ORDER_CANCELLED` / `REFUND_PROCESS` / `STOCK_RESTORE`
- 可靠投递：写入 `outbox_event`，由 `OrderOutboxRelay` 定时发布到 RocketMQ
- 消费幂等：使用 `MessageIdempotencyService`（Redis）防重
- 事务模式：当前为本地事务 + Outbox（Seata 配置存在，但未启用全局事务）

## 库存交互

- Dubbo 调用 `stock-service`：`reserve` / `confirm` / `release` / `rollback`
- 下单时并行预占库存，支付成功后确认，取消/退款时释放或回滚

## 调度任务

- 业务定时任务通过 `XXL-JOB` 执行
- 当前已接入 handler：`orderTimeoutCheckJob`
- 当前已接入 handler：`orderAutoConfirmReceiptJob`
- 当前已接入 handler：`afterSaleAutoApproveJob`

## 本地启动

```bash
mvn -pl order-service spring-boot:run
```
