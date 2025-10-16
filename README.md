# Spring Cloud 微服务架构项目

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2025.0.0.0--preview-orange.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 项目简介

这是一个基于 Spring Cloud 和 Spring Cloud Alibaba 的现代化微服务架构项目，采用最新的技术栈和最佳实践，提供完整的企业级解决方案。

### 🎯 核心特性

- ✅ **微服务架构**: 基于 Spring Cloud 2025.0.0 的完整微服务体系
- ✅ **服务治理**: 集成 Nacos 实现服务注册、发现和配置管理
- ✅ **认证授权**: OAuth2.1 + JWT 的安全认证体系,支持GitHub第三方登录
- ✅ **API网关**: Spring Cloud Gateway 实现统一入口、JWT验证、限流熔断
- ✅ **分布式事务**: Seata 支持跨服务事务一致性
- ✅ **消息队列**: RocketMQ 实现异步消息处理和事件驱动
- ✅ **搜索服务**: Elasticsearch 提供全文搜索能力(无SQL依赖架构)
- ✅ **多级缓存**: Redis + Caffeine 双层缓存,支持本地缓存和分布式缓存
- ✅ **分布式锁**: Redisson 实现分布式锁(支持注解方式)
- ✅ **限流降级**: Gateway层限流 + Resilience4j 熔断保护
- ✅ **链路追踪**: 完整的请求追踪和性能监控
- ✅ **API文档**: Knife4j 提供交互式API文档(网关聚合)

### 🏗️ 技术栈

| 技术                   | 版本                 | 说明        |
|----------------------|--------------------|-----------|
| Spring Boot          | 3.5.3              | 基础框架      |
| Spring Cloud         | 2025.0.0           | 微服务框架     |
| Spring Cloud Alibaba | 2025.0.0.0-preview | 阿里巴巴微服务组件 |
| Spring Security      | 6.x                | 安全框架      |
| OAuth2               | 2.1                | 认证授权标准    |
| Nacos                | 2.x                | 服务注册与配置中心 |
| Sentinel             | 1.8+               | 流量控制组件    |
| Seata                | 2.x                | 分布式事务     |
| RocketMQ             | 5.x                | 消息队列      |
| MySQL                | 9.3.0              | 关系型数据库    |
| Redis                | 7.x                | 缓存数据库     |
| Elasticsearch        | 8.x                | 搜索引擎      |
| MyBatis Plus         | 3.5.13             | ORM框架     |
| Redisson             | 3.51.0             | Redis客户端  |

---

## 📦 项目结构

```
cloud/
├── common-module/          # 公共模块 - 通用工具和配置
├── api-module/             # API模块 - Feign客户端定义
├── gateway/                # 网关服务 - 统一入口
├── auth-service/           # 认证服务 - OAuth2授权服务器
├── user-service/           # 用户服务 - 用户管理
├── order-service/          # 订单服务 - 订单管理
├── product-service/        # 商品服务 - 商品管理
├── stock-service/          # 库存服务 - 库存管理
├── payment-service/        # 支付服务 - 支付处理
├── search-service/         # 搜索服务 - 全文搜索
├── docker/                 # Docker配置文件
├── docs/                   # 项目文档
└── sql/                    # 数据库脚本
```

### 模块说明

#### 🔧 基础模块

- **common-module**: 提供通用工具类、配置类、异常处理、拦截器等
- **api-module**: 定义所有服务间调用的 Feign Client 接口

#### 🚪 网关层

- **gateway**: API网关，提供路由、鉴权、限流、日志等功能

#### 🔐 认证层

- **auth-service**: OAuth2.1 授权服务器，提供统一认证和授权

#### 💼 业务服务层

- **user-service** (8082): 用户中心 - 用户/商户/管理员管理、收货地址、统计分析、线程池监控
- **order-service** (8083): 订单中心 - 订单全生命周期管理、退款申请与审核、订单导出
- **product-service** (8084): 商品中心 - 商品管理、3级分类树、批量操作、多级缓存
- **stock-service** (8085): 库存中心 - 库存扣减/回滚、分布式锁、低库存预警
- **payment-service** (8086): 支付中心 - 支付宝集成、支付流水、退款处理

#### 📊 支撑服务层

- **search-service** (8087): 搜索中心 - Elasticsearch全文搜索、商品/商家搜索、无SQL依赖架构

---

## 🚀 快速开始

