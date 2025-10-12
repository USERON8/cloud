# API文档使用指南

## 概述

本文档为开发者和API使用者提供了完整的API文档访问和使用指南。

## 技术架构

### 文档技术栈

- **OpenAPI 3.0.2** - API规范标准 (swagger.version: 2.2.34)
- **Knife4j 4.5.0** - 增强的Swagger UI界面
- **SpringDoc OpenAPI** - Spring Boot 3.x集成

### 文档架构

```
┌─────────────────────────────────────────────────────────┐
│                     Gateway (Port 80)                     │
│  ┌─────────────────────────────────────────────────────┐ │
│  │           Knife4j Gateway Aggregation               │ │
│  │         http://localhost:80/doc.html               │ │
│  └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                           │
                           │ Service Discovery
            ┌────────────────┼────────────────┐
            │                │                │
    ┌───────▼────────┐ ┌───────▼────────┐ ┌───────▼────────┐
    │  User Service │ │  Auth Service │ │Product Service│
    │   Port: 8081   │ │   Port: 8081   │ │   Port: 8083   │
    │/doc.html       │ │/doc.html       │ │/doc.html       │
    └────────────────┘ └────────────────┘ └────────────────┘
```

## 访问方式

### 1. Gateway聚合文档（推荐）

**访问地址**: http://localhost:80/doc.html

**特点**:
- 所有服务API统一展示
- 支持服务间切换
- 统一的认证入口

### 2. 独立服务文档

| 服务名称 | 端口 | 文档地址 | OpenAPI规范 |
|---------|------|----------|-------------|
| Gateway | 80 | http://localhost:80/doc.html | http://localhost:80/v3/api-docs |
| Auth Service | 8081 | http://localhost:8081/doc.html | http://localhost:8081/v3/api-docs |
| User Service | 8081 | http://localhost:8081/doc.html | http://localhost:8081/v3/api-docs |
| Product Service | 8083 | http://localhost:8083/doc.html | http://localhost:8083/v3/api-docs |
| Order Service | 8082 | http://localhost:8082/doc.html | http://localhost:8082/v3/api-docs |
| Stock Service | 8084 | http://localhost:8084/doc.html | http://localhost:8084/v3/api-docs |
| Payment Service | 8085 | http://localhost:8085/doc.html | http://localhost:8085/v3/api-docs |
| Search Service | 8087 | http://localhost:8087/doc.html | http://localhost:8087/v3/api-docs |

## 文档功能特性

### 1. 接口浏览

- **分组展示**: 按业务模块分组API接口
- **搜索功能**: 支持接口名称和描述搜索
- **过滤功能**: 按HTTP方法、标签等过滤

### 2. 在线测试

- **参数填充**: 自动填充示例参数
- **认证测试**: 支持Bearer Token认证
- **响应查看**: 实时查看API响应结果

### 3. 文档导出

- **OpenAPI JSON**: 获取标准OpenAPI规范
- **Markdown文档**: 导出接口文档为Markdown格式
- **Postman Collection**: 导出Postman测试集合

## 认证流程

### 1. 获取访问令牌

**用户注册并登录**:
```bash
curl -X POST "http://localhost:8081/auth/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com",
    "userType": "USER"
  }'
```

**用户登录**:
```bash
curl -X POST "http://localhost:8081/auth/sessions" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "userType": "USER"
  }'
```

### 2. 使用令牌访问API

```bash
curl -X GET "http://localhost:80/api/v1/users/1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## API使用指南

### 1. 认证服务API

#### 用户注册
```http
POST /auth/users/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "phone": "13800138000",
  "nickname": "新用户",
  "userType": "USER"
}
```

#### 用户登录
```http
POST /auth/sessions
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123",
  "userType": "USER"
}
```

#### 令牌刷新
```http
POST /auth/tokens/refresh
Content-Type: application/x-www-form-urlencoded

refresh_token=YOUR_REFRESH_TOKEN
```

#### 用户登出
```http
DELETE /auth/sessions
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### 2. 用户服务API

#### 获取用户信息
```http
GET /api/v1/users/{id}
Authorization: Bearer YOUR_ACCESS_TOKEN
```

#### 更新用户信息
```http
PUT /api/v1/users/{id}
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "nickname": "新昵称",
  "email": "new@example.com"
}
```

