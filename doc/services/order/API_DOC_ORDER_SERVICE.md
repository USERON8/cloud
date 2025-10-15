# Order Service API Documentation

## 服务概述

订单服务负责订单管理、购物车管理、退款管理等电商核心功能。

**服务端口**: 8082
**Gateway路由前缀**: `/order`

---

## 订单管理模块

### 1. 订单查询

#### 1.1 分页查询订单列表
- **路径**: `GET /order/api/v1/orders`
- **权限**: `hasAuthority('SCOPE_read')`
- **参数**: page, size, userId, status, orderNo
- **返回**: PageResult<OrderVO>

#### 1.2 根据ID查询订单
- **路径**: `GET /order/api/v1/orders/{orderId}`
- **权限**: `hasAuthority('SCOPE_read')`
- **返回**: OrderVO

#### 1.3 查询当前用户订单
- **路径**: `GET /order/api/v1/orders/my-orders`
- **权限**: `hasAuthority('SCOPE_read')`
- **参数**: page, size, status

### 2. 订单创建

#### 2.1 创建订单
- **路径**: `POST /order/api/v1/orders`
- **权限**: `hasAuthority('SCOPE_write')`
- **请求体**: OrderCreateDTO
```json
{
  "productId": 1,
  "quantity": 2,
  "addressId": 1,
  "remark": "请尽快发货"
}
```

### 3. 订单操作

#### 3.1 取消订单
- **路径**: `POST /order/api/v1/orders/{orderId}/cancel`
- **权限**: `hasAuthority('SCOPE_write')`
- **参数**: cancelReason (optional)

#### 3.2 支付订单
- **路径**: `POST /order/api/v1/orders/{orderId}/pay`
- **权限**: `hasAuthority('SCOPE_write')`

#### 3.3 确认收货
- **路径**: `POST /order/api/v1/orders/{orderId}/confirm`
- **权限**: `hasAuthority('SCOPE_write')`

#### 3.4 发货 (商家)
- **路径**: `POST /order/api/v1/orders/{orderId}/ship`
- **权限**: `ROLE_MERCHANT`
- **参数**: trackingNo, shippingCompany

#### 3.5 完成订单
- **路径**: `POST /order/api/v1/orders/{orderId}/complete`
- **权限**: `ROLE_ADMIN` or `ROLE_MERCHANT`

#### 3.6 删除订单
- **路径**: `DELETE /order/api/v1/orders/{orderId}`
- **权限**: `ROLE_ADMIN`

---

## 退款管理模块

### 1. 退款申请

#### 1.1 创建退款申请
- **路径**: `POST /order/api/v1/refund/create`
- **权限**: `hasAuthority('SCOPE_read')`
- **请求体**: RefundCreateDTO
```json
{
  "orderId": 1,
  "refundType": "REFUND_ONLY",
  "refundReason": "不想要了",
  "refundAmount": 99.99,
  "description": "商品不满意"
}
```

#### 1.2 查询退款详情
- **路径**: `GET /order/api/v1/refund/{refundId}`
- **权限**: `hasAuthority('SCOPE_read')`

#### 1.3 根据订单查询退款
- **路径**: `GET /order/api/v1/refund/order/{orderId}`
- **权限**: `hasAuthority('SCOPE_read')`

### 2. 退款处理

#### 2.1 审核退款申请 (商家)
- **路径**: `POST /order/api/v1/refund/audit/{refundId}`
- **权限**: `ROLE_MERCHANT`
- **参数**: 
  - approved: Boolean
  - auditRemark: String (optional)

#### 2.2 取消退款申请
- **路径**: `POST /order/api/v1/refund/cancel/{refundId}`
- **权限**: `hasAuthority('SCOPE_read')`

### 3. 退款列表查询

#### 3.1 查询用户退款列表
- **路径**: `GET /order/api/v1/refund/list`
- **权限**: `hasAuthority('SCOPE_read')`
- **参数**: RefundPageDTO
- **返回**: PageResult<RefundVO>

#### 3.2 查询商家待处理退款
- **路径**: `GET /order/api/v1/refund/merchant/list`
- **权限**: `ROLE_MERCHANT`
- **参数**: RefundPageDTO

---

## Feign内部接口

### 1. 订单查询

#### 1.1 根据ID查询订单
- **路径**: `GET /order/internal/order/{orderId}`

#### 1.2 根据用户ID查询订单列表
- **路径**: `GET /order/internal/order/user/{userId}`