### 前置要求

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- MySQL 8.0+
- Redis 7.0+
- Nacos 2.x
- RocketMQ 5.x (可选)
- Elasticsearch 8.x (可选)

### 环境准备

#### 1. 启动基础服务

```bash
# 启动 Docker 基础服务
cd docker
docker-compose up -d

# 包含: MySQL, Redis, Nacos, RocketMQ, Elasticsearch
```

#### 2. 初始化数据库

```bash
# 执行数据库脚本
cd sql
mysql -u root -p < init.sql
```

#### 3. 配置 Nacos

访问 Nacos 控制台: http://localhost:8848/nacos  
默认账号密码: nacos/nacos

导入配置文件（位于 `docs/nacos-config/`）

### 编译项目

```bash
# 编译整个项目
mvn clean install -DskipTests

# 并行编译（加速）
mvn clean install -DskipTests -T 4
```

### 启动服务

**服务启动顺序** (按依赖关系):

1. ✅ **auth-service** (8081) - 认证服务【必须最先启动】
2. ✅ **gateway** (80) - 网关服务【依赖auth-service】
3. ✅ **user-service** (8082) - 用户服务【auth依赖此服务验证用户】
4. ⚡ **product-service** (8084) - 商品服务【可并行启动】
5. ⚡ **stock-service** (8085) - 库存服务【可并行启动】
6. ⚡ **search-service** (8087) - 搜索服务【可并行启动】
7. ⚡ **order-service** (8083) - 订单服务【依赖product/stock/user】
8. ⚡ **payment-service** (8086) - 支付服务【依赖order】

**注意**: 标记⚡的服务可以在前置服务启动后并行启动。

#### 方式一：IDE 启动（推荐开发环境）

按以下顺序启动各服务的主类：

1. `AuthServiceApplication` - 认证服务 (8081)
2. `GatewayApplication` - 网关服务 (80)
3. `UserServiceApplication` - 用户服务 (8082)
4. `OrderServiceApplication` - 订单服务 (8083)
5. `ProductServiceApplication` - 商品服务 (8084)
6. `StockServiceApplication` - 库存服务 (8085)
7. `PaymentServiceApplication` - 支付服务 (8086)
8. `SearchServiceApplication` - 搜索服务 (8087)

#### 方式二：命令行启动

```bash
# 启动认证服务
cd auth-service
mvn spring-boot:run

# 启动网关服务
cd gateway
mvn spring-boot:run

# ... 依次启动其他服务
```

#### 方式三：Docker 启动（推荐生产环境）

```bash
# 构建镜像
mvn clean package -DskipTests
docker-compose -f docker/docker-compose-services.yml build

# 启动所有服务
docker-compose -f docker/docker-compose-services.yml up -d
```

### 验证服务

```bash
# 检查服务健康状态
curl http://localhost:80/actuator/health

# 访问 API 文档
# 网关文档: http://localhost:80/doc.html
# 认证服务: http://localhost:8081/doc.html
```

---

## 🔑 OAuth2 认证流程

### 1. 用户注册并获取令牌

```bash
# 注册新用户(自动返回访问令牌)
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
```

### 2. 用户登录获取令牌

```bash
# 方式1: 简化登录接口(推荐)
curl -X POST "http://localhost:8081/auth/sessions" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "userType": "USER"
  }'

# 方式2: OAuth2标准接口
curl -X POST "http://localhost:8081/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "client_id=web-client" \
  -d "client_secret=WebClient@2024#Secure" \
  -d "scope=read write"
```

### 3. GitHub OAuth2 登录

```bash
# 获取GitHub登录URL
curl -X GET "http://localhost:8081/auth/oauth2/github/login-url"

# 用户访问返回的URL完成GitHub授权
# GitHub回调后自动创建用户并返回JWT令牌
```

### 4. 使用访问令牌

```bash
# 通过网关调用受保护的API
curl -X GET "http://localhost:80/api/query/users" \
  -H "Authorization: Bearer {access_token}"

# 刷新令牌
curl -X POST "http://localhost:8081/auth/tokens/refresh" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "YOUR_REFRESH_TOKEN"}'

# 用户登出(撤销令牌)
curl -X DELETE "http://localhost:8081/auth/sessions" \
  -H "Authorization: Bearer {access_token}"
```

---

## 📚 API 文档

### 访问地址

