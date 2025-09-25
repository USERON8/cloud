# 云商城微服务API迁移指南 v2.0

## 📋 概述

本指南详细说明了从旧版API到新版RESTful API的迁移过程，帮助开发者和客户端应用顺利完成API升级。

## 🎯 迁移目标

### 统一API设计
- **统一版本控制**: 所有API使用 `/api/v1` 前缀
- **RESTful标准**: 遵循REST架构风格
- **资源导向**: 使用名词而非动词描述资源
- **HTTP方法语义**: 正确使用GET、POST、PUT、PATCH、DELETE

### 路径标准化
```
旧版: /service/action/resource
新版: /api/v1/resource
```

## 🗺️ 详细迁移映射

### 认证服务 (auth-service)

#### 用户认证相关
| 旧版API | 新版API | HTTP方法 | 说明 |
|---------|---------|----------|------|
| `POST /auth/register` | `POST /api/v1/auth/users/register` | POST | 用户注册 |
| `POST /auth/login` | `POST /api/v1/auth/sessions` | POST | 用户登录 |
| `POST /auth/logout` | `DELETE /api/v1/auth/sessions` | DELETE | 用户登出 |
| `POST /auth/logout-all` | `DELETE /api/v1/auth/users/{username}/sessions` | DELETE | 批量登出 |
| `GET /auth/validate-token` | `GET /api/v1/auth/tokens/validate` | GET | 验证令牌 |
| `POST /auth/refresh-token` | `POST /api/v1/auth/tokens/refresh` | POST | 刷新令牌 |

#### OAuth2.1标准端点（保持不变）
| 端点 | 说明 |
|------|------|
| `/oauth2/authorize` | 授权端点 |
| `/oauth2/token` | 令牌端点 |
| `/oauth2/revoke` | 撤销端点 |
| `/.well-known/jwks.json` | 公钥端点 |

### 用户服务 (user-service)

#### 用户管理
| 旧版API | 新版API | HTTP方法 | 说明 |
|---------|---------|----------|------|
| `POST /user/manage/update` | `PUT /api/v1/users/{id}` | PUT | 更新用户 |
| `POST /user/query/page` | `GET /api/v1/users?page=1&size=20` | GET | 分页查询用户 |
| `GET /user/query/findByUsername` | `GET /api/v1/users/search?username={username}` | GET | 根据用户名查询 |
| `GET /user/query/{id}` | `GET /api/v1/users/{id}` | GET | 获取用户详情 |
| `POST /user/manage/create` | `POST /api/v1/users` | POST | 创建用户 |
| `DELETE /user/manage/{id}` | `DELETE /api/v1/users/{id}` | DELETE | 删除用户 |

#### 商家管理
| 旧版API | 新版API | HTTP方法 | 说明 |
|---------|---------|----------|------|
| `PUT /merchant/manage/approveMerchant/{id}` | `PATCH /api/v1/merchants/{id}/approve` | PATCH | 审核通过商家 |
| `PUT /merchant/manage/rejectMerchant/{id}` | `PATCH /api/v1/merchants/{id}/reject` | PATCH | 审核拒绝商家 |
| `GET /merchant/query/{id}` | `GET /api/v1/merchants/{id}` | GET | 获取商家详情 |
| `POST /merchant/manage/create` | `POST /api/v1/merchants` | POST | 创建商家 |

### 商品服务 (product-service)

#### 商品管理
| 旧版API | 新版API | HTTP方法 | 说明 |
|---------|---------|----------|------|
| `POST /product/manage` | `POST /api/v1/products` | POST | 创建商品 |
| `PUT /product/manage/{id}` | `PUT /api/v1/products/{id}` | PUT | 更新商品 |
| `DELETE /product/manage/{id}` | `DELETE /api/v1/products/{id}` | DELETE | 删除商品 |
| `GET /api/v1/products/{id}` | `GET /api/v1/products/{id}` | GET | 获取商品详情 |
| `POST /api/v1/products/page` | `GET /api/v1/products?page=1&size=20` | GET | 分页查询商品 |

