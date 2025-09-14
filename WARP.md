# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## 项目概览

这是一个基于 Spring Cloud 微服务架构的电商云平台，采用分布式系统设计模式。项目使用 Maven
多模块管理，包含用户服务、认证服务、商品服务、订单服务、支付服务、库存服务、搜索服务、日志服务和网关服务等核心模块。

### 核心架构特征

- **微服务架构**: 每个业务领域作为独立的微服务
- **服务网关**: 使用 Spring Cloud Gateway 作为统一入口
- **服务发现**: 基于 Nacos 的服务注册与发现
- **权限控制**: OAuth2 JWT 认证 + 自定义权限检查器
- **缓存策略**: Redis 缓存 + 本地缓存双层缓存
- **消息队列**: RocketMQ 用于异步处理和日志收集
- **数据存储**: MySQL 关系数据库 + Elasticsearch 日志搜索

## 开发环境设置

### 1. 本地开发基础设施启动

在开发前，需要启动必要的基础服务：

```bash
# 启动基础服务 (MySQL, Redis, Nacos, RocketMQ, Nginx)
cd docker
docker-compose up -d
```

### 2. 验证基础服务状态

```bash
# 验证各服务健康状态
docker ps  # 检查容器状态
docker logs nacos_server  # 检查 Nacos 日志
docker logs mysql_db      # 检查 MySQL 日志
docker logs redis_cache   # 检查 Redis 日志
```

### 3. 访问管理界面

- **Nacos 控制台**: http://localhost:8848/nacos (用户名/密码: root/root)
- **API 文档**: http://localhost/doc.html (通过网关访问)

## 编译问题修复记录

### stock-service 编译修复 (2025-09-13)

**问题描述**:
- `BaseConfig` 类不存在，删除了依赖
- `StockRequestDTO` 类不存在，删除了相关方法
- `CommonGlobalExceptionHandler` 不存在，修改为自定义异常处理器
- JWT 配置中 `cacheDuration` 方法不存在，删除相关调用
- 重复构造函数问题

**修复动作**:
1. 修改 `StockServiceConfig` 删除 BaseConfig 的继承
2. 删除所有对 `StockRequestDTO` 的引用和方法
3. 修改 `GlobalExceptionHandler` 为独立的异常处理器
4. 修复 `ResourceServerConfig` 中的 JWT 配置
5. 修复 `StockAlreadyExistsException` 的重复构造函数

**结果**: stock-service 现在可以成功编译。

---

## 常用开发命令

### 构建与编译

```bash
# 清理和重新编译整个项目
mvn clean compile

# 打包所有模块（跳过测试）
mvn clean package -DskipTests

# 只编译特定模块
mvn clean compile -pl user-service

# 打包特定模块及其依赖
mvn clean package -pl user-service -am
```

### 运行服务

```bash
# 启动网关服务（必须先启动）
cd gateway
mvn spring-boot:run

# 启动认证服务
cd auth-service  
mvn spring-boot:run

# 启动用户服务
cd user-service
mvn spring-boot:run

# 启动其他业务服务（按需启动）
cd order-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd stock-service && mvn spring-boot:run
```

### 测试命令

```bash
# 运行所有测试
mvn test

# 运行特定模块的测试
mvn test -pl user-service

# 运行特定的测试类
mvn test -Dtest=UserServiceTest

# 运行集成测试
mvn verify
```

## 服务启动顺序

服务之间存在依赖关系，建议按以下顺序启动：

1. **基础设施**: docker-compose up -d
2. **网关服务**: gateway (端口: 动态分配)
3. **认证服务**: auth-service (端口: 动态分配)
4. **核心业务服务**: user-service, product-service
5. **扩展服务**: order-service, payment-service, stock-service, search-service
6. **辅助服务**: log-service

## 核心架构模式

### 1. 统一权限控制模式

项目实现了基于注解的声明式权限控制：

- `@RequiresPermission`: 方法级别权限控制
- `@RequiresUserType`: 用户类型检查（USER/MERCHANT/ADMIN）
- `PermissionChecker`: 统一权限验证工具类

