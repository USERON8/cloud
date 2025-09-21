# 日志服务 (Log Service)

## 简介

日志服务是电商平台的操作日志管理服务，负责收集、存储和查询各服务的操作日志。通过消息队列接收来自其他服务的日志事件，并提供日志查询接口。

## 核心功能

1. **操作日志收集**
    - 通过消息队列接收各服务的操作日志
    - 实现日志的幂等性处理

2. **操作日志存储**
    - 将操作日志存储到Elasticsearch，便于搜索和分析
    - 支持多种服务类型的日志存储（管理员、商家、用户、商品、支付、库存等）

3. **操作日志查询**
    - 提供操作日志的查询接口
    - 支持分页查询

4. **用户事件日志处理** (新增)
    - 接收来自 user-service 的用户变更事件
    - 实现幂等性处理，避免重复消费
    - 自动进行敏感信息脱敏(如手机号)

5. **库存事件日志处理** (新增)
    - 接收来自 stock-service 的库存变更事件
    - 支持所有库存操作类型的事件处理
    - 提供库存变更历史追踪和分析

## 技术栈

- **核心框架**: Spring Boot 3.5.3, Spring Cloud 2025.0.0
- **服务治理**: Nacos (服务注册与发现、配置管理)
- **消息队列**: RocketMQ 5.3.2, Spring Cloud Stream
- **搜索引擎**: Elasticsearch (日志搜索和分析)
- **安全框架**: Spring Security, OAuth2 Resource Server

## 日志类型

1. **管理员操作日志** - 记录管理员的操作行为
2. **商家操作日志** - 记录商家的操作行为
3. **用户操作日志** - 记录用户的操作行为
4. **商品操作日志** - 记录商品相关的操作
5. **支付操作日志** - 记录支付相关的操作
6. **库存操作日志** - 记录库存变更操作
7. **用户事件日志** - 记录用户相关的变更事件
8. **库存事件日志** - 记录库存变更事件和历史

## 核心处理逻辑

通过RocketMQ消息队列接收来自其他服务的日志事件，处理各种类型的日志：

### 用户事件处理流程

```java
// 用户事件消费者
public Consumer<Message<UserChangeEvent>> userEventConsumer() {
    return message -> {
        // 1. 提取事件数据和追踪ID
        // 2. 幂等性检查（基于traceId）
        // 3. 数据转换和脱敏处理
        // 4. 存储到Elasticsearch
    };
}
```

### 库存事件处理流程

```java
// 库存事件消费者
public Consumer<Message<StockChangeEvent>> stockEventConsumer() {
    return message -> {
        // 1. 提取库存变更事件数据
        // 2. 幂等性检查（基于eventId）
        // 3. 数据转换和处理
        // 4. 存储到Elasticsearch
    };
}
```

### 架构组件

#### 用户事件处理组件

- **UserEventConsumer**: 用户事件消费者
- **UserEventProcessor**: 用户事件处理器
- **UserEventDocument**: ES文档模型
- **UserEventService**: 用户事件业务服务
- **UserEventRepository**: ES数据仓储

#### 库存事件处理组件

- **StockEventConsumer**: 库存事件消费者
- **StockEventProcessor**: 库存事件处理器
- **StockEventDocument**: ES文档模型
- **StockEventService**: 库存事件业务服务
- **StockEventRepository**: ES数据仓储

### 支持的库存事件类型

- **STOCK_IN**: 入库事件
- **STOCK_OUT**: 出库事件
- **STOCK_RESERVED**: 预占事件
- **STOCK_RELEASED**: 释放事件
- **STOCK_LOCKED**: 锁定事件
- **STOCK_UNLOCKED**: 解锁事件
- **STOCK_ADJUSTED**: 调整事件
- **STOCK_TRANSFERRED**: 转移事件
- **STOCK_FROZEN**: 冻结事件
- **STOCK_UNFROZEN**: 解冻事件
- **STOCK_SCRAPED**: 报废事件
- **STOCK_WARNING**: 预警事件
- **STOCK_SYNC**: 同步事件

### 消息队列配置

#### 库存事件消费配置

```yaml
spring:
  cloud:
    stream:
      function:
        definition: orderConsumer,paymentConsumer,userConsumer,stockConsumer
      bindings:
        stockConsumer-in-0:
          destination: stock-events
          content-type: application/json
          group: log-service-stock-group
      rocketmq:
        bindings:
          stockConsumer-in-0:
            consumer:
              subscription: 'stock-in||stock-out||stock-reserved||stock-released||stock-locked||stock-unlocked||stock-adjusted||stock-transferred||stock-frozen||stock-unfrozen||stock-scraped||stock-warning||stock-sync'
              messageModel: CLUSTERING
```

## 部署说明

```bash
# 编译打包
mvn clean package

# 运行服务
java -jar target/log-service.jar
```