# Observability Stack (SkyWalking + Prometheus + Grafana)
Version: 1.1.0

This document explains how to monitor both business services and middleware in the current project.

## 1. Start Infrastructure And Monitoring Components

```bash
bash scripts/dev/start-containers.sh --with-monitoring
# Windows PowerShell:
# powershell -File scripts/dev/start-containers.ps1 --with-monitoring
```

This command starts:

- Prometheus
- Grafana
- Redis Exporter
- MySQL Exporter
- Nginx Exporter
- Elasticsearch Exporter
- Blackbox Exporter

## 2. Start Services And Attach SkyWalking

### 2.1 SkyWalking OAP/UI

`docker/docker-compose.yml` already includes:

- `skywalking-oap` (gRPC: `11800`, HTTP: `12800`, Prometheus telemetry: `1234`)
- `skywalking-ui` (`13001`)

### 2.2 Inject The SkyWalking Agent Into Java Services

`start-platform.*` and `start-services.*` try to enable SkyWalking automatically:

- Prefer `SKYWALKING_AGENT_PATH` when provided
- Otherwise reuse the cached agent under `.tmp/skywalking/`
- If no local cache exists, download the agent into `.tmp/skywalking/downloads/`
- Auto-enable optional plugins for `gateway`, WebFlux, and MyBatis so gateway entry, Dubbo, Redis, and JDBC/MyBatis traces remain visible

Start everything directly:

```bash
bash scripts/dev/start-platform.sh --with-monitoring
```

To control the agent explicitly:

```bash
export SKYWALKING_AGENT_PATH=/path/to/skywalking-agent.jar
export SKYWALKING_COLLECTOR_BACKEND_SERVICE=127.0.0.1:11800
bash scripts/dev/start-services.sh
```

To disable automatic attachment:

```bash
export SKYWALKING_AUTO_ENABLE=false
```

## 3. Prometheus Scrape Coverage

Prometheus is configured to scrape:

- All Spring Boot services: `/actuator/prometheus` on `8080` through `8087`
- SkyWalking OAP telemetry: `http://127.0.0.1:1234/metrics`
- Redis Exporter
- MySQL Exporter
- Nginx Exporter
- Elasticsearch Exporter
- MinIO metrics: `/minio/v2/metrics/cluster`
- Blackbox HTTP/TCP availability probes for Nacos, RocketMQ, Kibana, SkyWalking, Sentinel, XXL-JOB, and similar dependencies

## 4. Grafana Dashboards

Added dashboard:

- `Cloud Middleware & Services` (UID: `cloud-middleware-services`)

Main panels cover:

- Service and middleware `up` state
- Blackbox availability probes
- Service HTTP RPS / P95
- Redis/MySQL QPS
- MySQL connection threads
- Nginx connections and request rate
- Elasticsearch health and document volume
- MinIO request rate

## 5. Access URLs

- Prometheus: `http://127.0.0.1:19099`
- Grafana: `http://127.0.0.1:13000` (default `admin/admin`)
- SkyWalking UI: `http://127.0.0.1:13001`

In SkyWalking UI, focus first on:

- `Services` / `Topology`: HTTP + Dubbo call topology
- `Trace`: Redis, JDBC, and MyBatis spans inside one request
- `Database`: slow SQL and latency distribution

## 6. Common Issues

- If some `host.docker.internal` probes fail:
  - Verify Docker Desktop is running and supports `host-gateway`
  - Verify the required local ports are listening, such as service ports `808x` and middleware-mapped ports
- If a Spring Boot service shows `up=0`:
  - Verify the service actually started
  - Check whether `/actuator/prometheus` is blocked by authentication or gateway policy
- If SkyWalking UI shows no service data:
  - Check agent logs under `.tmp/service-runtime/<service>/skywalking-agent/`
  - Confirm the startup parameters include `-javaagent:.../skywalking-agent.jar`
  - Confirm `skywalking-oap` port `11800` is reachable

## 7. Business Metrics And Outbox Focus Points

- Business metrics are reported by `TradeMetrics`: `trade_order_total`, `trade_payment_total`, `trade_refund_total`, `trade_stock_freeze_total`, and `trade_message_consume_total`
- Metric labels include `result` (`success`, `failed`, `retry`) and `eventType` (such as `payment_success` and `refund_completed`)
- Outbox health recommendation: track `NEW`, `FAILED`, and `DEAD` rows in `outbox_event` per database; `DEAD > 0` means the relay or MQ send path needs inspection
- MQ trace note: RocketMQ does not automatically propagate `traceId`, and the Outbox relay does not inject tracing headers either; manual propagation is required for full SkyWalking end-to-end traces
- Dead-letter monitoring note: failed consumers write into the `dead_letter` table, but Grafana does not visualize it by default; add a dedicated collector and dashboard if needed
