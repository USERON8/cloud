# k6 主链路压测

该目录用于电商主链路验收压测。

## 文件

- `acceptance-cases.js`：场景定义
- `gateway-route-only.js`：仅验证网关关键路由可达性（不允许 404 / 5xx）
- `order-create-only.js`：仅压测下单接口
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

## 单场景运行

```bash
# 路由可达性压测（示例：24 VUs，30s）
docker compose -f docker/monitoring-compose.yml --profile loadtest run --rm \
  -e K6_BASE_URL=http://host.docker.internal:18080 \
  -e ROUTE_VUS=24 -e ROUTE_DURATION=30s \
  k6 run /scripts/gateway-route-only.js

# 下单压测（示例：12 VUs，30s）
docker compose -f docker/monitoring-compose.yml --profile loadtest run --rm \
  -e K6_BASE_URL=http://host.docker.internal:18080 \
  -e AUTH_USERNAME=<username> -e AUTH_PASSWORD=<password> -e AUTH_USER_TYPE=USER \
  -e SHOP_ID=<shopId> -e ADDRESS_ID=<addressId> -e PRODUCT_ID=<productId> \
  -e ORDER_VUS=12 -e ORDER_DURATION=30s \
  k6 run /scripts/order-create-only.js
```

## 常用变量

- `K6_BASE_URL`（默认 `http://host.docker.internal:18080`）
- `BASE_URL`/`K6_BASE_URL`（脚本优先使用 `K6_BASE_URL`）
- `AUTH_TOKEN`
- `AUTH_USER_ID`（当使用 `AUTH_TOKEN` 时可指定 token 对应用户ID）
- `CASE04_AUTH_TOKEN`（可选，单独用于 case04，适合传商家/管理员 token）
- `CASE07_AUTH_TOKEN`（可选，单独用于 case07，适合传商家/管理员 token）
- `AUTH_LOGIN_MAX_RETRIES`（setup 登录重试次数，默认 `3`）
- `AUTH_LOGIN_RETRY_SLEEP_SECONDS`（setup 登录重试间隔，默认 `1` 秒）
- `DEBUG_CASE02`（`1` 时打印 case02 下单失败响应体）
- `HEALTH_TARGETS`（逗号分隔；默认按 `K6_BASE_URL` 自动推导网关 + `8081~8087` 健康检查）
- `USER_ID`、`SHOP_ID`、`ADDRESS_ID`、`PRODUCT_ID`、`ORDER_ID`、`PAYMENT_ID`
- `CASE_SLEEP_SECONDS`（每轮 case 间隔，默认 `1`）
- `REQUEST_TIMEOUT`（请求超时，默认 `30s`）
- `CASE_SUCCESS_RATE_THRESHOLD`（成功率阈值，默认 `0.9`）
- `CASE_DURATION_P95_THRESHOLD_MS`（case 耗时 p95 阈值，默认 `5000` 毫秒）
- `HTTP_FAILED_RATE_THRESHOLD`（HTTP 失败率阈值，默认 `0.05`）
- `ROUTE_VUS`、`ROUTE_DURATION`（`gateway-route-only.js`）
- `ORDER_VUS`、`ORDER_DURATION`（`order-create-only.js`）
