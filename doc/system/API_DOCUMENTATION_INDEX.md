# 微服务API文档总览

## 文档列表

本项目包含以下8个详细的API文档:

### 1. [API_DOC_AUTH_SERVICE.md](./API_DOC_AUTH_SERVICE.md)
**认证服务API文档**
- OAuth2.1标准认证
- 用户注册/登录/登出
- Token管理与黑名单
- GitHub OAuth2集成
- **端口**: 8081
- **路由前缀**: `/auth`, `/oauth2`

### 2. [API_DOC_USER_SERVICE.md](./API_DOC_USER_SERVICE.md)
**用户服务API文档**
- 用户管理(查询/创建/更新/删除)
- 用户地址管理
- 用户统计信息
- 商家管理与认证
- 管理员功能
- **端口**: 8081
- **路由前缀**: `/users`, `/merchant`, `/admin`

### 3. [API_DOC_ORDER_SERVICE.md](./API_DOC_ORDER_SERVICE.md)
**订单服务API文档**
- 订单管理(查询/创建/支付/取消)
- 订单状态流转
- 退款申请与审核
- 订单批量操作
- **端口**: 8082
- **路由前缀**: `/order`

### 4. [API_DOC_PRODUCT_SERVICE.md](./API_DOC_PRODUCT_SERVICE.md)
**商品服务API文档**
- 商品管理(查询/创建/更新/删除)
- 分类管理(树形结构)
- 品牌管理
- 批量操作(最多100个)
- **端口**: 8083
- **路由前缀**: `/product`, `/category`

### 5. [API_DOC_STOCK_SERVICE.md](./API_DOC_STOCK_SERVICE.md)
**库存服务API文档**
- 库存管理(入库/出库/预留/释放)
- 秒杀场景(公平锁)
- 分布式锁保护
- 批量操作
- **端口**: 8084
- **路由前缀**: `/stock`, `/stocks`

### 6. [API_DOC_PAYMENT_SERVICE.md](./API_DOC_PAYMENT_SERVICE.md)
**支付服务API文档**
- 支付管理(创建/查询/退款)
- 支付宝集成(PC/Mobile)
- 支付风控检查
- 异步通知处理
- **端口**: 8085
- **路由前缀**: `/payment`, `/payments`

### 7. [API_DOC_SEARCH_SERVICE.md](./API_DOC_SEARCH_SERVICE.md)
**搜索服务API文档**
- 商品搜索(关键词/复杂/智能)
- 店铺搜索
- 搜索建议与热门关键词
- Elasticsearch集成
- **端口**: 8087
- **路由前缀**: `/search`

### 8. [API_DOC_GATEWAY.md](./API_DOC_GATEWAY.md)
**网关路由API文档**
- 所有服务的路由规则
- JWT Token验证流程
- 跨域配置(CORS)
- 限流熔断策略
- API文档聚合
- **端口**: 80

---

## 快速开始

### 1. 获取访问令牌

使用OAuth2密码模式获取Token:

