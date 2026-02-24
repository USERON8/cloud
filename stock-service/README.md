# Stock Service

Stock domain service for inventory management, reservation, deduction, alerts, and stock counting.

- Service name: `stock-service`
- Default port: `8085`
- Main dependencies: MySQL, Redis, Nacos, OAuth2 Resource Server
- Optional dependency: RocketMQ profile (`application-rocketmq.yml`)

## Security Model

OAuth2 JWT is required for protected APIs.

- Internal APIs:
  - `/internal/stock/**` requires `SCOPE_internal_api`
- Business APIs:
  - `/api/stocks/**` requires authentication
  - Method-level authorization is enabled and enforced with `@PreAuthorize`

Role model used by APIs:

- `ROLE_ADMIN`
- `ROLE_MERCHANT`

## API Groups

### 1) Stock Management APIs
Base path: `/api/stocks`

- `POST /api/stocks/page`
- `GET /api/stocks/{id}`
- `GET /api/stocks/product/{productId}`
- `POST /api/stocks/batch/query`
- `POST /api/stocks`
- `PUT /api/stocks/{id}`
- `DELETE /api/stocks/{id}`
- `DELETE /api/stocks?ids=1&ids=2`

### 2) Stock Operation APIs
Base path: `/api/stocks`

- `POST /api/stocks/stock-in`
- `POST /api/stocks/stock-out`
- `POST /api/stocks/reserve`
- `POST /api/stocks/release`
- `GET /api/stocks/check/{productId}/{quantity}`
- `POST /api/stocks/seckill/{productId}`

### 3) Batch Stock Operation APIs
Base path: `/api/stocks`

- `POST /api/stocks/batch`
- `PUT /api/stocks/batch`
- `POST /api/stocks/stock-in/batch`
- `POST /api/stocks/stock-out/batch`
- `POST /api/stocks/reserve/batch`

### 4) Stock Alert APIs
Base path: `/api/stocks`

- `GET /api/stocks/alerts`
- `GET /api/stocks/alerts/threshold/{threshold}`
- `PUT /api/stocks/{productId}/threshold`
- `PUT /api/stocks/threshold/batch`

### 5) Stock Count APIs
Base path: `/api/stocks`

- `POST /api/stocks/count`
- `PUT /api/stocks/count/{countId}/confirm`
- `DELETE /api/stocks/count/{countId}`
- `GET /api/stocks/count/{countId}`
- `GET /api/stocks/count/product/{productId}`
- `GET /api/stocks/count/status/{status}`
- `GET /api/stocks/count/pending/count`

### 6) Stock Log APIs
Base path: `/api/stocks`

- `GET /api/stocks/logs/product/{productId}`
- `GET /api/stocks/logs/order/{orderId}`
- `GET /api/stocks/logs/type/{operationType}`

### 7) Async Stock Utility APIs
Base path: `/api/stocks`

- `POST /api/stocks/cache/refresh/{productId}`
- `POST /api/stocks/cache/refresh/batch`
- `POST /api/stocks/cache/preload?limit=100`
- `GET /api/stocks/analytics/value`

### 8) Internal APIs
Base path: `/internal/stock`

- `GET /internal/stock/product/{productId}`
- `PUT /internal/stock/{productId}?quantity={quantity}`
- `GET /internal/stock/{stockId}`
- `POST /internal/stock/batch`
- `GET /internal/stock/check/{productId}/{quantity}`
- `POST /internal/stock/deduct`
- `POST /internal/stock/reserve`
- `POST /internal/stock/release`
- `POST /internal/stock/stock-in`

## Database Schema (MySQL)

Current init script is in Docker bootstrap:

- `docker/docker-compose/mysql/init/initdb_stock_db.sql`

Main tables:

- `stock`
- `stock_in`
- `stock_out`
- `stock_log`
- `stock_count`

## Messaging

Configured in `stock-service/src/main/resources/application-rocketmq.yml`:

- Consumers:
  - `orderCreatedConsumer`
  - `paymentSuccessConsumer`
  - `stockRestoreConsumer`
- Producer:
  - `stockFreezeFailedProducer-out-0`

## Local Run

### 1) Start dependencies

```bash
cd docker
docker compose up -d mysql redis nacos
```

Optional for MQ flow:

```bash
docker compose up -d rmqnamesrv rmqbroker
```

### 2) Start service

```bash
cd stock-service
mvn spring-boot:run
```

### 3) Verify health

```bash
curl http://localhost:8085/actuator/health
```

## Testing

Run service tests:

```bash
mvn -pl stock-service -am test -DskipITs
```

## Notes

- Method-level authorization is now enabled and enforced.
- `stock_out` records are only persisted when an `orderId` is provided.
- Stock operation logs are written with null-safe quantity change handling.