- **网关聚合文档**: http://localhost:80/doc.html (推荐,包含所有服务)
- **认证服务**: http://localhost:8081/doc.html
- **用户服务**: http://localhost:8082/doc.html
- **订单服务**: http://localhost:8083/doc.html
- **商品服务**: http://localhost:8084/doc.html
- **库存服务**: http://localhost:8085/doc.html
- **支付服务**: http://localhost:8086/doc.html
- **搜索服务**: http://localhost:8087/doc.html

### 核心API概览

| 服务 | 端口 | 主要功能 | 核心接口数 |
|---------|------|----------|---------|
| Gateway | 80   | 路由转发、JWT验证、限流熔断 | N/A |
| Auth    | 8081 | 用户认证、令牌管理、第三方登录 | 20+ |
| User    | 8082 | 用户/商户/管理员管理、地址管理、统计分析 | 35+ |
| Order   | 8083 | 订单管理、退款管理 | 18+ |
| Product | 8084 | 商品管理、3级分类树、批量操作 | 25+ |
| Stock   | 8085 | 库存扣减、库存预警 | 12+ |
| Payment | 8086 | 支付宝支付、支付流水、退款 | 15+ |
| Search  | 8087 | 商品/商家搜索、索引管理 | 10+ |

**总计API接口**: 135+ 个

---

## 🔧 配置管理

### 配置中心

项目使用 Nacos 作为配置中心，支持动态配置刷新。

### 配置文件说明

```yaml
# bootstrap.yml - 启动配置
spring:
  application:
    name: service-name
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
```

### 环境配置

支持多环境配置：

- `application.yml` - 默认配置
- `application-dev.yml` - 开发环境
- `application-test.yml` - 测试环境
- `application-prod.yml` - 生产环境

激活方式：

```bash
# 开发环境
java -jar service.jar --spring.profiles.active=dev

# 生产环境
java -jar service.jar --spring.profiles.active=prod
```

---

## 🧪 测试

### 单元测试

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl user-service
```

### 集成测试

```bash
# 运行集成测试
mvn verify -P integration-test
```

### 性能测试

使用 JMeter 或 Gatling 进行性能测试，测试脚本位于 `docs/performance-test/`

---

## 📊 监控与运维

### 服务监控

- **Spring Boot Admin**: http://localhost:9090
- **Nacos 控制台**: http://localhost:8848/nacos
- **Sentinel 控制台**: http://localhost:8080/sentinel

### 日志管理

- 日志文件位置: `logs/`
- 日志级别配置: 在 Nacos 中动态调整

### 链路追踪

- **Zipkin**: http://localhost:9411

### 指标监控

所有服务暴露 Prometheus 指标端点: `/actuator/prometheus`

---

## 🐳 Docker 部署

### 构建镜像

```bash
# 构建所有服务镜像
./docker/build-all.sh

# 构建单个服务
docker build -t cloud/auth-service:latest -f auth-service/Dockerfile .
```

### 启动服务

```bash
# 启动所有服务
docker-compose -f docker/docker-compose-services.yml up -d

# 查看服务状态
docker-compose -f docker/docker-compose-services.yml ps

# 查看日志
docker-compose -f docker/docker-compose-services.yml logs -f auth-service
```

---

## 💾 缓存系统

### 架构说明

项目采用**双层缓存架构**(L1 + L2):

- **L1缓存**: Caffeine 本地缓存(响应时间1-5ms)
- **L2缓存**: Redis 分布式缓存(响应时间10-20ms)

### 核心特性

- ✅ **自动降级**: L1未命中自动查询L2
- ✅ **自动回填**: L2命中后自动回填L1
- ✅ **灵活配置**: 支持单Redis或多级缓存切换
- ✅ **指标监控**: 内置命中率、响应时间等指标
- ✅ **预热策略**: 启动时自动预热热点数据
- ✅ **统一管理**: REST API管理缓存

### 快速开始

在 `application-common.yml` 中启用:

```yaml
cache:
  multi-level: true  # 启用多级缓存
  ttl:
    user: 1800      # 用户缓存30分钟
    product: 2700   # 商品缓存45分钟
    stock: 300      # 库存缓存5分钟
```

代码中使用标准Spring Cache注解:

```java
@Cacheable(cacheNames = "user", key = "#userId")
public UserDTO getUserById(Long userId) {
    return userMapper.selectById(userId);
}
```

### 监控管理

访问缓存监控API: `http://localhost:8081/api/cache/monitor/stats`

