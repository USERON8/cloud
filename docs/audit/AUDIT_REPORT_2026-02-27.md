# 全服务审计报告（2026-02-27）

## 审计范围
- 后端：`gateway` `auth-service` `user-service` `product-service` `order-service` `payment-service` `stock-service` `search-service`
- 前端：`my-shop-web`
- 基础设施：`docker/docker-compose.yml` `docker/monitoring-compose.yml` `scripts/dev/*` `tests/perf/k6/*`

## 审计结论总览
- 高风险阻断：`auth-service` `gateway` `payment-service` `基础设施`
- 需整改后上线：`user-service` `product-service` `order-service` `stock-service` `search-service` `my-shop-web`
- 本轮未改业务代码，仅输出问题证据与整改建议。

## 1) gateway
### 服务概况
- 职责：统一入口与路由转发、JWT 资源服务器校验、搜索降级。
- 关键入口：`/api/**` `/auth/**` `/gateway/fallback/search`

### 发现清单
1. 标题：`testenv` 配置可将网关变为全放行  
等级：`P0`  
证据：`gateway/src/main/resources/application-testenv.yml:3`，`gateway/src/main/java/com/cloud/gateway/config/ResourceServerConfig.java:56`  
复现条件：启动 `testenv` profile 或注入 `app.security.testenv-bypass-enabled=true`。  
影响范围：网关所有接口鉴权绕过。  
修复建议：仅在本地联调镜像允许该开关；生产镜像强制 `false` 并加启动时阻断校验。  
回归验证：`testenv` 允许放开；`dev/prod` 匿名访问受限接口返回 401/403。

2. 标题：JWT 黑名单 Redis 异常为 fail-open  
等级：`P1`  
证据：`gateway/src/main/java/com/cloud/gateway/config/ResourceServerConfig.java:191`、`:193`  
复现条件：Redis 不可达或超时。  
影响范围：理论上可接受已加入黑名单的 token。  
修复建议：改为 fail-closed（黑名单校验异常时拒绝请求）或至少对高风险接口 fail-closed。  
回归验证：模拟 Redis 断连，黑名单 token 必须被拒绝。

3. 标题：管理与文档端点在网关层公开  
等级：`P2`  
证据：`gateway/src/main/java/com/cloud/gateway/config/ResourceServerConfig.java:78`  
复现条件：匿名访问 `/actuator/**` `/metrics/**` `/doc.html`。  
影响范围：可观测信息暴露、攻击面扩大。  
修复建议：仅内网暴露或加网关鉴权白名单收敛。  
回归验证：公网匿名访问返回 401/403。

4. 标题：CSP 含 `unsafe-inline` 与 `unsafe-eval`  
等级：`P3`  
证据：`gateway/src/main/resources/application.yml:73`  
影响范围：前端 XSS 防护强度下降。  
修复建议：逐步移除 `unsafe-eval`，并通过 nonce/hash 迁移 `unsafe-inline`。  
回归验证：关键页面脚本正常，CSP 报告无新增阻断。

### 服务结论
- `需整改后上线`（存在 1 个 P0，1 个 P1）。

## 2) auth-service
### 服务概况
- 职责：OAuth2/JWT 发放与 GitHub OAuth 登录。

### 发现清单
1. 标题：`testenv` 下全接口 permitAll  
等级：`P0`  
证据：`auth-service/src/main/resources/application-testenv.yml:3`，`auth-service/src/main/java/com/cloud/auth/config/TestSecurityFilterChainConfig.java:23`  
影响范围：认证服务接口可被匿名访问。  
修复建议：仅保留本地联调 profile；生产部署链路禁止 `testenv`。  
回归验证：非 testenv 下匿名调用管理接口应失败。

2. 标题：JWT 密钥允许默认动态生成  
等级：`P1`  
证据：`auth-service/src/main/resources/application.yml:114`，`auth-service/src/main/java/com/cloud/auth/config/JwtPasswordConfig.java:72`  
复现条件：未注入固定密钥。  
影响范围：多实例/重启后签发一致性与可追溯性风险。  
修复建议：生产强制配置固定密钥对，`allow-generated-keypair=false`。  
回归验证：重启前后签发/验签连续可用。

3. 标题：默认 client secret 回退到硬编码值  
等级：`P1`  
证据：`auth-service/src/main/resources/application.yml:67`、`:80`  
影响范围：默认凭据泄露风险。  
修复建议：移除默认 secret，启动前校验必须注入。  
回归验证：未注入 secret 时启动失败并给出明确错误。

