# RocketMQ Stream消息主题架构配置文档

## 📋 概述

本文档详细描述了云商城微服务系统基于RocketMQ Stream的消息主题架构配置，实现了高效、可靠的事件驱动架构。

## 🏗️ 架构设计

### 1. 消息流架构图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   user-service  │    │ product-service │    │  stock-service  │
│                 │    │                 │    │                 │
│ LogProducer ────┼────┼─────────────────┼────┼─────────────────┤
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
┌─────────────────┐    ┌─────────────────┐            │
│ payment-service │    │  order-service  │            │
│                 │    │                 │            │
│ LogProducer ────┼────┼─ LogProducer ───┤            │
│                 │    │                 │            │
│ PaymentSuccess ─┼────┼─ OrderCreated ──┼────────────┘
│   Producer      │    │   Producer      │
│                 │    │                 │
│ OrderCreated ───┤    │ OrderCompleted ─┼────────────┐
│  Consumer       │    │   Producer      │            │
└─────────────────┘    └─────────────────┘            │
                                │                     │
┌─────────────────┐            │                     │
│ search-service  │            │                     │
│                 │            │                     │
│ LogProducer ────┤            │                     │
└─────────────────┘            │                     │
                               │                     │
┌─────────────────┐            │                     │
│   log-service   │            │                     │
│                 │            │                     │
│ LogConsumer ────┤            │                     │
└─────────────────┘            │                     │
                               ▼                     ▼
                    ┌─────────────────┐    ┌─────────────────┐
                    │ PaymentSuccess  │    │ OrderCompleted  │
                    │   Consumer      │    │   Consumer      │
                    │ (order-service) │    │ (stock-service) │
                    └─────────────────┘    └─────────────────┘
```

### 2. Topic架构设计

| Topic名称 | 用途 | 生产者 | 消费者 | 分区数 |
|-----------|------|--------|--------|--------|
| `log-collection-topic` | 日志收集 | 6个微服务 | log-service | 4 |
| `order-created-topic` | 订单创建 | order-service | payment-service, stock-service | 4 |
| `payment-success-topic` | 支付成功 | payment-service | order-service | 4 |
| `order-completed-topic` | 订单完成 | order-service | stock-service | 4 |

## 🔧 技术实现

### 1. 核心组件

#### 1.1 消息常量定义
- **文件**: `common-module/src/main/java/com/cloud/common/constant/MessageTopicConstants.java`
- **功能**: 统一管理所有Topic名称、标签、消费者组等常量

#### 1.2 事件类定义
- `OrderCreatedEvent`: 订单创建事件
- `PaymentSuccessEvent`: 支付成功事件  
- `OrderCompletedEvent`: 订单完成事件
- `LogCollectionEvent`: 日志收集事件

#### 1.3 基础配置类
- `BaseMessageConfig`: 提供通用的消息配置和工具方法

### 2. 消息流配置

#### 2.1 日志收集流
```yaml
# 生产者配置 (6个微服务)
logProducer-out-0:
  destination: log-collection-topic
  content-type: application/json

# 消费者配置 (log-service)
logConsumer-in-0:
  destination: log-collection-topic
  group: log-collection-consumer-group
```

#### 2.2 订单业务流
```yaml
# 订单创建
orderCreatedProducer-out-0:
  destination: order-created-topic

orderCreatedConsumer-in-0:
  destination: order-created-topic
  group: payment-order-created-group / stock-order-created-group

# 支付成功
paymentSuccessProducer-out-0:
  destination: payment-success-topic

paymentSuccessConsumer-in-0:
  destination: payment-success-topic
  group: order-payment-success-group

# 订单完成
orderCompletedProducer-out-0:
  destination: order-completed-topic

orderCompletedConsumer-in-0:
  destination: order-completed-topic
  group: stock-order-completed-group
```

### 3. 可靠性保障

#### 3.1 消息重试机制
```yaml
consumer:
  max-attempts: 3
  back-off-initial-interval: 1000
  back-off-max-interval: 10000
  back-off-multiplier: 2.0
```

#### 3.2 幂等性处理
- 每个消息都包含唯一的`traceId`
- 消费者在处理前进行幂等性检查
- 处理完成后标记消息已处理

#### 3.3 数据脱敏
- 日志收集时自动脱敏敏感信息
- 支持手机号、邮箱、密码等敏感数据处理

## 📊 性能配置

### 1. 生产者配置
```yaml
producer:
  send-message-timeout: 3000
  compress-message-body-threshold: 4096
  max-message-size: 4194304
  retry-times-when-send-failed: 2
```

### 2. 消费者配置
```yaml
consumer:
  consume-thread-min: 5
  consume-thread-max: 20
  pull-batch-size: 16
  consume-timeout: 10000
```

## 🚀 部署配置

### 1. 环境配置
```yaml
# application.yml
spring:
  profiles:
    active: rocketmq  # 启用RocketMQ配置

# application-rocketmq.yml
spring:
  cloud:
    stream:
      rocketmq:
        binder:
          name-server: localhost:9876  # 生产环境需要修改
```

### 2. 服务启动顺序
1. 启动RocketMQ NameServer和Broker
2. 启动log-service (消费者)
3. 启动其他微服务 (生产者和消费者)

## 📈 监控指标

### 1. 关键指标
- 消息发送成功率
- 消息消费延迟
- 消费者积压情况
- 重试次数统计

### 2. 日志监控
```bash
# 查看消息发送日志
grep "消息发送成功\|消息发送失败" logs/application.log

# 查看消息消费日志  
grep "接收到.*消息\|消息处理成功\|消息处理失败" logs/application.log
```

## 🔍 故障排查

### 1. 常见问题
- **消息发送失败**: 检查NameServer连接和Topic是否存在
- **消息积压**: 检查消费者线程配置和处理逻辑
- **重复消费**: 检查幂等性处理逻辑

### 2. 调试配置
```yaml
logging:
  level:
    com.cloud.*.messaging: DEBUG
    org.springframework.cloud.stream: DEBUG
    org.apache.rocketmq: INFO
```

## 📝 使用示例

### 1. 发送订单创建事件
```java
@Autowired
private OrderEventProducer orderEventProducer;

public void createOrder(Order order) {
    // 创建订单
    Order savedOrder = orderRepository.save(order);
    
    // 发送订单创建事件
    OrderCreatedEvent event = buildOrderCreatedEvent(savedOrder);
    orderEventProducer.sendOrderCreatedEvent(event);
}
```

### 2. 处理支付成功事件
```java
@Bean
public Consumer<Message<PaymentSuccessEvent>> paymentSuccessConsumer() {
    return message -> {
        PaymentSuccessEvent event = message.getPayload();
        // 更新订单状态
        orderService.updateOrderToPaid(event);
    };
}
```

## 🎯 最佳实践

1. **消息设计**: 保持消息结构简洁，包含必要的业务信息
2. **错误处理**: 实现完善的异常处理和重试机制
3. **监控告警**: 配置消息积压和失败率告警
4. **性能调优**: 根据业务量调整分区数和消费者线程数
5. **版本兼容**: 保持消息格式的向后兼容性

---

**文档版本**: v1.0  
**更新时间**: 2025-01-15  
**维护人员**: cloud团队
