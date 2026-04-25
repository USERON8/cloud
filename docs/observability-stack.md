# Observability Stack

Updated: 2026-04-22

Current local observability stack includes SkyWalking, Prometheus, Grafana, and several middleware exporters.

## Start

```bash
bash scripts/dev/start-containers.sh --with-monitoring
```

PowerShell:

```powershell
powershell -File scripts/dev/start-containers.ps1 --with-monitoring
```

To start both services and monitoring together:

```bash
bash scripts/dev/start-platform.sh --with-monitoring
```

## Included Components

- SkyWalking OAP and UI
- Prometheus
- Grafana
- Redis Exporter
- MySQL Exporter
- Nginx Exporter
- Elasticsearch Exporter
- Blackbox Exporter

## Main URLs

- Prometheus: `http://127.0.0.1:19099`
- Grafana: `http://127.0.0.1:13000`
- SkyWalking UI: `http://127.0.0.1:13001`

## What To Watch

- SkyWalking:
  - HTTP and Dubbo topology
  - Redis, JDBC, and MyBatis spans
  - slow SQL and trace sampling
- Prometheus and Grafana:
  - service `up`
  - HTTP throughput and latency
  - Redis and MySQL load
  - Nginx request rate
  - Elasticsearch health
  - outbox backlog and MQ consumer lag

## Governance Entry Points

- `GET /api/admin/mq/consumers`
- `GET /api/admin/mq/dead-letters/pending`
- `POST /api/admin/mq/dead-letters/handle`
- `GET /api/admin/outbox/stats`
- `GET /api/admin/outbox/pending`
- `GET /api/admin/outbox/dead`
- `POST /api/admin/outbox/requeue`
- `POST /api/admin/outbox/requeue-batch`
- `GET /api/admin/observability/grafana`
- `GET /api/admin/observability/grafana/open`

## Common Problems

- No SkyWalking data:
  - check `.tmp/service-runtime/<service>/skywalking-agent/`
  - confirm `-javaagent` is attached
  - confirm OAP `11800` is reachable
- Service metrics missing:
  - confirm the service is started
  - confirm `/actuator/prometheus` is reachable in the local environment
- Blackbox failures:
  - verify Docker Desktop and `host.docker.internal`
  - verify mapped local ports are actually listening
