# Gateway (API 网关服务)

## 服务概述

Gateway 是整个微服务架构的**统一入口**和**API网关**,负责请求路由、负载均衡、JWT令牌验证、限流熔断、CORS处理、API文档聚合等核心功能。基于Spring
Cloud Gateway实现高性能的响应式网关。

- **服务端口**: 80
- **服务名称**: gateway
- **网关类型**: Spring Cloud Gateway (响应式、非阻塞)
- **认证方式**: OAuth2.1 JWT 令牌验证

## 技术栈

| 技术                         | 版本                 | 用途        |
|----------------------------|--------------------|-----------|
| Spring Boot                | 3.5.3              | 应用框架      |
| Spring Cloud Gateway       | 2025.0.0           | API网关核心   |
| Spring Security OAuth2     | 最新                 | JWT令牌验证   |
| Spring Cloud Alibaba Nacos | 2025.0.0.0-preview | 服务发现与配置中心 |
| Redis                      | -                  | 限流、熔断数据存储 |
| Knife4j                    | 最新                 | API文档聚合   |
| Resilience4j               | -                  | 限流、熔断、重试  |

## 核心功能

### 1. 路由转发

**动态路由配置** (application-route.yml)

Gateway支持基于Nacos服务发现的动态路由,将外部请求转发到对应的微服务:

- `/auth/**` → auth-service (认证服务)
- `/api/user/**` → user-service (用户服务)
- `/api/orders/**` → order-service (订单服务)
- `/api/payments/**` → payment-service (支付服务)
- `/api/product/**`, `/api/category/**` → product-service (商品服务)
- `/api/stocks/**` → stock-service (库存服务)
- `/api/search/**` → search-service (搜索服务)

**特性**:

