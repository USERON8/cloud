# Auth Service (认证服务)

## 服务概述

Auth Service 是整个微服务架构的**OAuth2.1 授权服务器**,负责用户认证、令牌颁发、令牌管理和会话管理。基于 Spring
Authorization Server 实现完整的 OAuth2.1 标准协议,支持多种授权模式和第三方登录。

- **服务端口**: 8081
- **服务名称**: auth-service
- **协议标准**: OAuth2.1 (RFC 8252, RFC 8628)
- **令牌格式**: JWT (JSON Web Token)

## 技术栈

| 技术                            | 版本                 | 用途             |
|-------------------------------|--------------------|----------------|
| Spring Boot                   | 3.5.3              | 应用框架           |
| Spring Security OAuth2        | 最新                 | OAuth2.1 授权服务器 |
| Spring Security OAuth2 Client | 最新                 | 第三方OAuth2客户端   |
| Spring Security OAuth2 JOSE   | 最新                 | JWT/JWKS支持     |
| Spring Cloud Alibaba Nacos    | 2025.0.0.0-preview | 服务注册与配置中心      |
| Redis                         | -                  | 令牌存储、黑名单       |
| Redisson                      | 3.51.0             | 分布式锁、Redis客户端  |
| Caffeine                      | -                  | 本地缓存           |
| RocketMQ                      | -                  | 认证日志事件         |

## 核心功能

### 1. 用户认证 (/auth)

- ✅ **用户注册**: POST `/auth/users/register` - 注册新用户并返回令牌
- ✅ **用户登录**: POST `/auth/sessions` - 密码验证登录
- ✅ **用户登出**: DELETE `/auth/sessions` - 撤销访问令牌
- ✅ **批量登出**: DELETE `/auth/users/{username}/sessions` - 撤销用户所有会话(需管理员权限)
- ✅ **令牌验证**: GET `/auth/tokens/validate` - 验证令牌有效性
- ✅ **令牌刷新**: POST `/auth/tokens/refresh` - 使用刷新令牌获取新访问令牌

### 2. OAuth2.1 标准端点

- ✅ **授权端点**: `/oauth2/authorize` - 授权码模式授权
- ✅ **令牌端点**: POST `/oauth2/token` - 获取访问令牌
- ✅ **JWKS端点**: GET `/.well-known/jwks.json` - 公钥集合
- ✅ **Token Introspection**: POST `/oauth2/introspect` - 令牌自省
- ✅ **Token Revocation**: POST `/oauth2/revoke` - 令牌撤销

### 3. Token 管理 (/auth/tokens)

- ✅ **统计信息**: GET `/auth/tokens/stats` - 查看令牌存储统计
- ✅ **授权详情**: GET `/auth/tokens/authorization/{id}` - 查看授权详情
- ✅ **撤销授权**: DELETE `/auth/tokens/authorization/{id}` - 撤销指定授权
- ✅ **清理过期**: POST `/auth/tokens/cleanup` - 手动清理过期令牌
- ✅ **黑名单管理**:
    - GET `/auth/tokens/blacklist/stats` - 黑名单统计
    - POST `/auth/tokens/blacklist/add` - 加入黑名单
    - GET `/auth/tokens/blacklist/check` - 检查黑名单
    - POST `/auth/tokens/blacklist/cleanup` - 清理黑名单

### 4. 第三方登录 (/auth/oauth2/github)

- ✅ **GitHub OAuth2**: GET `/auth/oauth2/github/login-url` - 获取GitHub登录URL
- ✅ **用户信息**: GET `/auth/oauth2/github/user-info` - 获取GitHub用户信息
- ✅ **认证状态**: GET `/auth/oauth2/github/status` - 检查GitHub认证状态
- ✅ **回调处理**: GET `/auth/oauth2/github/callback` - 处理GitHub回调

## 数据模型

### 核心实体

#### OAuth2Authorization (存储在Redis)

```java
- id: String                    // 授权ID
- registeredClientId: String    // 客户端ID
- principalName: String         // 用户名
- authorizationGrantType: GrantType // 授权类型
- authorizedScopes: Set<String> // 授权范围
- accessToken: Token            // 访问令牌
- refreshToken: Token           // 刷新令牌
```

### Redis 存储结构

