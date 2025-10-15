# Gateway API Route Documentation

## 网关概述

Spring Cloud Gateway作为整个微服务系统的统一入口,负责请求路由、认证授权、限流熔断、API聚合文档等功能。

**网关端口**: 80
**网关地址**: http://localhost:80
**API文档**: http://localhost:80/doc.html

---

## 网关架构

```
Client Request
    ↓
Gateway (Port 80)
    ↓
├─→ Auth Service (8081)      - 认证授权
├─→ User Service (8081)       - 用户管理
├─→ Product Service (8083)    - 商品管理
├─→ Order Service (8082)      - 订单管理
├─→ Stock Service (8084)      - 库存管理
├─→ Payment Service (8085)    - 支付管理
├─→ Search Service (8087)     - 搜索服务
└─→ Log Service (8086)        - 日志服务
```

---

## 路由规则总览

### 1. 认证服务路由 (Priority: 10)

| 路由ID | 路径匹配 | 目标服务 | 说明 |
|--------|---------|---------|------|
| oauth2-endpoints | `/oauth2/**` | auth-service | OAuth2.1标准端点 |
| oidc-endpoints | `/.well-known/**`, `/connect/**`, `/userinfo` | auth-service | OIDC和Well-Known端点 |
| auth-service-api | `/auth/**` | auth-service | 认证服务API |

**重要端点**:
- `POST /oauth2/token` - 获取访问令牌
- `GET /.well-known/jwks.json` - JWKS公钥端点
- `POST /auth/sessions` - 用户登录
- `POST /auth/users/register` - 用户注册
- `DELETE /auth/sessions` - 用户登出

---

### 2. 用户服务路由 (Priority: 20)

| 路由ID | 路径匹配 | 目标服务 | 说明 |
|--------|---------|---------|------|
| user-management | `/users/**` | user-service | 用户管理 |
| merchant-management | `/merchant/**` | user-service | 商家管理 |
| admin-management | `/admin/**` | user-service | 管理后台 |

**主要API**:
- `GET /users` - 查询用户列表
- `GET /users/{id}` - 查询用户详情
- `POST /users` - 创建用户
- `GET /users/{userId}/addresses` - 查询用户地址
- `GET /merchant/profile` - 查询商家信息

---

### 3. 商品服务路由 (Priority: 20)

| 路由ID | 路径匹配 | 目标服务 | 说明 |
|--------|---------|---------|------|
| product-management | `/product/**` | product-service | 商品管理 |
| category-management | `/category/**` | product-service | 分类管理 |
| brand-management | `/brand/**` | product-service | 品牌管理 |

**主要API**:
- `GET /product` - 查询商品列表
- `GET /product/{id}` - 查询商品详情
- `POST /product` - 创建商品
- `GET /category/tree` - 查询分类树
- `GET /product/category/{categoryId}` - 查询分类商品

---

### 4. 订单服务路由 (Priority: 20)

| 路由ID | 路径匹配 | 目标服务 | 说明 |
|--------|---------|---------|------|
| order-management | `/order/**` | order-service | 订单管理 |
| cart-management | `/cart/**` | order-service | 购物车管理 |

**主要API**:
- `GET /order/api/v1/orders` - 查询订单列表
- `POST /order/api/v1/orders` - 创建订单
- `POST /order/api/v1/orders/{orderId}/pay` - 支付订单
- `POST /order/api/v1/refund/create` - 创建退款申请
- `GET /order/api/v1/orders/my-orders` - 查询我的订单

---

### 5. 库存服务路由 (Priority: 20)

| 路由ID | 路径匹配 | 目标服务 | 说明 |
|--------|---------|---------|------|
| stock-management | `/stock/**` | stock-service | 库存管理 |

**主要API**:
- `GET /stocks/product/{productId}` - 查询商品库存
- `POST /stocks/stock-in` - 库存入库
- `POST /stocks/stock-out` - 库存出库
- `POST /stocks/reserve` - 预留库存
- `POST /stocks/seckill/{productId}` - 秒杀扣减

---

### 6. 支付服务路由 (Priority: 20)

| 路由ID | 路径匹配 | 目标服务 | 说明 |
|--------|---------|---------|------|
| payment-management | `/payment/**` | payment-service | 支付管理 |

