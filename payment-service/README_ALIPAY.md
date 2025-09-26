# 支付宝支付集成文档

## 📋 概述

本文档介绍了在payment-service中集成的支付宝支付功能，包括配置、API使用和测试方法。

## 🔧 配置说明

### 1. 环境变量配置

在生产环境中，需要设置以下环境变量：

```bash
# 支付宝应用配置
ALIPAY_APP_ID=你的支付宝应用ID
ALIPAY_PRIVATE_KEY=你的应用私钥
ALIPAY_PUBLIC_KEY=支付宝公钥
ALIPAY_GATEWAY_URL=https://openapi.alipay.com/gateway.do  # 生产环境
ALIPAY_NOTIFY_URL=https://yourdomain.com/api/v1/payment/alipay/notify
ALIPAY_RETURN_URL=https://yourdomain.com/payment/success
```

### 2. 沙箱环境配置

开发测试时使用沙箱环境：

```yaml
alipay:
  gateway-url: https://openapi-sandbox.dl.alipaydev.com/gateway.do
  app-id: 沙箱应用ID
  merchant-private-key: 沙箱应用私钥
  alipay-public-key: 沙箱支付宝公钥
```

## 🚀 API接口说明

### 1. 创建支付订单

**接口**: `POST /api/v1/payment/alipay/create`

**请求参数**:
```json
{
  "orderId": 1234567890,
  "amount": 99.99,
  "subject": "iPhone 15 Pro Max",
  "body": "苹果iPhone 15 Pro Max 256GB 深空黑色",
  "userId": 1001,
  "timeoutMinutes": 30
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "支付订单创建成功",
  "data": {
    "paymentForm": "<form name=\"punchout_form\" method=\"post\" action=\"https://openapi.alipay.com/gateway.do\">...",
    "paymentId": 1234567890123456789,
    "outTradeNo": "PAY_20250925202800_1234567890",
    "status": 0,
    "timestamp": 1727270880000,
    "traceId": "abc123def456"
  }
}
```

### 2. 支付宝异步通知

**接口**: `POST /api/v1/payment/alipay/notify`

此接口由支付宝服务器调用，用于通知支付结果。

### 3. 查询支付状态

**接口**: `GET /api/v1/payment/alipay/query/{outTradeNo}`

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": "TRADE_SUCCESS"
}
```

### 4. 申请退款

**接口**: `POST /api/v1/payment/alipay/refund`

**请求参数**:
```
outTradeNo: PAY_20250925202800_1234567890
refundAmount: 99.99
refundReason: 用户申请退款
```

### 5. 关闭订单

**接口**: `POST /api/v1/payment/alipay/close/{outTradeNo}`

### 6. 验证支付结果

**接口**: `GET /api/v1/payment/alipay/verify/{outTradeNo}`

## 💻 前端集成示例

### 1. 创建支付并跳转

```javascript
// 1. 调用创建支付接口
const response = await fetch('/api/v1/payment/alipay/create', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    orderId: 1234567890,
    amount: 99.99,
    subject: 'iPhone 15 Pro Max',
    userId: 1001
  })
});

const result = await response.json();

if (result.code === 200) {
  // 2. 将返回的表单插入页面并自动提交
  const form = result.data.paymentForm;
  document.body.innerHTML = form;
  document.forms[0].submit();
}
```

### 2. 支付结果处理

```javascript
// 在支付成功页面检查支付结果
const urlParams = new URLSearchParams(window.location.search);
const outTradeNo = urlParams.get('out_trade_no');

if (outTradeNo) {
  // 验证支付结果
  const verifyResponse = await fetch(`/api/v1/payment/alipay/verify/${outTradeNo}`);
  const verifyResult = await verifyResponse.json();
  
  if (verifyResult.data === true) {
    // 支付成功，显示成功页面
    showSuccessPage();
  } else {
    // 支付失败或未完成
    showFailurePage();
  }
}
```

## 🧪 测试方法

### 1. Postman测试

1. **创建支付订单**:
   - URL: `POST http://localhost:8084/api/v1/payment/alipay/create`
   - Body: 使用上述JSON格式

2. **查询支付状态**:
   - URL: `GET http://localhost:8084/api/v1/payment/alipay/query/PAY_20250925202800_1234567890`

### 2. Knife4j文档测试

访问: `http://localhost:8084/doc.html`

在"支付宝支付"分组中测试各个接口。

### 3. 沙箱测试账号

使用支付宝开放平台提供的沙箱测试账号进行支付测试。

## 🔒 安全注意事项

### 1. 私钥安全
- 应用私钥绝不能泄露
- 生产环境使用环境变量管理
- 定期更换密钥

### 2. 签名验证
- 所有异步通知必须验证签名
- 验证失败的通知一律拒绝

### 3. 幂等性处理
- 支付成功处理具有幂等性
- 防止重复处理同一笔支付

### 4. 金额校验
- 异步通知中的金额必须与订单金额一致
- 防止金额篡改攻击

## 🐛 常见问题

### 1. 签名验证失败
- 检查应用私钥和支付宝公钥是否正确
- 确认字符编码为UTF-8
- 检查签名类型是否为RSA2

### 2. 异步通知接收不到
- 确认notify_url可以从外网访问
- 检查防火墙和安全组设置
- 确认接口返回"success"字符串

### 3. 支付页面无法打开
- 检查网关地址是否正确
- 确认应用ID是否有效
- 检查请求参数格式

## 📚 相关文档

- [支付宝开放平台文档](https://opendocs.alipay.com/)
- [电脑网站支付接入指南](https://opendocs.alipay.com/open/270/105898)
- [异步通知说明](https://opendocs.alipay.com/open/270/105902)