```bash
curl -X POST "http://localhost:8081/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=admin&password=admin123&client_id=web-client&client_secret=WebClient@2024#Secure&scope=read write"
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

### 2. 使用Token访问API

在请求头中携带Token:

```bash
curl -X GET "http://localhost:80/users/1" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIs..."
```

---

## 服务端口总览

| 服务名称 | 端口 | Gateway路由前缀 | 说明 |
|---------|------|----------------|------|
| Gateway | 80 | - | 统一网关入口 |
| Auth Service | 8081 | `/auth`, `/oauth2` | 认证授权服务 |
| User Service | 8081 | `/users`, `/merchant` | 用户管理服务 |
| Order Service | 8082 | `/order` | 订单管理服务 |
| Product Service | 8083 | `/product`, `/category` | 商品管理服务 |
| Stock Service | 8084 | `/stock`, `/stocks` | 库存管理服务 |
| Payment Service | 8085 | `/payment` | 支付管理服务 |
| Search Service | 8087 | `/search` | 搜索服务 |

---

## Feign内部接口

各服务之间的内部调用接口统一使用`/internal`前缀:

| 服务 | 内部接口路径 | 说明 |
|------|-------------|------|
| User Service | `/internal/users/**` | 用户查询内部接口 |
| Product Service | `/internal/product/**` | 商品查询内部接口 |
| Order Service | `/internal/order/**` | 订单操作内部接口 |
| Stock Service | `/internal/stock/**` | 库存操作内部接口 |
| Payment Service | `/internal/payment/**` | 支付操作内部接口 |

**注意**: Feign内部接口不需要身份验证,仅供微服务间调用。

---

## API文档访问

### Knife4j聚合文档

访问Gateway聚合的API文档:
- **地址**: http://localhost:80/doc.html
- **说明**: 包含所有微服务的API文档

### 各服务独立文档

| 服务 | 文档地址 |
|------|---------|
| Auth Service | http://localhost:8081/doc.html |
| User Service | http://localhost:8081/doc.html |
| Order Service | http://localhost:8082/doc.html |
| Product Service | http://localhost:8083/doc.html |
| Stock Service | http://localhost:8084/doc.html |
| Payment Service | http://localhost:8085/doc.html |
| Search Service | http://localhost:8087/doc.html |

---

## 通用响应格式

所有API统一使用`Result<T>`包装响应:

### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {...},
  "timestamp": 1704067200000
}
```

### 错误响应
```json
{
  "code": 4001,
  "message": "商品不存在",
  "data": null,
  "timestamp": 1704067200000
}
```

### 分页响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [...],
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}
```

---

## 错误码范围

| 服务 | 错误码范围 | 说明 |
|------|----------|------|
| Auth Service | 1000-1999 | 认证相关错误 |
| User Service | 2000-2999 | 用户相关错误 |
| Order Service | 3000-3999 | 订单相关错误 |
| Product Service | 4000-4999 | 商品相关错误 |
| Stock Service | 5000-5999 | 库存相关错误 |
| Payment Service | 6000-6999 | 支付相关错误 |
| Search Service | 7000-7999 | 搜索相关错误 |
| Gateway | 9000-9999 | 网关相关错误 |

---

## 典型业务流程

### 1. 用户注册并下单流程

```
1. 用户注册
   POST /auth/users/register
   ↓
2. 获取Token (注册成功自动返回)
   ↓
3. 搜索商品
   GET /api/search/search?keyword=手机
   ↓
4. 查看商品详情
   GET /product/1
   ↓
5. 检查库存
   GET /stocks/product/1
   ↓
6. 创建订单
   POST /order/api/v1/orders
   ↓
7. 支付订单
   POST /api/v1/payment/alipay/create
   ↓
8. 支付成功回调
   POST /api/v1/payment/alipay/notify
   ↓
9. 订单状态更新为已支付
   自动触发库存扣减
```

### 2. 商家入驻并上架商品流程

```
1. 商家认证申请
   POST /merchant/auth/apply
   ↓
2. 管理员审核
   POST /merchant/auth/{authId}/review
   ↓
3. 商家登录
   POST /auth/sessions
   ↓
4. 创建商品
   POST /product
   ↓
5. 创建库存记录
   POST /stocks
   ↓
6. 商品上架
   PATCH /product/{id}/status?status=1
```

---

## 权限控制

### 角色类型
- **USER**: 普通用户
- **MERCHANT**: 商家
- **ADMIN**: 管理员

### 权限范围(Scope)
- `read`: 读取权限
- `write`: 写入权限
- `product:read`: 商品读取权限
- `product:write`: 商品写入权限
- `admin:read`: 管理员读取权限
- `admin:write`: 管理员写入权限

### 权限注解示例
```java
@PreAuthorize("hasRole('ADMIN')")  // 需要ADMIN角色

@PreAuthorize("hasAuthority('SCOPE_read')")  // 需要read权限

@PreAuthorize("hasRole('MERCHANT') or hasRole('ADMIN')")  // 需要MERCHANT或ADMIN角色
```

---

## 性能优化建议

1. **缓存策略**:
   - 商品信息缓存(Redis + Caffeine)
   - 用户信息缓存
   - 分类树缓存

2. **数据库优化**:
   - 合理使用索引
   - 分页查询避免深度分页
   - 批量操作限制数量(最多100个)

3. **分布式锁**:
   - 库存操作使用分布式锁
   - 支付操作使用分布式锁
   - 秒杀场景使用公平锁

4. **异步处理**:
   - 订单状态变更异步通知
   - 日志记录异步处理
   - 搜索索引异步更新

---

## 安全建议

1. **Token安全**:
   - Token有效期不超过2小时
   - 使用Refresh Token刷新
   - 支持Token黑名单机制

2. **HTTPS**:
   - 生产环境必须使用HTTPS
   - 敏感信息传输加密

3. **API限流**:
   - Gateway层面限流
   - 单用户限流
   - IP限流

4. **输入验证**:
   - 所有输入参数验证
   - SQL注入防护
   - XSS攻击防护

---

## 监控与运维

### Actuator端点

所有服务都提供Actuator监控端点:
- `/actuator/health` - 健康检查
- `/actuator/metrics` - 指标监控
- `/actuator/info` - 服务信息

### Nacos控制台

- **地址**: http://localhost:8848/nacos
- **用户名**: nacos
- **密码**: nacos

### 日志查看

日志文件位置: `[service-name]/logs/`
- `app.log` - 应用日志
- `error.log` - 错误日志

---

## 版本信息

- **Spring Boot**: 3.5.3
- **Spring Cloud**: 2025.0.0
- **Spring Cloud Alibaba**: 2025.0.0.0-preview
- **Nacos**: 2.4.0
- **MySQL**: 9.3.0
- **Redis**: 7.x
- **Elasticsearch**: 8.x
- **RocketMQ**: 5.x

---

## 文档更新日志

- **2025-01-15**: 初始版本,包含8个微服务的完整API文档

---

## 联系方式

如有问题或建议,请联系开发团队或提交Issue。
