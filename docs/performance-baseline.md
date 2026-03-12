# Performance Baseline
Version: 1.1.0

This document captures the local-development performance baseline that was established during the final tuning pass in March 2026.

## Scope

- Environment: local host + Docker infrastructure from `docker/docker-compose.yml`
- Startup path: `scripts/dev/start-platform.*` or `scripts/dev/start-services.*`
- Java profile: `dev`
- Observability: SkyWalking javaagent auto-injected by the startup scripts

These numbers are for regression comparison in the current workstation-like environment. They are not a production SLA.

## Current Baseline

### Search read path

- Scenario: direct search service singleton/max path
- Stable ceiling: about `900 QPS`
- Healthy reference point: `300 it/s` with `3` search requests per iteration, `p95 ~= 31.66 ms`, `0%` errors, `0%` timeouts
- Failure zone: around `1800+ QPS`, where `p95` rises to about `1.7 s` and timeout/error ratios become visible

### Order write path

- Scenario: hotspot order creation with Seata AT and stock reservation
- Before parallelization: stable ceiling was about `44 TPS`
- After parallelization and async write optimization:
  - Single SKU stable ceiling: about `112 TPS`
  - Single SKU edge ceiling: about `128-160 TPS`
  - Dual SKU stable ceiling: about `112 TPS`
  - Dual SKU edge ceiling: about `120-128 TPS`
- Interpretation:
  - `112 TPS` on dual SKU corresponds to about `224` stock reservation operations per second
  - Overload starts from the hotspot inventory lock path, not the HTTP layer

## Bottleneck Notes

- The dominant write bottleneck remains the hotspot row lock on `stock_ledger`
- Seata AT global lock contention is the main limiter on the order path
- MySQL tuning reduced local write overhead, but it did not remove the hotspot-lock ceiling

## Active Tuning Baseline

### Application side

- Druid is the active datasource pool for `order-service` and `stock-service`
- Dev SQL logging is suppressed by default to avoid stdout amplification during load
- Dubbo concurrency limits were increased on the order/stock path
- Order placement uses parallelized reservation and async write paths where safe

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
2. Confirm `SkyWalking`, `MySQL`, `Redis`, and `Seata` are healthy
3. Run smoke first
4. Compare new results against the ceilings above, not against raw peak spikes

## Related Docs

- `docs/dev-startup.md`
- `docs/observability-stack.md`
- `docs/seata-order-transaction.md`
- `docs/TEST_SCRIPT_INDEX.md`
