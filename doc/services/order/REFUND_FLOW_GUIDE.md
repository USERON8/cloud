# 退款流程完整指南

## 概述

本文档描述了电商平台中完整的退款/退货流程实现，包括业务流程、技术架构和消息流转。

## 业务流程

### 退款类型

1. **仅退款（Type 1）**
   - 适用场景：商品未发货、服务未开始等
   - 流程：用户申请 → 商家审核 → 自动退款 → 完成

2. **退货退款（Type 2）**
   - 适用场景：商品已发货/已收货，质量问题等
   - 流程：用户申请 → 商家审核 → 用户退货 → 商家确认收货 → 退款 → 完成

### 退款状态流转

```
0-待审核 (PENDING_AUDIT)
    ↓
1-审核通过 (AUDIT_PASSED) ─────→ 2-审核拒绝 (AUDIT_REJECTED)
    ↓                                      ↓
3-退货中 (RETURNING)                     结束
    ↓
4-已收货 (GOODS_RECEIVED)
    ↓
5-退款中 (REFUNDING)
    ↓
6-已完成 (COMPLETED)

用户可在待审核/审核通过状态时取消 → 7-已取消 (CANCELLED)
系统异常关闭 → 8-已关闭 (CLOSED)
```

## 技术架构

### 涉及服务

1. **order-service**
   - 退款申请创建
   - 退款审核管理
   - 退款状态管理
   - 接收退款完成事件

2. **payment-service**
   - 处理退款请求
   - 调用支付网关退款API
   - 发送退款完成事件

### 数据模型

#### 退款单实体 (Refund)

**数据库表：** `refunds`

**核心字段：**
- `id` - 退款单ID
- `refund_no` - 退款单号（格式：REF{timestamp}{random}）
- `order_id` / `order_no` - 关联订单
- `user_id` - 用户ID
- `merchant_id` - 商家ID
- `refund_type` - 退款类型（1-仅退款，2-退货退款）
- `refund_reason` - 退款原因
- `refund_amount` - 退款金额
- `status` - 退款状态（0-8）
- `audit_time` / `audit_remark` - 审核信息
- `logistics_company` / `logistics_no` - 物流信息
- `refund_transaction_no` - 退款交易流水号

## API 接口

### 用户端接口

#### 1. 创建退款申请

**Endpoint:** `POST /api/v1/refund/create`

**权限:** `@PreAuthorize("hasAuthority('SCOPE_read')")`

**请求体 (RefundCreateDTO):**
```json
{
  "orderId": 123,
  "orderNo": "ORD1234567890",
  "refundType": 1,
  "refundReason": "不想要了",
  "refundDescription": "详细说明",
  "refundAmount": 99.99,
  "refundQuantity": 1
}
```

**验证规则:**
- 订单必须存在且属于当前用户
- 订单状态必须是：已支付(PAID)、已发货(SHIPPED)、已完成(COMPLETED)
- 退款金额不能超过订单实付金额
- 同一订单不能有多个进行中的退款申请

**响应:**
```json
{
  "code": 0,
  "message": "退款申请已提交",
  "data": 456
}
```

**业务逻辑:**
1. 验证订单状态和权限
2. 检查是否已有退款申请
3. 创建退款记录（状态：待审核）
4. 发送 `refund-created` 事件（通知商家）

---

#### 2. 取消退款申请

**Endpoint:** `POST /api/v1/refund/cancel/{refundId}`

**权限:** `@PreAuthorize("hasAuthority('SCOPE_read')")`

**路径参数:**
- `refundId` - 退款单ID

**可取消状态:**
- 待审核 (PENDING_AUDIT)
- 审核通过 (AUDIT_PASSED)

**响应:**
```json
{
  "code": 0,
  "message": "退款申请已取消",
  "data": true
}
```

**业务逻辑:**
1. 验证用户权限
2. 检查退款状态是否可取消
3. 更新状态为已取消
4. 发送 `refund-cancelled` 事件

---

#### 3. 查询退款详情

**Endpoint:** `GET /api/v1/refund/{refundId}`

**权限:** `@PreAuthorize("hasAuthority('SCOPE_read')")`

**响应:**
```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 456,
    "refundNo": "REF1705312345678001",
    "orderId": 123,
    "orderNo": "ORD1234567890",
    "userId": 1,
    "refundType": 1,
    "refundReason": "不想要了",
    "refundAmount": 99.99,
    "status": 0,
    "createdAt": "2025-01-15T10:00:00"
  }
}
```

---

#### 4. 根据订单查询退款

**Endpoint:** `GET /api/v1/refund/order/{orderId}`

**权限:** `@PreAuthorize("hasAuthority('SCOPE_read')")`

