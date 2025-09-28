# 🌐 Cloud 微服务电商平台

<div align="center">

![Version](https://img.shields.io/badge/version-0.0.1--SNAPSHOT-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)
![License](https://img.shields.io/badge/license-Apache%202.0-blue)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)

**🚀 下一代云原生电商微服务平台**

基于 Spring Boot 3.5.3 + Spring Cloud 2025 + OAuth2.1 构建的高性能分布式电商解决方案

[快速开始](#-快速开始) • [架构设计](#-系统架构) • [技术栈](#-技术栈) • [缓存架构](docs/cache-architecture-guide.md) • [开发指南](#-开发指南) • [部署文档](#-部署指南)

</div>

## ✨ 项目亮点

### 🎆 核心特性

#### 💼 业务功能
- 🛒 **完整电商流程** - 商品管理、订单处理、库存管控、支付集成
- 👥 **多级用户体系** - 普通用户、商家用户、管理员权限分离
- 🔍 **智能搜索引擎** - 基于Elasticsearch的全文搜索
- 📊 **实时数据分析** - 订单统计、销售报表、用户行为分析

#### 🏗️ 架构优势
- ⚡ **高性能网关** - WebFlux响应式编程，支持万级QPS
- 🔄 **多级缓存策略** - Caffeine本地缓存 + Redis分布式缓存
- 🔒 **分布式锁机制** - Redisson实现高并发场景下的数据一致性
- 📡 **异步消息驱动** - RocketMQ实现解耦和削峰填谷

#### 🔐 安全保障
- 🔐 **OAuth2.1标准** - 完整实现授权服务器和资源服务器
- 📱 **PKCE增强** - 移动端和SPA应用安全增强
- 🎩 **JWT令牌管理** - 令牌生成、刷新、撤销全生命周期管理
- 🔒 **细粒度权限** - 方法级别的权限控制

#### 🛠️ 开发效率
- 🔧 **配置分离** - 服务自治，模板化配置管理
- 🤖 **自动化处理** - 字段自动填充、分页自动处理
- 📚 **API文档** - Knife4j自动生成接口文档
- 📊 **监控告警** - Actuator + Prometheus完善的监控体系

## 📊 技术栈

### 🔥 核心框架

| 组件                   | 版本                 | 说明                    |
|----------------------|--------------------|------------------------|
| Spring Boot          | 3.5.3              | 最新稳定版，原生GraalVM支持 |
| Spring Cloud         | 2025.0.0           | 下一代云原生微服务框架      |
| Spring Cloud Alibaba | 2025.0.0.0-preview | 阿里云微服务套件           |
| Java                 | 17                 | LTS长期支持版本           |

### 🗌️ 数据存储

| 组件           | 版本     | 说明                |
|--------------|--------|--------------------|  
| MySQL        | 8.0+   | 主数据库，支持事务       |
| Redis        | 7.0+   | 缓存、分布式锁、会话存储 |
| Elasticsearch| 8.x    | 全文搜索、日志存储      |
| MinIO        | 最新版  | 对象存储，图片文件管理   |
| MyBatis Plus | 3.5.13 | ORM增强，代码生成      |

### 📡 中间件

| 组件                  | 版本        | 说明                  |
|---------------------|-----------|----------------------|
| RocketMQ            | 5.3.2     | 消息队列，事件驱动        |
| Nacos               | 2.4.0     | 服务注册与配置中心      |
| Sentinel            | 1.8.8     | 流量控制、熔断降级        |
| Redisson            | 3.39.0    | 分布式锁、限流器          |

## 🏗️ 项目结构

```
cloud/                         # 根目录
├── gateway/                  # 🚪 API网关 [响应式编程、统一鉴权]
├── auth-service/             # 🔐 认证授权服务 [OAuth2.1、JWT管理]
├── user-service/             # 👥 用户服务 [用户管理、多级缓存]
├── product-service/          # 📦 商品服务 [商品管理、分类管理]
├── order-service/            # 📝 订单服务 [订单流程、状态机]
├── stock-service/            # 📋 库存服务 [库存管理、分布式锁]
├── payment-service/          # 💳 支付服务 [支付宝集成、支付流程]
├── search-service/           # 🔍 搜索服务 [ES集成、智能搜索]
├── log-service/              # 📊 日志服务 [日志收集、存储分析]
├── common-module/            # 🔧 公共模块 [工具类、配置模板]
├── api-module/               # 📌 API定义 [Feign接口、DTO定义]
├── sql/                      # 🗍️ 数据库脚本
├── docs/                     # 📚 项目文档
│   ├── cache-architecture-guide.md    # 🚀 缓存架构完整指南
│   ├── cache-migration-guide.md       # 🔄 缓存代码迁移指南  
│   ├── cache-best-practices.md        # 📋 缓存使用最佳实践
│   └── multi-level-cache-guide.md     # 🎯 多级缓存使用指南
├── pom.xml                   # Maven父POM
├── README.md                 # 项目说明
└── RULE.md                   # 开发规范
```

## 🌐 系统架构

| 服务              | 端口 | 描述               | 缓存策略              | 事务支持 |
|------------------|-----|--------------------|---------------------|--------|
| gateway          | 80  | API网关，统一入口      | -                   | -      |
| auth-service     | 8081| 认证授权，JWT管理      | Redis               | ✅      |
| user-service     | 8082| 用户管理，个人信息      | Redis + Caffeine    | ✅      |
| product-service  | 8083| 商品管理，分类管理      | Redis + Caffeine    | ✅/局部 |
| order-service    | 8084| 订单管理，状态机        | Redis               | ✅      |
| stock-service    | 8085| 库存管理，并发控制      | Redis + Redisson    | ✅      |
| payment-service  | 8086| 支付管理，支付流程      | Redis               | ✅      |
| search-service   | 8087| 搜索服务，商品搜索      | Redis + Caffeine    | 读为主  |
| log-service      | 8088| 日志服务，操作记录      | Elasticsearch       | 异步    |

## 🧬 核心特性

### 🚀 微服务架构
- **服务自治**: 每个服务独立部署、独立扩容
- **API网关**: WebFlux响应式编程，统一入口管理
- **服务注册**: Nacos动态服务发现与配置管理
- **负载均衡**: Spring Cloud LoadBalancer智能路由

### 🔄 高性能缓存
- **多级缓存**: L1(Caffeine) + L2(Redis) 双层缓存架构
- **智能存储**: 根据数据特性自动选择String/Hash存储策略
- **缓存一致性**: Redis Pub/Sub保证分布式缓存同步
- **缓存预热**: 启动时自动加载热点数据
- **缓存更新**: 基于消息队列的缓存同步机制
- **缓存穿透防护**: 布隆过滤器 + 空值缓存 + 智能防护机制
- **性能监控**: 完整的缓存指标收集、分析与告警

### 🔒 分布式并发控制
- **Redisson分布式锁**: 可重入锁、公平锁、读写锁
- **库存锁**: 悲观锁 + 乐观锁结合
- **幂等设计**: 基于唯一键和状态机的幂等保证
- **限流降级**: Sentinel流量控制与熔断

### 📡 事件驱动架构
- **消息队列**: RocketMQ实现异步解耦
- **事件源**: 用户、订单、库存变更事件
- **日志收集**: 基于Stream的异步日志收集
- **最终一致性**: Saga模式实现分布式事务

## 🚀 快速开始

### 💻 环境要求

| 组件         | 版本要求 | 说明                     |
|------------|---------|-------------------------|
| JDK        | 17+     | 推荐使用OpenJDK或Oracle JDK |
| Maven      | 3.8+    | 项目构建工具               |
| MySQL      | 8.0+    | 主数据库                  |
| Redis      | 7.0+    | 缓存和分布式锁             |
| Nacos      | 2.4.0+  | 服务注册与配置中心        |
| RocketMQ   | 5.3.2+  | 消息队列                  |
| MinIO      | 最新版   | 对象存储（可选）            |
| ES         | 8.x     | 搜索引擎（search-service需要）|

### 🎯 快速启动

#### 1️⃣ 启动基础设施

```bash
# Docker Compose 一键启动（推荐）
docker-compose -f docker/docker-compose.yml up -d

# 或者单独启动各组件
# MySQL
docker run -d -p 3306:3306 --name mysql \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=cloud_platform \
  mysql:8.0

# Redis
docker run -d -p 6379:6379 --name redis \
  redis:7-alpine redis-server --requirepass redis123

# Nacos
docker run -d -p 8848:8848 -p 9848:9848 --name nacos \
  -e MODE=standalone \
  nacos/nacos-server:v2.4.0
```

#### 2️⃣ 初始化数据库

```bash
# 执行数据库脚本
mysql -h127.0.0.1 -uroot -proot123 < sql/init.sql
mysql -h127.0.0.1 -uroot -proot123 < sql/cloud_user.sql
mysql -h127.0.0.1 -uroot -proot123 < sql/cloud_product.sql
mysql -h127.0.0.1 -uroot -proot123 < sql/cloud_order.sql
mysql -h127.0.0.1 -uroot -proot123 < sql/cloud_stock.sql
mysql -h127.0.0.1 -uroot -proot123 < sql/cloud_payment.sql
```

#### 3️⃣ 编译项目

```bash
# 在项目根目录执行
mvn clean install -DskipTests

# 或者单独编译某个服务
cd gateway && mvn clean package
```

#### 4️⃣ 启动服务

```bash
# 按顺序启动（重要！）

# 1. 启动网关
cd gateway
java -jar target/gateway-0.0.1-SNAPSHOT.jar

# 2. 启动认证服务
cd ../auth-service
java -jar target/auth-service-0.0.1-SNAPSHOT.jar

# 3. 启动核心业务服务
cd ../user-service
java -jar target/user-service-0.0.1-SNAPSHOT.jar

cd ../product-service
java -jar target/product-service-0.0.1-SNAPSHOT.jar

cd ../order-service
java -jar target/order-service-0.0.1-SNAPSHOT.jar

cd ../stock-service
java -jar target/stock-service-0.0.1-SNAPSHOT.jar

cd ../payment-service
java -jar target/payment-service-0.0.1-SNAPSHOT.jar

# 4. 启动辅助服务（可选）
cd ../search-service
java -jar target/search-service-0.0.1-SNAPSHOT.jar

cd ../log-service
java -jar target/log-service-0.0.1-SNAPSHOT.jar
```

### ✅ 验证服务

```bash
# 检查服务注册状态
curl http://localhost:8848/nacos

# 访问API文档
http://localhost/doc.html

# 健康检查
curl http://localhost/actuator/health
```

## 🚀 缓存架构文档

### 📋 缓存相关文档
- **[🚀 缓存架构完整指南](docs/cache-architecture-guide.md)** - 企业级多级缓存架构详细说明
- **[🔄 缓存代码迁移指南](docs/cache-migration-guide.md)** - 从服务专用注解迁移到统一注解
- **[📋 缓存使用最佳实践](docs/cache-best-practices.md)** - Redis缓存使用最佳实践
- **[🎯 多级缓存使用指南](docs/multi-level-cache-guide.md)** - 多级缓存配置与优化

### 🎨 缓存架构特点
- **统一注解系统**: 所有多级缓存注解统一到`common-module`
- **智能存储策略**: String/Hash自动选择，根据数据特性优化
- **缓存一致性**: Redis Pub/Sub保证分布式环境下的数据一致性
- **性能监控**: 完整的指标收集、分析与告警机制
- **防护机制**: 穿透、击穿、雪崩完整防护方案

---

## 📚 开发指南

### 🔧 开发规范

#### 代码风格
- **Java**: 遵循Alibaba Java编码规范
- **命名**: 驼峰命名法，类名首字母大写
- **注释**: 所有公共方法必须有JavaDoc注释
- **分层**: Controller -> Service -> Repository

#### 分支管理
```bash
main              # 主分支，生产代码
develop           # 开发分支
feature/xxx       # 功能分支
hotfix/xxx        # 紧急修复
release/x.x.x     # 发布分支
```

### 🔌 API设计

#### RESTful规范
```
GET    /api/v1/users          # 查询用户列表
GET    /api/v1/users/{id}     # 查询单个用户
POST   /api/v1/users          # 创建用户
PUT    /api/v1/users/{id}     # 更新用户
DELETE /api/v1/users/{id}     # 删除用户
```

#### 统一响应格式
```java
{
  "code": 200,           // 状态码
  "message": "success",  // 描述信息
  "data": {},           // 数据
  "timestamp": 1234567890 // 时间戳
}
```

### 🛡️ 安全开发

#### 认证授权
- 所有API必须经过网关认证
- 使用JWT令牌传递用户信息
- 敏感操作需要二次验证

#### 数据安全
- 密码使用BCrypt加密
- 敏感数据脱敏处理
- SQL参数化查询，防止SQL注入

### 📊 性能优化

#### 缓存策略
| 缓存级别 | 实现方式    | TTL    | 适用场景     |
|---------|-----------|--------|------------|
| L1缓存  | Caffeine  | 5分钟  | 热点数据     |
| L2缓存  | Redis     | 30分钟 | 共享数据     |
| 持久化   | MySQL     | -      | 全量数据     |

#### 数据库优化
- 合理创建索引
- 大表分页查询
- 读写分离（可选）
- 分库分表（可选）

## 🚀 部署指南

### 🐳 Docker部署

#### 构建Docker镜像
```bash
# 构建所有服务镜像
./build-docker.sh

# 或单独构建
docker build -t cloud/gateway:latest ./gateway
docker build -t cloud/auth-service:latest ./auth-service
```

#### Docker Compose部署
```yaml
version: '3.8'
services:
  gateway:
    image: cloud/gateway:latest
    ports:
      - "80:80"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - NACOS_ADDR=nacos:8848
    depends_on:
      - nacos
      - redis
```

### ☁️ Kubernetes部署

#### Helm Chart安装
```bash
# 添加Helm仓库
helm repo add cloud-platform https://charts.example.com

# 安装
helm install cloud cloud-platform/cloud \
  --set gateway.replicas=3 \
  --set mysql.enabled=true
```

#### kubectl部署
```bash
# 应用配置
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
```

### 📊 监控告警

#### Prometheus监控
- 服务健康状态
- QPS、响应时间
- JVM内存、GC情况
- 业务指标监控

#### Grafana大盘
- 系统总览图
- 服务调用链路
- 错误率统计
- 性能趋势图

## 📖 文档目录

### 核心文档
- [📘 开发规范](RULE.md) - 编码规范、架构原则
- [🏗️ 系统架构](docs/architecture.md) - 架构设计、技术选型
- [🔐 安全指南](docs/security.md) - OAuth2.1、JWT、权限控制

### 服务文档
- [🚪 网关服务](gateway/README.md)
- [🔐 认证服务](auth-service/README.md)
- [👥 用户服务](user-service/README.md)
- [📦 商品服务](product-service/README.md)
- [📝 订单服务](order-service/README.md)
- [📋 库存服务](stock-service/README.md)
- [💳 支付服务](payment-service/README.md)
- [🔍 搜索服务](search-service/README.md)
- [📊 日志服务](log-service/README.md)

### 技术文档
- [MyBatis Plus配置指南](docs/mybatis-plus-config-guide.md)
- [Redis缓存配置指南](docs/redis-cache-config-guide.md)
- [Elasticsearch优化指南](docs/elasticsearch-optimization-guide.md)
- [线程池优化指南](docs/thread-pool-optimization-guide.md)
- [依赖优化指南](docs/dependency-optimization-guide.md)

## 📦 版本发布

### v0.0.1-SNAPSHOT (当前版本) - 2025.01

#### 🔄 最新更新 (2025-01-18) - 配置架构重构完成
- ✅ **代码重构**: 删除重复的 SecurityUtils 类，统一使用 common-module 中的版本
- ✅ **配置重构**: 将 RedissonConfig 和 HybridCacheConfig 重构为抽象基类
- ✅ **继承优化**: 所有子服务配置类现在继承 common-module 基类，消除代码重复
- ✅ **编译验证**: 项目编译成功，配置重构无破坏性更改
- ✅ **架构优化**: 提高代码复用性，降低维护成本

#### ✅ 完成功能
- 🏗️ **微服务架构**: 完整的微服务基础设施
- 🔐 **OAuth2.1实现**: 授权服务器 + 资源服务器
- 🔄 **多级缓存**: Caffeine + Redis双层缓存
- 🔒 **分布式锁**: Redisson实现并发控制
- 📡 **消息队列**: RocketMQ事件驱动
- 📋 **库存管理**: 并发控制、分布式事务
- 💳 **支付集成**: 支付宝支付接入
- 🔍 **智能搜索**: Elasticsearch全文搜索

#### 🔄 进行中
- [ ] 微信支付集成
- [ ] 分布式事务完善
- [ ] 链路追踪集成
- [ ] 性能监控完善

### 📌 路线图

- **v0.1.0** - 完善监控体系、链路追踪
- **v0.2.0** - 支持容器化部署、K8s集成
- **v0.3.0** - 完善分布式事务、消息可靠性
- **v1.0.0** - 生产就绪版本

## 🤝 贡献指南

我们欢迎并感谢任何形式的贡献！

### 如何贡献

1. **Fork** 本仓库
2. **创建** 特性分支 (`git checkout -b feature/AmazingFeature`)
3. **提交** 你的更改 (`git commit -m 'Add: AmazingFeature'`)
4. **推送** 到分支 (`git push origin feature/AmazingFeature`)
5. **提交** Pull Request

### 贡献者

<a href="https://github.com/your-username/cloud/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=your-username/cloud" />
</a>

## 💙 致谢

感谢所有为此项目做出贡献的开发者！

特别感谢以下开源项目：
- Spring Boot / Spring Cloud
- Apache RocketMQ
- Alibaba Nacos
- Redisson
- MyBatis Plus

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

## 📧 联系我们

- 👨‍💻 **项目维护者**: what's up
- 📦 **邮箱**: project@example.com
- 💬 **讨论群**: QQ群 123456789
- 🐦 **Twitter**: @cloudplatform
- 📝 **博客**: https://blog.example.com

---

<div align="center">

**⭐ 如果这个项目对您有帮助，请给一个 Star！⭐**

[返回顶部](#-cloud-微服务电商平台)

</div>
