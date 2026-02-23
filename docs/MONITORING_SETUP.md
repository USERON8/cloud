# Prometheus + Grafana 接入说明

## 1. 启动监控栈

```bash
docker compose -f docker/monitoring-compose.yml up -d prometheus grafana
```

访问地址：

- Prometheus: `http://localhost:9099`
- Grafana: `http://localhost:3000`（默认 `admin/admin`）

## 2. 指标抓取目标

Prometheus 已配置抓取以下服务的 `/actuator/prometheus`：

- `host.docker.internal:80`（gateway）
- `host.docker.internal:8081`（auth-service）
- `host.docker.internal:8082`（user-service）
- `host.docker.internal:8083`（order-service）
- `host.docker.internal:8084`（product-service）
- `host.docker.internal:8085`（stock-service）
- `host.docker.internal:8086`（payment-service）
- `host.docker.internal:8087`（search-service）

## 3. Grafana 自动导入

已通过 provisioning 自动导入：

- 数据源：`Prometheus`（uid=`prometheus`）
- 看板：`Cloud Trade Chain`
- 看板：`Cloud Service Overview`
- 看板：`Cloud Acceptance Load`

配置文件位置：

- `docker/monitor/grafana/provisioning/datasources/prometheus.yml`
- `docker/monitor/grafana/provisioning/dashboards/dashboards.yml`
- `docker/monitor/grafana/provisioning/dashboards/trade-chain.json`
- `docker/monitor/grafana/provisioning/dashboards/cloud-overview.json`
- `docker/monitor/grafana/provisioning/dashboards/acceptance-load.json`

## 4. 核心业务指标

- `trade_order_total{service,result}`
- `trade_payment_total{service,result}`
- `trade_stock_freeze_total{service,result}`
- `trade_refund_total{service,result}`
- `trade_message_consume_total{service,eventType,result}`

## 5. 验收压测指标（k6 -> Prometheus）

`docker/monitoring-compose.yml` 已开启：

- Prometheus `remote-write receiver`（`--web.enable-remote-write-receiver`）
- `k6` 压测容器（profile: `loadtest`）

压测脚本位置：

- `tests/perf/k6/acceptance-cases.js`
- `tests/perf/k6/run-acceptance.ps1`
- `tests/perf/k6/run-acceptance.sh`

默认会按 8 个验收场景逐个执行，并写入 Prometheus，核心指标：

- `k6_acceptance_case_total{case_id,case_name,result}`
- `k6_acceptance_case_failed_total{case_id,case_name}`
- `k6_acceptance_case_skipped_total{case_id,case_name}`
- `k6_acceptance_case_duration_ms_*`
- `k6_http_reqs_total{scenario}`
- `k6_checks_rate{scenario}`

启动命令示例（PowerShell）：

```powershell
docker compose -f docker/monitoring-compose.yml up -d prometheus grafana
$env:K6_BASE_URL = "http://host.docker.internal:80"
.\tests\perf\k6\run-acceptance.ps1
```

常用环境变量（压测时注入）：

- `AUTH_TOKEN` 或 `AUTH_USERNAME` + `AUTH_PASSWORD`
- `USER_ID`、`SHOP_ID`、`PRODUCT_ID`
- `ORDER_ID`、`ORDER_NO`、`PAYMENT_ID`
- `CASE_VUS`、`CASE_DURATION`、`CASE_STAGE_SECONDS`

## 6. 验证步骤

1. 打开 `http://localhost:9099/targets`，确认 `spring-boot` 任务目标为 `UP`。
2. 访问任一服务 `http://localhost:{port}/actuator/prometheus`，确认返回文本指标。
3. 打开 Grafana 看板，检查交易成功率和消息重试曲线有数据。
4. 执行 `k6` 后，打开 `Cloud Acceptance Load` 看板，确认 8 场景吞吐/成功率/失败与跳过统计有数据。
