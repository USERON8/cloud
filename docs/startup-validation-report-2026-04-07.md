# 2026-04-07 Local Platform Startup And Validation Report

## Scope
- Start all infrastructure containers locally.
- Start all backend services on host.
- Run baseline health verification.
- Run benchmark and pressure tests.
- Capture logs, identify blockers, and fix startup/runtime defects found during validation.

## Environment
- Workspace: `D:\Download\Code\sofware\cloud`
- OS: Windows 11
- Java: 17.0.14
- Maven: 3.9.9
- Docker: 29.3.1 / Docker Desktop 4.67.0
- Date: 2026-04-07

## Startup Result
- Infrastructure containers: started successfully.
- Backend services: all 8 services started successfully.
- Smoke verification: passed.

## Issues Found And Fixes
### 1. Canal host ports were blocked by Windows reserved port range
- Symptom: `canal-server` failed to bind host ports `11110` and `11111`.
- Cause: Windows excluded TCP port range covered `11110-11161`.
- Fix: changed Canal host ports in `docker/.env` from `11110/11111` to `21110/21111`.
- Result: `canal-server` starts normally.

### 2. Host-started services still used old Redis port `16379`
- Symptom: `user-service` failed during startup because Redisson tried to connect to `127.0.0.1:16379`.
- Cause: local runtime startup injected Nacos and RocketMQ variables but did not inject Redis aliases for Spring and Redisson.
- Fix: exported `REDIS_HOST`, `REDIS_PORT`, `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `SPRING_REDIS_HOST`, and `SPRING_REDIS_PORT` in `scripts/dev/lib/runtime.ps1`.
- Result: all backend services now start successfully against Redis on `26379`.

### 3. Smoke script used outdated container names
- Symptom: `scripts/ci/smoke-local.ps1` failed even when containers were healthy.
- Cause: the script expected legacy names like `cloud-mysql`, while current compose uses names like `mysql` and `redis`.
- Fix: updated the smoke script to accept both current and legacy container aliases.
- Result: smoke verification now passes.

### 4. k6 preflight used outdated container names
- Symptom: k6 preflight would fail before load execution.
- Cause: preflight expected legacy container names.
- Fix: updated `tests/perf/k6/lib/preflight.ps1` to support both current and legacy aliases.
- Result: k6 scenarios can start normally.

### 5. Search API failed on legacy datetime format under load
- Symptom: `/api/search/search` returned internal errors during pressure testing.
- Cause: `ProductDocument.createdAt` and `updatedAt` were deserialized through Jackson using `LocalDateTime`, but indexed source values used format `yyyy-MM-dd HH:mm:ss` instead of ISO local datetime.
- Fix: added a flexible `LocalDateTime` deserializer and applied it to `ProductDocument` date fields.
- Result: direct search endpoint regression returned HTTP 200 after the fix, and the previous deserialization error no longer appeared in logs.

### 6. Nacos config was too centralized for service-specific tuning
- Symptom: all services effectively depended on one shared `common.yaml`, which made service-specific runtime tuning opaque and harder to audit.
- Cause: each service imported only `common.yaml` from Nacos.
- Fix: split config loading into `common.yaml` plus one dedicated `*.yaml` per service, and added matching `config_info` seed rows for `gateway`, `auth-service`, `user-service`, `product-service`, `order-service`, `payment-service`, `search-service`, and `stock-service`.
- Result: each service now has a clear per-service config slot while local shared values remain easy to inspect in `common.yaml`.

### 7. Local Nginx convenience entry used an unstable upstream path on Windows Docker Desktop
- Symptom: search pressure through `http://127.0.0.1:18080` intermittently returned `504` after about `5s`, even when gateway and search-service stayed healthy.
- Cause: Nginx connected to host-started services through Docker Desktop host routing, and `host.docker.internal` showed intermittent upstream connect timeouts under concurrent load.
- Fix:
  - switched Nginx config to explicit upstream blocks with keepalive;
  - changed the local Docker env to use the host primary IPv4 instead of `host.docker.internal`;
  - updated `scripts/dev/lib/runtime.ps1` to auto-detect the preferred local IPv4 and write `NGINX_GATEWAY_UPSTREAM` and `NGINX_AUTH_UPSTREAM` accordingly for local runs.
