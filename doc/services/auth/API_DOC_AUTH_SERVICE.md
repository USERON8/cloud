# Auth Service API Documentation

## 服务概述

认证服务（Auth Service）是整个微服务系统的OAuth2.1授权服务器，负责用户认证、令牌管理、会话控制等核心功能。

**服务端口**: 8081
**Gateway路由前缀**: `/auth`
**技术栈**: Spring Authorization Server 1.4.x + OAuth2.1 + JWT + PKCE

---

## 基础信息

- **服务名称**: auth-service
- **注册中心**: Nacos (localhost:8848)
- **负载均衡**: Gateway通过`lb://auth-service`访问
- **API文档**: http://localhost:8081/doc.html
- **Gateway文档**: http://localhost:80/auth-service/doc.html

---

## 认证与授权

### 1. 获取OAuth2令牌（标准端点）

**接口路径**: `POST /oauth2/token`
**接口说明**: OAuth2.1标准令牌端点，支持多种授权模式
**权限要求**: 无（公开端点）

#### 1.1 密码模式（Password Grant）

**请求参数** (application/x-www-form-urlencoded):
```
grant_type=password
username=admin
password=admin123
client_id=web-client
client_secret=WebClient@2024#Secure
scope=read write
```

**响应示例**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "refresh_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 7200,
  "scope": "read write"
}
```

#### 1.2 刷新令牌（Refresh Token）

**请求参数**:
```
grant_type=refresh_token
refresh_token={your_refresh_token}
client_id=web-client
client_secret=WebClient@2024#Secure
```

#### 1.3 客户端凭证模式（Client Credentials）

**请求参数**:
```
grant_type=client_credentials
client_id=service-client
client_secret=ServiceClient@2024#Secure
scope=admin:read admin:write
```

---

## API接口列表

### 1. 用户注册

**接口路径**: `POST /auth/users/register`
**接口说明**: 注册新用户并自动登录，返回OAuth2令牌
**权限要求**: 无（公开接口）

**请求体**:
```json
{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "phone": "13800138000",
  "nickname": "新用户",
  "userType": "USER"
}
```

**响应示例**:
```json
{
  "code": 201,
  "message": "注册成功",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJSUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": {
      "id": 1,
      "username": "newuser",
      "email": "user@example.com",
      "userType": "USER"
    }
  },
  "timestamp": 1704067200000
}
```

**错误响应**:
- `1002`: 用户名已存在
- `1001`: 参数验证失败

---

### 2. 用户登录

**接口路径**: `POST /auth/sessions`
**接口说明**: 用户名密码登录，返回OAuth2令牌
**权限要求**: 无（公开接口）

**请求体**:
```json
{
  "username": "testuser",
  "password": "password123",
  "userType": "USER"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJSUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": {
      "id": 1,
      "username": "testuser",
      "email": "user@example.com",
      "userType": "USER"
    }
  },
  "timestamp": 1704067200000
}
```

**错误响应**:
- `1003`: 用户名或密码错误
- `1004`: 账户已被禁用
- `1006`: 用户类型不匹配

---

### 3. 用户登出

**接口路径**: `DELETE /auth/sessions`
**接口说明**: 撤销当前访问令牌，使其立即失效
**权限要求**: 需要Bearer Token

**请求头**:
```
Authorization: Bearer {access_token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null,
  "timestamp": 1704067200000
}
```

---

### 4. 批量登出（撤销所有会话）

**接口路径**: `DELETE /auth/users/{username}/sessions`
**接口说明**: 撤销指定用户的所有活跃会话
**权限要求**: `ROLE_ADMIN` + `SCOPE_admin:write`

**路径参数**:
- `username`: 用户名

**响应示例**:
```json
{
  "code": 200,
  "message": "成功撤销用户 testuser 的 3 个活跃会话",
  "data": "成功撤销用户 testuser 的 3 个活跃会话",
  "timestamp": 1704067200000
}
```

---

### 5. 验证令牌有效性

**接口路径**: `GET /auth/tokens/validate`
**接口说明**: 验证令牌是否有效
**权限要求**: `isAuthenticated()`

**请求头**:
```
Authorization: Bearer {access_token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "令牌有效, 用户: admin, 权限: read, write, admin:read",
  "data": "令牌有效, 用户: admin, 权限: read, write, admin:read",
  "timestamp": 1704067200000
}
```

---

### 6. 令牌刷新（简化版）

**接口路径**: `POST /auth/tokens/refresh`
**接口说明**: 使用刷新令牌获取新的访问令牌
**权限要求**: 无（公开接口）
**注意**: 推荐使用标准OAuth2端点 `POST /oauth2/token`

**请求参数** (Query String):
- `refresh_token`: 刷新令牌

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJSUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": { ... }
  }
}
```

---

## OAuth2 Token管理接口

### 1. 获取Token存储统计

**接口路径**: `GET /auth/tokens/stats`
**接口说明**: 查看当前Redis中存储的token统计信息
**权限要求**: `ROLE_ADMIN`

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "authorizationCount": 125,
    "tokenIndexCount": 250,
    "redisInfo": "Hash存储模式",
    "storageType": "Redis Hash",
    "serviceStats": {
      "totalAuthorizations": 125,
      "totalTokens": 250
    }
  }
}
```

---

### 2. 查看授权详情

**接口路径**: `GET /auth/tokens/authorization/{id}`
**接口说明**: 根据授权ID查看详细的授权信息
**权限要求**: `ROLE_ADMIN`

**路径参数**:
- `id`: 授权ID

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "id": "auth-123456",
    "clientId": "web-client",
    "principalName": "admin",
    "grantType": "password",
    "scopes": ["read", "write"],
    "tokens": {
      "accessToken": {
        "issuedAt": "2025-01-15T10:00:00Z",
        "expiresAt": "2025-01-15T12:00:00Z",
        "scopes": ["read", "write"]
      },
      "refreshToken": {
        "issuedAt": "2025-01-15T10:00:00Z",
        "expiresAt": "2025-01-22T10:00:00Z"
      }
    }
  }
}
```

