# k6 Scripts

This directory contains the canonical k6 runners and scenario files for the cloud shop system.

Repository-wide test script index: `docs/TEST_SCRIPT_INDEX.md`

## Entry points

- `run-k6.ps1`
- `run-k6.sh`

Use the generic runner instead of per-scenario wrapper scripts.

Supported scenarios:

- `acceptance`
- `smoke`
- `search-chain`
- `search-max`
- `route-only`
- `order-only`

## Scenario files

- `acceptance-cases.js`
- `all-services-smoke.js`
- `search-chain.js`
- `search-singleton-max.js`
- `gateway-route-only.js`
- `order-create-only.js`

## Support scripts

- `lib/preflight.ps1`
- `lib/preflight.sh`

## Quick start

```powershell
powershell -File tests/perf/k6/run-k6.ps1 -Scenario acceptance
powershell -File tests/perf/k6/run-k6.ps1 -Scenario smoke -BaseUrl http://host.docker.internal:18080 -Profile loadtest
```

```bash
./tests/perf/k6/run-k6.sh acceptance
./tests/perf/k6/run-k6.sh smoke http://host.docker.internal:18080 loadtest
```

## Common environment variables

- `K6_BASE_URL` default `http://host.docker.internal:18080`
- `AUTH_TOKEN`
- `AUTH_USER_ID`
- `AUTH_PRIMARY_ROLE`
- `PAYMENT_INTERNAL_TOKEN` for internal-only payment callback/refund flows
- `CASE03_AUTH_TOKEN` / `CASE05_AUTH_TOKEN` / `CASE06_AUTH_TOKEN` overriding internal payment flow token per case
- `CASE04_AUTH_TOKEN` / `CASE07_AUTH_TOKEN` overriding stock/search write flow token per case
- `USER_ID` / `MERCHANT_ID` / `SPU_ID` / `SKU_ID`
- `PAYMENT_NO` / `REFUND_NO` / `AFTER_SALE_NO`
- `SMOKE_VUS` / `SMOKE_DURATION` / `SMOKE_P95_THRESHOLD_MS`
- `SEARCH_MAIN_VUS` / `SEARCH_MAIN_DURATION`
- `SEARCH_FALLBACK_VUS` / `SEARCH_FALLBACK_DURATION`
- `SERVICE_TARGETS` comma-separated URL list overriding default smoke targets
- `REQUEST_TIMEOUT`

Protected flows now require pre-issued tokens. `AUTH_TOKEN` is used for user/merchant/admin flows. Internal-only payment cases (`case03`, `case05`, `case06`) should use `PAYMENT_INTERNAL_TOKEN` or the matching `CASE0x_AUTH_TOKEN`. The removed username/password login chain is no longer supported in k6 setup.

## API contract check

```powershell
powershell -ExecutionPolicy Bypass -File docs/tools/check-api-contract.ps1
```