#### 获取用户地址列表
```http
GET /api/v1/users/{userId}/addresses
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### 3. 商品服务API

#### 获取商品列表
```http
GET /api/v1/products?page=1&size=20&status=1
Authorization: Bearer YOUR_ACCESS_TOKEN
```

#### 获取商品详情
```http
GET /api/v1/products/{id}
Authorization: Bearer YOUR_ACCESS_TOKEN
```

#### 创建商品
```http
POST /api/v1/products
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "name": "测试商品",
  "price": 99.99,
  "categoryId": 1,
  "description": "商品描述"
}
```

### 4. 订单服务API

#### 创建订单
```http
POST /api/v1/orders
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "price": 99.99
    }
  ]
}
```

#### 获取订单详情
```http
GET /api/v1/orders/{orderId}
Authorization: Bearer YOUR_ACCESS_TOKEN
```

### 5. 支付服务API

#### 创建支付订单
```http
POST /api/v1/payments
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "orderId": 1,
  "amount": 199.98,
  "paymentMethod": "ALIPAY"
}
```

#### 查询支付状态
```http
GET /api/v1/payments/{paymentId}
Authorization: Bearer YOUR_ACCESS_TOKEN
```

## 错误处理

### 标准错误响应格式

```json
{
  "code": 1001,
  "message": "用户不存在",
  "data": null,
  "timestamp": 1704067200000
}
```

### 常见错误码

| 错误码 | 说明 | HTTP状态码 |
|-------|------|------------|
| 1001 | 用户不存在 | 404 |
| 1002 | 用户已存在 | 400 |
| 1003 | 密码错误 | 401 |
| 1004 | 账户已禁用 | 403 |
| 1005 | 令牌无效 | 401 |
| 1006 | 参数验证失败 | 400 |
| 9999 | 系统错误 | 500 |

## 开发指南

### 1. 本地开发环境

#### 启动服务
```bash
# 1. 启动基础设施服务
cd docker && docker-compose up -d

# 2. 启动认证服务
cd auth-service && mvn spring-boot:run

# 3. 启动网关
cd gateway && mvn spring-boot:run

# 4. 启动业务服务
cd user-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
# ... 其他服务
```

#### 访问文档
- 启动完成后访问: http://localhost:80/doc.html
- 选择对应服务查看API文档

### 2. 测试环境

#### 环境配置
```yaml
# application-dev.yml
spring:
  profiles:
    active: dev
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yml
```

#### 测试账号
```json
{
  "admin": {
    "username": "admin",
    "password": "admin123"
  },
  "user": {
    "username": "testuser",
    "password": "password123"
  }
}
```

### 3. 生产环境

#### 安全配置
- 所有API都需要OAuth2.1认证
- 支持HTTPS协议
- 实施请求频率限制

#### 性能优化
- 启用API响应缓存
- 使用CDN加速静态资源
- 配置负载均衡

## 监控和维护

### 1. 文档监控

#### 文档可访问性检查
```bash
# 检查Gateway文档
curl -f http://localhost:80/doc.html

# 检查各服务文档
for port in 8081 8082 8083 8084 8085 8087; do
  curl -f http://localhost:$port/doc.html || echo "Service on port $port is down"
done
```

#### OpenAPI规范验证
```bash
# 验证OpenAPI规范
curl http://localhost:8081/v3/api-docs | jq .
```

### 2. API健康检查

#### 服务健康状态
```bash
# 检查各服务健康状态
for service in user-service product-service order-service; do
  curl -f http://localhost:8080/$service/actuator/health || echo "$service is unhealthy"
done
```

### 3. 文档更新流程

#### API变更时
1. 更新接口注解
2. 重新部署服务
3. 验证文档准确性
4. 通知相关开发人员

#### 版本发布时
1. 更新API版本号
2. 生成版本文档
3. 更新使用指南
4. 归档旧版本

## 常见问题

### 1. 文档无法访问

**问题**: 访问doc.html返回404
**解决方案**:
```bash
# 检查服务是否启动
curl http://localhost:8081/actuator/health

# 检查Swagger依赖
mvn dependency:tree | grep swagger

# 检查配置文件
grep -r "springdoc" src/main/resources/
```

### 2. 接口测试失败

**问题**: 在线测试接口返回401
**解决方案**:
1. 确保已获取有效的访问令牌
2. 在请求头中正确设置Authorization
3. 检查令牌是否已过期

### 3. 文档显示异常

**问题**: 接口参数或响应显示不正确
**解决方案**:
1. 检查@Schema注解配置
2. 验证DTO/VO类的字段注解
3. 重新构建项目

## 最佳实践

### 1. API设计

- 遵循RESTful设计原则
- 使用语义化的HTTP状态码
- 提供清晰的错误信息
- 支持分页和过滤

### 2. 文档维护

- 及时更新API文档
- 提供完整的请求/响应示例
- 添加详细的参数说明
- 定期检查文档准确性

### 3. 安全考虑

- 使用HTTPS协议
- 实施API访问控制
- 定期轮换访问密钥
- 监控API访问日志

## 总结

本文档提供了完整的API文档使用指南，包括：

1. **文档架构** - 统一的文档聚合和访问方式
2. **使用指南** - 详细的API调用示例
3. **开发指南** - 本地开发和测试环境配置
4. **监控维护** - 文档质量保证和更新流程

遵循这些指南可以确保API文档的准确性和可用性，为开发团队和API使用者提供良好的体验。