# Gateway Service 网关服务

## 概述

Gateway Service 是云微服务平台的核心网关组件，基于 Spring Cloud Gateway 构建，提供统一的 API 入口、安全认证、限流控制、负载均衡等功能。

### 版本信息

- **服务版本**: 0.0.1-SNAPSHOT
- **Spring Boot**: 3.5.3
- **Spring Cloud**: 2025.0.0
- **Spring Cloud Gateway**: 4.3.0
- **Java**: 17

## 架构设计

### 核心特性

1. **统一认证**: 基于 OAuth2.1 + JWT 的统一身份认证
2. **安全防护**: IP 白名单/黑名单、可疑请求检测
3. **限流控制**: 基于 Redis 的分布式限流
4. **性能监控**: 集成 Micrometer + Prometheus 监控
5. **API 聚合**: 统一的 Knife4j 文档聚合
6. **异常处理**: WebFlux 响应式异常处理

### 技术栈

- **网关框架**: Spring Cloud Gateway (WebFlux)
- **安全框架**: Spring Security OAuth2 Resource Server
- **缓存**: Redis (响应式)
- **监控**: Micrometer + Prometheus
- **文档**: Knife4j Gateway
- **配置中心**: Nacos
- **服务发现**: Nacos Discovery

## 重构历程

### 重构目标

在 2025-01-12 的重构中，我们移除了网关服务对 `common-module` 的依赖，实现了网关服务的独立性和自治性。

### 重构内容

#### 1. 移除 Common Module 依赖

**原因**: 
- 网关作为入口服务，应该保持轻量化和独立性
- 避免引入业务模块的复杂依赖
- 减少服务间的耦合

**改动**:
- 移除 `GatewayApplication.java` 中对 `com.cloud.common` 包的扫描
- 不再依赖 `common-module` 的配置和工具类

#### 2. 创建网关专用组件

**新增组件**:

##### GatewaySecurityAccessManager
- 路径: `com.cloud.gateway.security.GatewaySecurityAccessManager`
- 功能: IP访问控制、Token撤销检查
- 特性:
  - IP黑白名单支持
  - 可疑User-Agent检测
  - Redis分布式Token撤销
  - CIDR网段支持

##### GatewayRateLimitManager
- 路径: `com.cloud.gateway.security.GatewayRateLimitManager`
- 功能: 分布式限流管理
- 特性:
  - 基于Redis + Lua脚本的原子性限流
  - 本地缓存优化
  - 滑动窗口限流算法
  - 不同API的差异化限流配置

##### GatewayPerformanceMonitor
- 路径: `com.cloud.gateway.monitoring.GatewayPerformanceMonitor`
- 功能: 性能监控和指标收集
- 特性:
  - 响应时间统计
  - 错误率监控
  - 路径级别的性能指标
  - Micrometer集成

#### 3. 优化过滤器架构

**SecurityGatewayFilter 重构**:
- 使用网关专用的安全管理器
- 支持响应式编程模型
- 优化性能监控集成
- 改进异常处理机制

### 重构收益

1. **减少依赖**: 移除了对业务模块的依赖，提高了网关的独立性
2. **性能优化**: 去除不必要的组件扫描，提升启动速度
3. **维护性**: 网关代码更加专注和清晰
4. **扩展性**: 便于针对网关特性进行优化

## 配置说明

### 核心配置

```yaml
# 网关安全配置
gateway:
  security:
    ip:
      whitelist: ""  # IP白名单，逗号分隔
      blacklist: ""  # IP黑名单，逗号分隔
  
  # 限流配置
  ratelimit:
    default:
      permits: 100      # 默认限流次数
      window: 60        # 时间窗口(秒)
  
  # 性能监控
  monitoring:
    performance:
      enabled: true     # 是否启用性能监控

# OAuth2 资源服务器配置
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://auth-service/.well-known/jwks.json
```

### 限流规则配置

内置的限流规则：

