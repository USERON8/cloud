# 数据库脚本说明
Version: 1.1.0

本项目数据库脚本统一放在 `db/`，主流程仅保留 `init.sql` 与 `test.sql`。

## 目录结构

- `db/init/infra/nacos/init.sql`
- `db/init/infra/seata/init.sql`
- `db/init/infra/skywalking/init.sql`
- `db/init/infra/xxl-job/init.sql`
- `db/init/auth-service/init.sql`
- `db/init/user-service/init.sql`
- `db/init/product-service/init.sql`
- `db/init/order-service/init.sql`
- `db/init/stock-service/init.sql`
- `db/init/payment-service/init.sql`
- `db/test/*/test.sql`（含 auth-service 在内的业务服务）

## 执行顺序

1. `db/init/infra/nacos/init.sql`
2. 可选执行 `db/init/infra/seata/init.sql` / `db/init/infra/skywalking/init.sql` / `db/init/infra/xxl-job/init.sql`
3. 各业务服务 `db/init/*/init.sql`
4. 可选执行 `db/test/*/test.sql`

## 公共表说明

以下表在 `order-service` / `payment-service` / `stock-service` 的 `init.sql` 中统一包含，用于消息可靠性与事务补偿：

- `outbox_event`：本地事务内写入的消息盒，定时 Relay 发布到 RocketMQ
- `inbox_consume_log`：消费端幂等记录
- `undo_log`：Seata 回滚日志（当前 Seata 未启用全局事务，但表已预置）

## 示例（MySQL 在 127.0.0.1:3306）

```bash
mysql -h127.0.0.1 -P3306 -uroot -proot < db/init/user-service/init.sql
mysql -h127.0.0.1 -P3306 -uroot -proot < db/test/user-service/test.sql
```

如使用容器内 MySQL，可改为 `docker exec -i mysql_db mysql -uroot -proot` 执行同一脚本内容。