#### 1.3 检查订单支付状态
- **路径**: `GET /order/internal/order/{orderId}/paid-status`

### 2. 订单操作

#### 2.1 创建订单
- **路径**: `POST /order/internal/order/create`
- **请求体**: OrderCreateDTO

#### 2.2 更新订单
- **路径**: `PUT /order/internal/order/{orderId}`
- **请求体**: OrderDTO

#### 2.3 更新订单状态
- **路径**: `POST /order/internal/order/{orderId}/status/{status}`

#### 2.4 支付订单
- **路径**: `POST /order/internal/order/{orderId}/pay`

#### 2.5 发货订单
- **路径**: `POST /order/internal/order/{orderId}/ship`

#### 2.6 完成订单
- **路径**: `POST /order/internal/order/{orderId}/complete`

#### 2.7 取消订单
- **路径**: `POST /order/internal/order/{orderId}/cancel`
- **参数**: cancelReason (optional)

#### 2.8 删除订单
- **路径**: `DELETE /order/internal/order/{orderId}`

### 3. 批量操作

#### 3.1 批量删除订单
- **路径**: `DELETE /order/internal/order/batch`
- **请求体**: `List<Long> orderIds`

#### 3.2 批量取消订单
- **路径**: `POST /order/internal/order/batch/cancel`
- **请求体**: `List<Long> orderIds`
- **参数**: cancelReason (optional)

#### 3.3 批量支付订单
- **路径**: `POST /order/internal/order/batch/pay`
- **请求体**: `List<Long> orderIds`

#### 3.4 批量发货订单
- **路径**: `POST /order/internal/order/batch/ship`
- **请求体**: `List<Long> orderIds`

#### 3.5 批量完成订单
- **路径**: `POST /order/internal/order/batch/complete`
- **请求体**: `List<Long> orderIds`

---

## 数据模型

### OrderVO
```json
{
  "id": 1,
  "orderNo": "ORD20250115100000001",
  "userId": 1,
  "productId": 1,
  "productName": "测试商品",
  "quantity": 2,
  "unitPrice": 99.99,
  "totalAmount": 199.98,
  "status": 1,
  "paymentStatus": 1,
  "shippingStatus": 0,
  "addressId": 1,
  "remark": "请尽快发货",
  "createdAt": "2025-01-15T10:00:00Z",
  "updatedAt": "2025-01-15T10:00:00Z"
}
```

### RefundVO
```json
{
  "id": 1,
  "refundNo": "REF20250115100000001",
  "orderId": 1,
  "userId": 1,
  "merchantId": 1,
  "refundType": "REFUND_ONLY",
  "refundReason": "不想要了",
  "refundAmount": 99.99,
  "status": "PENDING",
  "description": "商品不满意",
  "createdAt": "2025-01-15T10:00:00Z"
}
```

### 订单状态枚举
- 0: 待支付
- 1: 已支付待发货
- 2: 已发货待收货
- 3: 已完成
- 4: 已取消
- 5: 退款中
- 6: 已退款

### 退款状态枚举
- PENDING: 待审核
- APPROVED: 已通过
- REJECTED: 已拒绝
- CANCELLED: 已取消
- REFUNDED: 已退款

---

## 使用示例

### 1. 创建订单
```bash
curl -X POST "http://localhost:80/order/api/v1/orders" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2,
    "addressId": 1,
    "remark": "请尽快发货"
  }'
```

### 2. 查询订单列表
```bash
curl -X GET "http://localhost:80/order/api/v1/orders?page=1&size=20&status=1" \
  -H "Authorization: Bearer {token}"
```

### 3. 取消订单
```bash
curl -X POST "http://localhost:80/order/api/v1/orders/1/cancel?cancelReason=不想要了" \
  -H "Authorization: Bearer {token}"
```

### 4. 创建退款申请
```bash
curl -X POST "http://localhost:80/order/api/v1/refund/create" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "refundType": "REFUND_ONLY",
    "refundReason": "不想要了",
    "refundAmount": 99.99,
    "description": "商品不满意"
  }'
```

---

## 错误码

| 错误码 | 说明 |
|-------|------|
| 3001 | 订单不存在 |
| 3002 | 订单状态不允许该操作 |
| 3003 | 库存不足 |
| 3004 | 支付失败 |
| 3005 | 退款申请已存在 |
| 3006 | 退款金额超出订单金额 |
| 3007 | 订单未支付 |

---

**文档更新**: 2025-01-15
