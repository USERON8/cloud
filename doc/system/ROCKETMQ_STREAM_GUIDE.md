# RocketMQ Stream 消息流系统文档

## 📋 目录

1. [系统概述](#系统概述)
2. [架构设计](#架构设计)
3. [消息流程](#消息流程)
4. [事件定义](#事件定义)
5. [服务配置](#服务配置)
6. [使用示例](#使用示例)
7. [故障处理](#故障处理)
8. [最佳实践](#最佳实践)

---

## 系统概述

本系统采用 **Spring Cloud Stream + RocketMQ** 实现微服务间的异步消息通信，主要涉及订单、支付、库存三个核心业务服务。通过事件驱动架构，实现业务解耦和最终一致性。

### 核心功能

- ✅ **订单创建流程**：订单创建后异步通知库存冻结和支付创建
- ✅ **支付处理流程**：支付成功后通知订单完成和库存扣减
- ✅ **库存管理流程**：库存冻结失败时通知订单取消
- ✅ **幂等性保证**：通过 eventId 防止重复处理
- ✅ **异步解耦**：服务间通过消息异步通信，提升系统吞吐量

---

## 架构设计

### 消息拓扑图

```
┌─────────────┐
│   Order     │
│  Service    │
└──────┬──────┘
       │
       │ ① OrderCreatedEvent
       ├──────────────────────────┐
       │                          │
       ▼                          ▼
┌─────────────┐            ┌─────────────┐
│   Stock     │            │  Payment    │
│  Service    │            │  Service    │
└──────┬──────┘            └──────┬──────┘
       │                          │
       │ ③ StockFreezeFailedEvent │ ② PaymentSuccessEvent
       │                          │
       └──────────┬───────────────┘
                  ▼
          ┌─────────────┐
          │   Order     │
          │  Service    │
          └─────────────┘
```

### 消息主题 (Topics)

| Topic                  | 生产者          | 消费者               | 描述                 |
|------------------------|-----------------|----------------------|----------------------|
| `order-created`        | Order Service   | Stock, Payment       | 订单创建事件         |
| `payment-success`      | Payment Service | Order, Stock         | 支付成功事件         |
| `stock-freeze-failed`  | Stock Service   | Order                | 库存冻结失败事件     |

### 消费者组 (Consumer Groups)

| 消费者组                  | 服务            | 订阅主题              |
|---------------------------|-----------------|----------------------|
| `order-consumer-group`    | Order Service   | payment-success, stock-freeze-failed |
| `stock-consumer-group`    | Stock Service   | order-created, payment-success |
| `payment-consumer-group`  | Payment Service | order-created        |

---

## 消息流程

### 流程 1: 订单创建 → 库存冻结 + 支付创建

```
1. 用户创建订单
   ↓
2. Order Service 发送 OrderCreatedEvent 到 order-created topic
   ↓
3a. Stock Service 消费事件         3b. Payment Service 消费事件
    - 检查库存是否充足                  - 创建支付记录
    - 冻结库存（预留库存）              - 立即完成支付（简化逻辑）
    - 成功：等待支付                    - 发送 PaymentSuccessEvent
    - 失败：发送 StockFreezeFailedEvent
```

### 流程 2: 支付成功 → 订单完成 + 库存扣减

```
1. Payment Service 支付成功后发送 PaymentSuccessEvent
   ↓
2a. Order Service 消费事件         2b. Stock Service 消费事件
    - 更新订单状态为已支付              - 解冻库存
    - 记录支付流水号                    - 扣减库存（确认出库）
    - 订单完成                          - 更新库存记录
```

### 流程 3: 库存冻结失败 → 订单取消

```
1. Stock Service 库存冻结失败
   ↓
2. 发送 StockFreezeFailedEvent 到 stock-freeze-failed topic
   ↓
3. Order Service 消费事件
   - 取消订单
   - 记录取消原因
   - 通知用户
```

---

## 事件定义

### OrderCreatedEvent（订单创建事件）

**路径**: `common-module/src/main/java/com/cloud/common/messaging/event/OrderCreatedEvent.java`

```java
@Data
@Builder
public class OrderCreatedEvent implements Serializable {
    private Long orderId;                           // 订单ID
    private String orderNo;                         // 订单号
    private Long userId;                            // 用户ID
    private BigDecimal totalAmount;                 // 订单总金额
    private Map<Long, Integer> productQuantityMap;  // 商品数量映射 <商品ID, 数量>
    private String remark;                          // 备注
    private Long timestamp;                         // 事件时间戳
    private String eventId;                         // 事件ID（幂等性）
}
```

**触发时机**: 用户创建订单成功后

**消费者**: Stock Service, Payment Service

---

### PaymentSuccessEvent（支付成功事件）

**路径**: `common-module/src/main/java/com/cloud/common/messaging/event/PaymentSuccessEvent.java`

```java
@Data
@Builder
public class PaymentSuccessEvent implements Serializable {
    private Long paymentId;        // 支付ID
    private Long orderId;          // 订单ID
    private String orderNo;        // 订单号
    private Long userId;           // 用户ID
    private BigDecimal amount;     // 支付金额
    private String paymentMethod;  // 支付方式（ALIPAY, WECHAT等）
    private String transactionNo;  // 支付流水号
    private Long timestamp;        // 事件时间戳
    private String eventId;        // 事件ID（幂等性）
}
```

**触发时机**: 支付完成后

**消费者**: Order Service, Stock Service

---

### StockFreezeFailedEvent（库存冻结失败事件）

**路径**: `common-module/src/main/java/com/cloud/common/messaging/event/StockFreezeFailedEvent.java`

```java
@Data
@Builder
public class StockFreezeFailedEvent implements Serializable {
    private Long orderId;      // 订单ID
    private String orderNo;    // 订单号
    private String reason;     // 失败原因
    private Long timestamp;    // 事件时间戳
    private String eventId;    // 事件ID（幂等性）
}
```

**触发时机**: 库存冻结失败时

**消费者**: Order Service

---

## 服务配置

### Order Service 配置

**文件**: `order-service/src/main/resources/application-rocketmq.yml`

```yaml
spring:
  cloud:
    stream:
      function:
        definition: paymentSuccessConsumer;stockFreezeFailedConsumer

      rocketmq:
        binder:
          name-server: ${ROCKETMQ_NAME_SERVER:127.0.0.1:39876}
        bindings:
          orderCreatedProducer-out-0:
            producer:
              group: order-producer-group
              sync: false
          paymentSuccessConsumer-in-0:
            consumer:
              subscription: 'PAYMENT_SUCCESS'
              orderly: false
          stockFreezeFailedConsumer-in-0:
            consumer:
              subscription: 'STOCK_FREEZE_FAILED'
              orderly: false

      bindings:
        orderCreatedProducer-out-0:
          destination: order-created
          content-type: application/json
        paymentSuccessConsumer-in-0:
          destination: payment-success
          content-type: application/json
          group: order-consumer-group
        stockFreezeFailedConsumer-in-0:
          destination: stock-freeze-failed
          content-type: application/json
          group: order-consumer-group
```

**关键类**:
- `OrderMessageProducer`: 发送订单创建和取消事件
- `OrderMessageConsumer`: 消费支付成功和库存冻结失败事件

---

### Stock Service 配置

**文件**: `stock-service/src/main/resources/application-rocketmq.yml`

```yaml
spring:
  cloud:
    stream:
      function:
        definition: orderCreatedConsumer;paymentSuccessConsumer

      rocketmq:
        binder:
          name-server: ${ROCKETMQ_NAME_SERVER:127.0.0.1:39876}
        bindings:
          stockFreezeFailedProducer-out-0:
            producer:
              group: stock-producer-group
              sync: false
          orderCreatedConsumer-in-0:
            consumer:
              subscription: 'ORDER_CREATED'
              orderly: false
          paymentSuccessConsumer-in-0:
            consumer:
              subscription: 'PAYMENT_SUCCESS'
              orderly: false

      bindings:
        stockFreezeFailedProducer-out-0:
          destination: stock-freeze-failed
          content-type: application/json
        orderCreatedConsumer-in-0:
          destination: order-created
          content-type: application/json
          group: stock-consumer-group
        paymentSuccessConsumer-in-0:
          destination: payment-success
          content-type: application/json
          group: stock-consumer-group
```

**关键类**:
- `StockMessageProducer`: 发送库存冻结失败事件
- `StockMessageConsumer`: 消费订单创建和支付成功事件

---

### Payment Service 配置

**文件**: `payment-service/src/main/resources/application-rocketmq.yml`

```yaml
spring:
  cloud:
    stream:
      function:
        definition: orderCreatedConsumer

      rocketmq:
        binder:
          name-server: ${ROCKETMQ_NAME_SERVER:127.0.0.1:39876}
        bindings:
          paymentSuccessProducer-out-0:
            producer:
              group: payment-producer-group
              sync: false
          orderCreatedConsumer-in-0:
            consumer:
              subscription: 'ORDER_CREATED'
              orderly: false

      bindings:
        paymentSuccessProducer-out-0:
          destination: payment-success
          content-type: application/json
        orderCreatedConsumer-in-0:
          destination: order-created
          content-type: application/json
          group: payment-consumer-group
```

**关键类**:
- `PaymentMessageProducer`: 发送支付成功事件
- `PaymentMessageConsumer`: 消费订单创建事件

---

## 使用示例

### 示例 1: Order Service 发送订单创建事件

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMessageProducer orderMessageProducer;

    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(OrderDTO orderDTO) {
        // 1. 创建订单记录
        Order order = saveOrderToDatabase(orderDTO);

        // 2. 构建订单创建事件
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .productQuantityMap(buildProductQuantityMap(orderDTO))
                .remark(orderDTO.getRemark())
                .build();

        // 3. 发送事件（异步）
        boolean sent = orderMessageProducer.sendOrderCreatedEvent(event);

        if (!sent) {
            log.error("订单创建事件发送失败: orderNo={}", order.getOrderNo());
            // 可以记录到失败表，后续补偿
        }

        return convertToVO(order);
    }
}
```

---

### 示例 2: Stock Service 消费订单创建事件

```java
@Component
@RequiredArgsConstructor
public class StockMessageConsumer {

    private final StockService stockService;
    private final StockMessageProducer stockMessageProducer;

    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();

            try {
                // 1. 幂等性检查（建议使用Redis存储已处理的eventId）
                if (isEventProcessed(event.getEventId())) {
                    log.warn("事件已处理，跳过: eventId={}", event.getEventId());
                    return;
                }

                // 2. 冻结库存
                Map<Long, Integer> products = event.getProductQuantityMap();
                boolean success = true;
                String failureReason = null;

                for (Map.Entry<Long, Integer> entry : products.entrySet()) {
                    Long productId = entry.getKey();
                    Integer quantity = entry.getValue();

                    // 检查库存是否充足
                    if (!stockService.checkStockSufficient(productId, quantity)) {
                        failureReason = "商品 " + productId + " 库存不足";
                        success = false;
                        break;
                    }

                    // 预留库存
                    success = stockService.reserveStock(productId, quantity);
                    if (!success) {
                        failureReason = "商品 " + productId + " 库存冻结失败";
                        break;
                    }
                }

                // 3. 处理结果
                if (!success) {
                    // 发送库存冻结失败事件
                    stockMessageProducer.sendStockFreezeFailedEvent(
                        event.getOrderId(),
                        event.getOrderNo(),
                        failureReason
                    );
                }

                // 4. 记录已处理事件
                markEventAsProcessed(event.getEventId());

            } catch (Exception e) {
                log.error("处理订单创建事件失败", e);
                throw new RuntimeException("处理失败，触发重试", e);
            }
        };
    }
}
```

---

### 示例 3: Payment Service 处理支付并发送成功事件

```java
@Component
@RequiredArgsConstructor
public class PaymentMessageConsumer {

    private final PaymentService paymentService;
    private final PaymentMessageProducer paymentMessageProducer;

    @Bean
    public Consumer<Message<OrderCreatedEvent>> orderCreatedConsumer() {
        return message -> {
            OrderCreatedEvent event = message.getPayload();

            try {
                // 1. 创建支付记录
                PaymentDTO paymentDTO = new PaymentDTO();
                paymentDTO.setOrderId(event.getOrderId());
                paymentDTO.setOrderNo(event.getOrderNo());
                paymentDTO.setUserId(event.getUserId());
                paymentDTO.setAmount(event.getTotalAmount());
                paymentDTO.setPaymentMethod("ALIPAY");

                Long paymentId = paymentService.createPayment(paymentDTO);

                // 2. 立即完成支付（简化逻辑）
                Boolean success = paymentService.processPaymentSuccess(paymentId);

                if (success) {
                    // 3. 发送支付成功事件
                    String transactionNo = "TXN" + System.currentTimeMillis() + paymentId;

                    paymentMessageProducer.sendPaymentSuccessEvent(
                        paymentId,
                        event.getOrderId(),
                        event.getOrderNo(),
                        event.getUserId(),
                        event.getTotalAmount(),
                        "ALIPAY",
                        transactionNo
                    );
                }

            } catch (Exception e) {
                log.error("处理订单创建事件失败", e);
                throw new RuntimeException("处理失败，触发重试", e);
            }
        };
    }
}
```

---

## 故障处理

### 1. 消息重试机制

RocketMQ 默认支持消息重试，当消费者抛出异常时会触发重试。

**配置重试次数**:

```yaml
spring:
  cloud:
    stream:
      rocketmq:
        bindings:
          orderCreatedConsumer-in-0:
            consumer:
              max-attempts: 3  # 最大重试3次
```

### 2. 死信队列

当消息重试次数超过限制后，消息会进入死信队列（DLQ）。

**查看死信队列**:
```bash
# 死信队列命名规则: %DLQ%ConsumerGroup
# 例如: %DLQ%stock-consumer-group
```

### 3. 幂等性保证

**推荐方案**: 使用 Redis 存储已处理的 eventId

```java
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String EVENT_KEY_PREFIX = "event:processed:";
    private static final long EVENT_TTL_SECONDS = 86400; // 24小时

    /**
     * 检查事件是否已处理
     */
    public boolean isEventProcessed(String eventId) {
        String key = EVENT_KEY_PREFIX + eventId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 标记事件已处理
     */
    public void markEventAsProcessed(String eventId) {
        String key = EVENT_KEY_PREFIX + eventId;
        redisTemplate.opsForValue().set(key, "1", EVENT_TTL_SECONDS, TimeUnit.SECONDS);
    }
}
```

### 4. 消息顺序性

如果需要保证消息顺序，设置 `orderly: true`:

```yaml
bindings:
  orderCreatedConsumer-in-0:
    consumer:
      orderly: true  # 顺序消费
```

**注意**: 顺序消费会降低并发性能。

### 5. 消息丢失处理

**策略**:
1. **生产者**: 使用同步发送 (`sync: true`) 确保消息发送成功
2. **消费者**: 手动ACK机制，处理成功后才确认
3. **补偿**: 定时任务扫描未完成的订单，补偿发送消息

---

## 最佳实践

### 1. 事件设计原则

✅ **DO**:
- 使用唯一的 eventId 保证幂等性
- 事件携带完整的业务上下文信息
- 使用时间戳记录事件发生时间
- 事件命名清晰，使用过去式（如 OrderCreated, PaymentSuccess）

❌ **DON'T**:
- 不要在事件中携带敏感信息（密码、完整银行卡号等）
- 避免事件过大（超过1MB），考虑分拆或存储引用
- 不要在事件处理中进行长时间同步调用

### 2. 消费者设计原则

✅ **DO**:
- 实现幂等性检查
- 快速失败，避免长时间阻塞
- 记录详细日志便于排查问题
- 异常时抛出 RuntimeException 触发重试

❌ **DON'T**:
- 避免在消费者中进行复杂的业务逻辑
- 不要吞掉异常（会导致消息被错误确认）
- 避免在消费者中调用外部慢速服务

### 3. 性能优化

**并发消费**:
```yaml
bindings:
  orderCreatedConsumer-in-0:
    consumer:
      orderly: false      # 并发消费
      concurrency: 20     # 并发线程数
```

**批量消费**:
```yaml
bindings:
  orderCreatedConsumer-in-0:
    consumer:
      batch-size: 10      # 一次消费10条消息
```

### 4. 监控告警

**关键指标**:
- 消息堆积量
- 消费失败率
- 消费延迟
- 死信队列消息数

**推荐工具**:
- RocketMQ Console: 查看Topic、消费者状态
- Prometheus + Grafana: 监控业务指标
- Spring Boot Actuator: 健康检查

### 5. 故障恢复

**补偿策略**:

```java
@Scheduled(cron = "0 */5 * * * ?") // 每5分钟执行一次
public void compensateFailedOrders() {
    // 查询超过10分钟仍未完成的订单
    List<Order> pendingOrders = orderMapper.selectPendingOrders(10);

    for (Order order : pendingOrders) {
        // 重新发送订单创建事件
        OrderCreatedEvent event = buildOrderCreatedEvent(order);
        orderMessageProducer.sendOrderCreatedEvent(event);

        log.info("补偿发送订单创建事件: orderNo={}", order.getOrderNo());
    }
}
```

---

## 附录

### A. RocketMQ 启动命令

```bash
# 启动 NameServer
cd docker && docker-compose up -d namesrv

# 启动 Broker
docker-compose up -d rmqbroker

# 启动 Console
docker-compose up -d rmqconsole

# 查看服务状态
docker-compose ps
```

### B. Topic 创建命令

```bash
# 手动创建 Topic（如果自动创建未生效）
docker exec -it rmqbroker sh

# 创建 order-created topic
mqadmin updateTopic -n localhost:9876 -t order-created -c DefaultCluster

# 创建 payment-success topic
mqadmin updateTopic -n localhost:9876 -t payment-success -c DefaultCluster

# 创建 stock-freeze-failed topic
mqadmin updateTopic -n localhost:9876 -t stock-freeze-failed -c DefaultCluster
```

### C. 常用排查命令

```bash
# 查看消费者组状态
mqadmin consumerProgress -n localhost:9876 -g stock-consumer-group

# 查看Topic订阅关系
mqadmin topicStatus -n localhost:9876 -t order-created

# 重置消费位点（慎用）
mqadmin resetOffsetByTime -n localhost:9876 -g stock-consumer-group -t order-created -s -1
```

---

## 更新记录

| 版本   | 日期       | 作者       | 说明                           |
|--------|------------|------------|--------------------------------|
| v1.0.0 | 2025-01-15 | what's up  | 初始版本，实现订单-支付-库存消息流 |

---

## 待优化项

⚠️ **已知问题**:

1. **Stock Service - paymentSuccessConsumer**:
   - PaymentSuccessEvent 中未携带商品信息（productQuantityMap）
   - 无法直接获取需要扣减的商品和数量
   - **建议**: 在 PaymentSuccessEvent 中添加 productQuantityMap 字段，或通过Feign调用订单服务获取详情

2. **幂等性实现**:
   - 目前仅在代码中标记了TODO
   - **建议**: 统一实现 IdempotentService，使用Redis存储已处理的eventId

3. **补偿机制**:
   - 消息发送失败时未记录到失败表
   - **建议**: 实现消息发送失败记录表和定时补偿任务

4. **监控告警**:
   - 未集成Prometheus和Grafana
   - **建议**: 添加自定义指标并配置告警规则

---

**文档结束** 📄
