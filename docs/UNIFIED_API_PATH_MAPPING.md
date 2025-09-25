# 云商城微服务统一API路径映射

## 📋 概述

本文档定义了云商城微服务平台的统一API路径映射规则，实现网关层统一版本控制，各服务内部使用简洁路径。

## 🎯 设计原则

### 网关层统一版本控制
- 外部访问：`/api/v1/{service-resource}/{path}`
- 内部路径：`/{resource}/{path}`
- 网关负责路径重写和版本管理

### 路径重写规则
```
外部路径 → 内部路径
/api/v1/auth/sessions → /auth/sessions
/api/v1/users/123 → /users/123
/api/v1/products/search → /products/search
```

## 🗺️ 服务路径映射表

### 认证服务 (auth-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `POST /api/v1/auth/users/register` | `/auth/users/register` | POST | 用户注册 |
| `POST /api/v1/auth/sessions` | `/auth/sessions` | POST | 用户登录 |
| `DELETE /api/v1/auth/sessions` | `/auth/sessions` | DELETE | 用户登出 |
| `DELETE /api/v1/auth/users/{username}/sessions` | `/auth/users/{username}/sessions` | DELETE | 批量登出 |
| `GET /api/v1/auth/tokens/validate` | `/auth/tokens/validate` | GET | 验证令牌 |
| `POST /api/v1/auth/tokens/refresh` | `/auth/tokens/refresh` | POST | 刷新令牌 |
| `GET /api/v1/auth/tokens/stats` | `/auth/tokens/stats` | GET | 令牌统计 |
| `DELETE /api/v1/auth/tokens/{id}` | `/auth/tokens/{id}` | DELETE | 撤销令牌 |

**OAuth2.1标准端点（直接映射）：**
- `/oauth2/authorize` → `/oauth2/authorize`
- `/oauth2/token` → `/oauth2/token`
- `/oauth2/revoke` → `/oauth2/revoke`
- `/.well-known/jwks.json` → `/.well-known/jwks.json`

### 用户服务 (user-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/users` | `/users` | GET | 获取用户列表 |
| `GET /api/v1/users/search` | `/users/search` | GET | 搜索用户 |
| `GET /api/v1/users/{id}` | `/users/{id}` | GET | 获取用户详情 |
| `POST /api/v1/users` | `/users` | POST | 创建用户 |
| `PUT /api/v1/users/{id}` | `/users/{id}` | PUT | 更新用户 |
| `PATCH /api/v1/users/{id}` | `/users/{id}` | PATCH | 部分更新用户 |
| `DELETE /api/v1/users/{id}` | `/users/{id}` | DELETE | 删除用户 |
| `GET /api/v1/users/{id}/profile` | `/users/{id}/profile` | GET | 获取用户档案 |
| `PUT /api/v1/users/{id}/profile` | `/users/{id}/profile` | PUT | 更新用户档案 |
| `GET /api/v1/users/{id}/addresses` | `/users/{id}/addresses` | GET | 获取用户地址 |
| `POST /api/v1/users/{id}/addresses` | `/users/{id}/addresses` | POST | 添加用户地址 |
| `PATCH /api/v1/users/{id}/status` | `/users/{id}/status` | PATCH | 更新用户状态 |

### 商家服务 (user-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/merchants` | `/merchants` | GET | 获取商家列表 |
| `GET /api/v1/merchants/{id}` | `/merchants/{id}` | GET | 获取商家详情 |
| `POST /api/v1/merchants` | `/merchants` | POST | 创建商家 |
| `PUT /api/v1/merchants/{id}` | `/merchants/{id}` | PUT | 更新商家信息 |
| `PATCH /api/v1/merchants/{id}/approve` | `/merchants/{id}/approve` | PATCH | 审核通过商家 |
| `PATCH /api/v1/merchants/{id}/reject` | `/merchants/{id}/reject` | PATCH | 审核拒绝商家 |
| `GET /api/v1/merchants/{id}/stores` | `/merchants/{id}/stores` | GET | 获取商家店铺 |
| `POST /api/v1/merchants/{id}/stores` | `/merchants/{id}/stores` | POST | 创建店铺 |

