# Payment Service (支付服务)

## 服务概述

Payment Service 是电商平台的**支付网关服务**,负责对接第三方支付平台(支付宝、微信支付等)
,处理支付创建、支付回调、退款处理等核心支付业务。通过RocketMQ与order-service异步协作完成支付流程。

- **服务端口**: 8086
- **服务名称**: payment-service
- **数据库**: MySQL (payments数据库)
- **支持渠道**: 支付宝、微信支付(规划中)

## 技术栈

| 技术           | 版本          | 用途     |
|--------------|-------------|--------|
| Spring Boot  | 3.5.3       | 应用框架   |
| MySQL        | 9.3.0       | 支付记录存储 |
| MyBatis Plus | 最新          | ORM框架  |
| Redis        | -           | 缓存、防重  |
| Redisson     | -           | 分布式锁   |
| RocketMQ     | -           | 支付事件   |
| Alipay SDK   | -           | 支付宝SDK |
| MapStruct    | 1.5.5.Final | DTO转换  |

## 核心功能

### 1. 支付管理 (/api/payments)

**PaymentController** - 支付CRUD与处理

- ✅ POST `/api/payments` - 创建支付订单
- ✅ GET `/api/payments/{id}` - 查询支付详情
- ✅ GET `/api/payments/order/{orderId}` - 查询订单支付记录
- ✅ GET `/api/payments` - 分页查询支付列表
- ✅ POST `/api/payments/{id}/cancel` - 取消支付
- ✅ POST `/api/payments/{id}/refund` - 发起退款
- ✅ GET `/api/payments/{id}/status` - 查询支付状态

### 2. 支付宝支付 (/api/payments/alipay)

**AlipayController** - 支付宝支付集成

- ✅ POST `/api/payments/alipay/create` - 创建支付宝支付订单(PC网站/手机网站/APP支付)
- ✅ POST `/api/payments/alipay/notify` - 支付宝异步通知(支付结果回调)
- ✅ GET `/api/payments/alipay/return` - 支付宝同步回调(用户支付完成跳转)
- ✅ POST `/api/payments/alipay/query` - 查询支付宝交易状态
- ✅ POST `/api/payments/alipay/refund` - 支付宝退款
- ✅ POST `/api/payments/alipay/close` - 关闭支付宝交易

### 3. 内部服务接口 (/internal/payments)

**PaymentFeignController** - 供其他服务调用

- ✅ POST `/internal/payments/create` - 内部创建支付订单(供order-service调用)
- ✅ GET `/internal/payments/order/{orderId}` - 查询订单支付信息
- ✅ POST `/internal/payments/notify` - 内部支付通知处理

## 数据模型

### Payment (payments表)

```sql
CREATE TABLE payments (
  id BIGINT PRIMARY KEY,
  payment_no VARCHAR(50) UNIQUE NOT NULL,  -- 支付编号
  order_id BIGINT NOT NULL,                -- 订单ID
  order_no VARCHAR(50),                    -- 订单编号
  user_id BIGINT NOT NULL,                 -- 用户ID
  amount DECIMAL(10,2) NOT NULL,           -- 支付金额
  payment_method VARCHAR(20) NOT NULL,     -- ALIPAY/WECHAT/BALANCE
  status VARCHAR(20) NOT NULL,             -- PENDING/SUCCESS/FAILED/CANCELLED/REFUNDED
  third_party_trade_no VARCHAR(100),       -- 第三方交易号
  notify_url VARCHAR(500),                 -- 回调URL
  return_url VARCHAR(500),                 -- 返回URL
  paid_at DATETIME,                        -- 支付时间
  refund_at DATETIME,                      -- 退款时间
  created_at DATETIME,
  updated_at DATETIME,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);
```

## 依赖服务

| 服务            | 用途     | 通信方式       |
|---------------|--------|------------|
| order-service | 订单信息确认 | RocketMQ异步 |
| 支付宝开放平台       | 支付处理   | HTTP API   |

## 配置说明

### 支付宝配置

```yaml
alipay:
  app-id: 2021000122671234                 # 应用ID
  merchant-private-key: ***                # 应用私钥
  alipay-public-key: ***                   # 支付宝公钥
  gateway-url: https://openapi-sandbox.dl.alipaydev.com/gateway.do
  notify-url: http://localhost:8086/api/payments/alipay/notify
  return-url: http://localhost:3000/payment/success
  sign-type: RSA2
  timeout: 30m
```

## 开发状态

### ✅ 已完成功能

1. **支付核心**
    - [x] 支付订单创建
    - [x] 支付宝支付完整集成(PC/手机/APP)
    - [x] 支付回调处理(异步通知+同步返回)
    - [x] 支付状态查询
    - [x] 支付取消
    - [x] 退款处理(支持部分退款)
    - [x] 交易关闭

2. **支付宝集成**
    - [x] 沙箱环境配置
    - [x] RSA2签名验证
    - [x] 支付订单创建
    - [x] 支付结果查询
    - [x] 退款申请
    - [x] 异步通知处理
    - [x] 同步回调处理

3. **支付流水**
    - [x] 支付流水记录(PaymentFlowServiceImpl)
    - [x] 流水状态追踪
    - [x] 支付锁定机制(PaymentLockServiceImpl)
    - [x] 防止重复支付

4. **数据转换**
    - [x] PaymentConverter
    - [x] AlipayConverter

5. **服务集成**
    - [x] 内部Feign接口(供order-service调用)
    - [x] RocketMQ支付事件发送
    - [x] 与order-service异步协作

### 📋 计划中功能

1. **更多支付渠道**
    - [ ] 微信支付
    - [ ] 余额支付
    - [ ] 银联支付

2. **支付安全**
    - [ ] 签名验证增强
    - [ ] 防重放攻击
    - [ ] 支付限额控制

## 本地运行

```bash
cd payment-service
mvn spring-boot:run
```

## 相关文档

- [API文档 - Payment Service](../doc/services/payment/API_DOC_PAYMENT_SERVICE.md)
- [项目整体文档](../doc/README.md)

## 快速链接

- Knife4j API文档: http://localhost:8086/doc.html
- Actuator Health: http://localhost:8086/actuator/health
