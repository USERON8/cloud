# Gateway Service - 响应式网关

## 📋 服务概述

Gateway Service 是基于 Spring Cloud Gateway 构建的响应式微服务网关，作为 OAuth2.1 资源服务器提供统一的认证鉴权入口。

**最近优化时间**: 2025-09-19

### 🔧 技术栈版本

- **Spring Boot**: 3.5.3
- **Spring Cloud Gateway**: 4.x (WebFlux响应式)
- **Spring Security OAuth2 Resource Server**: Boot管理
- **JWT**: Boot管理 + Nimbus JWT
- **Redis**: 7.0+ (会话和缓存)
- **Nacos**: 2.4.0+ (服务发现和配置)

## 🌟 核心功能

#### 1. OAuth2.1 资源服务器

- ✅ **统一鉴权入口**: 所有API请求统一认证
- ✅ **JWT Token验证**: 验证auth-service颁发的JWT
- ✅ **权限检查**: 基于JWT claims的权限验证
- ✅ **Token转发**: 将认证信息传递给下游服务

#### 2. 网关特性

- 🚀 **响应式架构**: WebFlux高并发处理
- 🔄 **动态路由**: 支持运行时路由配置
- 🛡️ **安全过滤**: IP检查、限流、性能监控
- 📊 **API文档聚合**: 聚合所有微服务的API文档

## 🚀 服务配置

### 端口与数据库

```yaml
# 网关端口
server:
  port: 80  # 统一入口

# Redis配置（已优化）
spring:
  data:
    redis:
      database: 6  # 网关专用数据库
      max-active: 16  # 提高连接池大小
      timeout: 5000ms  # 缩短超时时间
      min-idle: 2
```

### OAuth2.1 资源服务器配置

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # 通过网关访问auth-service的JWK端点
          jwk-set-uri: http://127.0.0.1:80/.well-known/jwks.json
          issuer-uri: http://127.0.0.1:80