### 商品服务 (product-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/products` | `/products` | GET | 获取商品列表 |
| `GET /api/v1/products/{id}` | `/products/{id}` | GET | 获取商品详情 |
| `POST /api/v1/products` | `/products` | POST | 创建商品 |
| `PUT /api/v1/products/{id}` | `/products/{id}` | PUT | 更新商品 |
| `DELETE /api/v1/products/{id}` | `/products/{id}` | DELETE | 删除商品 |
| `GET /api/v1/products/{id}/variants` | `/products/{id}/variants` | GET | 获取商品规格 |
| `POST /api/v1/products/{id}/variants` | `/products/{id}/variants` | POST | 添加商品规格 |
| `GET /api/v1/products/{id}/reviews` | `/products/{id}/reviews` | GET | 获取商品评价 |
| `POST /api/v1/products/{id}/reviews` | `/products/{id}/reviews` | POST | 添加商品评价 |

### 分类服务 (product-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/categories` | `/categories` | GET | 获取分类列表 |
| `GET /api/v1/categories/tree` | `/categories/tree` | GET | 获取分类树 |
| `GET /api/v1/categories/{id}` | `/categories/{id}` | GET | 获取分类详情 |
| `POST /api/v1/categories` | `/categories` | POST | 创建分类 |
| `PUT /api/v1/categories/{id}` | `/categories/{id}` | PUT | 更新分类 |
| `DELETE /api/v1/categories/{id}` | `/categories/{id}` | DELETE | 删除分类 |
| `GET /api/v1/categories/{id}/products` | `/categories/{id}/products` | GET | 获取分类商品 |

### 品牌服务 (product-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/brands` | `/brands` | GET | 获取品牌列表 |
| `GET /api/v1/brands/{id}` | `/brands/{id}` | GET | 获取品牌详情 |
| `POST /api/v1/brands` | `/brands` | POST | 创建品牌 |
| `PUT /api/v1/brands/{id}` | `/brands/{id}` | PUT | 更新品牌 |
| `DELETE /api/v1/brands/{id}` | `/brands/{id}` | DELETE | 删除品牌 |

### 订单服务 (order-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/orders` | `/orders` | GET | 获取订单列表 |
| `GET /api/v1/orders/{id}` | `/orders/{id}` | GET | 获取订单详情 |
| `POST /api/v1/orders` | `/orders` | POST | 创建订单 |
| `PUT /api/v1/orders/{id}` | `/orders/{id}` | PUT | 更新订单 |
| `PATCH /api/v1/orders/{id}/status` | `/orders/{id}/status` | PATCH | 更新订单状态 |
| `DELETE /api/v1/orders/{id}` | `/orders/{id}` | DELETE | 取消订单 |
| `GET /api/v1/orders/{id}/items` | `/orders/{id}/items` | GET | 获取订单项 |
| `GET /api/v1/orders/{id}/payments` | `/orders/{id}/payments` | GET | 获取订单支付 |
| `GET /api/v1/orders/{id}/shipments` | `/orders/{id}/shipments` | GET | 获取订单发货 |
| `POST /api/v1/orders/{id}/shipments` | `/orders/{id}/shipments` | POST | 创建发货记录 |

### 购物车服务 (order-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/carts/{userId}` | `/carts/{userId}` | GET | 获取购物车 |
| `GET /api/v1/carts/{userId}/items` | `/carts/{userId}/items` | GET | 获取购物车项 |
| `POST /api/v1/carts/{userId}/items` | `/carts/{userId}/items` | POST | 添加购物车项 |
| `PUT /api/v1/carts/{userId}/items/{itemId}` | `/carts/{userId}/items/{itemId}` | PUT | 更新购物车项 |
| `DELETE /api/v1/carts/{userId}/items/{itemId}` | `/carts/{userId}/items/{itemId}` | DELETE | 删除购物车项 |
| `DELETE /api/v1/carts/{userId}` | `/carts/{userId}` | DELETE | 清空购物车 |

### 支付服务 (payment-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/payments` | `/payments` | GET | 获取支付记录 |
| `GET /api/v1/payments/{id}` | `/payments/{id}` | GET | 获取支付详情 |
| `POST /api/v1/payments` | `/payments` | POST | 创建支付 |
| `PATCH /api/v1/payments/{id}/status` | `/payments/{id}/status` | PATCH | 更新支付状态 |
| `GET /api/v1/payments/{id}/refunds` | `/payments/{id}/refunds` | GET | 获取退款记录 |
| `POST /api/v1/payments/{id}/refunds` | `/payments/{id}/refunds` | POST | 创建退款 |

### 支付方式服务 (payment-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/payment-methods` | `/payment-methods` | GET | 获取支付方式 |
| `GET /api/v1/payment-methods/{id}` | `/payment-methods/{id}` | GET | 获取支付方式详情 |
| `POST /api/v1/payment-methods` | `/payment-methods` | POST | 创建支付方式 |
| `PUT /api/v1/payment-methods/{id}` | `/payment-methods/{id}` | PUT | 更新支付方式 |
| `DELETE /api/v1/payment-methods/{id}` | `/payment-methods/{id}` | DELETE | 删除支付方式 |