查看缓存命中率、响应时间等指标。

### 详细文档

完整的缓存使用指南请查看: [docs/CACHE.md](docs/CACHE.md)

---

## 🔒 分布式锁

### 技术选型

项目使用 **Redisson** 实现分布式锁,支持多种锁类型和灵活配置。

**核心特性:**

- 基于Redis的高性能分布式锁
- 支持可重入锁、公平锁、读写锁
- Watch Dog自动续期机制
- 支持注解和编程两种方式

### 快速开始

**注解方式** (推荐):

```java
@Service
public class StockService {

    @DistributedLock(
        key = "'stock:' + #productId",
        waitTime = 5,
        leaseTime = 10
    )
    public void deductStock(Long productId, Integer quantity) {
        // 业务逻辑自动在锁保护下执行
        stockMapper.deduct(productId, quantity);
    }
}
```

**编程方式**:

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final DistributedLockTemplate lockTemplate;

    public void processOrder(Long orderId) {
        lockTemplate.execute(
            "order:" + orderId,
            Duration.ofSeconds(10),
            () -> {
                // 业务逻辑
                orderMapper.updateStatus(orderId);
            }
        );
    }
}
```

### 锁类型

| 类型            | 说明       | 使用场景    |
|---------------|----------|---------|
| **REENTRANT** | 可重入锁(默认) | 通用场景    |
| **FAIR**      | 公平锁      | 需要按顺序处理 |
| **READ**      | 读锁       | 读多写少    |
| **WRITE**     | 写锁       | 写操作保护   |
| **RED_LOCK**  | 红锁       | 高可用场景   |

### 监控管理

访问分布式锁监控API: `http://localhost:8081/api/lock/monitor/stats`

查看锁状态、持有时间等信息。

### 详细文档

完整的分布式锁使用指南请查看: [docs/DISTRIBUTED_LOCK.md](docs/DISTRIBUTED_LOCK.md)

---

## 📖 开发规范

详细开发规范请查看: [RULE.md](RULE.md)

包括：

- 代码规范
- Git 提交规范
- 接口设计规范
- 数据库设计规范
- 异常处理规范

---

## 🔄 版本历史

### v0.1.18 (2025-10-16) 🎉 当前版本

**✨ 新增功能**:
- ✅ 完成Gateway网关服务完整功能(JWT验证、限流、API聚合)
- ✅ 新增完整的退款管理流程(用户申请、商家审核、退款处理)
- ✅ 新增商品分类完整功能(3级树结构、批量操作、分类移动)
- ✅ 新增库存预警功能(低库存查询、预警通知)
- ✅ 新增线程池监控功能(状态查看、动态调整、健康检查)
- ✅ 支付宝完整集成(PC/手机/APP支付、退款)
- ✅ GitHub OAuth2第三方登录完整实现

**🔧 优化改进**:
- ✅ 完善Token管理功能(授权详情、黑名单管理、手动清理)
- ✅ 优化Search服务架构(无SQL依赖、多级缓存)
- ✅ 增强并发控制(Redis分布式锁+乐观锁双重保证)
- ✅ 完善异步处理(RocketMQ事件驱动、线程池监控)

**📝 文档更新**:
- ✅ 更新所有子服务README.md文档
- ✅ 新建Gateway完整文档
- ✅ 更新父项目README.md
- ✅ 新建开发规范RULE.md

**📊 统计数据**:
- 总服务数: 8个
- API接口数: 135+
- Controller数: 27个
- Service实现: 50+
- 数据库表: 15+张

### v0.1.17 (2025-01-XX)

- ✅ 完成订单超时处理
- ✅ 完成订单导出功能
- ✅ 优化支付流水记录

### v0.1.16 (2025-01-XX)

- ✅ 修复多级缓存bug
- ✅ 优化Seata分布式事务配置

---

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证 - 详情请查看 [LICENSE](LICENSE) 文件

---

## 👥 团队

- **架构师**: Cloud Team
- **开发团队**: Backend Development Team
- **运维团队**: DevOps Team

---

## 📞 联系方式

- **项目主页**: https://github.com/yourorg/cloud
- **问题反馈**: https://github.com/yourorg/cloud/issues
- **技术支持**: support@example.com

---

## 🙏 致谢

感谢以下开源项目：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba)
- [Nacos](https://nacos.io/)
- [Sentinel](https://sentinelguard.io/)

---

**Happy Coding!** 🎉

