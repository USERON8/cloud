# 可观测性栈（SkyWalking + Prometheus + Grafana）
Version: 1.1.0

本文说明如何在本项目中同时监控业务服务和中间件。

## 1. 启动基础设施与监控组件

```bash
bash scripts/dev/start-containers.sh --with-monitoring
# 兼容 Windows PowerShell:
# powershell -File scripts/dev/start-containers.ps1 --with-monitoring
```

该命令会启动：
- Prometheus
- Grafana
- Redis Exporter
- MySQL Exporter
- Nginx Exporter
- Elasticsearch Exporter
- Blackbox Exporter

## 2. 启动服务并接入 SkyWalking

### 2.1 SkyWalking OAP/UI

`docker/docker-compose.yml` 已包含：
- `skywalking-oap`（gRPC: `11800`，HTTP: `12800`，Prometheus telemetry: `1234`）
- `skywalking-ui`（`13001`）

### 2.2 Java 服务注入 SkyWalking Agent

`start-platform.*` 和 `start-services.*` 现在会默认尝试接入 SkyWalking：
- 优先使用 `SKYWALKING_AGENT_PATH`
- 否则复用 `.tmp/skywalking/` 下的缓存 agent
- 本地没有缓存时，会自动下载 agent 到 `.tmp/skywalking/downloads/`
- 自动激活 `gateway/webflux/mybatis` 相关 optional plugins，便于查看网关入口、Dubbo、Redis、JDBC/MyBatis 链路

直接启动即可：

```bash
bash scripts/dev/start-platform.sh --with-monitoring
```

如需显式控制：

```bash
export SKYWALKING_AGENT_PATH=/path/to/skywalking-agent.jar
export SKYWALKING_COLLECTOR_BACKEND_SERVICE=127.0.0.1:11800
bash scripts/dev/start-services.sh
```

如需关闭自动接入：

```bash
export SKYWALKING_AUTO_ENABLE=false
```

## 3. Prometheus 抓取范围

Prometheus 已配置以下抓取：
- 全部 Spring Boot 服务：`8080`~`8087` 的 `/actuator/prometheus`
- SkyWalking OAP telemetry：`http://127.0.0.1:1234/metrics`
- Redis Exporter
- MySQL Exporter
- Nginx Exporter
- Elasticsearch Exporter
- MinIO 指标：`/minio/v2/metrics/cluster`
- Blackbox HTTP/TCP 可用性探测（Nacos、RocketMQ、Kibana、SkyWalking、Sentinel、XXL-Job 等）

## 4. Grafana 看板

新增看板：
- `Cloud Middleware & Services`（UID: `cloud-middleware-services`）

主要展示：
- 服务与中间件 `up` 状态
- Blackbox 探测可用性
- 服务 HTTP RPS / P95
- Redis/MySQL QPS
- MySQL 连接线程
- Nginx 连接与请求速率
- Elasticsearch 健康与文档量
- MinIO 请求速率

## 5. 访问地址

- Prometheus: `http://127.0.0.1:19099`
- Grafana: `http://127.0.0.1:13000`（默认 `admin/admin`）
- SkyWalking UI: `http://127.0.0.1:13001`

在 SkyWalking UI 里可以优先查看：
- `Services` / `Topology`: HTTP + Dubbo 调用链
- `Trace`: 单次请求里的 Redis/JDBC/MyBatis span
- `Database`: 慢 SQL 与数据库耗时分布

## 6. 常见问题

- 如果某些 `host.docker.internal` 抓取失败：
  - 确认 Docker Desktop 已运行并支持 `host-gateway`。
  - 确认本机对应端口已监听（例如服务 `808x`、中间件映射端口）。
- 如果 `spring-boot` 某服务 `up=0`：
  - 检查该服务是否已启动。
  - 检查该服务的 `/actuator/prometheus` 是否被鉴权或被网关策略拦截。
- 如果 SkyWalking UI 没有服务数据：
  - 检查 `.tmp/service-runtime/<service>/skywalking-agent/` 下的 agent 日志。
  - 检查服务启动参数里是否包含 `-javaagent:.../skywalking-agent.jar`。
  - 检查 `skywalking-oap` 的 `11800` 端口是否可达。
