# Order Service

订单与退款服务，覆盖订单生命周期和批量状态操作。

- 服务名：`order-service`
- 端口：`8083`
- 数据库脚本：`db/init/order-service/init.sql`
- 测试数据：`db/test/order-service/test.sql`

## 核心接口

- 订单：`/api/orders/**`
- 退款：`/api/v1/refund/**`
- 内部调用：`/internal/order/**`

## 本地启动

```bash
mvn -pl order-service spring-boot:run
```
