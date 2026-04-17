# Project Closeout
Version: 1.1.0

This note captures the current frozen state of the repository before the project is temporarily paused.

## What Is Settled

- Unified local startup exists through `scripts/dev/start-platform.*`
- `start-services.*` can run standalone because runtime addresses and development secrets are auto-injected
- Service rolling logs no longer spill into ad-hoc directories under the repository root
- SkyWalking is auto-wired for Java services on startup and now exposes HTTP, Dubbo, Redis, and JDBC/MyBatis traces in the local stack
- Order/payment/stock messaging uses `outbox_event` + scheduled relay for reliable delivery
- Thread pools default to fast-fail on saturation (`FAST_FAIL`)
- Governance operations are now centralized in `governance-service` for statistics, thread-pool inspection, user/admin governance, token governance, MQ governance, outbox governance, notification operations, stock ledger reads, and governed Grafana entrypoints
- Merchant certification and shop certification intentionally remain on the original `user-service` admin surface in this frozen state
- Local performance numbers need re-baselining after recent changes (see `docs/performance-baseline.md`)

## Current Local Runbook

1. Start infrastructure and services:
   - `bash scripts/dev/start-platform.sh --with-monitoring`
2. Run local smoke:
   - `bash scripts/ci/smoke-local.sh`
3. Open the main consoles when needed:
   - Nacos: `http://127.0.0.1:18848`
   - Grafana: `http://127.0.0.1:13000`
   - SkyWalking UI: `http://127.0.0.1:13001`
4. Re-run pressure tests only after smoke succeeds

## Known Constraints

- The current MySQL Docker profile is optimized for local throughput experiments and intentionally reduces durability
- The order path still bottlenecks on hotspot inventory row locking under heavy load
- Secured actuator endpoints are expected to return `401` or redirects during startup health checks
- The first SkyWalking startup needs agent download access unless `.tmp/skywalking/` is already warm

## Recommended Resume Checklist

When work resumes, start from this order:

1. `docs/dev-startup.md`
2. `docs/observability-stack.md`
3. `docs/performance-baseline.md`
4. `docs/transaction-strategy.md`
5. `docs/TEST_SCRIPT_INDEX.md`

## Related Entry Points

- `README.md`
- `README-en.md`
- `scripts/dev/start-platform.sh`
- `scripts/dev/start-platform.ps1`
