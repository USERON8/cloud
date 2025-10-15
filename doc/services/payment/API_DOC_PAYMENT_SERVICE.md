# Payment Service API Documentation

## 服务概述

支付服务负责支付管理、支付宝集成、退款处理、支付风控等功能。

**服务端口**: 8085
**Gateway路由前缀**: `/payment`, `/payments`

---

## 支付管理API

### 查询接口

| 接口 | 路径 | 方法 | 权限 | 说明 |
|------|------|------|------|------|
| 分页查询支付 | `/payments` | GET | ADMIN + `payment:read` | 支持userId,status,channel筛选 |
| 根据ID查询 | `/payments/{id}` | GET | ADMIN + `payment:read` | 获取支付详情 |
| 根据订单查询 | `/payments/order/{orderId}` | GET | ADMIN + `payment:read` | 查询订单支付信息 |

**查询参数**:
- page: 页码 (default: 1)
- size: 每页数量 (default: 10, max: 100)
- userId: 用户ID (optional)
- status: 支付状态 (optional, 0-9)
- channel: 支付渠道 (optional, 0-9)

---

### 管理接口

| 接口 | 路径 | 方法 | 权限 | 说明 |
|------|------|------|------|------|
| 创建支付 | `/payments` | POST | ADMIN | 创建支付记录 |
| 更新支付 | `/payments/{id}` | PUT | ADMIN | 更新支付信息 |
| 删除支付 | `/payments/{id}` | DELETE | ADMIN | 删除支付记录 |

---

### 支付操作接口

#### 1. 支付成功处理
**路径**: `POST /payments/{id}/success`
**权限**: `ADMIN` or `payment:write`
**分布式锁**: `payment:success:{id}` (等待5s, 持有30s)

**说明**: 处理支付成功状态变更,更新订单状态

**响应示例**:
```json
{
  "code": 200,
  "message": "支付成功处理完成",
  "data": true
}
```

#### 2. 支付失败处理
**路径**: `POST /payments/{id}/fail`
**权限**: `ADMIN` or `payment:write`
**分布式锁**: `payment:fail:{id}`

**请求参数**:
- failReason: 失败原因 (optional)

#### 3. 支付退款
**路径**: `POST /payments/{id}/refund`
**权限**: `ADMIN` or `payment:write`
**分布式锁**: `payment:refund:{id}` (等待3s, 持有20s)

**请求参数**:
- refundAmount: 退款金额 (required)
- refundReason: 退款原因 (optional)

**响应示例**:
```json
{
  "code": 200,
  "message": "退款处理完成",
  "data": true
}
```

---

### 风控检查接口

#### 支付风控检查
**路径**: `POST /payments/risk-check`
**权限**: `ADMIN` or `payment:read`
**分布式锁**: `payment:risk:user:{userId}` (快速检查, 持有3s)

**请求参数**:
- userId: 用户ID (required)
- amount: 支付金额 (required)
- paymentMethod: 支付方式 (required)

**响应示例**:
```json
{
  "code": 200,
  "message": "风控检查通过",
  "data": true
}
```

---

## 支付宝支付API

**基础路径**: `/api/v1/payment/alipay`

### 1. 创建支付宝支付
**路径**: `POST /api/v1/payment/alipay/create`

**请求体**:
```json
{
  "orderId": 1,
  "orderNo": "ORD20250115001",
  "subject": "商品名称",
  "amount": 99.99,
  "returnUrl": "http://example.com/return",
  "notifyUrl": "http://example.com/notify"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "支付订单创建成功",
  "data": {
    "paymentForm": "<form>...</form>",
    "paymentUrl": "https://openapi.alipay.com/..."
  }
}
```

### 2. 支付宝异步通知
**路径**: `POST /api/v1/payment/alipay/notify`
**说明**: 接收支付宝异步通知回调

**响应**:
- 成功: "success"
- 失败: "failure"