#### 分类管理
| 旧版API | 新版API | HTTP方法 | 说明 |
|---------|---------|----------|------|
| `GET /categories/tree` | `GET /api/v1/categories/tree` | GET | 获取分类树 |
| `GET /categories/{id}` | `GET /api/v1/categories/{id}` | GET | 获取分类详情 |
| `POST /categories` | `POST /api/v1/categories` | POST | 创建分类 |
| `PUT /categories/{id}` | `PUT /api/v1/categories/{id}` | PUT | 更新分类 |
| `DELETE /categories/{id}` | `DELETE /api/v1/categories/{id}` | DELETE | 删除分类 |

### 订单服务 (order-service)

#### 订单管理
| 旧版API | 新版API | HTTP方法 | 说明 |
|---------|---------|----------|------|
| `POST /order/manage` | `PUT /api/v1/orders/{id}` | PUT | 更新订单 |
| `POST /order/query/page` | `GET /api/v1/orders?page=1&size=20` | GET | 分页查询订单 |
| `GET /order/query/{id}` | `GET /api/v1/orders/{id}` | GET | 获取订单详情 |
| `POST /order/manage/create` | `POST /api/v1/orders` | POST | 创建订单 |
| `POST /order/manage/cancel/{id}` | `DELETE /api/v1/orders/{id}` | DELETE | 取消订单 |
| `POST /order/manage/updateStatus` | `PATCH /api/v1/orders/{id}/status` | PATCH | 更新订单状态 |

#### 购物车管理
| 旧版API | 新版API | HTTP方法 | 说明 |
|---------|---------|----------|------|
| `GET /cart/{userId}` | `GET /api/v1/carts/{userId}` | GET | 获取购物车 |
| `POST /cart/add` | `POST /api/v1/carts/{userId}/items` | POST | 添加购物车项 |
| `PUT /cart/update` | `PUT /api/v1/carts/{userId}/items/{itemId}` | PUT | 更新购物车项 |
| `DELETE /cart/remove` | `DELETE /api/v1/carts/{userId}/items/{itemId}` | DELETE | 删除购物车项 |

### 支付服务 (payment-service)

#### 支付管理
| 旧版API | 新版API | HTTP方法 | 说明 |
|---------|---------|----------|------|
| `POST /payment/manage` | `POST /api/v1/payments` | POST | 创建支付 |
| `POST /payment/query/page` | `GET /api/v1/payments?page=1&size=20` | GET | 分页查询支付 |
| `GET /payment/query/{id}` | `GET /api/v1/payments/{id}` | GET | 获取支付详情 |
| `POST /payment/manage/updateStatus` | `PATCH /api/v1/payments/{id}/status` | PATCH | 更新支付状态 |
| `POST /payment/manage/refund` | `POST /api/v1/payments/{id}/refunds` | POST | 创建退款 |

### 库存服务 (stock-service)

#### 库存管理
| 旧版API | 新版API | HTTP方法 | 说明 |
|---------|---------|----------|------|
| `GET /api/v1/stock/query/{id}` | `GET /api/v1/stocks/{id}` | GET | 获取库存详情 |
| `GET /api/v1/stock/query/product/{productId}` | `GET /api/v1/stocks/product/{productId}` | GET | 根据商品ID获取库存 |
| `POST /stock/manage/create` | `POST /api/v1/stocks` | POST | 创建库存 |
| `PUT /stock/manage/update` | `PUT /api/v1/stocks/{id}` | PUT | 更新库存 |
| `DELETE /stock/manage/delete/{id}` | `DELETE /api/v1/stocks/{id}` | DELETE | 删除库存 |
| `POST /stock/manage/adjust` | `PATCH /api/v1/stocks/{id}/adjust` | PATCH | 调整库存 |

### 搜索服务 (search-service)

#### 搜索功能
| 旧版API | 新版API | HTTP方法 | 说明 |
|---------|---------|----------|------|
| `GET /api/v1/products/search` | `GET /api/v1/search/products` | GET | 商品搜索 |
| `POST /search/advanced` | `POST /api/v1/search/products` | POST | 复杂搜索 |
| `GET /api/v1/suggestions` | `GET /api/v1/search/suggestions` | GET | 搜索建议 |
| `GET /search/trending` | `GET /api/v1/search/keywords/trending` | GET | 热门关键词 |
| `GET /search/analytics` | `GET /api/v1/search/analytics` | GET | 搜索分析 |