- Result:
  - the `18080` convenience entry improved significantly but still shows occasional Windows-specific connect jitter;
  - the direct gateway entry on `8080` passes the full search pressure scenario cleanly.

### 8. Browser write requests were blocked by gateway signature enforcement
- Symptom: authenticated browser requests like `POST /api/user/address/add/{userId}` returned `401` with `signature headers missing`.
- Cause: `ApiSignatureReplayFilter` enforced HMAC signature headers for all non-GET `/api/**` requests, but the H5 browser client only sends `Authorization: Bearer ...`.
- Fix: bypass signature validation for requests carrying a bearer access token and keep signature enforcement for non-user signed traffic.
- Result: real browser address creation now succeeds with HTTP 200 and no console errors.

### 9. Order list page failed because `order_main` pagination did not use a compliant index
- Symptom: the browser order page returned `500` on `GET /api/orders?page=1&size=30`.
- Cause: `OrderQueryServiceImpl` used `BaseMapper.selectPage(...)` with a generic wrapper on `order_main`, which triggered `IllegalSQLInnerInterceptor` because the query shape did not use an accepted index for `deleted` and `user_id`.
- Fix:
  - added explicit mapper SQL `selectPageActive(...)` with `FORCE INDEX`;
  - added `idx_order_main_user_deleted_id (user_id, deleted, id)` and `idx_order_main_deleted_id (deleted, id)` to the schema seed and the running MySQL instance.
- Result: browser order list requests now return HTTP 200 and the page loads normally.

## Baseline Verification
### Service startup state
- gateway: `UP`
- auth-service: `UP_SECURED`
- user-service: `UP_SECURED`
- order-service: `UP_SECURED`
- product-service: `UP_SECURED`
- stock-service: `UP_SECURED`
- payment-service: `UP_SECURED`
- search-service: `UP_SECURED`

### Smoke script
- Command: `powershell -ExecutionPolicy Bypass -File scripts/ci/smoke-local.ps1 -HttpTimeoutSeconds 30`
- Result: `SMOKE_ALL_OK`

## Benchmark Result
### k6 smoke scenario
- Scenario: `smoke`
- Duration: 30s
- VUs: 5
- Result: passed
- `http_req_failed`: `0.08%`
- `services_smoke_error_rate`: `0.09%`
- `services_smoke_latency_ms p95`: `191.85ms`
- Note: a very small number of transient failures occurred on gateway health and search smart-search paths, but the run stayed within thresholds.

### k6 route-only scenario
- Scenario: `route-only`
- Effective default load from script: 12 VUs for 30s
- Result: thresholds passed, but `gateway_route_unhealthy_rate` reached `3.49%`
- Observation: this scenario mixes protected routes without tokens and is better treated as a routing probe than a production throughput benchmark.

## Pressure Test Result
### Search pressure scenario before search datetime fix
- Scenario: search singleton max with staged ramp to 180 iterations/s
- Result: failed badly
- `http_req_failed`: `24.64%`
- `search_singleton_latency_ms p95`: `8.26s`
- `search_singleton_timeout_rate`: `88.12%`
- Root causes:
  - gateway search route rate limiter started rejecting requests after capacity was exhausted;
  - `/api/search/search` also threw internal errors because of legacy datetime parsing.

### Search pressure scenario through local Nginx convenience entry
- Target: `http://127.0.0.1:18080`
- Scenario: controlled staged ramp to 12 iterations/s
- Result: improved but still not fully stable
- Best observed controlled probe:
  - `http_req_failed`: `0.47%`
  - `http_req_duration p95`: `59.10ms`
- Formal `search-singleton-max.js` run still showed intermittent failures on some runs:
  - `http_req_failed`: up to `3.64%`
  - `search_singleton_timeout_rate`: up to `4.11%`
- Interpretation:
  - remaining failures come from local Nginx upstream connect timeouts on Windows Docker Desktop;
  - this is not the search business path itself.

