# 云商城微服务统一RESTful API设计标准

## 📋 概述

本文档为云商城微服务平台制定统一的RESTful API设计标准，确保所有服务的API接口设计一致、规范、易用。

## 🎯 设计原则

### 1. 资源导向设计
- API围绕**资源(Resource)**设计，而非动作(Action)
- 使用**名词复数形式**表示资源集合
- 避免在URL中使用动词

### 2. 统一路径结构
```
/api/v{version}/{service-prefix}/{resource}
/api/v{version}/{service-prefix}/{resource}/{id}
/api/v{version}/{service-prefix}/{resource}/{id}/{sub-resource}
```

### 3. HTTP方法语义化
- **GET**: 获取资源（幂等、安全）
- **POST**: 创建资源或复杂查询（非幂等）
- **PUT**: 完整更新资源（幂等）
- **PATCH**: 部分更新资源（幂等）
- **DELETE**: 删除资源（幂等）

## 🏗️ 各服务API路径标准

### 认证服务 (auth-service)
```
基础路径: /api/v1/auth

资源设计:
- /api/v1/auth/tokens              # 令牌资源
- /api/v1/auth/sessions            # 会话资源
- /api/v1/auth/users/register      # 用户注册
- /api/v1/auth/users/login         # 用户登录
- /api/v1/auth/oauth2/authorize    # OAuth2授权
- /api/v1/auth/oauth2/token        # OAuth2令牌
```

### 用户服务 (user-service)
```
基础路径: /api/v1/users

资源设计:
- /api/v1/users                    # 用户集合
- /api/v1/users/{id}               # 特定用户
- /api/v1/users/{id}/profile       # 用户档案
- /api/v1/users/{id}/addresses     # 用户地址
- /api/v1/merchants                # 商家集合
- /api/v1/merchants/{id}           # 特定商家
- /api/v1/merchants/{id}/stores    # 商家店铺
```

### 商品服务 (product-service)
```
基础路径: /api/v1/products

资源设计:
- /api/v1/products                 # 商品集合
- /api/v1/products/{id}            # 特定商品
- /api/v1/products/{id}/variants   # 商品规格
- /api/v1/products/{id}/reviews    # 商品评价
- /api/v1/categories               # 分类集合
- /api/v1/categories/{id}          # 特定分类
- /api/v1/categories/{id}/products # 分类下的商品
- /api/v1/brands                   # 品牌集合
- /api/v1/brands/{id}              # 特定品牌
```

### 订单服务 (order-service)
```
基础路径: /api/v1/orders

资源设计:
- /api/v1/orders                   # 订单集合
- /api/v1/orders/{id}              # 特定订单
- /api/v1/orders/{id}/items        # 订单项
- /api/v1/orders/{id}/payments     # 订单支付
- /api/v1/orders/{id}/shipments    # 订单发货
- /api/v1/orders/{id}/refunds      # 订单退款
- /api/v1/carts                    # 购物车集合
- /api/v1/carts/{userId}/items     # 购物车项
```

### 支付服务 (payment-service)
```
基础路径: /api/v1/payments

资源设计:
- /api/v1/payments                 # 支付记录集合
- /api/v1/payments/{id}            # 特定支付记录
- /api/v1/payments/{id}/refunds    # 支付退款
- /api/v1/payment-methods          # 支付方式
- /api/v1/payment-channels         # 支付渠道
- /api/v1/transactions             # 交易记录
- /api/v1/transactions/{id}        # 特定交易
```

### 库存服务 (stock-service)
```
基础路径: /api/v1/stocks

资源设计:
- /api/v1/stocks                   # 库存集合
- /api/v1/stocks/{productId}       # 商品库存
- /api/v1/stocks/reservations      # 库存预留
- /api/v1/stocks/adjustments       # 库存调整
- /api/v1/warehouses               # 仓库集合
- /api/v1/warehouses/{id}          # 特定仓库
- /api/v1/warehouses/{id}/stocks   # 仓库库存
```

### 搜索服务 (search-service)
```
基础路径: /api/v1/search

资源设计:
- /api/v1/search/products          # 商品搜索
- /api/v1/search/suggestions       # 搜索建议
- /api/v1/search/keywords          # 关键词管理
- /api/v1/search/analytics         # 搜索分析
- /api/v1/search/geo-locations     # 地理位置搜索
```

