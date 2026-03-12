# Order Service
Version: 1.1.0

订单与退款服务，覆盖订单生命周期和批量状态操作。

- 服务名：`order-service`
- 端口：`8083`
- 数据库脚本：`db/init/order-service/init.sql`
- 测试数据：`db/test/order-service/test.sql`

## 核心接口

- 订单与售后：`/api/orders/**`

## 调度任务

- 业务定时任务通过 `XXL-JOB` 执行
- 当前已接入 handler：`orderTimeoutCheckJob`
- 当前已接入 handler：`orderAutoConfirmReceiptJob`
- 当前已接入 handler：`afterSaleAutoApproveJob`

## 本地启动

```bash
mvn -pl order-service spring-boot:run
```
