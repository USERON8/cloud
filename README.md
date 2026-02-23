# Spring Cloud 微服务架构项目

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2025.0.0.0--preview-orange.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 1. 项目简介与定位

这是一个基于 Spring Cloud + Spring Cloud Alibaba 的微服务后端项目，核心面向电商业务场景，包含认证、网关、用户、商品、库存、订单、支付、搜索等服务。

项目当前重点：

- 统一鉴权：OAuth2.1 + JWT
- 服务治理：Nacos 注册与配置
- 异步解耦：RocketMQ
- 搜索能力：Elasticsearch
- 缓存与并发控制：Redis/Caffeine + Redisson
- 可观测性：Actuator + Prometheus 指标

## 2. 技术栈（仅保留已验证）

| 技术                          | 版本                 | 说明            |
|-----------------------------|--------------------|---------------|
| Java                        | 17                 | 运行时           |
| Spring Boot                 | 3.5.3              | 基础框架          |
| Spring Cloud                | 2025.0.0           | 微服务框架         |
| Spring Cloud Alibaba        | 2025.0.0.0-preview | Nacos 等组件     |
| Spring Security             | 6.x                | 安全框架          |
| Spring Authorization Server | 1.x                | OAuth2.1 授权能力 |
| MyBatis Plus                | 3.5.13             | ORM           |
| MySQL                       | 9.3.0（镜像）          | 关系型数据库        |
| Redis                       | 7.4.5（镜像）          | 缓存            |
| RocketMQ                    | 5.3.2（镜像）          | 消息队列          |
| Elasticsearch               | 9.1.2（镜像）          | 搜索            |
| Redisson                    | 3.51.0             | 分布式锁          |
| Knife4j                     | 4.5.0              | API 文档        |

## 3. 仓库结构与模块职责

```text
cloud/
├── common-module/      # 公共能力：配置、异常、缓存、锁、线程池等
├── api-module/         # 服务间 Feign API 定义
├── gateway/            # 网关服务（端口 80）
├── auth-service/       # 认证服务（端口 8081）
├── user-service/       # 用户服务（端口 8082）
├── order-service/      # 订单服务（端口 8083）
├── product-service/    # 商品服务（端口 8084）
├── stock-service/      # 库存服务（端口 8085）
├── payment-service/    # 支付服务（端口 8086）
├── search-service/     # 搜索服务（端口 8087）
├── docker/             # 中间件与监控 compose 配置
├── docs/               # 项目专题文档
└── sql/                # 初始化与测试 SQL
```

## 4. 快速开始

### 4.1 环境要求

- JDK 17+
- Maven 3.8+
- Docker + Docker Compose

### 4.2 启动基础中间件

```bash
cd docker
docker-compose -f docker-compose.yml up -d
```

该 compose 包含 MySQL、Redis、Nacos、RocketMQ、MinIO、Elasticsearch、Kibana、Nginx。

### 4.3 初始化数据库

初始化脚本位于 `sql/init/`，按需执行：

- `sql/init/infra/nacos/init.sql`
- `sql/init/user-service/init.sql`
- `sql/init/product-service/init.sql`
- `sql/init/stock-service/init.sql`
- `sql/init/order-service/init.sql`
- `sql/init/payment-service/init.sql`

测试数据脚本位于 `sql/test/`：

- `sql/test/user-service/test.sql`
- `sql/test/product-service/test.sql`
- `sql/test/stock-service/test.sql`
- `sql/test/order-service/test.sql`
- `sql/test/payment-service/test.sql`

历史迁移/监控/遗留脚本已归档到 `sql/archive/`。

示例（Windows PowerShell）：

```powershell
mysql -u root -p < sql/init/infra/nacos/init.sql
mysql -u root -p < sql/init/user-service/init.sql
mysql -u root -p < sql/init/product-service/init.sql
mysql -u root -p < sql/init/stock-service/init.sql
mysql -u root -p < sql/init/order-service/init.sql
mysql -u root -p < sql/init/payment-service/init.sql
```

### 4.4 Nacos 基础说明

服务默认通过 `application.yml` 连接 Nacos，并通过：

```yaml
spring:
  config:
    import: optional:nacos:common
```

加载公共配置。请在 Nacos 中维护 `common` 配置内容，可参考 `common-module/src/main/resources/application-common.yml`。

### 4.5 编译项目

```bash
mvn clean install -DskipTests
```

## 5. 服务启动顺序与入口

### 5.1 推荐启动顺序