### Search pressure scenario through direct gateway entry
- Target: `http://127.0.0.1:8080`
- Scenario: `search-singleton-max.js` with staged ramp to 12 iterations/s
- Result: passed all thresholds
- `http_req_failed`: `0.00%`
- `search_singleton_error_rate`: `0.00%`
- `search_singleton_timeout_rate`: `0.00%`
- `http_req_duration p95`: `64.17ms`
- Interpretation:
  - the local host-started gateway plus downstream search chain is stable for the validated pressure profile;
  - the remaining risk is isolated to the Docker Desktop convenience ingress on `18080`.

## Current Deployment Readiness Assessment
- Local startup readiness: yes.
- Basic service health readiness: yes.
- Functional smoke readiness: yes.
- Search pressure readiness through direct gateway entry (`8080`): yes.
- Search pressure readiness through local Nginx convenience entry (`18080`): no.
- Public browser access through `https://6e314e0f.r6.cpolar.cn`: yes for browse, login, address, order list, and payment workspace entry.
- Full browser checkout readiness: no.

## Remaining Risks
- The local Windows Docker Desktop Nginx convenience layer can still show intermittent upstream connect timeouts under pressure.
- If local `18080` is used for pressure verification, results may include host bridge noise that does not reproduce on direct gateway traffic.
- The storefront still lacks a product detail and variant-selection flow for multi-SKU goods, so a real browser user cannot complete add-to-cart and order creation from the current search result UI.
- Some startup warnings remain from third-party frameworks such as Dubbo and Spring Security; they are non-blocking for local startup but should be reviewed separately if production log cleanliness is required.

## Public Browser Verification
### Verified with real browser on `https://6e314e0f.r6.cpolar.cn`
- Home page loads successfully.
- Search suggestions and search results load successfully.
- Login state persists with bearer token in browser storage.
- Address page loads successfully.
- Address creation succeeds with:
  - consignee: `itest`
  - phone: `13492870207`
  - region: `Guangdong / Shenzhen / Nanshan`
  - street: `Keji South 12th Road`
  - detail: `Tower A, Room 1201`
- Cart page loads successfully and displays the default address.
- Order page loads successfully and `GET /api/orders?page=1&size=30` returns `200`.
- Payment page loads successfully without console errors.

### Verified remaining blocker
- Search result entry `Cloud Phone 15` is a multi-SKU product.
- Clicking `加入购物车` in the list view does not create a cart item request.
- The UI only shows the toast `This product has multiple variants and cannot be added from the list view`.
- There is still no browser-exposed product detail / variant picker route, so a real user cannot complete checkout for the current catalog path.

## Changed Files In This Validation Round
- `docker/.env`
- `scripts/dev/lib/runtime.ps1`
- `docker/docker-compose/nginx/config/nginx.conf`
- `scripts/ci/smoke-local.ps1`
- `tests/perf/k6/lib/preflight.ps1`
- `services/gateway/src/main/java/com/cloud/gateway/filter/ApiSignatureReplayFilter.java`
- `services/gateway/src/main/java/com/cloud/gateway/config/GatewayServerNettyConfig.java`
- `services/gateway/src/main/resources/application-route.yml`
- `services/gateway/src/main/resources/application.yml`
- `services/auth-service/src/main/resources/application.yml`
- `services/user-service/src/main/resources/application.yml`
- `services/product-service/src/main/resources/application.yml`
- `services/order-service/src/main/resources/application.yml`
- `services/order-service/src/main/java/com/cloud/order/mapper/OrderMainMapper.java`
- `services/order-service/src/main/java/com/cloud/order/service/impl/OrderQueryServiceImpl.java`
- `services/payment-service/src/main/resources/application.yml`
- `services/stock-service/src/main/resources/application.yml`
- `services/search-service/src/main/resources/application.yml`
- `db/init/infra/nacos/init.sql`
- `db/init/order-service/init.sql`
- `services/search-service/src/main/java/com/cloud/search/document/ProductDocument.java`
- `services/search-service/src/main/java/com/cloud/search/jackson/FlexibleLocalDateTimeDeserializer.java`

## Git Status
- Local branch created: `codex/startup-and-search-fixes`
- Local commits created successfully.
- Push failed because the remote SSH connection was closed by the remote side.

## Commits
1. `fix: stabilize local startup and verification scripts`
2. `fix: accept legacy datetime format in search documents`
