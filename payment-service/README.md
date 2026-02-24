# Payment Service

支付服务，包含支付单管理和支付宝聚合接口。

- 服务名：`payment-service`
- 端口：`8086`
- 数据库脚本：`db/init/payment-service/init.sql`
- 测试数据：`db/test/payment-service/test.sql`

## 核心接口

- 支付单：`/api/payments/**`
- 支付宝：`/api/v1/payment/alipay/**`
- 内部调用：`/internal/payment/**`

## 本地启动

```bash
mvn -pl payment-service spring-boot:run
```