1. `auth-service` (8081)
2. `gateway` (80)
3. `user-service` (8082)
4. `product-service` (8084)
5. `stock-service` (8085)
6. `search-service` (8087)
7. `order-service` (8083)
8. `payment-service` (8086)

### 5.2 启动主类清单

- `auth-service/src/main/java/com/cloud/auth/AuthApplication.java`
- `gateway/src/main/java/com/cloud/gateway/GatewayApplication.java`
- `user-service/src/main/java/com/cloud/user/UserApplication.java`
- `order-service/src/main/java/com/cloud/order/OrderApplication.java`
- `product-service/src/main/java/com/cloud/product/ProductApplication.java`
- `stock-service/src/main/java/com/cloud/stock/StockApplication.java`
- `payment-service/src/main/java/com/cloud/payment/PaymentApplication.java`
- `search-service/src/main/java/com/cloud/search/SearchApplication.java`

## 6. 常用验证方式

### 6.1 健康检查

```bash
curl http://localhost:80/actuator/health
```

### 6.2 API 文档入口

- 网关聚合文档：`http://localhost:80/doc.html`
- 认证服务：`http://localhost:8081/doc.html`
- 用户服务：`http://localhost:8082/doc.html`
- 订单服务：`http://localhost:8083/doc.html`
- 商品服务：`http://localhost:8084/doc.html`
- 库存服务：`http://localhost:8085/doc.html`
- 支付服务：`http://localhost:8086/doc.html`
- 搜索服务：`http://localhost:8087/doc.html`

### 6.3 关键调用示例

```bash
# 注册
curl -X POST "http://localhost:8081/auth/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "email": "user@example.com",
    "phone": "13800138000",
    "nickname": "新用户",
    "userType": "USER"
  }'

# 登录
curl -X POST "http://localhost:8081/auth/sessions" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "userType": "USER"
  }'

# OAuth2 标准令牌
curl -X POST "http://localhost:8081/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "client_id=web-client" \
  -d "client_secret=WebClient@2024#Secure" \
  -d "scope=read write"

# 通过网关访问用户查询
curl -X GET "http://localhost:80/api/query/users" \
  -H "Authorization: Bearer {access_token}"
```

## 7. 配置与环境

### 7.1 配置文件

每个服务均包含：

- `application.yml`
- `application-dev.yml`
- `application-prod.yml`

### 7.2 Nacos 与本地配置关系

- 本地 `application.yml` 负责基础启动参数（端口、注册中心地址、基础安全配置等）。
- `spring.config.import: optional:nacos:common` 用于加载公共配置。
- 当 Nacos 不可用时，`optional:` 机制允许服务按本地配置继续启动（具体取决于服务功能依赖）。

## 8. 监控与运维

### 8.1 启动监控组件

```bash
cd docker
docker-compose -f monitoring-compose.yml up -d
```

`docker/monitoring-compose.yml` 包含：Prometheus、Grafana、Elasticsearch、Logstash、Kibana。

### 8.2 常用入口

- Prometheus：`http://localhost:9099`
- Grafana：`http://localhost:3000`
- Kibana：`http://localhost:5601`

### 8.3 指标端点

各服务已开启 Actuator 与 Prometheus 端点，可通过：

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/prometheus`

## 9. 扩展文档索引

- `docs/CACHE.md`
- `docs/DISTRIBUTED_LOCK.md`
- `docs/API_DOCUMENTATION_GUIDE.md`
- `docs/API_DOCUMENTATION_STANDARDS.md`
- `docs/ASYNC_CONFIG_USAGE_GUIDE.md`
- `docs/ASYNC_THREADPOOL_OPTIMIZATION.md`
- `docs/CODE_STANDARDIZATION.md`
- `docs/DATABASE_INDEX_OPTIMIZATION.md`
- `docs/FEIGN_INTERFACE_STANDARDS.md`
- `docs/TESTING_FRAMEWORK.md`

## 10. 开发规范与贡献指南

- 开发规范：`RULE.md`

基础贡献流程：

1. Fork 项目
2. 新建分支：`git checkout -b feature/xxx`
3. 提交改动：`git commit -m "feat: ..."`
4. 推送分支：`git push origin feature/xxx`
5. 发起 Pull Request

## 11. 许可证与联系方式

- 许可证：`MIT`，详见 `LICENSE`
- 项目主页：`https://github.com/yourorg/cloud`
- 问题反馈：`https://github.com/yourorg/cloud/issues`
- 技术支持：`support@example.com`
