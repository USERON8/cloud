# 🌐 Cloud 微服务平台

<div align="center">

![Version](https://img.shields.io/badge/version-0.0.1--SNAPSHOT-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-brightgreen)
![License](https://img.shields.io/badge/license-Apache%202.0-blue)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)

**现代化企业级微服务架构平台**

基于 Spring Boot 3.x + Spring Cloud 2025 + OAuth2.1 构建的高性能、高可用、可扩展的企业级微服务解决方案

</div>

## 🚀 项目特色

### 🏗️ 现代化架构

- ✨ **Spring Boot 3.5.3** - 最新稳定版，原生Java 17支持
- 🌍 **Spring Cloud 2025.0.0** - 下一代云原生架构
- 🔐 **OAuth2.1标准** - 完整实现授权服务器和资源服务器
- ⚡ **WebFlux响应式** - 高并发响应式网关

### 📊 性能优化

- 💾 **多级缓存** - L1(Caffeine) + L2(Redis)双层缓存
- 🚀 **缓存策略** - 按业务选择多级或Redis统一缓存
- 🔗 **连接池调优** - HikariCP高性能数据库连接
- 🏃 **异步处理** - CompletableFuture异步编程

### 🔒 安全保障

- 📱 **PKCE增强** - 移动端安全增强
- 🎩 **JWT全生命周期** - 生成、刷新、撤销管理
- 🚪 **网关统一鉴权** - 所有API请求统一安全验证
- 📝 **细粒度权限** - 方法级@PreAuthorize权限控制

### ⚙️ 开发效率

- 🛠️ **配置分离架构** - 服务自治，模板化配置
- 🔄 **自动字段填充** - 创建时间、更新时间、操作人自动填充
- 📚 **文档自动化** - Knife4j + SpringDoc API文档
- 🎁 **响应标准化** - Result<T>和PageResult<T>统一格式

## 📊 技术栈

### 🔥 核心框架

| 组件                   | 版本                 | 说明        |
|----------------------|--------------------|-----------|
| Spring Boot          | 3.5.3              | 主框架，最新稳定版 |
| Spring Cloud         | 2025.0.0           | 云原生微服务架构  |
| Spring Cloud Alibaba | 2025.0.0.0-preview | 阿里云微服务组件  |
| Java                 | 17                 | LTS长期支持版本 |

### 🗄️ 数据存储

| 组件           | 版本     | 说明         |
|--------------|--------|------------|
| MySQL        | 8.0+   | 主数据库       |
| Redis        | 7.0+   | 缓存与分布式锁    |
| MyBatis Plus | 3.5.13 | ORM框架，代码生成 |
| HikariCP     | 5.1.0  | 高性能数据库连接池  |
| Caffeine     | 3.2.2  | 本地缓存L1层    |

### 📨 消息队列

| 组件                  | 版本        | 说明         |
|---------------------|-----------|------------|
| RocketMQ            | 5.3.2     | 阿里云原生消息中间件 |
| Spring Cloud Stream | Alibaba管理 | 消息驱动微服务框架  |

## 🏗️ 项目结构

```
cloud/
├── api-module/              # API接口定义模块
├── auth-service/            # 认证授权服务
├── common-module/           # 公共组件模块
├── gateway/                 # API网关
├── log-service/             # 日志服务
├── order-service/           # 订单服务
├── payment-service/         # 支付服务
├── product-service/         # 商品服务
├── search-service/          # 搜索服务
├── stock-service/           # 库存服务
├── user-service/            # 用户服务
├── sql/                     # 数据库脚本
├── docs/                    # 项目文档
├── pom.xml                  # 根POM文件
├── README.md                # 项目说明
└── RULE.md                  # 开发规范
```

## 🌐 服务架构

| 服务              | 端口   | 描述         | 缓存策略             | 事务支持 |
|-----------------|------|------------|------------------|------|
| gateway         | 8080 | API网关，统一入口 | -                | -    |
| auth-service    | 8081 | 认证授权，JWT管理 | Redis            | ✅    |
| user-service    | 8082 | 用户管理，个人信息  | Redis + Caffeine | ✅    |
| product-service | 8083 | 商品管理，分类管理  | Redis + Caffeine | ❌    |
| order-service   | 8084 | 订单管理，订单流程  | Redis            | ✅    |
| stock-service   | 8085 | 库存管理，库存扣减  | Redis            | ✅    |
| payment-service | 8086 | 支付管理，支付流程  | Redis            | ✅    |
| search-service  | 8087 | 搜索服务，商品搜索  | Redis + Caffeine | ❌    |
| log-service     | 8088 | 日志服务，操作记录  | Redis            | ❌    |

## 🔧 核心特性

### 🚀 配置分离架构

- **服务自治**: 每个服务独立配置Redis和缓存策略
- **模板化设计**: 提供多种预定义配置模板
- **按需启用**: 仅在需要的服务中启用多级缓存

### 🔄 多级缓存策略

- **L1缓存**: Caffeine本地缓存 (user、product、search服务)
- **L2缓存**: Redis分布式缓存 (所有服务)
- **智能过期**: 根据业务特点设置不同过期时间

### 🔒 分布式锁机制

- **Redisson集成**: 支持可重入锁、读写锁、信号量
- **自动续期**: 看门狗机制防止锁过期
- **高性能**: 基于Redis Lua脚本实现

### 📊 自动字段填充

- **时间字段**: 自动填充创建时间、更新时间
- **操作人字段**: 自动填充创建人、更新人
- **业务字段**: 根据服务特点自动填充默认值

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+
- RocketMQ 5.0+
- Nacos 2.4.0+

### 启动步骤

1. **启动基础设施**
   ```bash
   # 启动MySQL、Redis、RocketMQ、Nacos
   ```

2. **编译项目**
   ```bash
   mvn clean compile
   ```

3. **启动服务**
   ```bash
   # 按顺序启动
   # 1. gateway (API网关)
   # 2. auth-service (认证服务)
   # 3. 其他业务服务
   ```

## 📚 开发指南

### 配置管理

项目采用分层配置管理：

1. **公共配置** (`common-module`)
    - 基础配置模板和工厂类
    - 工具类和通用组件
    - 不包含具体Bean实例

2. **服务配置** (各服务模块)
    - 继承公共配置模板
    - 个性化定制配置
    - 使用@Primary覆盖默认配置

### 缓存配置模板

| 配置模板       | 特点        | 适用场景 | 使用服务                     |
|------------|-----------|------|--------------------------|
| **基础配置**   | 一般缓存，无事务  | 统计查询 | log-service              |
| **高性能配置**  | 高并发，支持事务  | 业务核心 | user、order、stock、payment |
| **缓存专用配置** | 纯缓存，无事务   | 查询优化 | product、search           |
| **会话专用配置** | 会话存储，支持事务 | 认证授权 | auth-service             |

### API文档

- **Knife4j**: http://localhost:8080/doc.html
- **Postman**: 导入API集合进行测试

## 📖 文档目录

- [开发规范](RULE.md)
- [MyBatis Plus配置指南](docs/mybatis-plus-config-guide.md)
- [Redis缓存配置指南](docs/redis-cache-config-guide.md)

## 📝 版本历史

### v0.0.1-SNAPSHOT (当前版本)

- ✅ 基础微服务架构搭建
- ✅ 统一认证授权
- ✅ Redis配置分离重构
- ✅ MyBatis Plus配置分离
- ✅ 多级缓存实现 (user、product、search)
- ✅ 分布式锁集成
- ✅ 自动字段填充
- ✅ API网关配置

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache 2.0 许可证。

## 📧 联系方式

- 项目维护者: what's up
- 文档更新: 2025-01-15
