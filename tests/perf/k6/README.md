# k6 主链路压测

该目录用于电商主链路验收压测。

## 文件

- `acceptance-cases.js`：场景定义
- `run-acceptance.ps1`：PowerShell 启动
- `run-acceptance.sh`：Shell 启动

## 快速运行

```bash
# PowerShell
powershell -File tests/perf/k6/run-acceptance.ps1

# Shell
./tests/perf/k6/run-acceptance.sh
```

默认会通过 `docker/monitoring-compose.yml` 的 `k6` 服务执行。

## 常用变量

- `K6_BASE_URL`（默认 `http://host.docker.internal:80`）
- `AUTH_TOKEN`
- `USER_ID`、`PRODUCT_ID`、`ORDER_ID`、`PAYMENT_ID`
- `CASE_SLEEP_SECONDS`（每轮 case 间隔，默认 `1`）
- `REQUEST_TIMEOUT`（请求超时，默认 `30s`）
- `CASE_SUCCESS_RATE_THRESHOLD`（成功率阈值，默认 `0.9`）
- `CASE_DURATION_P95_THRESHOLD_MS`（case 耗时 p95 阈值，默认 `5000` 毫秒）
- `HTTP_FAILED_RATE_THRESHOLD`（HTTP 失败率阈值，默认 `0.05`）