| API分类 | 限流Key | 限制次数 | 时间窗口 |
|---------|---------|----------|----------|
| 登录接口 | auth:login | 10次 | 60秒 |
| 注册接口 | auth:register | 5次 | 300秒 |
| 文件上传 | file:upload | 20次 | 60秒 |
| 测试接口 | api:test | 50次 | 60秒 |
| 普通API | api:access | 200次 | 60秒 |

## 路由配置

网关支持动态路由配置，主要路由规则：

```yaml
spring:
  cloud:
    gateway:
      routes:
        # 认证服务路由
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/auth/**
          filters:
            - Security=true,true,true,true  # IP检查,限流,Token检查,性能监控

        # API服务路由
        - id: api-services
          uri: lb://{segment}
          predicates:
            - Path=/api/{segment}/**
          filters:
            - Security=false,true,true,true
```

## API文档聚合

网关集成了 Knife4j，提供统一的API文档入口：

- **访问地址**: `http://gateway:port/doc.html`
- **聚合范围**: 所有后端微服务的API文档
- **功能特性**: 
  - 统一认证
  - 在线测试
  - 接口分组
  - 版本管理

## 监控指标

### Prometheus 指标

网关暴露以下监控指标：

| 指标名称 | 类型 | 描述 |
|----------|------|------|
| `gateway.request.duration` | Timer | 请求处理耗时 |
| `gateway.request.total` | Gauge | 总请求数 |
| `gateway.request.error` | Gauge | 错误请求数 |

### 健康检查

- **端点**: `/actuator/health`
- **监控项**: 
  - Redis连接状态
  - Nacos连接状态
  - JWT解码器状态

## 安全特性

### 1. OAuth2.1 认证

- 基于JWT的无状态认证
- 支持Token撤销机制
- 集成Spring Security

### 2. IP访问控制

- 支持IP白名单和黑名单
- CIDR网段匹配
- 通配符支持

### 3. 可疑请求检测

自动检测并拦截可疑User-Agent：
- 渗透测试工具 (sqlmap, nikto, nmap等)
- 漏洞扫描器 (burp, zap等)
- 目录爆破工具 (dirbuster, gobuster等)

## 部署说明

### 环境要求

- JDK 17+
- Redis 6.0+
- Nacos 2.0+

### 启动命令

```bash
# 开发环境
java -jar gateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 生产环境
java -jar gateway-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=80
```

### 容器化部署

```dockerfile
FROM openjdk:17-jre-slim
COPY target/gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 开发指南

### 新增过滤器

1. 继承 `AbstractGatewayFilterFactory`
2. 实现 `apply` 方法
3. 添加 `@Component` 注解
4. 在路由配置中使用

### 自定义限流规则

在 `GatewayRateLimitManager.RATE_LIMIT_CONFIGS` 中添加新规则：

```java
"your-api:key", new RateLimitConfig(permits, windowSeconds)
```

### 扩展安全检查

在 `GatewaySecurityAccessManager` 中添加新的安全检查逻辑。

## 故障排查

### 常见问题

1. **Token认证失败**
   - 检查JWT配置
   - 确认认证服务状态
   - 验证Token格式

2. **限流异常**
   - 检查Redis连接
   - 确认限流配置
   - 查看Lua脚本执行日志

3. **路由不生效**
   - 检查Nacos服务注册
   - 确认路由配置
   - 验证负载均衡器状态

### 日志级别

```yaml
logging:
  level:
    com.cloud.gateway: DEBUG
    org.springframework.cloud.gateway: DEBUG
    reactor.netty.http.server: DEBUG
```

## 性能调优

### JVM参数建议

```bash
-Xms512m -Xmx1024m 
-XX:+UseG1GC 
-XX:G1HeapRegionSize=16m 
-XX:+UseStringDeduplication
```

### 连接池配置

```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
```

## 版本历史

### v0.0.1-SNAPSHOT (2025-01-12)

- 🔥 **重大重构**: 移除对common-module的依赖
- ✨ 新增网关专用的安全管理组件
- ✨ 优化限流和监控机制  
- 🐛 修复响应式编程模型的兼容性问题
- 📝 完善文档和配置说明

---

## 联系方式

如有问题或建议，请联系开发团队或提交Issue。
