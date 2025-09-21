# Auth Service - OAuth2.1 认证服务

## 📋 服务概述

Auth Service 是基于 OAuth2.1 标准实现的认证授权服务器，提供完整的 JWT Token 管理和用户认证功能。

### 🔧 技术栈版本

- **Spring Boot**: 3.5.3
- **Spring Security OAuth2**: Boot管理
- **JWT**: Boot管理 + Nimbus JWT
- **Redis**: 7.0+ (授权信息存储)
- **MySQL**: 8.0+ (用户数据存储)

### 🌟 核心功能

#### 1. OAuth2.1 标准支持

- ✅ **授权码模式 + PKCE**: Web/移动端安全认证
- ✅ **客户端凭证模式**: 服务间通信
- ✅ **JWT Token 管理**: 生成、刷新、撤销
- ✅ **多客户端支持**: Web、移动端、服务端

#### 2. 安全特性

- 🔒 **PKCE 增强**: 移动端必须使用 PKCE
- 🔄 **Token 不重用**: OAuth2.1 推荐的 Refresh Token 策略
- ⏰ **短期 Token**: Access Token 2小时，Refresh Token 30天
- 💾 **Redis 持久化**: 所有授权信息存储在 Redis

## 🚀 服务配置

### 端口与数据库

```yaml
# 服务端口（已优化）
server:
  port: 8080

# Redis配置（已优化）
spring:
  data:
    redis:
      database: 5  # 认证专用数据库
      max-active: 16  # 提高连接池大小
      min-idle: 2
```

### OAuth2.1 客户端配置

| 客户端类型 | Client ID      | 认证方式                | 授权模式               | PKCE | 用途    |
|-------|----------------|---------------------|--------------------|------|-------|
| Web应用 | web-client     | Client Secret Basic | Authorization Code | ✓    | Web前端 |
| 移动应用  | mobile-client  | None (公共客户端)        | Authorization Code | ✓    | 移动端   |
| 服务间通信 | client-service | Client Secret Basic | Client Credentials | ✗    | 内部服务  |

### Token 配置

```yaml
# Token 生命周期
Web客户端:
  access_token: 2小时
  refresh_token: 30天

移动客户端:
  access_token: 1小时  
  refresh_token: 7天

服务客户端:
  access_token: 12小时
```

## 🔧 优化改进

### 1. 配置优化

- ✅ **端口修正**: 8082 → 8080 (与开发文档一致)
- ✅ **Redis数据库**: database: 5 (认证专用)
- ✅ **连接池优化**: max-active: 16, min-idle: 2
- ✅ **客户端配置统一**: 移除重复配置项

### 2. 代码优化

- ✅ **JWT解码器修正**: localhost:8080 端点检查
- ✅ **RedisOAuth2AuthorizationService完善**: 实现findById和findByToken方法
- ✅ **错误处理增强**: 添加详细的调试日志

### 3. 安全增强

- ✅ **OAuth2.1标准**: 严格按照最新标准实现
- ✅ **PKCE强制**: Web和移动端必须使用
- ✅ **Token不重用**: reuseRefreshTokens: false

## 🌐 API 端点

### OAuth2.1 标准端点

```
# 授权端点
GET  /oauth2/authorize

# 令牌端点  
POST /oauth2/token

# 令牌撤销
POST /oauth2/revoke

# 令牌内省
POST /oauth2/introspect

# JWK端点
GET  /.well-known/jwks.json

# 用户信息端点
GET  /userinfo
```

### 自定义认证端点

```
# 用户登录
POST /api/v1/auth/login

# 用户注册
POST /api/v1/auth/register

# Token验证
POST /api/v1/auth/verify-token

# Token刷新
POST /api/v1/auth/refresh-token
```

## 🧪 测试方式

### 1. 授权码模式测试 (PKCE)

```bash
# 1. 生成PKCE参数
code_verifier=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-43)
code_challenge=$(echo -n $code_verifier | openssl dgst -sha256 -binary | base64 | tr -d "=+/" | cut -c1-43)

# 2. 获取授权码
GET http://127.0.0.1:80/oauth2/authorize?
    response_type=code&
    client_id=web-client&
    redirect_uri=http://127.0.0.1:80/authorized&
    scope=openid profile read write&
    code_challenge=$code_challenge&
    code_challenge_method=S256

# 3. 交换访问令牌
POST http://127.0.0.1:80/oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic [web-client:WebClient@2024#Secure]

grant_type=authorization_code&
code=[授权码]&
redirect_uri=http://127.0.0.1:80/authorized&
code_verifier=$code_verifier
```

### 2. 客户端凭证模式测试

```bash
POST http://127.0.0.1:80/oauth2/token
Content-Type: application/x-www-form-urlencoded  
Authorization: Basic [client-service:ClientService@2024#Secure]

grant_type=client_credentials&
scope=internal_api service.read service.write
```

## 🔍 监控与调试

### 健康检查

```bash
# 服务健康状态
GET http://localhost:8080/actuator/health

# JWK端点验证
GET http://localhost:8080/.well-known/jwks.json
```

### Redis监控

```bash
# 查看授权信息
redis-cli -h localhost -p 6379 -n 5
KEYS oauth2:*

# 查看特定授权
GET oauth2:authorization:web-client-id:[authorization-id]
```

## 🚨 注意事项

### 1. 生产环境配置

- 🔐 使用真实的RSA密钥对，不要使用代码生成
- 🛡️ 启用HTTPS，OAuth2.1要求安全传输
- 📊 配置Redis集群，确保高可用性
- 🔒 使用强密码策略，定期轮换客户端密钥

### 2. 开发调试

- 📝 查看 `org.springframework.security: debug` 日志
- 🔍 使用 Redis 客户端监控授权信息存储
- 📊 通过 `/actuator/metrics` 监控性能指标

### 3. 集成要点

- 🔗 所有请求必须通过 Gateway (端口80) 访问
- 🎯 下游服务使用 JWT Bearer Token 认证
- 📡 服务间调用使用 client_credentials 模式

## 📚 相关文档

- [OAuth2.1 RFC标准](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1)
- [PKCE RFC7636](https://tools.ietf.org/html/rfc7636)
- [JWT RFC7519](https://tools.ietf.org/html/rfc7519)
- [项目整体架构](../README.md)
- [Gateway网关服务](../gateway/README.md)

---

**最后更新**: 2025-09-18  
**维护团队**: Cloud Development Team  
**服务版本**: 0.0.1-SNAPSHOT
