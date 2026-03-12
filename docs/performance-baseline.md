# Performance Baseline
Version: 1.1.0

This document tracks **local-development** performance notes. It is not a production SLA.

## Status (2026-03-12)

- The baseline numbers below are **historical** and were captured before the Outbox relay and thread-pool fast-fail changes.
- After the recent changes (Outbox + fast-fail + gateway caching adjustments), **a new baseline should be re-run**.
- Use the scripts in `tests/perf/k6/` to re-establish current ceilings.

## Environment Scope

- Environment: local host + Docker infrastructure from `docker/docker-compose.yml`
- Startup path: `scripts/dev/start-platform.*` or `scripts/dev/start-services.*`
- Java profile: `dev`
- Observability: SkyWalking javaagent auto-injected by the startup scripts

## Historical Baseline (for reference only)

### Search read path

- Scenario: direct search service singleton/max path
- Stable ceiling: about `900 QPS`
- Healthy reference point: `300 it/s` with `3` search requests per iteration, `p95 ~= 31.66 ms`, `0%` errors, `0%` timeouts
- Failure zone: around `1800+ QPS`, where `p95` rises to about `1.7 s` and timeout/error ratios become visible

### Order write path

- Scenario: hotspot order creation with parallel stock reservation
- Before parallelization: stable ceiling was about `44 TPS`
- After parallelization and async write optimization:
  - Single SKU stable ceiling: about `112 TPS`
  - Single SKU edge ceiling: about `128-160 TPS`
  - Dual SKU stable ceiling: about `112 TPS`
  - Dual SKU edge ceiling: about `120-128 TPS`
- Interpretation:
  - `112 TPS` on dual SKU corresponds to about `224` stock reservation operations per second
  - Overload starts from the hotspot inventory lock path, not the HTTP layer

## Active Tuning Baseline (current behavior)

### Application side

- Druid is the active datasource pool for `order-service` and `stock-service`
- Order placement uses parallel reservation and async write paths where safe
- Thread pools now default to **fast-fail** (`FAST_FAIL`) on queue saturation
- Transactional messaging uses **Outbox + scheduled relay** in `order/payment/stock`

### Database side

The local Docker MySQL profile is intentionally tuned for pressure testing, not durability:

- `log_bin=OFF`
- `slow_query_log=OFF`
- `innodb_flush_log_at_trx_commit=2`
- `sync_binlog=0`
- `innodb_doublewrite=OFF`
- `transaction_isolation=READ-COMMITTED`

Do not reuse this MySQL profile as a production default.

## Regression Workflow

Recommended scripts:

- `tests/perf/k6/run-k6.sh`
- `tests/perf/k6/run-k6.ps1`
- `tests/perf/k6/search-singleton-max.js`
- `tests/perf/k6/search-chain.js`
- `tests/perf/k6/order-create-only.js`
- `tests/perf/k6/all-services-smoke.js`

Before re-running a pressure test:

1. Start the platform with `bash scripts/dev/start-platform.sh --with-monitoring`
2. Confirm `SkyWalking`, `MySQL`, and `Redis` are healthy
3. Run smoke first
4. Compare new results against **fresh runs**, not the historical numbers above

## Related Docs

- `docs/dev-startup.md`
- `docs/observability-stack.md`
- `docs/seata-order-transaction.md`
- `docs/TEST_SCRIPT_INDEX.md`
