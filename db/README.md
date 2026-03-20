# Database Script Guide
Version: 1.1.0

Project database scripts live under `db/`. The main development path keeps only `init.sql` and `test.sql`.

## Directory Layout

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
- `db/test/*/test.sql` (including `auth-service` and the business services)

## Execution Order

1. `db/init/infra/nacos/init.sql`
2. Optionally run `db/init/infra/seata/init.sql`, `db/init/infra/skywalking/init.sql`, and `db/init/infra/xxl-job/init.sql`
3. Run each business service bootstrap script under `db/init/*/init.sql`
4. Optionally run `db/test/*/test.sql`

## Shared Tables

The following tables are included consistently in the `init.sql` files for `order-service`, `payment-service`, and `stock-service` to support reliable messaging and transaction compensation:

- `outbox_event`: local transaction outbox persisted before scheduled relay publishing to RocketMQ
- `inbox_consume_log`: consumer-side idempotency records
- `undo_log`: Seata rollback log (currently pre-created even though global Seata transactions are not fully enabled everywhere)

## Example (MySQL on `127.0.0.1:3306`)

```bash
mysql -h127.0.0.1 -P3306 -uroot -proot < db/init/user-service/init.sql
mysql -h127.0.0.1 -P3306 -uroot -proot < db/test/user-service/test.sql
```

If you use the MySQL container directly, run the same script through `docker exec -i mysql_db mysql -uroot -proot`.
