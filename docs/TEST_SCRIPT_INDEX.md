# Test Script Index

This repository keeps executable test scripts in two stable groups.

## Contract checks

- `docs/tools/check-api-contract.ps1`
  - Static controller contract check.
  - Canonical entry point for API path, `@PathVariable`, and `Result<T>` wrapper checks.

## Performance and smoke tests

- `tests/perf/k6/run-k6.ps1`
- `tests/perf/k6/run-k6.sh`
  - Canonical k6 runners.
  - Pick scenarios with `acceptance`, `smoke`, `search-chain`, `search-max`, `route-only`, or `order-only`.

- `tests/perf/k6/*.js`
  - Scenario implementations.

- `tests/perf/k6/lib/preflight.ps1`
- `tests/perf/k6/lib/preflight.sh`
  - Shared preflight checks for k6 runs.

## Cleanup rules

- Thin per-scenario wrapper scripts have been removed; use `run-k6.ps1` or `run-k6.sh`.
- Duplicate copies of the API contract checker have been removed; keep only `docs/tools/check-api-contract.ps1`.