- 基于URI路径匹配
- 支持负载均衡(lb://)
- 断言(Predicate)灵活配置
- 过滤器(Filter)链式处理

### 2. JWT令牌验证

**JwtTokenForwardFilter** - JWT令牌验证与转发

- ✅ 从请求头提取JWT令牌(Authorization: Bearer token)
- ✅ 使用auth-service的JWKS端点验证令牌签名
- ✅ 解析JWT Claims(用户ID、用户名、权限等)
- ✅ 将用户信息转发到下游服务(通过请求头)
- ✅ 令牌过期自动拒绝请求
- ✅ 黑名单令牌检查(集成Redis)

**JWKS配置**:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://127.0.0.1:8081/.well-known/jwks.json
          issuer-uri: http://127.0.0.1:8081
```

### 3. 安全增强

**ResourceServerConfig** - OAuth2资源服务器配置

- ✅ 白名单路径配置(无需令牌即可访问)
    - `/auth/**` - 认证相关接口
    - `/actuator/**` - 健康检查
    - `/doc.html`, `/v3/api-docs/**` - API文档
    - `/webjars/**`, `/favicon.ico` - 静态资源
- ✅ CORS跨域配置
- ✅ CSRF防护
- ✅ 安全响应头(XSS防护、内容安全策略、X-Frame-Options)

**CORS配置**:

```yaml
spring:
  cloud:
    gateway:
      globals:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: ["http://localhost:*"]
            allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]
            allowedHeaders: ["Authorization", "Content-Type"]
            allowCredentials: true
```

### 4. 限流与熔断

**默认过滤器配置**:

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: Security
          args:
            enableIpCheck: true          # IP检查
            enableRateLimit: true        # 限流
            enableTokenCheck: true       # 令牌检查
            enablePerformanceMonitoring: true  # 性能监控
```

**限流策略**:

- 基于用户的限流(authenticated users)
- 基于IP的限流(anonymous users)
- 基于路径的限流
- Redis存储限流计数器

**熔断策略**:

- 失败率熔断
- 慢调用熔断
- 半开状态自动恢复

### 5. API文档聚合

**Knife4j Gateway聚合** - 聚合所有微服务的API文档

访问地址: http://localhost:80/doc.html

**聚合的服务**:

- ✅ auth-service (认证服务)
- ✅ user-service (用户服务)
- ✅ product-service (商品服务)
- ✅ order-service (订单服务)
- ✅ payment-service (支付服务)
- ✅ stock-service (库存服务)
- ✅ search-service (搜索服务)

**配置**:

```yaml
knife4j:
  gateway:
    enabled: true
    strategy: discover
    discover:
      enabled: true
      version: openapi3
      excluded-services: [gateway]
```

### 6. 服务发现

**Nacos集成** - 自动服务发现与负载均衡

- ✅ 自动从Nacos获取可用服务实例
- ✅ 客户端负载均衡(LoadBalancer)
- ✅ 服务健康检查
- ✅ 服务实例动态上下线

### 7. 监控与观测

**Spring Boot Actuator**

- `/actuator/health` - 健康检查
- `/actuator/metrics` - 性能指标
- `/actuator/prometheus` - Prometheus监控数据
- `/actuator/gateway/routes` - 查看当前路由配置
- `/actuator/gateway/filters` - 查看过滤器

**SystemMonitor** - 系统监控

- ✅ JVM内存监控
- ✅ CPU使用率监控
- ✅ 线程池状态监控
- ✅ 请求统计(QPS、响应时间)

## 配置说明

### 端口配置

```yaml
server:
  port: 80
  tomcat:
    threads:
      max: 200              # 最大线程数
      min-spare: 10         # 最小空闲线程
    max-connections: 8192   # 最大连接数
    accept-count: 100       # 等待队列长度
```

### Nacos配置

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        username: nacos
        password: nacos
        namespace: public
        group: DEFAULT_GROUP
```

### 安全头配置

```yaml
spring:
  security:
    headers:
      content-security-policy: "default-src 'self'; ..."
      xss-protection-enabled: true
      x-frame-options:
        mode: DENY
      cache-control: true
      content-type-options: true
```

## 路由配置示例

在 `application-route.yml` 中配置路由:

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
            - StripPrefix=0

        # 用户服务路由
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**,/api/manage/**,/api/query/**
          filters:
            - StripPrefix=0
```

## 开发状态

### ✅ 已完成功能

1. **路由核心**
    - [x] 动态路由配置(基于Nacos服务发现)
    - [x] 路径匹配路由
    - [x] 负载均衡(LoadBalancer)
    - [x] 路由断言(Predicate)
    - [x] 路由过滤器(Filter)

2. **JWT令牌验证**
    - [x] JWT令牌提取与验证
    - [x] JWKS公钥验证
    - [x] 令牌过期检查
    - [x] 用户信息提取
    - [x] 用户信息转发到下游服务

3. **安全功能**
    - [x] OAuth2资源服务器配置
    - [x] 白名单路径配置
    - [x] CORS跨域处理
    - [x] CSRF防护
    - [x] 安全响应头(XSS、CSP、X-Frame-Options)
    - [x] IP检查功能

4. **限流熔断**
    - [x] 基于用户的限流
    - [x] 基于IP的限流
    - [x] 基于路径的限流
    - [x] 熔断器集成
    - [x] 降级处理

5. **API文档聚合**
    - [x] Knife4j网关模式
    - [x] 自动发现服务API文档
    - [x] OpenAPI 3.0支持
    - [x] 分组显示(按服务)

6. **监控观测**
    - [x] Spring Boot Actuator
    - [x] 健康检查
    - [x] 性能指标收集
    - [x] Prometheus集成
    - [x] 系统监控(JVM、CPU、线程)

7. **服务发现**
    - [x] Nacos服务发现
    - [x] 动态服务实例获取
    - [x] 服务健康检查
    - [x] 客户端负载均衡

### 🚧 进行中功能

1. **限流增强**
    - [ ] 分布式限流(Redis)
    - [ ] 自定义限流策略
    - [ ] 限流监控面板

2. **日志增强**
    - [ ] 访问日志详细记录
    - [ ] 审计日志
    - [ ] 日志级别动态调整

### 📋 计划中功能

1. **灰度发布**
    - [ ] 基于Header的灰度路由
    - [ ] 基于权重的流量分配
    - [ ] A/B测试支持

2. **链路追踪**
    - [ ] Sleuth集成
    - [ ] Zipkin/Jaeger集成
    - [ ] 分布式链路追踪

3. **缓存**
    - [ ] 网关层响应缓存
    - [ ] 热点数据缓存

### ⚠️ 技术债

1. **性能优化**
    - JWT验证缓存优化(减少重复验证)
    - 路由缓存优化

2. **可观测性**
    - 请求追踪完善
    - 错误日志聚合

## 本地运行

### 前置条件

1. **基础设施启动**

```bash
cd docker
docker-compose up -d nacos redis
```

2. **依赖服务启动**

```bash
# Auth Service 必须先启动(JWT验证依赖)
cd auth-service
mvn spring-boot:run
```

### 启动服务

```bash
# 使用Maven启动
cd gateway
mvn spring-boot:run

# 或指定环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或使用JAR包启动
mvn clean package -DskipTests
java -jar target/gateway-0.0.1-SNAPSHOT.jar
```

### 验证服务

```bash
# 健康检查
curl http://localhost:80/actuator/health

# 查看路由配置
curl http://localhost:80/actuator/gateway/routes

# 访问聚合API文档
浏览器打开: http://localhost:80/doc.html
```

## 测试

### 手动测试流程

#### 1. 测试路由转发

```bash
# 通过网关访问user-service
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:80/api/query/users
```

#### 2. 测试JWT验证

```bash
# 无令牌访问(应返回401)
curl http://localhost:80/api/query/users

# 有效令牌访问(应返回200)
curl -H "Authorization: Bearer YOUR_VALID_TOKEN" \
  http://localhost:80/api/query/users

# 过期令牌访问(应返回401)
curl -H "Authorization: Bearer YOUR_EXPIRED_TOKEN" \
  http://localhost:80/api/query/users
```

#### 3. 测试CORS

```bash
# OPTIONS 预检请求
curl -X OPTIONS http://localhost:80/api/query/users \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET"
```

#### 4. 测试限流

```bash
# 快速发送多个请求触发限流
for i in {1..100}; do
  curl http://localhost:80/api/query/users
done
```

## 注意事项

### 安全相关

1. **JWT验证**: 所有需要认证的请求都会在网关层进行JWT验证,无效令牌直接拒绝
2. **白名单配置**: 确保白名单路径配置正确,避免敏感接口暴露
3. **CORS配置**: 生产环境需要严格限制允许的Origin
4. **HTTPS**: 生产环境必须使用HTTPS,避免令牌泄露

### 路由配置

1. **路径匹配**: 注意路径匹配顺序,更具体的路径应该放在前面
2. **StripPrefix**: 根据需要配置是否剥离路径前缀
3. **服务名称**: 确保lb://后的服务名称与Nacos注册的服务名一致

### 性能建议

1. **线程池配置**: 根据实际并发量调整Tomcat线程池参数
2. **JWT缓存**: 考虑缓存已验证的JWT,减少重复验证开销
3. **连接池**: 调整WebClient连接池参数

### 监控指标

重点关注以下指标:

- 网关QPS (requests/sec)
- 平均响应时间 (avg latency)
- JWT验证失败率
- 限流触发次数
- 熔断触发次数
- 下游服务可用性

## 常见问题

### 1. 路由404找不到

**原因**:

- 服务未在Nacos注册
- 路由配置路径不匹配
- 服务实例全部下线

**解决**:

```bash
# 检查Nacos注册中心
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=user-service

# 检查网关路由配置
curl http://localhost:80/actuator/gateway/routes
```

### 2. JWT验证失败

**原因**:

- Auth-service未启动
- JWKS端点无法访问
- 令牌格式错误或过期

**解决**:

```bash
# 验证JWKS端点可访问
curl http://127.0.0.1:8081/.well-known/jwks.json

# 检查令牌格式
# Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

### 3. CORS错误

**原因**:

- CORS配置不正确
- Origin不在白名单

**解决**: 检查并更新CORS配置

```yaml
allowedOriginPatterns:
  - "http://localhost:*"
  - "http://your-domain.com"
```

## 相关文档

- [Spring Cloud Gateway官方文档](https://spring.io/projects/spring-cloud-gateway)
- [API文档 - Gateway](../doc/services/gateway/API_DOC_GATEWAY.md)
- [项目整体文档](../doc/README.md)

## 快速链接

- Knife4j聚合文档: http://localhost:80/doc.html
- Actuator Health: http://localhost:80/actuator/health
- 路由配置: http://localhost:80/actuator/gateway/routes
- Nacos控制台: http://localhost:8848/nacos