**主要API**:
- `GET /payments` - 查询支付列表
- `POST /api/v1/payment/alipay/create` - 创建支付宝支付
- `POST /api/v1/payment/alipay/notify` - 支付宝异步通知
- `POST /payments/{id}/refund` - 支付退款
- `POST /payments/risk-check` - 支付风控检查

---

### 7. 搜索服务路由 (Priority: 20)

| 路由ID | 路径匹配 | 目标服务 | 说明 |
|--------|---------|---------|------|
| search-api | `/search/**` | search-service | 搜索API |

**主要API**:
- `GET /api/search/search` - 关键词搜索商品
- `POST /api/search/complex-search` - 复杂商品搜索
- `GET /api/search/suggestions` - 获取搜索建议
- `GET /api/search/hot-keywords` - 获取热门搜索
- `POST /api/search/shops/complex-search` - 店铺搜索

---

### 8. 日志服务路由 (Priority: 20)

| 路由ID | 路径匹配 | 目标服务 | 说明 |
|--------|---------|---------|------|
| log-api | `/log/**` | log-service | 日志API |

---

### 9. API文档路由 (Priority: 50)

| 路由ID | 路径匹配 | 目标服务 | 说明 |
|--------|---------|---------|------|
| auth-service-doc | `/auth-service/doc.html`, `/auth-service/v3/api-docs/**` | auth-service | 认证服务文档 |
| user-service-doc | `/user-service/doc.html`, `/user-service/v3/api-docs/**` | user-service | 用户服务文档 |
| product-service-doc | `/product-service/doc.html`, `/product-service/v3/api-docs/**` | product-service | 商品服务文档 |
| order-service-doc | `/order-service/doc.html`, `/order-service/v3/api-docs/**` | order-service | 订单服务文档 |
| payment-service-doc | `/payment-service/doc.html`, `/payment-service/v3/api-docs/**` | payment-service | 支付服务文档 |
| stock-service-doc | `/stock-service/doc.html`, `/stock-service/v3/api-docs/**` | stock-service | 库存服务文档 |
| search-service-doc | `/search-service/doc.html`, `/search-service/v3/api-docs/**` | search-service | 搜索服务文档 |
| log-service-doc | `/log-service/doc.html`, `/log-service/v3/api-docs/**` | log-service | 日志服务文档 |

**Knife4j聚合文档**: http://localhost:80/doc.html

---

## 网关功能特性

### 1. 认证授权

- **JWT Token验证**: 所有受保护的API需要在请求头中携带`Authorization: Bearer {token}`
- **JWKS验证**: Gateway从auth-service获取公钥验证JWT签名
- **权限检查**: 支持基于角色(ROLE)和作用域(SCOPE)的权限控制

### 2. 跨域配置 (CORS)

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
```

### 3. 限流熔断

- **限流**: 使用Spring Cloud Gateway限流过滤器
- **熔断**: 集成Resilience4j实现熔断保护
- **降级**: 提供统一的降级响应

### 4. 负载均衡

- **服务发现**: 通过Nacos自动发现后端服务
- **负载均衡**: 使用`lb://service-name`实现负载均衡

### 5. 请求日志

- 记录所有请求的URL、方法、响应时间
- 记录异常请求的详细信息

---

## 网关路由优先级

路由优先级(order)越小,优先级越高:

1. **Priority 10**: 认证服务路由 (最高优先级)
   - 确保OAuth2端点优先匹配
   - 避免被其他路由拦截

2. **Priority 20**: 业务服务路由
   - 用户服务、商品服务、订单服务等
   - 按路径前缀匹配

3. **Priority 50**: API文档路由 (最低优先级)
   - Knife4j文档资源
   - 避免与业务路由冲突

---

## 请求流程

### 1. 带Token的请求流程

```
1. Client → Gateway
   Request: GET /users/1
   Header: Authorization: Bearer eyJhbGc...

2. Gateway验证Token
   - 从Header提取Token
   - 使用JWKS验证签名
   - 检查Token是否过期
   - 检查Token是否在黑名单

3. Gateway路由请求
   - 匹配路由规则: /users/** → user-service
   - 通过Nacos发现user-service实例
   - 使用负载均衡选择实例

4. Gateway → User Service
   Request: GET /users/1
   Header: Authorization: Bearer eyJhbGc...

5. User Service → Gateway → Client
   Response: 200 OK
   Body: {...}
```

