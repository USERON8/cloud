# 📚 Cloud微服务电商平台 - 技术文档中心

<div align="center">

![Documentation](https://img.shields.io/badge/Documentation-Complete-green)
![Version](https://img.shields.io/badge/Version-6.0-blue)
![Last Update](https://img.shields.io/badge/Update-2025.01.25-orange)

**完整的技术文档体系，助力开发团队高效协作**

[架构设计](#-架构设计) • [开发指南](#-开发指南) • [部署运维](#-部署运维) • [API文档](#-api文档) • [最佳实践](#-最佳实践)

</div>

---

## 📋 文档导航

### 🏗️ 架构设计

| 文档 | 说明 | 更新时间 |
|------|------|----------|
| [系统架构总览](./architecture.md) | 整体架构设计、技术选型 | 2025.01 |
| [微服务架构](./microservice-architecture.md) | 服务拆分原则、通信机制 | 2025.01 |
| [数据库设计](../sql/README.md) | 数据库架构、表结构设计 | 2025.01 |
| [缓存架构](./cache-architecture.md) | 多级缓存策略、缓存更新机制 | 2025.01 |

### 💻 开发指南

| 文档 | 说明 | 更新时间 |
|------|------|----------|
| [开发规范](../RULE.md) | 编码规范、架构原则 | 2025.01 |
| [快速开始](../README.md#-快速开始) | 环境搭建、项目启动 | 2025.01 |
| [API设计规范](./api-design-guide.md) | RESTful规范、接口设计 | 2025.01 |
| [数据库开发指南](./database-development-guide.md) | SQL规范、性能优化 | 2025.01 |

### 🔧 配置指南

| 文档 | 说明 | 更新时间 |
|------|------|----------|
| [MyBatis Plus配置](./mybatis-plus-config-guide.md) | ORM框架配置与优化 | 2025.01 |
| [Redis配置指南](./redis-cache-config-guide.md) | 缓存配置与最佳实践 | 2025.01 |
| [线程池优化](./thread-pool-optimization-guide.md) | 异步处理与线程池配置 | 2025.01 |
| [依赖优化](./dependency-optimization-guide.md) | Maven依赖管理与优化 | 2025.01 |

### 🚀 部署运维

| 文档 | 说明 | 更新时间 |
|------|------|----------|
| [Docker部署](./docker-deployment.md) | 容器化部署方案 | 2025.01 |
| [Kubernetes部署](./k8s-deployment.md) | K8s编排与管理 | 2025.01 |
| [CI/CD流程](./cicd-pipeline.md) | 持续集成与部署 | 2025.01 |
| [监控告警](./monitoring-alerting.md) | Prometheus监控方案 | 2025.01 |

### 🛡️ 安全指南

| 文档 | 说明 | 更新时间 |
|------|------|----------|
| [OAuth2.1实现](./oauth2-implementation.md) | 认证授权详细设计 | 2025.01 |
| [安全最佳实践](./security-best-practices.md) | 安全开发指南 | 2025.01 |
| [数据安全](./data-security.md) | 数据加密与脱敏 | 2025.01 |
| [API安全](./api-security.md) | 接口安全防护 | 2025.01 |

### 📊 性能优化

| 文档 | 说明 | 更新时间 |
|------|------|----------|
| [性能测试指南](./performance-testing.md) | 压测方案与工具 | 2025.01 |
| [JVM调优](./jvm-tuning.md) | JVM参数优化 | 2025.01 |
| [数据库优化](./database-optimization.md) | SQL优化与索引设计 | 2025.01 |
| [Elasticsearch优化](./elasticsearch-optimization-guide.md) | 搜索引擎优化 | 2025.01 |

## 📦 服务文档

### 核心服务

| 服务 | 文档 | 说明 | 端口 |
|------|------|------|------|
| 🚪 Gateway | [README](../gateway/README.md) | API网关，统一入口 | 80 |
| 🔐 Auth Service | [README](../auth-service/README.md) | 认证授权服务 | 8081 |
| 👥 User Service | [README](../user-service/README.md) | 用户管理服务 | 8082 |
| 📦 Product Service | [README](../product-service/README.md) | 商品管理服务 | 8083 |
| 📝 Order Service | [README](../order-service/README.md) | 订单管理服务 | 8084 |
| 📋 Stock Service | [README](../stock-service/README.md) | 库存管理服务 | 8085 |
| 💳 Payment Service | [README](../payment-service/README.md) | 支付服务 | 8086 |

### 支撑服务

| 服务 | 文档 | 说明 | 端口 |
|------|------|------|------|
| 🔍 Search Service | [README](../search-service/README.md) | 搜索服务 | 8087 |
| 📊 Log Service | [README](../log-service/README.md) | 日志服务 | 8088 |

## 📚 专题文档

### 分布式事务

- [分布式事务方案](./distributed-transaction.md)
- [Seata集成指南](./seata-integration.md)
- [Saga模式实践](./saga-pattern.md)

### 消息队列

- [RocketMQ使用指南](./rocketmq-guide.md)
- [消息可靠性保证](./message-reliability.md)
- [事件驱动架构](./event-driven-architecture.md)

### 分布式锁

- [Redisson使用指南](./redisson-guide.md)
- [分布式锁最佳实践](./distributed-lock-best-practices.md)
- [并发控制策略](./concurrency-control.md)

## 🔄 API文档

### 在线文档

- **开发环境**: http://localhost/doc.html
- **测试环境**: http://test-api.example.com/doc.html
- **生产环境**: http://api.example.com/doc.html

### API规范

| 文档 | 说明 | 版本 |
|------|------|------|
| [RESTful API标准](./UNIFIED_RESTFUL_API_STANDARDS.md) | 统一API规范 | v2.0 |
| [API迁移指南](./API_MIGRATION_GUIDE_V2.md) | API版本迁移 | v2.0 |
| [API路径映射](./UNIFIED_API_PATH_MAPPING.md) | 路径规范 | v2.0 |

## 🛠️ 开发工具

### 推荐IDE配置

- **IntelliJ IDEA**: [配置文件](./.idea)
- **VS Code**: [配置文件](./.vscode)
- **代码模板**: [Templates](./templates)

### 开发插件

| 插件 | 用途 | 下载地址 |
|------|------|----------|
| Lombok | 简化代码 | [IDEA Plugin](https://plugins.jetbrains.com/plugin/6317-lombok) |
| MyBatis Log | SQL日志 | [IDEA Plugin](https://plugins.jetbrains.com/plugin/10065-mybatis-log) |
| Alibaba Java Coding Guidelines | 代码规范检查 | [IDEA Plugin](https://plugins.jetbrains.com/plugin/10046-alibaba-java-coding-guidelines) |
| RestfulTool | API测试 | [IDEA Plugin](https://plugins.jetbrains.com/plugin/14280-restfultool) |

## 📈 最佳实践

### 设计模式

- [工厂模式在配置管理中的应用](./pattern-factory.md)
- [策略模式在支付中的应用](./pattern-strategy.md)
- [观察者模式在事件系统中的应用](./pattern-observer.md)

### 问题排查

- [常见问题FAQ](./faq.md)
- [故障排查指南](./troubleshooting.md)
- [性能问题定位](./performance-troubleshooting.md)

## 🔗 外部资源

### 官方文档

- [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [MyBatis Plus](https://baomidou.com/)
- [Nacos](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [RocketMQ](https://rocketmq.apache.org/docs/)

### 学习资源

- [微服务架构设计](https://microservices.io/)
- [OAuth 2.1规范](https://oauth.net/2.1/)
- [RESTful API设计](https://restfulapi.net/)

## 📝 文档维护

### 文档规范

- 使用Markdown格式
- 包含目录结构
- 提供代码示例
- 标注更新时间
- 使用图表说明复杂概念

### 贡献指南

1. Fork项目
2. 创建文档分支
3. 编写或更新文档
4. 提交Pull Request
5. 等待审核合并

### 文档版本

| 版本 | 发布日期 | 主要更新 |
|------|----------|----------|
| v6.0 | 2025.01.25 | 完整重构文档体系 |
| v5.0 | 2025.01.15 | 添加安全规范 |
| v4.0 | 2024.12.01 | 性能优化指南 |
| v3.0 | 2024.10.01 | 部署文档完善 |

---

<div align="center">

**📧 文档问题反馈**: docs@cloud-platform.com

**⭐ 持续更新中，欢迎贡献 ⭐**

[返回主页](../README.md)

</div>
