# Cloud Shop Microservices
Version: 1.1.0

[Chinese](./README-zh.md)

A simplified e-commerce microservices project.
Backend: Spring Boot + Spring Cloud Alibaba.
Frontend: UniApp (Vue 3 + TypeScript).

## Current Consistency Model

- Order/stock/refund events use local DB transactions + `outbox_event` for reliable messaging.
- Outbox relay runs inside each service (scheduled polling) and publishes to RocketMQ.
- Payment success is delivered via RocketMQ transactional message in `payment-service`.
- Consumers are idempotent (Redis-based) to tolerate replays.
- User notifications are enqueued to RocketMQ (`user-notification`) and retried on consumer failure.
- Seata TCC (order placement) and Seata SAGA (refund) are enabled in `order-service`; `payment-service` keeps Seata disabled.

## Modules And Ports

| Module | Port | Description |
| --- | --- | --- |
| `gateway` | `8080` | Unified gateway for `/api/**` and `/auth/**` |
| `auth-service` | `8081` | OAuth2/JWT auth and GitHub login |
| `user-service` | `8082` | User, merchant (enable + audit status), admin, profile, address |
| `order-service` | `8083` | Order and refund |
| `product-service` | `8084` | Product and category |
| `stock-service` | `8085` | Inventory and stock movement |
| `payment-service` | `8086` | Payment and Alipay integration |
| `search-service` | `8087` | Elasticsearch search |
| `my-shop-uniapp` | `5173` (dev) | Web/Android/iOS frontend (UniApp) |

## Service Documentation Map

| Service | Document |
| --- | --- |
| `gateway` | [services/gateway/README.md](./services/gateway/README.md) |
| `auth-service` | [services/auth-service/README.md](./services/auth-service/README.md) |
| `user-service` | [services/user-service/README.md](./services/user-service/README.md) |
| `order-service` | [services/order-service/README.md](./services/order-service/README.md) |
| `product-service` | [services/product-service/README.md](./services/product-service/README.md) |
| `stock-service` | [services/stock-service/README.md](./services/stock-service/README.md) |
| `payment-service` | [services/payment-service/README.md](./services/payment-service/README.md) |
| `search-service` | [services/search-service/README.md](./services/search-service/README.md) |

## Current Cache Rollout Status

- `user-service`
  - User basic info cache is explicitly implemented as Redis single-level cache.
  - User address cache is explicitly implemented as Redis single-level cache.
  - Admin, merchant, statistics, async warmup, and some file-upload related paths still use Spring Cache annotations or `CacheManager`.
- `search-service`
  - Hot keyword list is cached through Redis single-level hot-data cache.
  - Today hot-selling product id list is cached through Redis single-level hot-data cache.
  - Hot keyword and keyword recommendation paths inside `ElasticsearchOptimizedService` no longer keep local L1 caches.
  - Smart search result cache and suggestion cache still keep local Caffeine L1 plus Redis L2.
- Other services
  - No cache-strategy refactor was performed in this sync round.

## Known Findings From This Sync Round

- `user-service` still mixes explicit Redis cache services with Spring Cache annotations, so the module is not yet on one coherent cache model.
- `search-service` no longer uses the old dev-only multi-level cache configuration for hot data, but smart search and suggestions still keep Caffeine-based local caches and related configuration.
- Service README files were previously too brief to reflect the current runtime model. They are now expanded, but some modules still need deeper endpoint-level auditing if the team wants a full operational handbook.
- Existing historical audit references remain useful:
  - [docs/code-audit-2026-03-13-en.md](./docs/code-audit-2026-03-13-en.md)
  - [docs/code-audit-2026-03-13-zh.md](./docs/code-audit-2026-03-13-zh.md)

## Quick Start

1. Start infrastructure:

```bash
bash scripts/dev/start-containers.sh
# PowerShell compatibility:
# powershell -File scripts/dev/start-containers.ps1
```

Optional: start with full monitoring stack (Prometheus + Grafana + exporters):

```bash
bash scripts/dev/start-containers.sh --with-monitoring
# PowerShell compatibility:
# powershell -File scripts/dev/start-containers.ps1 --with-monitoring
```

