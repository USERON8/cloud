# Test Script Index

Updated: 2026-04-22

## Contract Checks

- `scripts/tools/check-api-contract.sh`
- `scripts/tools/check-api-contract.ps1`

Purpose:

- verify controller path contracts
- verify `@PathVariable` alignment
- verify `Result<T>` wrapper conventions

## Smoke And Performance

- `scripts/ci/smoke-local.sh`
- `scripts/ci/smoke-local.ps1`
- `tests/perf/k6/run-k6.sh`
- `tests/perf/k6/run-k6.ps1`
- `tests/perf/k6/*.js`
- `tests/perf/k6/lib/preflight.sh`
- `tests/perf/k6/lib/preflight.ps1`

Current k6 scenarios:

- `acceptance-cases.js`
- `all-services-smoke.js`
- `gateway-route-only.js`
- `order-create-only.js`
- `search-chain.js`
- `search-singleton-max.js`

## Usage Notes

- Prefer `run-k6.sh` or `run-k6.ps1` instead of creating new thin wrapper scripts.
- Run smoke checks before pressure tests.
- Historical performance numbers are no longer kept as a separate baseline document; rerun local k6 tests for current results.
- `scripts/tools/check-api-contract.*` currently has two intentional raw-response exceptions:
  - `GET /api/payment-checkouts/{ticket}`
  - `GET /api/admin/observability/grafana/open`
