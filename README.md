# Cloud Shop Microservices
Version: 1.1.0

[Chinese](./README-zh.md)

Cloud Shop is a microservice e-commerce project built with Spring Boot, Spring Cloud Alibaba, Dubbo, RocketMQ, MySQL, Redis, Elasticsearch, and a UniApp frontend.

## Current System Model

- Public entry: `gateway` is the only public backend entry. In the local Docker setup, Nginx exposes the platform at `http://127.0.0.1:18080`.
- Authentication and trust: `gateway` validates public JWTs, injects signed `X-Internal-*` headers, and downstream services verify HMAC or accept direct bearer-token traffic.
- Consistency: cross-service writes use local transactions, `outbox_event`, RocketMQ delivery, and idempotent consumers.
- Cache: business reads follow Cache-Aside with delayed double delete. `payment-service` cache is limited to idempotency, checkout tickets, short-lived status helpers, and rate limiting.
- Search: `search-service` serves public product and shop discovery from Elasticsearch and Redis-backed hot data.

## Modules

| Module | Port | Responsibility |
| --- | --- | --- |
| `gateway` | `8080` | Public routing, JWT validation, HMAC forwarding, rate limiting, fallback handling |
| `auth-service` | `8081` | OAuth2 authorization server, JWT issuance, session logout, GitHub OAuth login |
| `user-service` | `8082` | User profile, address, merchant, merchant-auth, and admin domain data |
| `order-service` | `8083` | Cart, order lifecycle, after-sale, timeout handling |
| `product-service` | `8084` | Product, SKU, SPU, and category management |
| `stock-service` | `8085` | Stock reservation, release, confirmation, and ledger queries |
| `payment-service` | `8086` | Payment orders, checkout sessions, refunds, payment callback handling |
| `search-service` | `8087` | Product search, shop search, suggestions, recommendations |
| `governance-service` | `8088` | Admin aggregation, MQ and Outbox governance, observability redirects |
| `my-shop-uniapp` | `5173` (dev) | UniApp frontend for H5 and app builds |

## Repository Layout

- `common-parent/`: shared libraries such as `common-api`, `common-db`, `common-security`, and `common-messaging`
- `services/`: backend services and service-level README files
- `my-shop-uniapp/`: UniApp frontend
- `db/`: bootstrap and test SQL scripts
- `scripts/dev/`: local startup scripts
- `docker/`: infrastructure, monitoring, and local container definitions
- `docs/`: API, startup, consistency, cache, and observability notes

## Quick Start

1. Prepare local environment files.

```bash
cp .env.example .env
cp docker/.env.example docker/.env
```

PowerShell:

```powershell
Copy-Item .env.example .env
Copy-Item docker/.env.example docker/.env
```

2. Start infrastructure, or start the whole platform in one command.

```bash
bash scripts/dev/start-containers.sh --with-monitoring
# or
bash scripts/dev/start-platform.sh --with-monitoring
```

Windows PowerShell:

```powershell
powershell -File scripts/dev/start-containers.ps1 --with-monitoring
# or
powershell -File scripts/dev/start-platform.ps1 --with-monitoring
```

3. Build backend modules.

```bash
mvn -T 1C clean package -DskipTests
```

4. Start backend services if you did not use `start-platform.*`.

```bash
bash scripts/dev/start-services.sh
```

5. Run or build the frontend.

```bash
pnpm --dir my-shop-uniapp install
pnpm --dir my-shop-uniapp dev:h5
# or
pnpm --dir my-shop-uniapp build:h5
```

Notes:

- The MySQL container mounts `db/init/**` and `db/test/**` and bootstraps data through `docker/docker-compose.yml`.
- Service process logs are written under `.tmp/service-runtime/<service>/`.
- For detailed startup flags, linked-host workflows, and monitoring options, use `docs/dev-startup.md`.

## Common URLs

- Frontend: `http://127.0.0.1:18080`
- Gateway OpenAPI / Knife4j: `http://127.0.0.1:18080/doc.html`
- Nacos: `http://127.0.0.1:18080/nacos`
- RocketMQ Dashboard: `http://127.0.0.1:38082`
- MinIO Console: `http://127.0.0.1:19001`
- Elasticsearch: `http://127.0.0.1:19200`
- Kibana: `http://127.0.0.1:15601`
- Prometheus: `http://127.0.0.1:19099`
- Grafana: `http://127.0.0.1:13000`
- SkyWalking UI: `http://127.0.0.1:13001`
- Sentinel Dashboard: `http://127.0.0.1:18718`
- XXL-Job Admin: `http://127.0.0.1:18089`

## Documentation Map

| Document | Purpose |
| --- | --- |
| `docs/backend-api.md` | Current backend route ownership, trust boundary, and API surface |
| `docs/frontend-api.md` | Current UniApp API modules and frontend request rules |
| `docs/backend-runtime.md` | Backend boundary, consistency, cache, and exception rules |
| `docs/dev-startup.md` | Local startup scripts, flags, and linked-host workflows |
| `docs/observability-stack.md` | SkyWalking, Prometheus, and Grafana setup |
| `docs/TEST_SCRIPT_INDEX.md` | Contract, smoke, and performance script entrypoints |
| `db/README.md` | SQL bootstrap layout and execution order |
| `services/*/README.md` | Service-specific responsibility and runtime notes |