---

### 3. 撤销授权

**接口路径**: `DELETE /auth/tokens/authorization/{id}`
**接口说明**: 撤销指定ID的OAuth2授权
**权限要求**: `ROLE_ADMIN`

**路径参数**:
- `id`: 授权ID

---

### 4. 清理过期Token

**接口路径**: `POST /auth/tokens/cleanup`
**接口说明**: 手动触发过期token清理
**权限要求**: `ROLE_ADMIN`

---

### 5. 令牌黑名单管理

#### 5.1 获取黑名单统计

**接口路径**: `GET /auth/tokens/blacklist/stats`
**权限要求**: `ROLE_ADMIN`

#### 5.2 手动加入黑名单

**接口路径**: `POST /auth/tokens/blacklist/add`
**权限要求**: `ROLE_ADMIN`

**请求参数**:
- `tokenValue`: 令牌值
- `reason`: 撤销原因 (默认: admin_manual)

#### 5.3 检查令牌是否在黑名单

**接口路径**: `GET /auth/tokens/blacklist/check`
**权限要求**: `ROLE_ADMIN`

**请求参数**:
- `tokenValue`: 令牌值

#### 5.4 清理过期黑名单

**接口路径**: `POST /auth/tokens/blacklist/cleanup`
**权限要求**: `ROLE_ADMIN`

---

## GitHub OAuth2登录接口

### 1. 获取GitHub登录URL

**接口路径**: `GET /auth/oauth2/github/login-url`
**接口说明**: 获取GitHub OAuth2登录跳转链接

**响应示例**:
```json
{
  "code": 200,
  "data": "/oauth2/authorization/github"
}
```

---

### 2. GitHub OAuth2回调处理

**接口路径**: `GET /auth/oauth2/github/callback`
**接口说明**: 处理GitHub OAuth2登录回调

**请求参数**:
- `code`: 授权码
- `state`: 状态参数 (可选)

---

### 3. 获取GitHub用户信息

**接口路径**: `GET /auth/oauth2/github/user-info`
**接口说明**: OAuth2回调后获取用户信息和JWT令牌

---

### 4. GitHub OAuth2登录状态检查

**接口路径**: `GET /auth/oauth2/github/status`
**接口说明**: 检查当前用户是否已通过GitHub OAuth2认证

---

## Well-Known Endpoints

### JWKS端点

**接口路径**: `GET /.well-known/jwks.json`
**接口说明**: 获取JWT公钥集合（JSON Web Key Set）
**权限要求**: 无（公开端点）

**响应示例**:
```json
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "kid": "key-id-1",
      "alg": "RS256",
      "n": "..."
    }
  ]
}
```

---

### OAuth2授权端点

**接口路径**: `GET /oauth2/authorize`
**接口说明**: OAuth2授权码模式的授权端点
**权限要求**: 需要用户登录

**请求参数**:
- `client_id`: 客户端ID
- `redirect_uri`: 回调地址
- `response_type`: 响应类型 (code)
- `scope`: 授权范围
- `state`: 状态参数
- `code_challenge`: PKCE挑战码 (推荐)
- `code_challenge_method`: 挑战方法 (S256)

---

## 错误码说明

| 错误码 | 说明 | HTTP状态码 |
|-------|------|-----------|
| 1001 | 参数验证失败 | 400 |
| 1002 | 用户已存在 | 400 |
| 1003 | 用户名或密码错误 | 401 |
| 1004 | 账户已被禁用 | 403 |
| 1005 | 令牌无效或缺失 | 401 |
| 1006 | 用户类型不匹配 | 400 |

---

## 使用示例

### 1. 用户注册并登录

```bash
curl -X POST "http://localhost:80/auth/users/register" \
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

### 2. 用户登录

```bash
curl -X POST "http://localhost:80/auth/sessions" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "userType": "USER"
  }'
```

### 3. 使用OAuth2标准端点获取令牌

```bash
curl -X POST "http://localhost:8081/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=admin&password=admin123&client_id=web-client&client_secret=WebClient@2024#Secure&scope=read write"
```

### 4. 验证令牌

```bash
curl -X GET "http://localhost:80/auth/tokens/validate" \
  -H "Authorization: Bearer {access_token}"
```

### 5. 登出

```bash
curl -X DELETE "http://localhost:80/auth/sessions" \
  -H "Authorization: Bearer {access_token}"
```

---

## 安全注意事项

1. **生产环境配置**:
   - 必须使用HTTPS
   - 客户端密钥必须保密
   - Token有效期建议设置为1-2小时
   - Refresh Token有效期建议设置为7-30天

2. **PKCE支持**:
   - 授权码模式推荐使用PKCE增强安全性
   - code_challenge_method推荐使用S256

3. **令牌管理**:
   - Token存储在Redis中，支持TTL自动过期
   - 支持Token黑名单机制
   - 支持批量撤销用户会话

4. **访问控制**:
   - 所有管理接口需要ADMIN角色
   - Token管理接口需要admin:read或admin:write权限
   - 用户登出可由用户本人或管理员执行

---

## 版本信息

- **API版本**: v1.0
- **文档更新**: 2025-01-15
- **Spring Authorization Server**: 1.4.x
- **OAuth2协议**: OAuth 2.1
