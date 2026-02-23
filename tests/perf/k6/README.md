# k6 Acceptance Load

This folder contains the load script for 8 acceptance scenarios.

## Files

- `acceptance-cases.js`: scenario definitions and case metrics.
- `run-acceptance.ps1`: run with Docker Compose on PowerShell.
- `run-acceptance.sh`: run with Docker Compose on shell.

## Quick Start

1. Start monitoring stack:

```bash
docker compose -f docker/monitoring-compose.yml up -d prometheus grafana
```

2. Set required environment variables (example):

```bash
export K6_BASE_URL=http://host.docker.internal:80
export AUTH_TOKEN=<jwt>
export USER_ID=1
export SHOP_ID=1
export PRODUCT_ID=1
```

3. Run:

```bash
./tests/perf/k6/run-acceptance.sh
```

## Optional Inputs

- `AUTH_USERNAME` + `AUTH_PASSWORD` (if `AUTH_TOKEN` is not provided)
- `ORDER_ID`, `ORDER_NO`, `PAYMENT_ID`
- `CASE_VUS`, `CASE_DURATION`, `CASE_STAGE_SECONDS`
- `HEALTH_TARGETS` (comma-separated URL list)

## Output Metrics

- `k6_acceptance_case_total{case_id,case_name,result}`
- `k6_acceptance_case_failed_total{case_id,case_name}`
- `k6_acceptance_case_skipped_total{case_id,case_name}`
- `k6_acceptance_case_duration_ms_*`
- built-in `k6_http_reqs_total`, `k6_checks_rate`