```
Hash存储模式:
- oauth2:auth:{authorizationId}    // 授权信息Hash
  - data: OAuth2Authorization JSON
  - clientId: 客户端ID
  - principalName: 用户名
  - createTime: 创建时间

- oauth2:token:{tokenValue} -> authorizationId  // Token索引

黑名单:
- oauth2:blacklist:{tokenValue}
  - subject: 用户名
  - reason: 撤销原因
  - revokedAt: 撤销时间
  - TTL: 自动过期
```

## 依赖服务

| 服务           | 用途          | 调用方式                           |
|--------------|-------------|--------------------------------|
| user-service | 用户信息验证、注册   | Feign Client (UserFeignClient) |
| Redis        | 令牌存储、黑名单、缓存 | RedisTemplate, Redisson        |
| Nacos        | 服务注册、配置管理   | Spring Cloud Alibaba           |
| RocketMQ     | 认证日志事件发送    | Spring Cloud Stream            |

## 配置说明

### 端口配置

```yaml
server:
  port: 8081
```

### OAuth2 客户端配置

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          web-client:              # Web应用客户端
            client-id: web-client
            client-secret: WebClient@2024#Secure
            scope: openid,profile,read,write

          github:                  # GitHub第三方登录
            client-id: Ov23li4lW4aaO4mlFGRf
            client-secret: ***

          client-service:          # 服务间调用客户端
            client-id: client-service
            client-secret: ClientService@2024#Secure
            authorization-grant-type: client_credentials
```

### JWT 配置

```yaml
app:
  jwt:
    issuer: http://127.0.0.1:8081
    access-token-validity: PT2H      # 访问令牌2小时
    test-token-validity: P365D       # 测试令牌365天
```

### RocketMQ 配置

```yaml
spring:
  cloud:
    stream:
      bindings:
        log-in-0:
          destination: LOG_AUTH_TOPIC    # 认证日志主题
          group: auth-log-group
```

## 开发状态

### ✅ 已完成功能

1. **核心认证流程**
    - [x] 用户注册与自动登录
    - [x] 用户名密码登录验证
    - [x] 用户类型验证(USER/ADMIN/MERCHANT)
    - [x] 账户状态检查
    - [x] 密码加密存储(BCrypt)
    - [x] 自动令牌生成与返回

2. **OAuth2.1 标准实现**
    - [x] Authorization Code Flow (授权码模式)
    - [x] Client Credentials Flow (客户端凭证模式)
    - [x] Token Refresh Flow (刷新令牌模式)
    - [x] PKCE 支持
    - [x] Token Rotation (令牌轮转)
    - [x] JWT 令牌签发(RSA256)
    - [x] JWKS 公钥端点
    - [x] Token Introspection (令牌自省)
    - [x] Token Revocation (令牌撤销)

3. **令牌管理**
    - [x] Redis Hash存储优化
    - [x] 令牌黑名单机制
    - [x] 自动过期清理(Redis TTL)
    - [x] 令牌撤销与吊销
    - [x] 多会话管理
    - [x] 批量登出功能
    - [x] 授权详情查询(/auth/tokens/authorization/{id})
    - [x] 手动清理过期令牌(/auth/tokens/cleanup)
    - [x] 黑名单统计与管理

4. **第三方登录**
    - [x] GitHub OAuth2 完整集成
    - [x] 获取GitHub登录URL (/auth/oauth2/github/login-url)
    - [x] GitHub回调处理 (/auth/oauth2/github/callback)
    - [x] GitHub用户信息获取 (/auth/oauth2/github/user-info)
    - [x] 认证状态检查 (/auth/oauth2/github/status)
    - [x] 自动用户信息同步到user-service
    - [x] JWT令牌自动生成

5. **监控与管理**
    - [x] Token 统计信息 (总数、活跃、过期统计)
    - [x] 黑名单管理API (添加、检查、清理)
    - [x] 授权详情查询
    - [x] Spring Boot Actuator集成
    - [x] 完整的API文档(Knife4j)
    - [x] RocketMQ认证日志事件发送

### 🚧 进行中功能

1. **安全增强**
    - [ ] 登录失败次数限制
    - [ ] IP白名单/黑名单
    - [ ] 验证码支持(图形/短信)
    - [ ] 设备指纹识别

2. **多因素认证**
    - [ ] TOTP (Time-based OTP)
    - [ ] SMS 短信验证码
    - [ ] Email 邮箱验证

### 📋 计划中功能

1. **更多第三方登录**
    - [ ] 微信登录
    - [ ] 支付宝登录
    - [ ] 钉钉/企业微信登录

2. **高级会话管理**
    - [ ] 设备管理(查看所有登录设备)
    - [ ] 异地登录提醒
    - [ ] 单点登录(SSO)
    - [ ] 会话并发控制

3. **审计日志**
    - [ ] 详细的认证审计日志
    - [ ] 失败登录记录
    - [ ] 敏感操作追踪

### ⚠️ 技术债

1. **性能优化**
    - 考虑使用本地缓存(Caffeine)缓存用户信息减少Feign调用
    - Token验证性能优化(考虑JWT自包含特性)

2. **可扩展性**
    - 令牌存储考虑分片策略(当前单Redis实例)
    - JWKS密钥轮转机制完善

3. **测试覆盖**
    - 增加集成测试覆盖率
    - 安全测试用例补充

## 本地运行

### 前置条件

1. **基础设施启动**

```bash
cd docker
docker-compose up -d mysql redis nacos rocketmq
```

2. **依赖服务启动**

```bash
# User Service 必须先启动(Feign依赖)
cd user-service
mvn spring-boot:run
```

### 启动服务

```bash
# 使用Maven启动
cd auth-service
mvn spring-boot:run