### 日志服务 (log-service)
```
基础路径: /api/v1/logs

资源设计:
- /api/v1/logs                     # 日志集合
- /api/v1/logs/applications        # 应用日志
- /api/v1/logs/operations          # 操作日志
- /api/v1/logs/errors              # 错误日志
- /api/v1/logs/access              # 访问日志
- /api/v1/logs/audit               # 审计日志
```

## 📝 HTTP方法使用规范

### GET方法
```http
# 获取资源集合
GET /api/v1/products?page=1&size=20&sort=price,asc

# 获取特定资源
GET /api/v1/products/123

# 获取子资源
GET /api/v1/products/123/reviews
```

### POST方法
```http
# 创建资源
POST /api/v1/products
Content-Type: application/json

{
  "name": "iPhone 15",
  "price": 5999.00,
  "categoryId": 1
}

# 复杂查询
POST /api/v1/products/search
Content-Type: application/json

{
  "query": "手机",
  "filters": {
    "priceRange": {"min": 1000, "max": 8000}
  }
}
```

### PUT方法
```http
# 完整更新资源
PUT /api/v1/products/123
Content-Type: application/json

{
  "name": "iPhone 15 Pro",
  "price": 7999.00,
  "categoryId": 1,
  "description": "最新款iPhone"
}
```

### PATCH方法
```http
# 部分更新资源
PATCH /api/v1/products/123
Content-Type: application/json

{
  "price": 7499.00
}
```

### DELETE方法
```http
# 删除资源
DELETE /api/v1/products/123
```

## 📊 统一响应格式

### 成功响应
```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {
    // 实际数据
  },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### 分页响应
```json
{
  "success": true,
  "code": 200,
  "message": "查询成功",
  "data": {
    "content": [...],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100,
      "totalPages": 5,
      "hasNext": true,
      "hasPrevious": false
    }
  },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### 错误响应
```json
{
  "success": false,
  "code": 400,
  "message": "请求参数错误",
  "errors": [
    {
      "field": "price",
      "message": "价格必须大于0"
    }
  ],
  "timestamp": "2025-01-15T10:30:00Z"
}
```

## 🔧 查询参数规范

### 分页参数
- `page`: 页码（从1开始）
- `size`: 每页数量（默认20，最大100）
- `sort`: 排序字段和方向，格式：`field,direction`

### 过滤参数
- `q`: 搜索关键词
- `filter`: 过滤条件
- `startTime`: 开始时间
- `endTime`: 结束时间

### 示例
```
GET /api/v1/products?page=1&size=20&sort=price,asc&q=手机&filter=categoryId:1
```

## 🏷️ 状态码规范

### 2xx 成功
- `200 OK`: 请求成功
- `201 Created`: 资源创建成功
- `204 No Content`: 请求成功但无返回内容

### 4xx 客户端错误
- `400 Bad Request`: 请求参数错误
- `401 Unauthorized`: 未认证
- `403 Forbidden`: 无权限
- `404 Not Found`: 资源不存在
- `409 Conflict`: 资源冲突
- `422 Unprocessable Entity`: 请求格式正确但语义错误

### 5xx 服务器错误
- `500 Internal Server Error`: 服务器内部错误
- `502 Bad Gateway`: 网关错误
- `503 Service Unavailable`: 服务不可用

## 🔒 安全规范

### 认证授权
- 使用OAuth 2.1标准
- JWT Token传递：`Authorization: Bearer {token}`
- 权限控制粒度到资源级别

### 参数验证
- 严格验证输入参数
- 防止SQL注入和XSS攻击
- 参数长度和格式限制

## 📚 文档规范

### OpenAPI规范
- 使用OpenAPI 3.0标准
- 详细的参数描述和示例
- 完整的错误码说明

### 版本控制
- URL路径版本控制：`/api/v1/`
- 向后兼容策略
- 废弃通知机制

## ✅ 实施检查清单

### API设计检查
- [ ] URL路径使用名词而非动词
- [ ] 资源名称使用复数形式
- [ ] HTTP方法语义正确
- [ ] 状态码使用恰当
- [ ] 查询参数命名规范

### 响应格式检查
- [ ] 统一的响应结构
- [ ] 错误信息详细且有用
- [ ] 分页信息完整
- [ ] 时间戳格式统一

### 安全检查
- [ ] 认证机制完善
- [ ] 参数验证严格
- [ ] 权限控制到位
- [ ] 限流策略合理

---

**更新时间**: 2025/9/24  
**版本**: v1.0.0  
**适用范围**: 云商城微服务平台所有服务
