# Test Script Index

This repository keeps executable test scripts in two stable groups.

## Contract checks

- `scripts/tools/check-api-contract.sh`
- `scripts/tools/check-api-contract.ps1`
  - Static controller contract check.
  - Canonical entry point for API path, `@PathVariable`, and `Result<T>` wrapper checks.

## Performance and smoke tests

- `scripts/ci/smoke-local.sh`
- `scripts/ci/smoke-local.ps1`
  - Local smoke checks for the started platform.
  - Treat secured actuator endpoints (`401`/redirect) as healthy.

- `tests/perf/k6/run-k6.sh`
- `tests/perf/k6/run-k6.ps1`
  - Canonical k6 runners.
  - Pick scenarios with `acceptance`, `smoke`, `search-chain`, `search-max`, `route-only`, or `order-only`.

- `tests/perf/k6/*.js`
  - Scenario implementations.

- `tests/perf/k6/lib/preflight.sh`
- `tests/perf/k6/lib/preflight.ps1`
  - Shared preflight checks for k6 runs.

## Cleanup rules

- Thin per-scenario wrapper scripts have been removed; use `run-k6.sh` or `run-k6.ps1`.
- Keep the API contract checker under `scripts/tools/`; prefer `check-api-contract.sh` in WSL/Linux.
