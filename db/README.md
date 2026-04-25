# Database Script Guide
Version: 1.1.0

## Layout

- `db/init/infra/nacos/init.sql`
- `db/init/infra/skywalking/init.sql`
- `db/init/infra/xxl-job/init.sql`
- `db/init/auth-service/init.sql`
- `db/init/user-service/init.sql`
- `db/init/product-service/init.sql`
- `db/init/order-service/init.sql`
- `db/init/stock-service/init.sql`
- `db/init/payment-service/init.sql`
- `db/test/*/test.sql`

## Execution Order

1. Run `db/init/infra/nacos/init.sql`
2. Run optional infra scripts for SkyWalking and XXL-Job if needed
3. Run each service bootstrap script under `db/init/*/init.sql`
4. Run optional test data under `db/test/*/test.sql`

## Shared Tables

`order-service`, `payment-service`, and `stock-service` keep these shared reliability tables:

- `outbox_event`
- `inbox_consume_log`

## Local Example

```bash
mysql -h127.0.0.1 -P3306 -uroot -proot < db/init/user-service/init.sql
mysql -h127.0.0.1 -P3306 -uroot -proot < db/test/user-service/test.sql
```

In the Docker setup, MySQL bootstrap is wired through `docker/docker-compose.yml`.
