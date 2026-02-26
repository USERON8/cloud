# k6 压测脚本

该目录用于主链路验收、全服务 smoke 和搜索链路压测。

## 推荐入口

- `run-k6.ps1`（PowerShell）
- `run-k6.sh`（Shell）

支持场景：

- `acceptance`
- `smoke`
- `search-chain`
- `search-max`
- `route-only`
- `order-only`

## 快速运行

```powershell
powershell -File tests/perf/k6/run-k6.ps1 -Scenario acceptance
powershell -File tests/perf/k6/run-k6.ps1 -Scenario smoke -BaseUrl http://host.docker.internal:18080 -Profile loadtest
```

```bash
./tests/perf/k6/run-k6.sh acceptance
./tests/perf/k6/run-k6.sh smoke http://host.docker.internal:18080 loadtest
```

## 兼容脚本

以下脚本仍可用，但内部已统一转发到 `run-k6`：

- `run-acceptance.ps1/.sh`
- `run-all-services-smoke.ps1/.sh`
- `run-search-chain.ps1/.sh`
- `run-search-singleton-max.ps1/.sh`

## 常用变量

- `K6_BASE_URL`（默认 `http://host.docker.internal:18080`）
- `AUTH_TOKEN`
- `AUTH_USERNAME` / `AUTH_PASSWORD` / `AUTH_USER_TYPE`
- `SMOKE_VUS` / `SMOKE_DURATION` / `SMOKE_P95_THRESHOLD_MS`
- `SEARCH_MAIN_VUS` / `SEARCH_MAIN_DURATION`
- `SEARCH_FALLBACK_VUS` / `SEARCH_FALLBACK_DURATION`
- `SERVICE_TARGETS`（逗号分隔 URL，覆盖默认 smoke 目标）
- `REQUEST_TIMEOUT`
