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

### Search pressure scenario after search datetime fix
- Scenario: controlled staged ramp to 12 iterations/s
- Result: still failed thresholds
- `http_req_failed`: `10.29%`
- `search_singleton_latency_ms p95`: `5.00s`
- `search_singleton_timeout_rate`: `7.29%`
- Interpretation:
  - the search datetime bug was fixed;
  - the remaining failures are consistent with gateway-side rate limiting and some timeout behavior under sustained search pressure.

## Current Deployment Readiness Assessment
- Local startup readiness: yes.
- Basic service health readiness: yes.
- Functional smoke readiness: yes.
- Search pressure readiness through current gateway policy: no.

## Remaining Risks
- Search throughput through the gateway is still constrained by current route rate-limiter settings.
- Moderate search pressure can still trigger timeout and rejection behavior.
- Some startup warnings remain from third-party frameworks such as Dubbo and Spring Security; they are non-blocking for local startup but should be reviewed separately if production log cleanliness is required.

## Changed Files In This Validation Round
- `docker/.env`
- `scripts/dev/lib/runtime.ps1`
- `scripts/ci/smoke-local.ps1`
- `tests/perf/k6/lib/preflight.ps1`
- `services/search-service/src/main/java/com/cloud/search/document/ProductDocument.java`
- `services/search-service/src/main/java/com/cloud/search/jackson/FlexibleLocalDateTimeDeserializer.java`

## Git Status
- Local branch created: `codex/startup-and-search-fixes`
- Local commits created successfully.
- Push failed because the remote SSH connection was closed by the remote side.

## Commits
1. `fix: stabilize local startup and verification scripts`
2. `fix: accept legacy datetime format in search documents`
