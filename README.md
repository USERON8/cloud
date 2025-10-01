# 🌐 Cloud 微服务电商平台

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Status](https://img.shields.io/badge/status-production--ready-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)
![License](https://img.shields.io/badge/license-Apache%202.0-blue)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)

**🚀 生产级云原生电商微服务平台**

基于 Spring Boot 3.5.3 + Spring Cloud 2025 + OAuth2.1 构建的高性能分布式电商解决方案  
✅ **支持快速部署上线** | 💳 **简化支付流程** | 🔒 **完整业务闭环**
   
## ✨ 项目亮点

### 🌟 生产环境就绪

✅ **完整业务功能** - 所有核心业务流程已实现，可直接部署上线  
✅ **简化支付模式** - 无需配置第三方支付，即可快速启动  
✅ **生产级架构** - 分布式锁、缓存、异步处理等企业级方案  
✅ **完整文档** - 每个服务都有详细的README和部署说明  
✅ **标准化开发** - 遵循统一的开发规范和代码风格  

### 💳 简化支付说明

本项目采用**简化支付模式**，无需对接支付宝/微信等第三方支付网关，通过前端直接调用支付成功/失败/退款接口，实现完整的业务闭环。

**优势**：
- ⚡ 快速部署：无需配置第三方商户号、密钥、回调地址
- 🧪 易于测试：前端可直接模拟各种支付场景
- 🔒 安全可靠：使用分布式锁保证幂等性，支持并发处理
- 🔄 完整流程：保留完整的支付状态流转和业务逻辑
- 🚀 易于扩展：需要时可轻松集成真实支付网关

**支付流程**：
```
创建订单 → 预扣库存 → 创建支付单 → 前端模拟支付 → 调用成功接口 → 更新订单状态
```

### 🎆 核心特性

#### 💼 业务功能

- 🛍️ **完整电商流程** - 商品管理、订单处理、库存管控、简化支付
- 👥 **多级用户体系** - 普通用户、商家用户、管理员权限分离
- 🔍 **智能搜索引擎** - 基于Elasticsearch的商品全文搜索
- 📊 **实时数据分析** - 订单统计、销售报表、日志分析

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

*
* | 组件 | 版本 | 说明 |
* |----------------------|--------------------|------------------------|
* | Spring Boot | 3.5.3 | 最新稳定版，原生GraalVM支持 |
* | Spring Cloud | 2025.0.0 | 下一代云原生微服务框架 |
* | Spring Cloud Alibaba | 2025.0.0.0-preview | 阿里云微服务套件 |
* | Java | 17 | LTS长期支持版本 |
*

### 🗌️ 数据存储

*
* | 组件 | 版本 | 说明 |
* |--------------|--------|--------------------|
* | MySQL | 8.0+ | 主数据库，支持事务 |
* | Redis | 7.0+ | 缓存、分布式锁、会话存储 |
* | Elasticsearch| 8.x | 全文搜索、日志存储 |
* | MinIO | 最新版 | 对象存储，图片文件管理 |
* | MyBatis Plus | 3.5.13 | ORM增强，代码生成 |
*

### 📡 中间件

*
* | 组件 | 版本 | 说明 |
* |---------------------|-----------|----------------------|
* | RocketMQ | 5.3.2 | 消息队列，事件驱动 |
* | Nacos | 2.4.0 | 服务注册与配置中心 |
* | Sentinel | 1.8.8 | 流量控制、熔断降级 |
* | Redisson | 3.39.0 | 分布式锁、限流器 |
*

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


## 🌐 系统架构
* 
* | 服务              | 端口 | 描述               | 缓存策略              | 事务支持 |
* |------------------|-----|--------------------|---------------------|--------|
* | gateway          | 80  | API网关，统一入口      | -                   | -      |
* | auth-service     | 8081| 认证授权，JWT管理      | Redis               | ✅      |
* | user-service     | 8082| 用户管理，个人信息      | Redis + Caffeine    | ✅      |
* | product-service  | 8083| 商品管理，分类管理      | Redis + Caffeine    | ✅/局部 |
* | order-service    | 8084| 订单管理，状态机        | Redis               | ✅      |
* | stock-service    | 8085| 库存管理，并发控制      | Redis + Redisson    | ✅      |
* | payment-service  | 8086| 支付管理，支付流程      | Redis               | ✅      |
* | search-service   | 8087| 搜索服务，商品搜索      | Redis + Caffeine    | 读为主  |
* | log-service      | 8088| 日志服务，操作记录      | Elasticsearch       | 异步    |
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
| ES         | 9.1.2     | 搜索引擎（search-service需要）|

#### 3️⃣ 编译项目

```bash
# 在项目根目录执行
mvn clean install -DskipTests

# 或者单独编译某个服务
cd gateway && mvn clean package
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

Result
{
"code":200, // 状态码
"message":"success", // 描述信息
"data":{}, // 数据
"timestamp":1234567890 // 时间戳
}

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

| 缓存级别 | 实现方式     | TTL  | 适用场景 |
|------|----------|------|------|
| L1缓存 | Caffeine | 5分钟  | 热点数据 |
| L2缓存 | Redis    | 30分钟 | 共享数据 |
| 持久化  | MySQL    | -    | 全量数据 |

#### 数据库优化

- 合理创建索引
- 大表分页查询
- 读写分离（可选）
- 分库分表（可选）

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

## 📦 版本发布

### v1.0.0 (生产版本) - 2025-01-25

#### ✨ 新增功能

- 💎 **简化支付模式**: 前端直接触发支付，无需第三方配置
- 📚 **完整文档**: 所有服务都有详细的README和部署说明
- 🔧 **标准化开发**: 统一的代码风格和开发规范
- 🚀 **生产级就绪**: 所有核心功能已实现，可直接部署

#### ✅ 核心特性

- 🏗️ **微服务架构**: Gateway + 8个业务服务 + 2个公共模块
- 🔐 **OAuth2.1认证**: 完整的授权服务器和资源服务器实现
- 🔄 **多级缓存**: L1(Caffeine) + L2(Redis)双层缓存架构
- 🔒 **分布式锁**: Redisson实现高并发控制
- 📡 **消息驱动**: RocketMQ实现异步解耦
- 📋 **库存管理**: 完整的库存预扣、扣减、回滚机制
- 💳 **简化支付**: 前端直接调用支付成功/失败/退款接口
- 🔍 **全文搜索**: Elasticsearch实现商品搜索和索引
- 📊 **日志分析**: 基于ES的日志收集、存储和查询

#### 🐛 修复问题

- 优化了所有服务的异常处理
- 统一了API响应格式
- 完善了分布式锁的使用
- 修复了缓存一致性问题

## 🤝 贡献指南

我们欢迎并感谢任何形式的贡献！

### 如何贡献

1. **Fork** 本仓库
2. **创建** 特性分支 (`git checkout -b feature/AmazingFeature`)
3. **提交** 你的更改 (`git commit -m 'Add: AmazingFeature'`)
4. **推送** 到分支 (`git push origin feature/AmazingFeature`)
5. **提交** Pull Request
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



**⭐ 如果这个项目对您有帮助，请给一个 Star！⭐**

[返回顶部](#-cloud-微服务电商平台)

