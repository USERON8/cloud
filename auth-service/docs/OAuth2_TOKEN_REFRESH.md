# OAuth2.1 令牌刷新机制

## 概述

本系统完全支持OAuth2.1标准的令牌刷新机制，包括令牌轮转（Token Rotation）特性，确保安全性。

## 标准OAuth2.1令牌刷新端点

### 端点信息
- **URL**: `POST /oauth2/token`
- **Content-Type**: `application/x-www-form-urlencoded`
- **认证**: 客户端认证（Basic Auth或POST参数）

### 请求参数

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8080
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {base64(client_id:client_secret)}

grant_type=refresh_token&refresh_token={your_refresh_token}
```

或者使用POST参数方式：

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8080
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&refresh_token={your_refresh_token}&client_id={client_id}&client_secret={client_secret}
```

### 响应格式

成功响应：
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 7200,
  "refresh_token": "new_refresh_token_value",
  "scope": "read write user.read user.write"
}
```

错误响应：
```json
{
  "error": "invalid_grant",
  "error_description": "The provided authorization grant is invalid, expired, revoked, or does not match the redirection URI used in the authorization request."
}
```

## 令牌轮转特性

本系统实现了OAuth2.1推荐的令牌轮转特性：

1. **旧令牌撤销**: 使用刷新令牌时，旧的访问令牌和刷新令牌都会被撤销
2. **新令牌生成**: 生成全新的访问令牌和刷新令牌
3. **安全增强**: 防止令牌重放攻击，提高安全性

## 简化版刷新接口

除了标准OAuth2.1端点外，系统还提供了简化版的刷新接口：

### 端点信息
- **URL**: `GET/POST /auth/refresh-token`
- **参数**: `refresh_token={your_refresh_token}`

### 使用示例

```http
POST /auth/refresh-token HTTP/1.1
Host: localhost:8080
Content-Type: application/x-www-form-urlencoded

refresh_token={your_refresh_token}
```

### 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 7200,
    "refresh_token": "new_refresh_token_value",
    "scope": "read write user.read user.write"
  }
}
```

## 客户端配置

### Web应用客户端
- **client_id**: `web-client`
- **client_secret**: `WebClient@2024#Secure`
- **支持的授权类型**: `authorization_code`, `refresh_token`

### 移动应用客户端
- **client_id**: `mobile-client`
- **client_secret**: 无（公共客户端）
- **支持的授权类型**: `authorization_code`, `refresh_token`
- **必须使用PKCE**: 是

### 服务间通信客户端
- **client_id**: `service-client`
- **client_secret**: `ServiceClient@2024#Secure`
- **支持的授权类型**: `client_credentials`

### 内部服务调用客户端
- **client_id**: `client-service`
- **client_secret**: `ClientService@2024#Secure`
- **支持的授权类型**: `client_credentials`
- **作用域**: `internal_api`

## 安全注意事项

1. **刷新令牌保护**: 刷新令牌应安全存储，避免泄露
2. **HTTPS使用**: 生产环境必须使用HTTPS传输
3. **令牌有效期**: 
   - 测试模式：访问令牌365天，刷新令牌30天
   - 生产模式：访问令牌2小时，刷新令牌30天
4. **客户端认证**: 机密客户端必须进行客户端认证
5. **作用域验证**: 新令牌的作用域不会超过原始授权的作用域

## 错误处理

常见错误码：
- `invalid_request`: 请求格式错误
- `invalid_client`: 客户端认证失败
- `invalid_grant`: 刷新令牌无效或过期
- `unsupported_grant_type`: 不支持的授权类型
- `invalid_scope`: 请求的作用域无效

## 测试示例

### 使用curl测试标准端点

```bash
# 使用Basic认证
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $(echo -n 'web-client:WebClient@2024#Secure' | base64)" \
  -d "grant_type=refresh_token&refresh_token=your_refresh_token_here"

# 使用POST参数认证
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token&refresh_token=your_refresh_token_here&client_id=web-client&client_secret=WebClient@2024#Secure"
```

### 使用curl测试简化端点

```bash
curl -X POST http://localhost:8080/auth/refresh-token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "refresh_token=your_refresh_token_here"
```

## 监控和管理

系统提供了令牌管理接口用于监控和管理：

- `GET /oauth2/manage/stats` - 查看令牌统计信息
- `GET /oauth2/manage/authorization/{id}` - 查看授权详情
- `DELETE /oauth2/manage/authorization/{id}` - 撤销指定授权
- `POST /oauth2/manage/cleanup` - 清理过期令牌

这些接口需要管理员权限才能访问。
