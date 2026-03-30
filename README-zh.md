# Cloud Shop Microservices
Version: 1.1.0

[English](./README.md)

简化版电商微服务项目，后端基于 Spring Boot + Spring Cloud Alibaba，前端为 UniApp（Vue 3 + TypeScript）。

## 当前一致性与消息模型

- 订单/库存/退款事件使用本地事务 + `outbox_event` 保证可靠消息。
- Outbox 在各服务内部定时轮询并发布到 RocketMQ。
- 支付成功使用 RocketMQ 事务消息（`payment-service`）。
- 消费端使用 Redis 幂等处理，避免重复。
- 用户通知改为 RocketMQ 异步投递（`user-notification`），失败会重试。
- Seata TCC（下单）和 Seata SAGA（退款）在 `order-service` 中启用；`payment-service` 保持禁用。

## 模块与端口

| 模块 | 端口 | 说明 |
| --- | --- | --- |
| `gateway` | `8080` | 统一网关，转发 `/api/**` 与 `/auth/**` |
| `auth-service` | `8081` | OAuth2/JWT 认证与 GitHub 登录 |
| `user-service` | `8082` | 用户、商家、管理员、资料与地址 |
| `order-service` | `8083` | 订单与退款 |
| `product-service` | `8084` | 商品与分类 |
| `stock-service` | `8085` | 库存与库存变更 |
| `payment-service` | `8086` | 支付与支付宝接入 |
| `search-service` | `8087` | Elasticsearch 搜索 |
| `my-shop-uniapp` | `5173`(dev) | Web/Android/iOS 前端（UniApp） |

## 服务文档索引

| 服务 | 文档 |
| --- | --- |
| `gateway` | [services/gateway/README.md](./services/gateway/README.md) |
| `auth-service` | [services/auth-service/README.md](./services/auth-service/README.md) |
| `user-service` | [services/user-service/README.md](./services/user-service/README.md) |
| `order-service` | [services/order-service/README.md](./services/order-service/README.md) |
| `product-service` | [services/product-service/README.md](./services/product-service/README.md) |
| `stock-service` | [services/stock-service/README.md](./services/stock-service/README.md) |
| `payment-service` | [services/payment-service/README.md](./services/payment-service/README.md) |
| `search-service` | [services/search-service/README.md](./services/search-service/README.md) |

## 当前缓存改造状态

- `user-service`
  - 用户基础信息缓存已经收敛为显式的 Redis 单级缓存。
  - 用户地址缓存已经收敛为显式的 Redis 单级缓存。
  - 管理员、商家、统计、异步预热、文件上传回写等路径仍然在使用 Spring Cache 注解或 `CacheManager`，整体缓存模型仍是混合态。
- `search-service`
  - 搜索热词列表已经接入 Redis 单级热点缓存。
  - 今日热销商品 ID 列表已经接入 Redis 单级热点缓存。
  - `ElasticsearchOptimizedService` 内部的智能搜索、搜索建议、热词和推荐词都已经统一为 Redis 单级缓存。
- 其它服务
  - 本轮未对缓存策略做重构。

## 本轮已确认的问题

- `user-service` 当前同时存在显式 Redis 缓存服务和 Spring Cache 注解，缓存模型并不统一，后续仍需要继续收口。
- `search-service` 已经去掉热词、智能搜索和建议词的本地 L1 残留，但缓存新鲜度仍主要依赖 TTL 和失效时机，而不是严格的事件驱动失效。
- 之前各服务 README 过于简略，无法真实反映运行方式。本轮已补齐，但如果要形成完整运维手册，仍需再做更细的接口与任务审计。
- 历史审计文档仍建议保留参考：
  - [docs/code-audit-2026-03-13-en.md](./docs/code-audit-2026-03-13-en.md)
  - [docs/code-audit-2026-03-13-zh.md](./docs/code-audit-2026-03-13-zh.md)

## 快速启动

1. 启动基础依赖（含端口占用清理）：

```bash
bash scripts/dev/start-containers.sh
# 兼容 Windows PowerShell:
# powershell -File scripts/dev/start-containers.ps1
```

可选：启动完整监控栈（Prometheus + Grafana + exporters）：

```bash
bash scripts/dev/start-containers.sh --with-monitoring
# 兼容 Windows PowerShell:
# powershell -File scripts/dev/start-containers.ps1 --with-monitoring
```

一键启动（容器 + 服务）：

