# 数据库脚本说明

本项目数据库脚本统一放在 `db/`，主流程仅保留 `init.sql` 与 `test.sql`。

## 目录结构

- `db/init/infra/nacos/init.sql`
- `db/init/user-service/init.sql`
- `db/init/product-service/init.sql`
- `db/init/order-service/init.sql`
- `db/init/stock-service/init.sql`
- `db/init/payment-service/init.sql`
- `db/test/*/test.sql`（5 个业务服务）
- `db/archive/**`（历史归档，不参与主流程）

## 执行顺序

1. `db/init/infra/nacos/init.sql`
2. 5 个业务服务 `db/init/*/init.sql`
3. 可选执行 `db/test/*/test.sql`

## 示例（MySQL 在 127.0.0.1:3306）

```bash
mysql -h127.0.0.1 -P3306 -uroot -proot < db/init/user-service/init.sql
mysql -h127.0.0.1 -P3306 -uroot -proot < db/test/user-service/test.sql
```

如使用容器内 MySQL，可改为 `docker exec -i mysql_db mysql -uroot -proot` 执行同一脚本内容。