**说明:** 返回该订单最新的退款记录

---

### 商家端接口

#### 审核退款申请

**Endpoint:** `POST /api/v1/refund/audit/{refundId}`

**权限:** `@PreAuthorize("hasAuthority('ROLE_MERCHANT')")`

**请求参数:**
- `approved` (Boolean, required) - 是否通过
- `auditRemark` (String, optional) - 审核备注

**示例:**
```
POST /api/v1/refund/audit/456?approved=true&auditRemark=同意退款
```

**响应:**
```json
{
  "code": 0,
  "message": "审核通过",
  "data": true
}
```

**业务逻辑:**
1. 验证商家权限（merchantId匹配）
2. 检查退款状态（必须是待审核）
3. 更新审核结果和备注
4. 如果审核通过且是"仅退款"类型 → 自动调用退款处理
5. 发送 `refund-audited` 事件（通知用户）

---

## 消息流转

### 事件类型

#### 1. refund-created (退款创建事件)

**Topic:** `refund-created`

**Tag:** `REFUND_CREATED`

**生产者:** order-service (RefundMessageProducer)

**消费者:** 暂无（预留给通知服务）

**载荷:**
```json
{
  "eventId": "uuid",
  "eventType": "REFUND_CREATED",
  "timestamp": 1705312345678,
  "refundId": 456,
  "refundNo": "REF1705312345678001",
  "orderId": 123,
  "orderNo": "ORD1234567890",
  "userId": 1,
  "merchantId": 10,
  "refundType": 1,
  "refundAmount": 99.99,
  "refundReason": "不想要了",
  "status": 0
}
```

**用途:** 通知商家有新的退款申请需要审核

---

#### 2. refund-audited (退款审核事件)

**Topic:** `refund-audited`

**Tag:** `REFUND_AUDITED`

**生产者:** order-service (RefundMessageProducer)

**消费者:** 暂无（预留给通知服务）

**载荷:**
```json
{
  "eventId": "uuid",
  "eventType": "REFUND_AUDITED",
  "timestamp": 1705312345678,
  "refundId": 456,
  "refundNo": "REF1705312345678001",
  "orderId": 123,
  "orderNo": "ORD1234567890",
  "approved": true,
  "status": 1
}
```

**用途:** 通知用户退款申请的审核结果

---

#### 3. refund-process (退款处理事件)

**Topic:** `refund-process`

**Tag:** `REFUND_PROCESS`

**生产者:** order-service (RefundMessageProducer)

**消费者:** payment-service (PaymentMessageConsumer.refundProcessConsumer)

**载荷:**
```json
{
  "eventId": "uuid",
  "eventType": "REFUND_PROCESS",
  "timestamp": 1705312345678,
  "refundId": 456,
  "refundNo": "REF1705312345678001",
  "orderId": 123,
  "orderNo": "ORD1234567890",
  "userId": 1,
  "refundAmount": 99.99
}
```

**用途:** 通知支付服务处理退款（调用支付网关API）

**payment-service 处理流程:**
```
1. 接收 refund-process 事件
2. 查询原支付记录（获取支付方式、交易流水等）
3. 调用支付网关退款API（支付宝/微信）
   - 目前简化为直接成功
   - TODO: 实际对接支付宝/微信退款接口
4. 生成退款交易流水号
5. 发送 refund-completed 事件
```

---

#### 4. refund-completed (退款完成事件)

**Topic:** `refund-completed`

**Tag:** `REFUND_COMPLETED`

**生产者:** payment-service (PaymentMessageProducer)

**消费者:** order-service (OrderMessageConsumer.refundCompletedConsumer)

**载荷:**
```json
{
  "eventId": "uuid",
  "eventType": "REFUND_COMPLETED",
  "timestamp": 1705312345678,
  "refundId": 456,
  "refundNo": "REF1705312345678001",
  "orderId": 123,
  "orderNo": "ORD1234567890",
  "userId": 1,
  "refundAmount": 99.99,
  "refundTransactionNo": "REFUND_TXN1705312345678456"
}
```

**用途:** 通知订单服务退款已完成

**order-service 处理流程:**
```
1. 接收 refund-completed 事件
2. 更新退款单状态为已完成
3. TODO: 更新订单的退款状态字段
4. TODO: 如果需要，恢复库存
```

---

#### 5. refund-cancelled (退款取消事件)

**Topic:** `refund-cancelled`

**Tag:** `REFUND_CANCELLED`

**生产者:** order-service (RefundMessageProducer)

**消费者:** 暂无（预留）

**载荷:**
```json
{
  "eventId": "uuid",
  "eventType": "REFUND_CANCELLED",
  "timestamp": 1705312345678,
  "refundId": 456,
  "refundNo": "REF1705312345678001",
  "orderId": 123,
  "status": 7
}
```