### 交易服务 (payment-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/transactions` | `/transactions` | GET | 获取交易记录 |
| `GET /api/v1/transactions/{id}` | `/transactions/{id}` | GET | 获取交易详情 |
| `POST /api/v1/transactions` | `/transactions` | POST | 创建交易 |

### 库存服务 (stock-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/stocks` | `/stocks` | GET | 获取库存列表 |
| `GET /api/v1/stocks/{productId}` | `/stocks/{productId}` | GET | 获取商品库存 |
| `POST /api/v1/stocks` | `/stocks` | POST | 创建库存 |
| `PUT /api/v1/stocks/{productId}` | `/stocks/{productId}` | PUT | 更新库存 |
| `PATCH /api/v1/stocks/{productId}/adjust` | `/stocks/{productId}/adjust` | PATCH | 调整库存 |
| `POST /api/v1/stocks/reservations` | `/stocks/reservations` | POST | 预留库存 |
| `DELETE /api/v1/stocks/reservations/{id}` | `/stocks/reservations/{id}` | DELETE | 释放预留 |

### 仓库服务 (stock-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/warehouses` | `/warehouses` | GET | 获取仓库列表 |
| `GET /api/v1/warehouses/{id}` | `/warehouses/{id}` | GET | 获取仓库详情 |
| `POST /api/v1/warehouses` | `/warehouses` | POST | 创建仓库 |
| `PUT /api/v1/warehouses/{id}` | `/warehouses/{id}` | PUT | 更新仓库 |
| `GET /api/v1/warehouses/{id}/stocks` | `/warehouses/{id}/stocks` | GET | 获取仓库库存 |

### 搜索服务 (search-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/search/products` | `/search/products` | GET | 商品搜索 |
| `POST /api/v1/search/products` | `/search/products` | POST | 复杂商品搜索 |
| `GET /api/v1/search/suggestions` | `/search/suggestions` | GET | 搜索建议 |
| `GET /api/v1/search/keywords/trending` | `/search/keywords/trending` | GET | 热门关键词 |
| `GET /api/v1/search/analytics` | `/search/analytics` | GET | 搜索分析 |

### 日志服务 (log-service)

| 外部API路径 | 内部控制器路径 | HTTP方法 | 功能描述 |
|------------|---------------|----------|----------|
| `GET /api/v1/logs` | `/logs` | GET | 获取日志列表 |
| `GET /api/v1/logs/applications` | `/logs/applications` | GET | 获取应用日志 |
| `GET /api/v1/logs/operations` | `/logs/operations` | GET | 获取操作日志 |
| `GET /api/v1/logs/errors` | `/logs/errors` | GET | 获取错误日志 |
| `GET /api/v1/logs/access` | `/logs/access` | GET | 获取访问日志 |
| `GET /api/v1/logs/audit` | `/logs/audit` | GET | 获取审计日志 |

## 🔄 兼容性路由

为了向后兼容，网关还提供了旧版API路径的支持：

| 旧路径模式 | 新路径模式 | 说明 |
|-----------|-----------|------|
| `/auth/**` | `/api/v1/auth/**` | 认证服务兼容 |
| `/user/**` | `/api/v1/users/**` | 用户服务兼容 |
| `/product/**` | `/api/v1/products/**` | 商品服务兼容 |
| `/order/**` | `/api/v1/orders/**` | 订单服务兼容 |
| `/payment/**` | `/api/v1/payments/**` | 支付服务兼容 |
| `/stock/**` | `/api/v1/stocks/**` | 库存服务兼容 |
| `/search/**` | `/api/v1/search/**` | 搜索服务兼容 |
| `/log/**` | `/api/v1/logs/**` | 日志服务兼容 |

## 📋 实施步骤

1. **网关配置更新** ✅
   - 配置统一的路径重写规则
   - 添加版本控制前缀
   - 保留兼容性路由

2. **服务控制器重构** 🔄
   - 移除服务内部的`/api/v1`前缀
   - 使用RESTful资源路径
   - 统一HTTP方法使用

3. **API文档更新** ⏳
   - 更新OpenAPI文档
   - 更新Knife4j配置
   - 生成新的API文档

4. **客户端迁移** ⏳
   - 提供迁移指南
   - 逐步迁移到新API
   - 监控使用情况

---

**更新时间**: 2025/9/24  
**版本**: v1.0.0  
**状态**: 实施中
