# Stock Service
Version: 1.1.0

库存服务，支持库存增减、预留/释放、盘点与变更日志查询。

- 服务名：`stock-service`
- 端口：`8085`
- 数据库脚本：`db/init/stock-service/init.sql`
- 测试数据：`db/test/stock-service/test.sql`

## 核心接口

- 统一入口：`/api/stocks/**`
- 内部调用：`/internal/stock/**`

## 消息与一致性

- 生产消息：库存不足时发送 `STOCK_FREEZE_FAILED`
- 可靠投递：写入 `outbox_event`，由 `StockOutboxRelay` 定时发布到 RocketMQ
- 消费消息：`STOCK_RESTORE`（退款完成后回滚库存）
- 事务模式：本地事务 + Outbox（Seata 配置存在，但未启用全局事务）

## 本地启动

```bash
mvn -pl stock-service spring-boot:run
```