4. 标题：默认 token 有效期偏长（含测试 token）  
等级：`P2`  
证据：`auth-service/src/main/resources/application.yml:109`，`auth-service/src/main/resources/application-testenv.yml:6`  
影响范围：凭据泄露后可利用窗口扩大。  
修复建议：缩短默认 TTL，测试 token 仅限测试环境隔离库。  
回归验证：TTL 配置符合安全基线。

5. 标题：默认日志级别过于详细  
等级：`P2`  
证据：`auth-service/src/main/resources/application.yml:101`、`:102`  
影响范围：敏感上下文泄露概率提升。  
修复建议：`dev` 可 debug，默认/生产降为 `warn`。  
回归验证：生产 profile 不输出敏感 debug 日志。

### 服务结论
- `高风险阻断`（P0 + 多个 P1）。

## 3) user-service
### 服务概况
- 职责：用户/商家/管理员/资料与地址管理。

### 发现清单
1. 标题：继承公共资源服务配置的 test bypass 风险  
等级：`P0`  
证据：`user-service/src/main/resources/application-testenv.yml:3`，`common-module/src/main/java/com/cloud/common/config/BaseResourceServerConfig.java:69`、`:72`  
影响范围：testenv 下全接口绕过。  
修复建议：同 gateway/auth，环境硬隔离与启动防误用。  
回归验证：非 testenv 鉴权生效。

2. 标题：`/actuator/**` 在公共安全配置中公开  
等级：`P2`  
证据：`common-module/src/main/java/com/cloud/common/config/BaseResourceServerConfig.java:115`  
影响范围：健康与指标面暴露。  
修复建议：收敛为内网或鉴权访问。  
回归验证：外部匿名不可访问。

### 服务结论
- `需整改后上线`。

## 4) product-service
### 服务概况
- 职责：商品与分类管理。

### 发现清单
1. 标题：继承公共 test bypass 风险  
等级：`P0`  
证据：`product-service/src/main/resources/application-testenv.yml:3`，`common-module/src/main/java/com/cloud/common/config/BaseResourceServerConfig.java:69`  
修复建议：同上。

2. 标题：公共 CORS 策略允许任意来源（含凭据）  
等级：`P2`  
证据：`common-module/src/main/java/com/cloud/common/config/BaseResourceServerConfig.java:58`、`:59`  
影响范围：跨域调用面过宽。  
修复建议：改为白名单域名，分环境配置。  
回归验证：仅允许期望来源。

### 服务结论
- `需整改后上线`。

## 5) order-service
### 服务概况
- 职责：订单与退款，消费支付/库存事件。

### 发现清单
1. 标题：消费异常后释放幂等锁，存在重复副作用窗口  
等级：`P1`  
证据：`order-service/src/main/java/com/cloud/order/messaging/OrderMessageConsumer.java:76`、`:119`、`:199`  
复现条件：处理链路部分成功后抛异常。  
影响范围：订单状态/退款回写重复执行风险。  
修复建议：幂等状态机从“锁”升级为“处理记录 + 业务唯一约束 + 终态不可逆”。  
回归验证：重复消息和异常重试下状态只变化一次。

2. 标题：继承公共 test bypass 风险  
等级：`P0`  
证据：`order-service/src/main/resources/application-testenv.yml:3`，`common-module/.../BaseResourceServerConfig.java:69`

### 服务结论
- `需整改后上线`（含 P0 环境风险与 P1 一致性风险）。

## 6) payment-service
### 服务概况
- 职责：支付记录、回调处理、支付成功事件生产。

### 发现清单
1. 标题：模块编译失败，阻断全量构建与回归  
等级：`P1`  
证据：`payment-service/src/main/java/com/cloud/payment/config/AlipayConfig.java:49`，`mvn -T 1C -DskipITs test` 输出（DefaultAlipayClient 构造器不匹配）  
影响范围：CI/本地全量测试无法通过，发布门禁失效。  
修复建议：按当前 SDK 构造签名修正 `DefaultAlipayClient` 初始化。  
回归验证：`mvn -T 1C -DskipITs test` 全仓通过。

2. 标题：消费异常后释放幂等锁，重复处理窗口  
等级：`P1`  
证据：`payment-service/src/main/java/com/cloud/payment/messaging/PaymentMessageConsumer.java:80`、`:136`  
修复建议：同 order-service。