### 日志服务 (log-service)

#### 日志查询
| 旧版API | 新版API | HTTP方法 | 说明 |
|---------|---------|----------|------|
| `GET /log/query/application` | `GET /api/v1/logs/applications` | GET | 获取应用日志 |
| `GET /log/query/operation` | `GET /api/v1/logs/operations` | GET | 获取操作日志 |
| `GET /log/query/error` | `GET /api/v1/logs/errors` | GET | 获取错误日志 |
| `GET /log/query/access` | `GET /api/v1/logs/access` | GET | 获取访问日志 |
| `GET /log/query/audit` | `GET /api/v1/logs/audit` | GET | 获取审计日志 |

## 🔄 HTTP方法变更

### 查询操作 (POST → GET)
```javascript
// 旧版 - 使用POST进行查询
POST /user/query/page
Content-Type: application/json
{
  "page": 1,
  "size": 20,
  "username": "john"
}

// 新版 - 使用GET进行查询
GET /api/v1/users?page=1&size=20&username=john
```

### 更新操作 (POST → PUT/PATCH)
```javascript
// 旧版 - 使用POST进行更新
POST /user/manage/update
Content-Type: application/json
{
  "id": 123,
  "name": "John Doe",
  "email": "john@example.com"
}

// 新版 - 使用PUT进行完整更新
PUT /api/v1/users/123
Content-Type: application/json
{
  "name": "John Doe",
  "email": "john@example.com"
}

// 新版 - 使用PATCH进行部分更新
PATCH /api/v1/users/123
Content-Type: application/json
{
  "email": "newemail@example.com"
}
```

### 删除操作 (POST → DELETE)
```javascript
// 旧版 - 使用POST进行删除
POST /user/manage/delete/123

// 新版 - 使用DELETE进行删除
DELETE /api/v1/users/123
```

## 📅 迁移时间表

### 阶段1: 准备阶段 (已完成)
- ✅ 新版API开发完成
- ✅ 网关路由配置更新
- ✅ API文档更新

### 阶段2: 并行运行阶段 (当前)
- 🔄 新旧API同时提供服务
- 🔄 兼容性路由保持可用
- 🔄 客户端逐步迁移

### 阶段3: 迁移完成阶段 (计划中)
- ⏳ 所有客户端完成迁移
- ⏳ 移除兼容性路由
- ⏳ 清理旧版代码

## 🛠️ 客户端迁移步骤

### 1. 更新基础URL
```javascript
// 旧版
const BASE_URL = 'http://gateway:8080';

// 新版
const BASE_URL = 'http://gateway:8080/api/v1';
```

### 2. 更新请求方法
```javascript
// 旧版 - 分页查询用户
const getUsers = async (pageData) => {
  return await fetch('/user/query/page', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(pageData)
  });
};

// 新版 - 分页查询用户
const getUsers = async (page = 1, size = 20, filters = {}) => {
  const params = new URLSearchParams({ page, size, ...filters });
  return await fetch(`/api/v1/users?${params}`, {
    method: 'GET'
  });
};
```

### 3. 更新错误处理
```javascript
// 新版API统一错误响应格式
{
  "success": false,
  "code": 400,
  "message": "请求参数错误",
  "data": null,
  "timestamp": "2025-01-15T10:30:00Z"
}
```

## 📊 监控和验证

### 迁移进度监控
- API调用量统计
- 新旧版本使用比例
- 错误率对比

### 验证清单
- [ ] 所有核心功能正常
- [ ] 性能指标达标
- [ ] 错误处理正确
- [ ] 文档更新完整

## 🆘 支持和帮助

### 技术支持
- **文档**: 查看最新API文档
- **示例**: 参考代码示例
- **测试**: 使用Postman集合测试

### 常见问题
1. **Q**: 旧版API什么时候停止服务？
   **A**: 计划在所有客户端完成迁移后3个月停止服务

2. **Q**: 新版API有什么性能优化？
   **A**: 统一缓存策略、优化查询性能、减少网络请求

3. **Q**: 如何处理认证授权？
   **A**: 继续使用OAuth2.1标准，令牌格式保持不变

---

**更新时间**: 2025/9/24  
**版本**: v2.0.0  
**状态**: 实施中