```java
// 示例：管理员权限检查
@RequiresUserType("ADMIN")
@PostMapping("/create/user")
public ResponseEntity<?> createUser(@RequestBody UserCreateRequest request) {
    // 业务逻辑
}
```

### 2. 分层架构模式

每个服务都遵循标准的分层架构：

```
Controller Layer (控制器层) 
    ↓
Service Layer (业务逻辑层)
    ↓  
Mapper Layer (数据访问层)
    ↓
Database Layer (数据库层)
```

### 3. 跨服务通信模式

- **同步调用**: 使用 OpenFeign 客户端
- **异步通信**: 使用 RocketMQ 消息队列
- **服务发现**: 通过 Nacos 自动服务发现

### 4. 缓存策略模式

- **分布式缓存**: Redis 存储共享数据
- **本地缓存**: Caffeine 缓存热点数据
- **多级缓存**: MultiLevelCacheService 实现缓存逐级查找

## 数据库和缓存

### MySQL 数据库结构

- **用户域**: users, user_address, merchants, admins
- **商品域**: products, categories, shops
- **订单域**: orders, order_items
- **支付域**: payments, payment_flows
- **库存域**: stock, stock_in, stock_out

### Redis 缓存策略

- **用户信息缓存**: user:info:{userId}
- **商品信息缓存**: product:info:{productId}
- **权限缓存**: permissions:{userId}
- **会话缓存**: session:{token}

## 日志和监控

### 日志收集

项目使用 RocketMQ 进行分布式日志收集：

- **操作日志**: 记录用户/管理员/商家的关键操作
- **业务日志**: 记录订单、支付、库存等业务事件
- **系统日志**: 记录服务运行状态和错误信息

### 健康检查

每个服务都集成了 Actuator 健康检查：

```bash
# 检查服务健康状态
curl http://localhost:port/actuator/health

# 查看服务信息
curl http://localhost:port/actuator/info
```

## 开发注意事项

### 1. 模块依赖规则

- **common-module**: 通用工具和配置，被所有业务模块依赖
- **api-module**: Feign 客户端定义，用于服务间调用
- **业务模块**: 只能依赖 common-module 和 api-module

### 2. 权限控制规范

- 所有 Controller 方法必须添加适当的权限注解
- 使用 `PermissionChecker` 进行编程式权限检查
- 用户只能操作自己的数据，管理员可以操作所有数据

### 3. 错误处理规范

- 使用 `GlobalExceptionHandler` 进行统一异常处理
- 自定义业务异常继承 `RuntimeException`
- 返回统一的错误响应格式

### 4. 代码规范

- 使用 MapStruct 进行对象转换
- 使用 Lombok 减少样板代码
- 遵循 RESTful API 设计原则
- 数据库操作使用 MyBatis-Plus

## 故障排查

### 常见问题诊断

```bash
# 检查服务注册状态
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=gateway-service

# 检查数据库连接
docker exec -it mysql_db mysql -uroot -proot -e "SHOW DATABASES;"

# 检查 Redis 连接
docker exec -it redis_cache redis-cli ping

# 查看 RocketMQ 主题
docker exec -it rmqnamesrv sh mqadmin topicList -n localhost:9876
```

### 日志查看

```bash
# 查看服务日志
docker logs -f nacos_server
docker logs -f mysql_db
docker logs -f redis_cache

# 查看应用日志
tail -f logs/application.log
tail -f logs/error.log
```

## 部署相关

### Docker 容器管理

```bash
# 停止所有服务
docker-compose down

# 重启特定服务
docker-compose restart nacos

# 查看服务状态
docker-compose ps

# 清理未使用的资源
docker system prune -f
```

### 生产环境配置

生产环境需要调整的关键配置：

- **数据库连接**: 修改为生产数据库地址和凭据
- **Redis 配置**: 配置生产 Redis 集群
- **Nacos 配置**: 使用生产 Nacos 集群
- **日志级别**: 调整为 INFO 或 WARN 级别
- **JVM 参数**: 根据服务器配置调整内存分配
