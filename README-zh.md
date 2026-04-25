# Cloud Shop Microservices
Version: 1.1.0

[English](./README.md)

Cloud Shop 是一个基于 Spring Boot、Spring Cloud Alibaba、Dubbo、RocketMQ、MySQL、Redis、Elasticsearch 和 UniApp 的电商微服务项目。

## 当前系统模型

- 公网入口：`gateway` 是唯一公网后端入口；本地 Docker 默认通过 Nginx 暴露为 `http://127.0.0.1:18080`。
- 认证与信任：`gateway` 负责校验公网 JWT，并向下游注入带 HMAC 签名的 `X-Internal-*` 身份头。
- 一致性：跨服务写链路统一采用本地事务、`outbox_event`、RocketMQ 和消费端幂等。
- 缓存：业务读路径遵循 Cache-Aside + 延迟双删；`payment-service` 的缓存只用于幂等、防重、短时状态辅助和限流。
- 搜索：`search-service` 基于 Elasticsearch 提供商品和店铺搜索，并配合 Redis 热点数据缓存。

## 模块概览

| 模块 | 端口 | 职责 |
| --- | --- | --- |
| `gateway` | `8080` | 公网路由、JWT 校验、HMAC 转发、限流、降级处理 |
| `auth-service` | `8081` | OAuth2 授权服务、JWT 签发、会话登出、GitHub OAuth 登录 |
| `user-service` | `8082` | 用户、地址、商家、商家认证、管理员域数据 |
| `order-service` | `8083` | 购物车、订单生命周期、售后、超时处理 |
| `product-service` | `8084` | 商品、SKU、SPU、分类管理 |
| `stock-service` | `8085` | 库存预占、释放、确认、台账查询 |
| `payment-service` | `8086` | 支付单、收银台会话、退款、支付回调 |
| `search-service` | `8087` | 商品搜索、店铺搜索、联想词、推荐 |
| `governance-service` | `8088` | 管理后台聚合、MQ/Outbox 治理、可观测性入口 |
| `my-shop-uniapp` | `5173`（dev） | UniApp 前端（H5 / App） |

## 目录说明

- `common-parent/`：共享基础模块，如 `common-api`、`common-db`、`common-security`、`common-messaging`
- `services/`：后端服务和服务级 README
- `my-shop-uniapp/`：UniApp 前端
- `db/`：初始化和测试 SQL
- `scripts/dev/`：本地启动脚本
- `docker/`：基础设施、监控和本地容器编排
- `docs/`：API、启动、事务、缓存、可观测性文档

## 快速启动

1. 准备环境文件。

```bash
cp .env.example .env
cp docker/.env.example docker/.env
```

PowerShell：

```powershell
Copy-Item .env.example .env
Copy-Item docker/.env.example docker/.env
```

2. 启动基础设施，或一键启动整个平台。

```bash
bash scripts/dev/start-containers.sh --with-monitoring
# 或
bash scripts/dev/start-platform.sh --with-monitoring
```

Windows PowerShell：

```powershell
powershell -File scripts/dev/start-containers.ps1 --with-monitoring
# 或
powershell -File scripts/dev/start-platform.ps1 --with-monitoring
```

3. 构建后端。

```bash
mvn -T 1C clean package -DskipTests
```

4. 如果没有使用 `start-platform.*`，单独启动后端服务。

```bash
bash scripts/dev/start-services.sh
```

5. 启动或构建前端。

```bash
pnpm --dir my-shop-uniapp install
pnpm --dir my-shop-uniapp dev:h5
# 或
pnpm --dir my-shop-uniapp build:h5
```

说明：

- MySQL 容器会挂载 `db/init/**` 和 `db/test/**`，启动时按 `docker/docker-compose.yml` 的引导脚本初始化数据。
- 服务运行日志位于 `.tmp/service-runtime/<service>/`。
- 更完整的启动参数、宿主机联调和监控说明见 `docs/dev-startup.md`。

## 常用入口

- 前端：`http://127.0.0.1:18080`
- 网关 OpenAPI / Knife4j：`http://127.0.0.1:18080/doc.html`
- Nacos：`http://127.0.0.1:18080/nacos`
- RocketMQ Dashboard：`http://127.0.0.1:38082`
- MinIO Console：`http://127.0.0.1:19001`
- Elasticsearch：`http://127.0.0.1:19200`
- Kibana：`http://127.0.0.1:15601`
- Prometheus：`http://127.0.0.1:19099`
- Grafana：`http://127.0.0.1:13000`
- SkyWalking UI：`http://127.0.0.1:13001`
- Sentinel Dashboard：`http://127.0.0.1:18718`
- XXL-Job Admin：`http://127.0.0.1:18089`

## 文档索引

| 文档 | 说明 |
| --- | --- |
| `docs/backend-api.md` | 当前后端路由归属、信任边界和接口面 |
| `docs/frontend-api.md` | 当前 UniApp API 模块和前端请求规则 |
| `docs/backend-runtime.md` | 后端边界、事务一致性、缓存和异常规则 |
| `docs/dev-startup.md` | 本地启动脚本、参数和联调流程 |
| `docs/observability-stack.md` | SkyWalking、Prometheus、Grafana 说明 |
| `docs/TEST_SCRIPT_INDEX.md` | 契约、冒烟和性能脚本入口 |
| `db/README.md` | SQL 初始化目录和执行顺序 |
| `services/*/README.md` | 各服务职责和运行说明 |
