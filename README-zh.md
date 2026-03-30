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
- 当前不再依赖 Seata 协调器，跨服务一致性统一采用本地事务、Outbox、RocketMQ 和消费端幂等。

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
  - 用户基础信息、地址、管理员、商家、商家认证、统计等缓存路径都已经统一成显式 Redis 单级缓存服务。
  - 异步刷新和头像上传后的缓存回写，也已经统一到同一套显式 Redis 缓存模型。
  - `UserApplication` 还保留了 `@EnableCaching`，但当前用户域业务流程已经不再依赖注解式缓存行为。
- `search-service`
  - 搜索热词列表已经接入 Redis 单级热点缓存。
  - 今日热销商品 ID 列表已经接入 Redis 单级热点缓存。
  - `ElasticsearchOptimizedService` 内部的智能搜索、搜索建议、热词和推荐词都已经统一为 Redis 单级缓存。
  - 搜索热点路径遗留的本地 L1 配置和未使用的 Caffeine 依赖也已经清理掉。
- `product-service`
  - 商品详情继续保留 `ProductDetailCacheService` 的显式多级缓存设计，本地 Caffeine + Redis 仍然是当前热详情页方案。
  - 分类树、店铺查询和统计缓存已经从 `Spring Cache` 注解切到显式 Redis 缓存服务。
  - `ProductApplication` 已不再依赖 `@EnableCaching`。
- `stock-service`
  - 库存账本查询现在采用务实的多级缓存：超短 TTL 的本地 L1 + Redis 账本缓存。
  - 预占、释放、确认、回滚等库存变更，跨请求一致性仍以 Redis Lua 更新结果为准。
- `order-service`
  - 已完成主单的订单聚合查询现在采用务实的多级缓存：超短 TTL 的本地 L1 + Redis 聚合缓存。
- 子单状态流转、发货、售后、退款更新、库存命令链路已经显式接入聚合缓存失效。
- `payment-service`
  - 支付安全和防重路径已经使用显式 Redis 单级缓存，覆盖幂等键、结果复用、短 TTL 状态查询、checkout ticket 和限流计数。
  - 这里没有引入本地 L1，因为这层缓存的目标是正确性支撑和滥用控制，不是业务读加速。
- `auth-service` 与 `gateway`
  - JWT 黑名单校验在 Redis 不可用时默认改为 fail-closed，直接拒绝 token。
  - 用户 access token 和内部 service access token 的默认有效期都已收短到 `PT15M`，避免严格拒绝策略把影响窗口拉得过长。
  - `auth-service` 启动时会校验：在 fail-closed 模式下，不允许再把 access token TTL 配得更长。

## 本轮已确认的问题

- `user-service` 业务缓存路径已经统一到显式 Redis 单级缓存，但框架层的 `@EnableCaching` 开关还在，后续如果继续改造，应该避免重新引入注解式缓存。
- `search-service` 已经去掉热词、智能搜索和建议词的本地 L1 残留，但缓存新鲜度仍主要依赖 TTL 和失效时机，而不是严格的事件驱动失效。
- `product-service` 当前是有意保留的分层方案：商品详情走多级缓存，分类和店铺走 Redis 单级缓存。后续如果继续优化，应该按场景扩展，而不是回退到通用注解缓存。
- `stock-service` 现在已经有本地 L1 账本缓存，但跨节点的一致性仍主要依赖短 TTL 窗口，本轮还没有补事件驱动的 L1 失效。
- `order-service` 的聚合缓存是刻意收窄的，只服务于已完成主单的聚合读优化，后续不应该把它扩成订单状态真相源。
- `payment-service` 的缓存范围是刻意偏防御型和短生命周期的，后续如果要加本地缓存，应该先证明它不会伤到支付正确性或防重语义。
- JWT 黑名单 fail-closed 策略虽然更严格，但它把 Redis 可用性和短 access token TTL 绑定成了一组运维控制项，后续不能只改其中一个。
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

