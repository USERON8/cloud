# k6 Scripts
Version: 1.1.0

Repository-wide script index: `docs/TEST_SCRIPT_INDEX.md`

## Entrypoints

- `run-k6.sh`
- `run-k6.ps1`

Use the generic runner instead of adding per-scenario wrapper scripts.

## Scenarios

- `acceptance-cases.js`
- `all-services-smoke.js`
- `gateway-route-only.js`
- `order-create-only.js`
- `search-chain.js`
- `search-singleton-max.js`

Runner aliases:

- `acceptance`
- `smoke`
- `route-only`
- `order-only`
- `search-chain`
- `search-max`

## Support Files

- `lib/preflight.sh`
- `lib/preflight.ps1`

## Quick Start

```bash
bash tests/perf/k6/run-k6.sh acceptance
bash tests/perf/k6/run-k6.sh smoke http://host.docker.internal:18080 loadtest
```

```powershell
powershell -File tests/perf/k6/run-k6.ps1 -Scenario acceptance
powershell -File tests/perf/k6/run-k6.ps1 -Scenario smoke -BaseUrl http://host.docker.internal:18080 -Profile loadtest
```

## Common Environment Variables

- `K6_BASE_URL`
- `AUTH_TOKEN`
- `AUTH_USER_ID`
- `AUTH_PRIMARY_ROLE`
- `PAYMENT_INTERNAL_TOKEN`
- `CASE03_AUTH_TOKEN`, `CASE05_AUTH_TOKEN`, `CASE06_AUTH_TOKEN`
- `CASE04_AUTH_TOKEN`, `CASE07_AUTH_TOKEN`
- `USER_ID`, `MERCHANT_ID`, `SPU_ID`, `SKU_ID`
- `PAYMENT_NO`, `REFUND_NO`, `AFTER_SALE_NO`
- `SMOKE_VUS`, `SMOKE_DURATION`, `SMOKE_P95_THRESHOLD_MS`
- `SEARCH_MAIN_VUS`, `SEARCH_MAIN_DURATION`
- `SEARCH_FALLBACK_VUS`, `SEARCH_FALLBACK_DURATION`
- `SERVICE_TARGETS`
- `REQUEST_TIMEOUT`