**用途:** 记录退款取消事件

---

## 完整流程示例

### 场景1：仅退款（商品未发货）

```
1. 用户提交退款申请
   POST /api/v1/refund/create
   {
     "orderId": 123,
     "refundType": 1,
     "refundReason": "不想要了",
     "refundAmount": 99.99
   }
   ↓
   order-service 创建退款记录（status=0 待审核）
   ↓
   发送 refund-created 事件

2. 商家审核通过
   POST /api/v1/refund/audit/456?approved=true
   ↓
   order-service 更新状态（status=1 审核通过）
   ↓
   发送 refund-audited 事件
   ↓
   因为是"仅退款"类型，自动触发退款处理
   ↓
   发送 refund-process 事件

3. 支付服务处理退款
   payment-service 接收 refund-process 事件
   ↓
   调用支付网关退款API（当前简化为直接成功）
   ↓
   生成退款流水号：REFUND_TXN1705312345678456
   ↓
   发送 refund-completed 事件

4. 订单服务完成退款
   order-service 接收 refund-completed 事件
   ↓
   更新退款单状态（status=6 已完成）
   ↓
   记录退款信息
   ↓
   流程结束
```

### 场景2：退货退款（商品已发货）

```
1. 用户提交退款申请
   POST /api/v1/refund/create
   {
     "orderId": 123,
     "refundType": 2,
     "refundReason": "商品质量问题",
     "refundAmount": 199.99
   }
   ↓
   order-service 创建退款记录（status=0 待审核）
   ↓
   发送 refund-created 事件

2. 商家审核通过
   POST /api/v1/refund/audit/456?approved=true
   ↓
   order-service 更新状态（status=1 审核通过）
   ↓
   发送 refund-audited 事件
   ↓
   因为是"退货退款"类型，等待用户退货

3. 用户填写物流信息
   TODO: POST /api/v1/refund/{id}/logistics
   {
     "logisticsCompany": "顺丰速运",
     "logisticsNo": "SF1234567890"
   }
   ↓
   order-service 更新状态（status=3 退货中）

4. 商家确认收货
   TODO: POST /api/v1/refund/{id}/confirm-goods
   ↓
   order-service 更新状态（status=4 已收货）
   ↓
   自动触发退款处理
   ↓
   发送 refund-process 事件

5. 支付服务处理退款
   payment-service 接收 refund-process 事件
   ↓
   调用支付网关退款API
   ↓
   发送 refund-completed 事件

6. 订单服务完成退款
   order-service 接收 refund-completed 事件
   ↓
   更新退款单状态（status=6 已完成）
   ↓
   流程结束
```

## 配置说明

### order-service 配置

**application-rocketmq.yml:**

```yaml
spring:
  cloud:
    stream:
      function:
        definition: paymentSuccessConsumer;stockFreezeFailedConsumer;refundCompletedConsumer

      bindings:
        # 退款相关生产者
        refundCreatedProducer-out-0:
          destination: refund-created
        refundAuditedProducer-out-0:
          destination: refund-audited
        refundProcessProducer-out-0:
          destination: refund-process
        refundCancelledProducer-out-0:
          destination: refund-cancelled

        # 退款完成消费者
        refundCompletedConsumer-in-0:
          destination: refund-completed
          group: order-consumer-group
```

### payment-service 配置

**application-rocketmq.yml:**

```yaml
spring:
  cloud:
    stream:
      function:
        definition: orderCreatedConsumer;refundProcessConsumer

      bindings:
        # 退款完成生产者
        refundCompletedProducer-out-0:
          destination: refund-completed

        # 退款处理消费者
        refundProcessConsumer-in-0:
          destination: refund-process
          group: payment-consumer-group
```

## 核心代码

### order-service 退款服务

**文件位置:**
- [RefundService.java](order-service/src/main/java/com/cloud/order/service/RefundService.java)
- [RefundServiceImpl.java](order-service/src/main/java/com/cloud/order/service/impl/RefundServiceImpl.java)
- [RefundController.java](order-service/src/main/java/com/cloud/order/controller/RefundController.java)
- [RefundMessageProducer.java](order-service/src/main/java/com/cloud/order/messaging/RefundMessageProducer.java)
- [OrderMessageConsumer.java](order-service/src/main/java/com/cloud/order/messaging/OrderMessageConsumer.java) (refundCompletedConsumer方法)

**核心业务方法:**
- `createRefund()` - 创建退款申请
- `auditRefund()` - 审核退款
- `cancelRefund()` - 取消退款
- `processRefund()` - 处理退款（发送给payment-service）

### payment-service 退款处理

