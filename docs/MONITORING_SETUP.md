# Prometheus + Grafana 监控接入说明

## 1. 启动监控组件

建议使用统一启动脚本（包含端口占用清理）：

```bash
powershell -File scripts/dev/start-containers.ps1 --with-monitoring
```

也可以单独启动：

```bash
cd docker
docker compose -f monitoring-compose.yml up -d prometheus grafana
```

访问地址：
- Prometheus: `http://localhost:19099`
- Grafana: `http://localhost:13000`（默认 `admin/admin`）

## 2. 指标抓取目标

Prometheus 已配置抓取以下服务的 `/actuator/prometheus`：
- `host.docker.internal:8080`（gateway）
- `host.docker.internal:8081`（auth-service）
- `host.docker.internal:8082`（user-service）
- `host.docker.internal:8083`（order-service）
- `host.docker.internal:8084`（product-service）
- `host.docker.internal:8085`（stock-service）
- `host.docker.internal:8086`（payment-service）
- `host.docker.internal:8087`（search-service）

## 3. Grafana 自动导入

通过 provisioning 自动导入：
- 数据源：`Prometheus`（uid=`prometheus`）
- 看板：`Cloud Trade Chain`
- 看板：`Cloud Service Overview`
- 看板：`Cloud Acceptance Load`

配置位置：
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

## 5. k6 验收压测指标（k6 -> Prometheus）

`docker/monitoring-compose.yml` 已启用：
- Prometheus remote-write receiver（`--web.enable-remote-write-receiver`）
- `k6` 压测容器（profile: `loadtest`）

脚本位置：
- `tests/perf/k6/acceptance-cases.js`
- `tests/perf/k6/run-acceptance.ps1`
- `tests/perf/k6/run-acceptance.sh`

PowerShell 示例：

```powershell
powershell -File scripts/dev/start-containers.ps1 --with-monitoring
$env:K6_BASE_URL = "http://host.docker.internal:18080"
.\tests\perf\k6\run-acceptance.ps1
```

## 6. 验证步骤

1. 打开 `http://localhost:19099/targets`，确认 `spring-boot` 目标为 `UP`。
2. 访问任一服务 `http://localhost:{port}/actuator/prometheus`，确认返回指标文本。
3. 打开 Grafana 看板，确认交易成功率和消息消费指标有数据。
4. 执行 k6 后，确认 `Cloud Acceptance Load` 看板出现场景指标。