# 或指定环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或使用JAR包启动
mvn clean package -DskipTests
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

### 验证服务

```bash
# 健康检查
curl http://localhost:8081/actuator/health

# JWKS端点
curl http://localhost:8081/.well-known/jwks.json

# API文档
浏览器打开: http://localhost:8081/doc.html
```

## 测试

### 运行单元测试

```bash
mvn test
```

### 手动测试流程

#### 1. 用户注册

```bash
curl -X POST "http://localhost:8081/auth/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com",
    "phone": "13800138000",
    "nickname": "测试用户",
    "userType": "USER"
  }'
```

#### 2. 用户登录

```bash
curl -X POST "http://localhost:8081/auth/sessions" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "userType": "USER"
  }'
```

#### 3. OAuth2.1 标准令牌获取

```bash
curl -X POST "http://localhost:8081/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=password123" \
  -d "client_id=web-client" \
  -d "client_secret=WebClient@2024#Secure" \
  -d "scope=read write"
```

#### 4. 令牌刷新

```bash
curl -X POST "http://localhost:8081/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=YOUR_REFRESH_TOKEN" \
  -d "client_id=web-client" \
  -d "client_secret=WebClient@2024#Secure"
```

#### 5. 用户登出

```bash
curl -X DELETE "http://localhost:8081/auth/sessions" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 注意事项

### 安全相关

1. **密钥管理**: 生产环境必须使用独立的RSA密钥对,不要使用默认密钥
2. **客户端凭证**: 客户端密钥必须妥善保管,建议使用环境变量或密钥管理服务
3. **令牌有效期**: 根据业务需求调整令牌有效期,建议访问令牌短期(2小时),刷新令牌长期(7天)
4. **HTTPS**: 生产环境必须使用HTTPS,OAuth2.1强制要求
5. **CORS配置**: 确保Gateway层正确配置CORS,避免跨域安全问题

### 数据库

Auth Service **不直接访问数据库**,所有用户信息通过 **UserFeignClient** 从 user-service 获取。这种设计:

- ✅ 解耦认证与用户管理
- ✅ 认证服务无状态化(仅依赖Redis)
- ✅ 支持多用户源集成

### 性能建议

1. **Redis连接池**: 生产环境调整Lettuce/Redisson连接池参数
2. **Feign超时**: 配置合理的Feign超时时间,避免级联超时
3. **令牌缓存**: 考虑在Gateway层缓存令牌验证结果减少请求

### 监控指标

重点关注以下指标:

- Token生成速率 (tokens/sec)
- Token验证失败率
- 黑名单命中率
- Feign调用user-service延迟
- Redis连接池使用率

## 相关文档

- [API文档 - Auth Service](../doc/services/auth/API_DOC_AUTH_SERVICE.md)
- [OAuth2.1 标准](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-07)
- [项目整体文档](../doc/README.md)

## 快速链接

- Knife4j API文档: http://localhost:8081/doc.html
- Actuator Health: http://localhost:8081/actuator/health
- Nacos控制台: http://localhost:8848/nacos