3. 标题：继承公共 test bypass 风险  
等级：`P0`  
证据：`payment-service/src/main/resources/application-testenv.yml:3`，`common-module/.../BaseResourceServerConfig.java:69`

### 服务结论
- `高风险阻断`（编译阻断 + P0 环境风险）。

## 7) stock-service
### 服务概况
- 职责：库存预占、确认扣减、回滚恢复。

### 发现清单
1. 标题：消费异常后释放幂等锁，重复处理窗口  
等级：`P1`  
证据：`stock-service/src/main/java/com/cloud/stock/messaging/StockMessageConsumer.java:129`、`:201`、`:278`  
修复建议：同交易域统一整改。

2. 标题：继承公共 test bypass 风险  
等级：`P0`  
证据：`stock-service/src/main/resources/application-testenv.yml:3`，`common-module/.../BaseResourceServerConfig.java:69`

### 服务结论
- `需整改后上线`。

## 8) search-service
### 服务概况
- 职责：搜索、建议词、热词与推荐。

### 发现清单
1. 标题：继承公共 test bypass 风险  
等级：`P0`  
证据：`search-service/src/main/resources/application-testenv.yml:3`，`common-module/.../BaseResourceServerConfig.java:69`

2. 标题：实现与文档对数据同步方式描述不一致  
等级：`P2`  
证据：`search-service/README.md:7`、`:17` 与 `docs/ENV_CONFIG_MATRIX.md`（当前说明为 Logstash MySQL->ES）  
影响范围：运维与故障排查认知偏差。  
修复建议：统一文档并标注当前生效链路。  
回归验证：文档与部署拓扑一致。

### 服务结论
- `需整改后上线`。

## 9) my-shop-web
### 服务概况
- 职责：Web 端入口，调用 `/api/**` 与 `/auth/**`。

### 发现清单
1. 标题：访问令牌与刷新令牌存储于 `localStorage`  
等级：`P2`  
证据：`my-shop-web/src/auth/session.ts:30`~`:35`、`:42`~`:48`  
影响范围：若发生 XSS，token 易被窃取。  
修复建议：优先迁移到 HttpOnly Cookie（后端配合）；短期至少加强 CSP 与输入净化。  
回归验证：token 不可被前端脚本直接读取（目标态）。

2. 标题：前端路由权限仅作展示/跳转控制  
等级：`P3`  
证据：`my-shop-web/src/router/index.ts:97`~`:116`  
影响范围：前端权限不可作为安全边界（需后端兜底，当前后端已承担）。  
修复建议：保留后端强校验并在前端文档明确“仅 UX 控制”。  
回归验证：越权 API 请求由后端拒绝。

### 服务结论
- `需整改后上线`。

## 10) 基础设施与运行面
### 发现清单
1. 标题：多个组件使用弱默认口令/明文凭据  
等级：`P1`  
证据：`docker/docker-compose.yml:22`、`:107`、`:236`，`docker/monitoring-compose.yml:48`，`docker/docker-compose/minio/config/minio.conf:3`  
影响范围：低门槛未授权访问。  
修复建议：全部改为环境注入密钥；默认值仅允许本地临时 profile。  
回归验证：未提供密钥时启动失败。

2. 标题：Elasticsearch 显式关闭安全  
等级：`P1`  
证据：`docker/docker-compose.yml:254`  
影响范围：ES 数据平面暴露风险。  
修复建议：启用 xpack 安全，至少内网隔离与认证。  
回归验证：匿名访问 ES 被拒绝。

3. 标题：自动化测试覆盖面偏窄  
等级：`P2`  
证据：仓库仅 `11` 个 `src/test` Java 测试文件（以异步配置与局部权限测试为主）  
影响范围：交易主链路缺少稳定回归门禁。  
修复建议：补齐交易事件幂等、支付回调重放、库存回滚场景的集成测试。  
回归验证：新增场景纳入 CI 并持续通过。

### 服务结论
- `高风险阻断`。

## 批次汇总（共性问题）
- 共性 1：`testenv` 放开机制在所有服务存在，需强制环境隔离与发布防线。
- 共性 2：交易域幂等以“锁+释放”为主，需升级为“处理记录+业务约束+终态机”。
- 共性 3：默认凭据与公开管理端点增加整体攻击面。

