# Spring Cloud 微服务配置指南

本文档详细说明了最新版本中新增的配置功能和使用方法。

## 📋 目录

1. [消息配置 (MessageProperties)](#1-消息配置-messageproperties)
2. [OAuth2资源服务器配置 (OAuth2ResourceServerProperties)](#2-oauth2资源服务器配置-oauth2resourceserverproperties)
3. [异步配置 (AsyncProperties)](#3-异步配置-asyncproperties)
4. [配置优先级和继承](#4-配置优先级和继承)
5. [实际应用示例](#5-实际应用示例)

---

## 1. 消息配置 (MessageProperties)

### 1.1 功能说明

`MessageProperties` 提供了 RocketMQ 消息配置的外部化支持，允许通过配置文件灵活控制消息发送行为。

### 1.2 配置项说明

#### 基础配置

```yaml
app:
  message:
    enabled: true                        # 是否启用消息功能
    send-retry-times: 3                  # 消息发送重试次数
    send-timeout: 3000                   # 消息发送超时时间(毫秒)
    trace-enabled: true                  # 是否启用消息追踪
    idempotent-enabled: false            # 是否启用幂等性检查
    idempotent-expire-seconds: 86400     # 幂等性检查过期时间(秒)
```

#### 消息头配置

```yaml
app:
  message:
    header:
      auto-trace-id: true                # 是否自动添加追踪ID
      auto-timestamp: true               # 是否自动添加时间戳
      auto-service-name: true            # 是否自动添加服务名称
      custom-prefix: ""                  # 自定义消息头前缀
```

#### 日志配置

```yaml
app:
  message:
    log:
      verbose: true                      # 是否启用详细日志
      log-payload: false                 # 是否记录消息体
      log-headers: true                  # 是否记录消息头
      payload-max-length: 1000           # 消息体日志最大长度
```

### 1.3 使用方式

#### 继承 BaseMessageConfig

```java
@Configuration
public class OrderMessageConfig extends BaseMessageConfig {
    
    @Override
    protected String getServiceName() {
        return "订单服务";
    }
}
```

BaseMessageConfig 会自动读取配置并应用，无需额外代码。

### 1.4 配置场景

| 场景 | 推荐配置 |
|------|---------|
| 生产环境 | `log-payload: false`, `verbose: false` |
| 开发环境 | `log-payload: true`, `verbose: true` |
| 测试环境 | `log-payload: true`, `verbose: true` |
| 高并发场景 | `trace-enabled: false`, `verbose: false` |

---

## 2. OAuth2资源服务器配置 (OAuth2ResourceServerProperties)

### 2.1 功能说明

`OAuth2ResourceServerProperties` 提供了 JWT 验证和安全配置的外部化支持，支持灵活的权限控制和安全策略。

### 2.2 配置项说明

#### JWT配置

```yaml
app:
  security:
    oauth2:
      resource-server:
        jwt:
          cache-minutes: 30              # JWT验证器缓存时长（分钟）
          blacklist-enabled: false       # 是否启用JWT黑名单检查
          clock-skew-seconds: 60         # JWT时钟偏移容忍度（秒）
          validate-audience: false       # 是否验证audience
          expected-audiences:            # 期望的audience列表
            - api-client
            - web-client
          validate-not-before: true      # 是否验证not before
          max-validity-hours: 24         # JWT最大有效时长（小时）
```

#### 权限配置

```yaml
app:
  security:
    oauth2:
      resource-server:
        authority:
          prefix: "SCOPE_"               # 权限前缀
          claim-name: "scope"            # 权限声明名称
          role-mapping: false            # 是否启用角色映射
          role-prefix: "ROLE_"           # 角色前缀
          multi-claim-extraction: false  # 是否从多个声明中提取权限
          additional-claim-names:        # 额外的权限声明名称列表
            - authorities
            - permissions
```

#### CORS配置

```yaml
app:
  security:
    oauth2:
      resource-server:
        cors:
          enabled: true                  # 是否启用CORS
          allowed-origins:               # 允许的源
            - "http://localhost:3000"
          allowed-methods:               # 允许的方法
            - GET
            - POST
            - PUT
            - DELETE
          allowed-headers:               # 允许的请求头
            - "*"
          allow-credentials: true        # 是否允许凭证
          max-age-seconds: 3600          # 预检请求缓存时间（秒）
```

### 2.3 使用方式

#### 继承 BaseOAuth2ResourceServerConfig

```java
@Configuration
@Order(101)
public class ResourceServerConfig extends BaseOAuth2ResourceServerConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        SecurityFilterChain chain = createSecurityFilterChain(http);
        logConfigurationComplete();
        return chain;
    }
    
    @Override
    protected void configurePublicPaths(AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers("/actuator/**", "/doc.html").permitAll();
    }
    
    @Override
    protected void configureProtectedPaths(AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers("/api/**").hasAnyAuthority("SCOPE_read", "SCOPE_write");
    }
    
    @Override
    protected String getServiceName() {
        return "订单服务";
    }
}
```

### 2.4 配置场景

| 场景 | 推荐配置 |
|------|---------|
| 内部API | `validate-audience: false`, `cors.enabled: false` |
| 对外API | `validate-audience: true`, `cors.enabled: true`, `blacklist-enabled: true` |
| 微服务间调用 | `prefix: SCOPE_`, `claim-name: scope` |
| 前端应用 | `cors.allow-credentials: true`, `max-age-seconds: 7200` |

---

## 3. 异步配置 (AsyncProperties)

### 3.1 功能说明

`AsyncProperties` 提供了线程池配置的外部化支持，支持多种预设线程池类型和自定义配置。

### 3.2 配置项说明

#### 通用配置

```yaml
app:
  async:
    enabled: true                        # 是否启用异步功能
    common:
      monitoring-enabled: false          # 是否启用线程池监控
      monitoring-interval-seconds: 60    # 线程池监控间隔（秒）
      pre-start-core-threads: false      # 是否启用线程池预热
      log-slow-tasks: true               # 是否记录慢任务
      slow-task-threshold-ms: 5000       # 慢任务阈值（毫秒）
```

#### 默认线程池配置

```yaml
app:
  async:
    default-executor:
      core-pool-size: 4                  # 核心线程数
      max-pool-size: 12                  # 最大线程数
      queue-capacity: 300                # 队列容量
      keep-alive-seconds: 60             # 空闲线程存活时间（秒）
      thread-name-prefix: "async-default-"
      allow-core-thread-time-out: false
      rejected-execution-handler: "CALLER_RUNS"  # 拒绝策略
      wait-for-tasks-to-complete-on-shutdown: true
      await-termination-seconds: 60
```

#### 预设线程池类型

- **default-executor**: 默认通用异步线程池
- **message-executor**: 消息处理专用线程池
- **batch-executor**: 批处理专用线程池
- **io-executor**: IO密集型任务线程池
- **cpu-executor**: CPU密集型任务线程池

### 3.3 使用方式

#### 继承 BaseAsyncConfig

```java
@Configuration
@EnableAsync
public class OrderAsyncConfig extends BaseAsyncConfig {
    
    @Bean("orderAsyncExecutor")
    public Executor orderAsyncExecutor() {
        // 使用配置创建线程池
        Executor executor = createDefaultAsyncExecutor();
        log.info("订单异步线程池初始化完成");
        return executor;
    }
    
    @Bean("orderMessageExecutor")
    public Executor orderMessageExecutor() {
        // 使用消息线程池配置
        Executor executor = createAsyncMessageExecutor();
        log.info("订单消息线程池初始化完成");
        return executor;
    }
}
```

### 3.4 拒绝策略说明

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| `CALLER_RUNS` | 由调用线程执行 | 不能丢失任务的场景 |
| `ABORT` | 抛出异常 | 需要感知任务拒绝的场景 |
| `DISCARD` | 直接丢弃 | 可以容忍任务丢失的场景 |
| `DISCARD_OLDEST` | 丢弃最旧的任务 | 优先执行新任务的场景 |

### 3.5 配置场景

| 场景 | 推荐配置 |
|------|---------|
| 高并发查询 | `io-executor`: core=8, max=16, queue=500 |
| 消息发送 | `message-executor`: core=3, max=8, queue=100 |
| 批量处理 | `batch-executor`: core=2, max=6, queue=1000 |
| 计算密集 | `cpu-executor`: core=CPU数, max=CPU数+1, queue=100 |

---

## 4. 配置优先级和继承

### 4.1 配置优先级

1. **application.yml** (最高优先级)
2. **application-{profile}.yml**
3. **bootstrap.yml**
4. **默认值** (最低优先级)

### 4.2 配置继承关系

```
application.yml (全局配置)
    ↓
application-dev.yml (开发环境)
    ↓
服务特定配置 (order-service/application.yml)
```

### 4.3 配置覆盖示例

```yaml
# application.yml (全局默认)
app:
  async:
    default-executor:
      core-pool-size: 4
      max-pool-size: 12

# application-prod.yml (生产环境覆盖)
app:
  async:
    default-executor:
      core-pool-size: 8
      max-pool-size: 24

# order-service/application.yml (服务特定覆盖)
app:
  async:
    default-executor:
      core-pool-size: 6
      max-pool-size: 18
```

---

## 5. 实际应用示例

### 5.1 高并发订单服务配置

```yaml
app:
  # 消息配置 - 优化性能
  message:
    log:
      verbose: false
      log-payload: false
    send-retry-times: 5
  
  # 安全配置
  security:
    oauth2:
      resource-server:
        jwt:
          cache-minutes: 60
          blacklist-enabled: true
        cors:
          enabled: true
          allowed-origins:
            - "https://shop.example.com"
  
  # 异步配置 - 大容量线程池
  async:
    default-executor:
      core-pool-size: 8
      max-pool-size: 32
      queue-capacity: 1000
```

### 5.2 轻量级查询服务配置

```yaml
app:
  # 消息配置 - 轻量化
  message:
    enabled: false
  
  # 安全配置
  security:
    oauth2:
      resource-server:
        jwt:
          cache-minutes: 120
        cors:
          enabled: true
  
  # 异步配置 - 小容量线程池
  async:
    default-executor:
      core-pool-size: 2
      max-pool-size: 8
      queue-capacity: 100
```

### 5.3 开发环境配置

```yaml
app:
  # 消息配置 - 详细日志
  message:
    log:
      verbose: true
      log-payload: true
      payload-max-length: 5000
  
  # 安全配置 - 宽松设置
  security:
    oauth2:
      resource-server:
        jwt:
          blacklist-enabled: false
        cors:
          allowed-origins:
            - "*"
  
  # 异步配置 - 调试友好
  async:
    common:
      monitoring-enabled: true
      log-slow-tasks: true
      slow-task-threshold-ms: 1000
```

---

## 6. 最佳实践

### 6.1 配置管理

1. **环境分离**: 不同环境使用不同的配置文件
2. **敏感信息**: 使用配置中心或环境变量存储敏感信息
3. **文档同步**: 及时更新配置文档
4. **版本控制**: 配置文件纳入版本控制

### 6.2 性能优化

1. **生产环境**: 关闭详细日志 (`verbose: false`)
2. **线程池调优**: 根据实际负载调整线程池参数
3. **JWT缓存**: 适当增加缓存时长 (`cache-minutes: 60+`)
4. **异步处理**: 合理使用不同类型的线程池

### 6.3 安全建议

1. **JWT验证**: 生产环境启用黑名单检查
2. **CORS配置**: 明确指定允许的源，避免使用 `*`
3. **权限控制**: 使用细粒度的权限配置
4. **会话管理**: 使用无状态会话 (`STATELESS`)

---

## 7. 故障排查

### 7.1 消息发送失败

**问题**: 消息发送频繁失败

**解决方案**:
```yaml
app:
  message:
    send-retry-times: 5
    send-timeout: 5000
    log:
      verbose: true
```

### 7.2 JWT验证失败

**问题**: JWT验证频繁失败

**解决方案**:
```yaml
app:
  security:
    oauth2:
      resource-server:
        jwt:
          clock-skew-seconds: 120
          validate-audience: false
```

### 7.3 线程池满载

**问题**: 线程池频繁拒绝任务

**解决方案**:
```yaml
app:
  async:
    default-executor:
      max-pool-size: 32
      queue-capacity: 1000
      rejected-execution-handler: "CALLER_RUNS"
```

---

## 8. 更新日志

### v2.0.0 (2025-01-20)

- ✅ 新增 `MessageProperties` 配置类
- ✅ 新增 `OAuth2ResourceServerProperties` 配置类
- ✅ 新增 `AsyncProperties` 配置类
- ✅ 支持配置文件化管理
- ✅ 向后兼容旧版本配置

---

## 9. 参考资料

- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [Spring @Async](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)

---

**文档维护**: 云架构团队  
**最后更新**: 2025-01-20  
**版本**: 2.0.0