### 2. 获取Token的流程

```
1. Client → Gateway
   Request: POST /oauth2/token
   Body: grant_type=password&username=admin&password=xxx

2. Gateway路由请求
   - 匹配路由: /oauth2/** → auth-service
   - 直接转发,不验证Token

3. Auth Service验证并生成Token
   - 验证用户名密码
   - 生成JWT Token
   - 返回Token

4. Client收到Token
   {
     "access_token": "eyJhbGc...",
     "token_type": "Bearer",
     "expires_in": 7200
   }
```

---

## 完整的API请求示例

### 1. 用户注册并登录

```bash
# 1. 注册新用户
curl -X POST "http://localhost:80/auth/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
  }'

# 响应包含access_token
{
  "code": 201,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "userInfo": {...}
  }
}
```

### 2. 使用Token查询用户信息

```bash
# 使用返回的access_token查询用户信息
curl -X GET "http://localhost:80/users/1" \
  -H "Authorization: Bearer eyJhbGc..."

# 响应
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com"
  }
}
```

### 3. 创建订单

```bash
curl -X POST "http://localhost:80/order/api/v1/orders" \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2,
    "addressId": 1
  }'
```

### 4. 支付订单

```bash
curl -X POST "http://localhost:80/order/api/v1/orders/1/pay" \
  -H "Authorization: Bearer eyJhbGc..."
```

### 5. 搜索商品

```bash
curl -X GET "http://localhost:80/api/search/search?keyword=手机&page=0&size=20"
```

---

## 网关配置说明

### Nacos配置

**配置文件**: `application.yml`
```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: public
        group: DEFAULT_GROUP
    gateway:
      discovery:
        locator:
          enabled: false  # 禁用自动服务发现
          lower-case-service-id: true
```

### 路由配置

**配置文件**: `application-route.yml`
- 定义所有微服务的路由规则
- 设置路由优先级
- 配置路由过滤器

---

## 监控与运维

### 1. Actuator端点

- **健康检查**: `GET /actuator/health`
- **路由信息**: `GET /actuator/gateway/routes`
- **网关指标**: `GET /actuator/metrics`

### 2. 路由刷新

```bash
# 刷新路由配置
curl -X POST "http://localhost:80/actuator/gateway/refresh"
```

### 3. 查看所有路由

```bash
curl -X GET "http://localhost:80/actuator/gateway/routes"
```

---

## 常见问题

### 1. Token验证失败

**问题**: 401 Unauthorized
**原因**:
- Token已过期
- Token签名验证失败
- Token在黑名单中
- 缺少Authorization Header

**解决**:
- 使用refresh_token刷新Token
- 重新登录获取新Token
- 检查Header格式: `Authorization: Bearer {token}`

### 2. 路由404

**问题**: 404 Not Found
**原因**:
- 路由配置错误
- 目标服务未启动
- Nacos服务发现失败

**解决**:
- 检查路由配置是否正确
- 确认目标服务已在Nacos注册
- 查看Gateway日志

### 3. 跨域问题

**问题**: CORS error
**原因**: 跨域配置不正确

**解决**:
- 检查Gateway的CORS配置
- 确认allowedOrigins包含前端域名

---

## 安全建议

1. **生产环境**:
   - 启用HTTPS
   - 配置IP白名单
   - 启用请求签名验证
   - 配置合理的限流策略

2. **Token管理**:
   - 设置合理的Token过期时间
   - 实现Token刷新机制
   - 支持Token黑名单

3. **API安全**:
   - 敏感操作需要二次验证
   - 关键接口启用CSRF保护
   - 记录所有敏感操作日志

---

## 性能优化

1. **连接池配置**: 优化到后端服务的连接池
2. **缓存策略**: 缓存路由配置、JWKS公钥
3. **负载均衡**: 配置合理的负载均衡策略
4. **超时设置**: 设置合理的超时时间

---

## 版本信息

- **Gateway版本**: Spring Cloud Gateway 4.2.x
- **Spring Cloud版本**: 2025.0.0
- **负载均衡**: Spring Cloud LoadBalancer
- **服务发现**: Nacos 2.4.0

**文档更新**: 2025-01-15