```bash
bash scripts/dev/start-platform.sh --with-monitoring
# 兼容 Windows PowerShell:
# powershell -File scripts/dev/start-platform.ps1 --with-monitoring
```

兼容命令别名：

```bash
bash scripts/dev/start-all.sh --with-monitoring
# 兼容 Windows PowerShell:
# powershell -File scripts/dev/start-all.ps1 --with-monitoring
```

2. 初始化数据库（先 `init` 再 `test`，可选）：详见 `db/README.md`。

说明（当前封闭开发期默认策略）：
- MySQL 容器每次启动都会清空 `/var/lib/mysql` 后重建。
- 启动时自动顺序执行 `db/init/**/*.sql` 与 `db/test/**/*.sql`。
- 不保留历史数据，不做迁移兼容。

3. 构建后端：

```bash
mvn -T 1C clean package -DskipTests
```

4. 启动后端服务（含端口占用清理）：

```bash
bash scripts/dev/start-services.sh
# 兼容 Windows PowerShell:
# powershell -File scripts/dev/start-services.ps1
```

说明：直接执行 `start-services.*` 时，脚本会自动注入本地开发环境需要的基础地址与默认 key/secret，例如 `GATEWAY_SIGNATURE_SECRET`、`CLIENT_SERVICE_SECRET`、`APP_OAUTH2_*_CLIENT_SECRET`、`APP_JWT_ALLOW_GENERATED_KEYPAIR`。
补充：`start-platform.*` / `start-services.*` 会为 8 个业务服务配置 `JAVA_TOOL_OPTIONS` 与 `SW_AGENT_NAME`，使用 `docker/monitor/skywalking/agent/` 的 agent，便于在 SkyWalking UI 中查看 HTTP、Dubbo、Redis、JDBC/MyBatis 链路。

宿主机先验收的工作流：

```bash
bash scripts/dev/start-host-linked.sh --services=gateway,auth-service,user-service
# 兼容 Windows PowerShell:
# powershell -File scripts/dev/start-host-linked.ps1 --services=gateway,auth-service,user-service
```

`start-host-linked.*` 会先拉起基础依赖和指定的宿主机 Java 服务，再校验 `.tmp/acceptance/startup.csv` 及对应 stdout/stderr 日志。只有当服务健康状态为 `UP` 或 `UP_SECURED`、stderr 为空且 stdout 不含关键启动错误模式时，才视为宿主机验收通过。

宿主机验收通过后再接入容器群：

```bash
bash scripts/dev/start-cluster-linked.sh --services=auth-service,user-service
# 兼容 Windows PowerShell:
# powershell -File scripts/dev/start-cluster-linked.ps1 --services=auth-service,user-service
```

`start-cluster-linked.*` 依赖前一步的宿主机验收结果。它会重建 Nginx 和指定服务容器，把应用服务宿主机映射端口避让到 `28080-28087`，对外入口仍保持 `18080`，并把 `NGINX_GATEWAY_UPSTREAM` 切到容器网络内的 `gateway:8080`。

仅重启变更的服务：

```bash
bash scripts/dev/start-platform.sh --skip-containers --services=order-service,stock-service
# 兼容 Windows PowerShell:
# powershell -File scripts/dev/start-platform.ps1 --skip-containers --services=order-service
```

服务进程日志写入 `.tmp/service-runtime/<service>/stdout.log` 与 `.tmp/service-runtime/<service>/stderr.log`。
滚动应用与错误日志默认写入 `services/<service>/logs/`，若模块目录不可写，则输出到 `.tmp/service-runtime/<service>/app-logs/`。

5. 构建前端并部署到 Nginx 静态目录：

```bash
pnpm --dir my-shop-uniapp install
pnpm --dir my-shop-uniapp build:h5
```

## 常用入口

- 前端首页：`http://127.0.0.1:18080`
- 网关 API 文档：`http://127.0.0.1:18080/doc.html`
- Nacos：`http://127.0.0.1:18080/nacos`
- RocketMQ Dashboard：`http://127.0.0.1:38082`
- MinIO Console：`http://127.0.0.1:19001`
- Elasticsearch：`http://127.0.0.1:19200`
- Kibana：`http://127.0.0.1:15601`
- Prometheus：`http://127.0.0.1:19099`
- Grafana：`http://127.0.0.1:13000`
- SkyWalking UI：`http://127.0.0.1:13001`
- Sentinel Dashboard：`http://127.0.0.1:18718`