One-command startup for containers + services:

```bash
bash scripts/dev/start-platform.sh --with-monitoring
# PowerShell compatibility:
# powershell -File scripts/dev/start-platform.ps1 --with-monitoring
```

Compatibility alias:

```bash
bash scripts/dev/start-all.sh --with-monitoring
# PowerShell compatibility:
# powershell -File scripts/dev/start-all.ps1 --with-monitoring
```

2. Database bootstrap:
- Run `db/init/**/*.sql` first, then `db/test/**/*.sql` (optional).
- In the current closed development phase:
  - MySQL is rebuilt on each container restart.
  - History data is not preserved.
  - Migration compatibility is not required.

3. Build backend:

```bash
mvn -T 1C clean package -DskipTests
```

4. Start backend services:

```bash
bash scripts/dev/start-services.sh
# PowerShell compatibility:
# powershell -File scripts/dev/start-services.ps1
```

When `start-services.*` is run directly, it auto-exports the local runtime addresses and development secrets required by gateway/auth, including `GATEWAY_SIGNATURE_SECRET`, `CLIENT_SERVICE_SECRET`, `APP_OAUTH2_*_CLIENT_SECRET`, and `APP_JWT_ALLOW_GENERATED_KEYPAIR`.
`start-platform.*` and `start-services.*` set `JAVA_TOOL_OPTIONS` and `SW_AGENT_NAME` for the 8 business services, using the agent under `docker/monitor/skywalking/agent/`, so HTTP, Dubbo, Redis, and JDBC/MyBatis traces show up in SkyWalking UI.

Host-first acceptance workflow:

```bash
bash scripts/dev/start-host-linked.sh --services=gateway,auth-service,user-service
# PowerShell compatibility:
# powershell -File scripts/dev/start-host-linked.ps1 --services=gateway,auth-service,user-service
```

`start-host-linked.*` starts infrastructure plus the selected host-side Java services, then validates `.tmp/acceptance/startup.csv` and the corresponding stdout/stderr logs. A service is accepted only when health status is `UP` or `UP_SECURED`, stderr stays empty, and stdout has no critical startup error patterns.

Cluster-linked deployment after host acceptance:

```bash
bash scripts/dev/start-cluster-linked.sh --services=auth-service,user-service
# PowerShell compatibility:
# powershell -File scripts/dev/start-cluster-linked.ps1 --services=auth-service,user-service
```

`start-cluster-linked.*` requires a successful `start-host-linked.*` run first. It recreates Nginx plus the selected service containers with host ports remapped to `28080-28087`, keeps the public gateway entrance on `18080`, and switches `NGINX_GATEWAY_UPSTREAM` to the in-cluster `gateway:8080`.

Restart only the services you changed:

```bash
bash scripts/dev/start-platform.sh --skip-containers --services=order-service,stock-service
# PowerShell compatibility:
# powershell -File scripts/dev/start-platform.ps1 --skip-containers --services=order-service
```

Service process logs are written to `.tmp/service-runtime/<service>/stdout.log` and `.tmp/service-runtime/<service>/stderr.log`.
Rolling application and error logs are written to `services/<service>/logs/` by default, or `.tmp/service-runtime/<service>/app-logs/` if the module log directory is not writable.

5. Build frontend:

```bash
pnpm --dir my-shop-uniapp install
pnpm --dir my-shop-uniapp build:h5
```

## Common Entrances

- Frontend: `http://127.0.0.1:18080`
- Gateway API Docs: `http://127.0.0.1:18080/doc.html`
- Nacos: `http://127.0.0.1:18080/nacos`
- RocketMQ Dashboard: `http://127.0.0.1:38082`
- MinIO Console: `http://127.0.0.1:19001`
- Elasticsearch: `http://127.0.0.1:19200`
- Kibana: `http://127.0.0.1:15601`
- Prometheus: `http://127.0.0.1:19099`
- Grafana: `http://127.0.0.1:13000`
- SkyWalking UI: `http://127.0.0.1:13001`
- Sentinel Dashboard: `http://127.0.0.1:18718`