**文件位置:**
- [PaymentMessageConsumer.java](payment-service/src/main/java/com/cloud/payment/messaging/PaymentMessageConsumer.java) (refundProcessConsumer方法)
- [PaymentMessageProducer.java](payment-service/src/main/java/com/cloud/payment/messaging/PaymentMessageProducer.java) (sendRefundCompletedEvent方法)

**处理流程:**
```java
@Bean
public Consumer<Message<Map<String, Object>>> refundProcessConsumer() {
    return message -> {
        // 1. 解析事件
        Map<String, Object> event = message.getPayload();
        Long refundId = ((Number) event.get("refundId")).longValue();
        BigDecimal refundAmount = new BigDecimal(event.get("refundAmount").toString());

        // 2. 查询原支付记录（TODO）
        // Payment payment = paymentService.getPaymentByOrderId(orderId);

        // 3. 调用支付网关退款（TODO）
        // RefundResult result = alipayService.refund(refundNo, refundAmount);

        // 4. 生成退款流水号
        String refundTransactionNo = "REFUND_TXN" + System.currentTimeMillis() + refundId;

        // 5. 发送退款完成事件
        paymentMessageProducer.sendRefundCompletedEvent(...);
    };
}
```

## 待完善功能 (TODO)

### 高优先级

1. **支付网关集成**
   - 对接支付宝退款API
   - 对接微信支付退款API
   - 处理退款失败重试逻辑

2. **退货物流功能**
   - 用户填写退货物流信息接口
   - 商家确认收货接口
   - 物流状态查询

3. **幂等性保证**
   - 使用Redis存储已处理的eventId
   - 防止消息重复消费导致重复退款

4. **库存恢复**
   - 退款完成后恢复商品库存
   - 发送stock-restore事件给stock-service

### 中优先级

5. **订单状态联动**
   - 在orders表增加refund_status字段
   - 退款完成后更新订单状态

6. **退款记录查询**
   - 用户查询自己的退款列表
   - 商家查询待审核的退款列表
   - 支持分页和筛选

7. **通知功能**
   - 退款申请提交后通知商家
   - 审核结果通知用户
   - 退款完成通知用户

### 低优先级

8. **退款超时处理**
   - 商家超过7天未审核自动通过
   - 用户超过15天未退货自动关闭

9. **退款凭证上传**
   - 支持用户上传问题图片
   - 商家上传退货检查结果

10. **退款统计报表**
    - 退款率统计
    - 退款金额统计
    - 退款原因分析

## 测试建议

### 单元测试

1. **RefundServiceImpl 测试**
   - 测试创建退款的各种校验逻辑
   - 测试审核权限和状态流转
   - 测试取消退款的边界条件

2. **消息发送测试**
   - Mock StreamBridge验证消息发送
   - 验证消息payload的完整性

### 集成测试

1. **完整流程测试**
   - 启动order-service、payment-service、RocketMQ
   - 测试仅退款流程（创建→审核→退款→完成）
   - 测试退货退款流程
   - 测试取消流程

2. **异常场景测试**
   - 消息消费失败重试
   - 支付网关退款失败处理
   - 并发创建退款申请

### 压力测试

- 并发创建退款申请
- 消息积压处理能力
- 数据库连接池压力

## 监控指标

### 业务指标

- 退款申请数（按小时/天）
- 退款审核通过率
- 平均退款处理时长
- 退款成功率

### 技术指标

- refund-process消息消费延迟
- refund-completed消息消费延迟
- 支付网关退款接口响应时间
- 退款服务接口P99延迟

## 常见问题

### Q1: 为什么"仅退款"审核通过后自动退款，而"退货退款"需要等待？

A: "仅退款"通常是订单未发货或服务未开始的情况，商家审核通过即表示同意退款，无需等待退货。而"退货退款"需要用户先退货，商家确认收货后才能退款，保障双方权益。

### Q2: 退款失败如何处理？

A: 当前简化实现中退款直接成功。实际场景中，如果支付网关返回退款失败，需要：
1. 记录失败原因
2. 更新退款状态为"退款失败"
3. 支持人工介入处理或自动重试

### Q3: 如何保证退款不重复？

A: 需要实现幂等性检查：
1. 使用Redis存储已处理的eventId
2. 在处理退款前检查eventId是否已存在
3. 支付网关调用时使用退款单号作为幂等键

### Q4: 用户能否部分退款？

A: 当前实现支持用户指定退款金额（不超过订单实付金额）。如需商品维度的部分退款，需要：
1. 扩展退款单支持多个退款商品
2. 增加退款商品明细表
3. 计算部分商品的退款金额

---

**文档版本:** v1.0
**最后更新:** 2025-01-15
**维护人员:** CloudDevAgent