```

## 🌯️ 路由配置

### 路由优先级策略

为了解决路径冲突和提高匹配准确性，路由按优先级排序：

1. **具体路径优先** - 精确匹配的路径优于Pattern匹配
2. **API路由优先** - `/api/{service}/**` 优于通用路由
3. **服务直接路由** - `/{service}-service/**` 优于通用路由
4. **通用路由最后** - `/{service}/**` 作为备选方案

### 路由映射规则

#### 认证服务路由 (OAuth2.1)

| 外部路径                     | 内部路由                | 优先级 | 说明              |
|------------------------------|----------------------------|------|-------------------|
| `/oauth2/authorize`          | `/oauth2/authorize`        | 1    | OAuth2授权端点     |
| `/oauth2/token`              | `/oauth2/token`            | 1    | 令牌端点           |
| `/oauth2/revoke`             | `/oauth2/revoke`           | 1    | 令牌撤销端点       |
| `/.well-known/jwks.json`     | `/.well-known/jwks.json`   | 1    | JWK公钥端点        |
| `/userinfo`                  | `/userinfo`                | 1    | 用户信息端点       |
| `/api/auth/**`               | `/auth/api/**`             | 2    | 认证服务API      |
| `/login/**`, `/register/**`  | 直接转发                   | 3    | 认证页面           |
| `/auth-service/**`           | `/**`                      | 4    | 服务直接访问       |
| `/auth/**`                   | `/auth/**`                 | 5    | 通用认证路径       |

#### 其他微服务路由

| 服务类型 | API路由             | 直接路由              | 通用路由        |
|----------|--------------------|-----------------------|-----------------|
| 用户服务 | `/api/user/**`     | `/user-service/**`    | `/user/**`      |
| 商品服务 | `/api/product/**`  | `/product-service/**` | `/product/**`   |
| 订单服务 | `/api/order/**`    | `/order-service/**`   | `/order/**`     |
| 库存服务 | `/api/stock/**`    | `/stock-service/**`   | `/stock/**`     |
| 支付服务 | `/api/payment/**`  | `/payment-service/**` | `/payment/**`   |
| 搜索服务 | `/api/search/**`   | -                     | `/search/**`    |
| 日志服务 | `/api/log/**`      | -                     | `/log/**`       |

### 路径重写规则

- **API路由**: `/api/{service}/path` → `/{service}/api/path`
- **直接路由**: `/{service}-service/path` → `/path`  
- **通用路由**: `/{service}/path` → `/{service}/path` (保持不变)

## 🔧 优化改进

### 1. 配置优化

- ✅ **Redis数据库**: database: 6 (网关专用)
- ✅ **连接池优化**: max-active: 16, timeout: 5000ms
- ✅ **配置去重**: 合并重复的security配置

### 2. 安全增强

- ✅ **API路径权限收紧**: 修复过于宽松的`/api/**`配置
- ✅ **环境化配置**: 支持测试和生产环境的不同安全策略
- ✅ **JWT验证**: 完整的OAuth2.1资源服务器配置
- ✅ **CORS统一配置**: 支持跨域请求

### 2. 过滤器优化

- ✅ **JWT Token转发**: 简化转发逻辑，添加用户ID头
- ✅ **安全过滤器**: 统一的安全检查和监控
- ✅ **性能监控**: 请求响应时间和状态码统计
- ✅ **错误处理**: 友好的OAuth2错误响应

### 3. 安全增强

- ✅ **Auth服务完全开放**: 认证服务所有端点无需token验证
- ✅ **OAuth2.1标准端点**: 完全开放访问（/oauth2/**、/.well-known/**、/userinfo）
- ✅ **自定义认证API**: 开放访问（/api/auth/**、/api/v1/auth/**）
- ✅ **统一鉴权**: 其他资源服务API需要JWT认证
- ✅ **错误处理**: 标准化的401/403错误响应

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

| API分类 | 限流Key         | 限制次数 | 时间窗口 |
|-------|---------------|------|------|
| 登录接口  | auth:login    | 10次  | 60秒  |
| 注册接口  | auth:register | 5次   | 300秒 |
| 文件上传  | file:upload   | 20次  | 60秒  |
| 测试接口  | api:test      | 50次  | 60秒  |
| 普通API | api:access    | 200次 | 60秒  |

## 路由配置

### 服务发现策略

为了确保路由匹配的可预测性和稳定性，网关采用：

- **禁用自动服务发现**: `discovery.locator.enabled: false`
- **手动路由配置**: 所有路由都显式定义在 `application-route.yml`
- **避免路径冲突**: 手动配置的路由不会与自动发现的路由产生冲突

网关支持精细化的路由配置，主要路由规则：

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

| 指标名称                       | 类型    | 描述     |
|----------------------------|-------|--------|
| `gateway.request.duration` | Timer | 请求处理耗时 |
| `gateway.request.total`    | Gauge | 总请求数   |
| `gateway.request.error`    | Gauge | 错误请求数  |

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

### 2. 认证服务安全策略

**完全开放的路径**（无需token验证）：

- `/oauth2/**` - OAuth2.1标准端点
- `/.well-known/**` - OIDC发现端点
- `/userinfo` - 用户信息端点
- `/auth/**` - 认证服务直接路径
- `/auth-service/**` - 认证服务网关路径
- `/api/auth/**` - 自定义认证API
- `/api/v1/auth/**` - 版本化认证API
- `/login/**`, `/register/**`, `/logout/**` - 认证相关页面
- `/actuator/**` - 健康检查和监控端点

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
- Redis 6.0+ (database: 6)
- Nacos 2.4.0+

### 启动命令

```bash
# 开发环境
java -jar gateway-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

## 🔍 安全审计报告 (2025-09-22)

### OAuth2资源服务器配置

#### ✅ 正确配置

- JWT验证端点：`http://127.0.0.1:80/.well-known/jwks.json`
- 响应式JWT解码器：`NimbusReactiveJwtDecoder`
- 统一认证入口：端口80
- JWT令牌转发：包含用户信息头

#### 🚨 严重安全问题

1. **API路径完全开放**
   - 问题：`/api/**` 路径设置为 `permitAll()`
   - 位置：`OAuth2ResourceServerConfig.springSecurityFilterChain()`
   - 风险：绕过认证直接访问所有业务API
   - 状态：❌ 待修复
   - 建议：移除测试配置，严格控制API访问权限

#### ⚠️ 中等问题

2. **认证服务路径过于宽泛**
   - 问题：多个认证相关路径都完全开放
   - 影响路径：`/auth/**`, `/auth-service/**`, `/api/auth/**`
   - 建议：精确控制需要开放的端点

#### 📋 技术栈版本

- **Spring Boot**: 3.5.3
- **Spring Cloud Gateway**: 4.x (WebFlux响应式)
- **Spring Security OAuth2 Resource Server**: Boot管理
- **JWT**: Boot管理 + Nimbus JWT
- **Redis**: 7.0+ (database: 6)
- **Nacos**: 2.4.0+

### 路由配置状态

#### ✅ 正确配置

- 禁用自动服务发现 (`discovery.locator.enabled: false`)
- 手动路由配置避免冲突
- OAuth2.1标准端点路由完整
- 支持服务发现的负载均衡

#### 📊 限流规则

| API分类 | 限流Key | 限制次数 | 时间窗口 | 状态 |
|-------|---------|------|------|------|
| 登录接口 | auth:login | 10次 | 60秒 | ✅ 已配置 |
| 注册接口 | auth:register | 5次 | 300秒 | ✅ 已配置 |
| 文件上传 | file:upload | 20次 | 60秒 | ✅ 已配置 |
| 测试接口 | api:test | 50次 | 60秒 | ⚠️ 生产环境应移除 |
| 普通API | api:access | 200次 | 60秒 | ✅ 已配置 |

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
"your-api:key",new RateLimitConfig(permits, windowSeconds)
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

### v0.0.1-SNAPSHOT (2025-09-19)

- 🔒 **安全优化**: 认证服务所有端点完全开放，无需token验证
- 🌯️ **路由优化**: 重构路由配置，按优先级排序，消除路径冲突和重复配置
- ✅ **OAuth2.1标准**: 严格遵循OAuth2.1规范，所有认证端点公开访问
- 🔧 **配置优化**: 统一认证路径管理，支持多种路径格式
- 🚫 **服务发现**: 禁用自动服务发现，优先手动路由配置提高可预测性
- 📝 **文档更新**: 详细记录安全策略和路由优先级配置

### v0.0.1-SNAPSHOT (2025-01-12)

- 🔥 **重大重构**: 移除对common-module的依赖
- ✨ 新增网关专用的安全管理组件
- ✨ 优化限流和监控机制
- 🐛 修复响应式编程模型的兼容性问题
- 📝 完善文档和配置说明

---

## 联系方式

如有问题或建议，请联系开发团队或提交Issue。