### 3. 查询支付状态
**路径**: `GET /api/v1/payment/alipay/query/{outTradeNo}`

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": "TRADE_SUCCESS"
}
```

### 4. 申请退款
**路径**: `POST /api/v1/payment/alipay/refund`

**请求参数**:
- outTradeNo: 商户订单号 (required)
- refundAmount: 退款金额 (required)
- refundReason: 退款原因 (required)

### 5. 关闭订单
**路径**: `POST /api/v1/payment/alipay/close/{outTradeNo}`

### 6. 验证支付结果
**路径**: `GET /api/v1/payment/alipay/verify/{outTradeNo}`

**响应示例**:
```json
{
  "code": 200,
  "message": "支付成功",
  "data": true
}
```

---

## Feign内部接口

**基础路径**: `/internal/payment`

| 接口 | 路径 | 方法 | 说明 |
|------|------|------|------|
| 查询支付 | `/{paymentId}` | GET | 根据支付ID查询 |
| 根据订单查询 | `/order/{orderId}` | GET | 根据订单ID查询 |
| 创建支付 | `/` | POST | 创建支付记录 |
| 更新状态 | `/{paymentId}/status` | PUT | 更新支付状态 |
| 支付成功 | `/{paymentId}/success` | POST | 支付成功处理 |
| 支付失败 | `/{paymentId}/fail` | POST | 支付失败处理 |
| 检查状态 | `/{paymentId}/status` | GET | 检查支付状态 |
| 验证金额 | `/validate-amount` | POST | 验证支付金额 |
| 用户统计 | `/stats/user/{userId}` | GET | 获取用户支付统计 |

---

## 数据模型

### PaymentDTO
```json
{
  "id": 1,
  "orderId": 1,
  "orderNo": "ORD20250115001",
  "userId": 1,
  "amount": 99.99,
  "currency": "CNY",
  "paymentMethod": "ALIPAY",
  "paymentChannel": "ALIPAY_PC",
  "status": 1,
  "transactionId": "2025011522001400001",
  "paidAt": "2025-01-15T10:00:00Z",
  "createdAt": "2025-01-15T09:50:00Z"
}
```

### 支付状态枚举
- 0: 待支付
- 1: 支付成功
- 2: 支付失败
- 3: 已关闭
- 4: 已退款
- 5: 部分退款

### 支付方式枚举
- ALIPAY: 支付宝
- WECHAT: 微信支付
- UNION: 银联支付
- BALANCE: 余额支付

---

## 使用示例

### 1. 创建支付宝支付
```bash
curl -X POST "http://localhost:80/api/v1/payment/alipay/create" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "orderNo": "ORD20250115001",
    "subject": "测试商品",
    "amount": 99.99
  }'
```

### 2. 查询支付状态
```bash
curl -X GET "http://localhost:80/api/v1/payment/alipay/query/ORD20250115001"
```

### 3. 支付退款
```bash
curl -X POST "http://localhost:80/payments/1/refund?refundAmount=99.99&refundReason=用户申请退款" \
  -H "Authorization: Bearer {token}"
```

### 4. 风控检查
```bash
curl -X POST "http://localhost:80/payments/risk-check?userId=1&amount=999.99&paymentMethod=ALIPAY" \
  -H "Authorization: Bearer {token}"
```

---

## 支付宝配置说明

### 必需配置
- `alipay.app-id`: 应用ID
- `alipay.private-key`: 应用私钥
- `alipay.public-key`: 支付宝公钥
- `alipay.gateway-url`: 网关地址
- `alipay.notify-url`: 异步通知地址

### 沙箱环境
- 网关: https://openapi-sandbox.dl.alipaydev.com/gateway.do
- 测试账号: 需在支付宝开放平台申请

---

## 错误码

| 错误码 | 说明 |
|-------|------|
| 6001 | 支付记录不存在 |
| 6002 | 支付状态不允许该操作 |
| 6003 | 支付金额验证失败 |
| 6004 | 支付风控检查不通过 |
| 6005 | 退款金额超出支付金额 |
| 6006 | 支付宝接口调用失败 |

---

## 安全注意事项

1. **异步通知处理**:
   - 必须验证签名
   - 必须验证支付金额
   - 必须幂等处理
   - 必须返回"success"

2. **分布式锁保护**:
   - 支付成功处理使用分布式锁
   - 退款处理使用分布式锁
   - 风控检查使用用户级锁

3. **金额校验**:
   - 所有金额使用BigDecimal
   - 必须验证金额与订单一致
   - 退款金额不能超过支付金额

**文档更新**: 2025-01-15
