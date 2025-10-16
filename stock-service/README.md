# Stock Service (库存服务)

## 服务概述

Stock Service 是电商平台的**库存管理服务**,负责商品库存的扣减、回滚、查询和预警。通过Redis分布式锁和乐观锁机制保证高并发场景下的库存一致性,与order-service通过RocketMQ异步协作完成库存操作。

- **服务端口**: 8085
- **服务名称**: stock-service
- **数据库**: MySQL (stocks数据库)
- **并发控制**: Redis分布式锁 + 数据库乐观锁

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.3 | 应用框架 |
| MySQL | 9.3.0 | 库存数据存储 |
| MyBatis Plus | 最新 | ORM框架 |
| Redis | - | 分布式锁、缓存 |
| Redisson | - | 分布式锁实现 |
| RocketMQ | - | 库存事件 |
| MapStruct | 1.5.5.Final | DTO转换 |

## 核心功能

### 1. 库存管理 (/api/stocks)

**StockController** - 库存CRUD与操作

- ✅ POST `/api/stocks` - 创建库存记录
- ✅ GET `/api/stocks/{id}` - 查询库存详情
- ✅ GET `/api/stocks/product/{productId}` - 查询商品库存
- ✅ GET `/api/stocks` - 分页查询库存列表
- ✅ PUT `/api/stocks/{id}` - 更新库存数量
- ✅ POST `/api/stocks/deduct` - 扣减库存(带Redis分布式锁)
- ✅ POST `/api/stocks/rollback` - 回滚库存
- ✅ POST `/api/stocks/batch/deduct` - 批量扣减库存
- ✅ GET `/api/stocks/low-stock` - 查询低库存商品(低于阈值)
- ✅ GET `/api/stocks/alert` - 库存预警列表
- ✅ POST `/api/stocks/alert/notify` - 发送库存预警通知

### 2. 内部服务接口 (/internal/stocks)

**StockFeignController** - 供其他服务调用

- ✅ POST `/internal/stocks/deduct` - 内部扣减库存(供order-service调用)
- ✅ POST `/internal/stocks/rollback` - 内部回滚库存
- ✅ GET `/internal/stocks/product/{productId}` - 查询商品可用库存

## 数据模型

### Stock (stocks表)
```sql
CREATE TABLE stocks (
  id BIGINT PRIMARY KEY,
  product_id BIGINT NOT NULL UNIQUE,       -- 商品ID
  total_stock INT NOT NULL DEFAULT 0,      -- 总库存
  available_stock INT NOT NULL DEFAULT 0,  -- 可用库存
  locked_stock INT NOT NULL DEFAULT 0,     -- 锁定库存
  sold_stock INT NOT NULL DEFAULT 0,       -- 已售库存
  low_stock_threshold INT DEFAULT 10,      -- 低库存预警阈值
  created_at DATETIME,
  updated_at DATETIME,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0                    -- 乐观锁版本号
);
```

### StockOperationResult (库存操作结果DTO)
```java
public class StockOperationResult {
    private Boolean success;              // 操作是否成功
    private String message;               // 结果消息
    private Long productId;               // 商品ID
    private Integer stockBefore;          // 操作前库存
    private Integer stockAfter;           // 操作后库存
    private Integer quantity;             // 操作数量
}
```

## 依赖服务

| 服务 | 用途 | 通信方式 |
|------|------|----------|
| order-service | 订单库存扣减/回滚 | RocketMQ异步 |
| product-service | 商品库存同步 | Feign同步 |
| Redis | 分布式锁、缓存 | Redisson |

## 消息队列配置

### RocketMQ Topics

**消费者:**
- `ORDER_TO_STOCK_TOPIC` - 订单库存扣减事件
- `STOCK_ROLLBACK_TOPIC` - 库存回滚事件

**生产者:**
- `STOCK_UPDATE_TOPIC` - 库存变更通知

## 并发控制机制

### 1. Redis 分布式锁
```java
@RedisLock(key = "stock:product:#{productId}")
public StockOperationResult deductStock(Long productId, Integer quantity)
```

### 2. 数据库乐观锁
```sql
UPDATE stocks
SET available_stock = available_stock - #{quantity},
    locked_stock = locked_stock + #{quantity},
    version = version + 1
WHERE product_id = #{productId}
  AND available_stock >= #{quantity}
  AND version = #{version}
```

### 3. 库存操作流程
1. 获取Redis分布式锁
2. 查询当前库存(带version)
3. 检查库存是否充足
4. 执行乐观锁更新
5. 更新失败则重试
6. 释放分布式锁

## 开发状态

### ✅ 已完成功能

1. **库存核心**
   - [x] 库存CRUD
   - [x] 库存扣减(Redis分布式锁)
   - [x] 库存回滚
   - [x] 批量扣减
   - [x] 低库存预警查询
   - [x] 库存预警列表
   - [x] 预警通知发送(StockAlertServiceImpl)

2. **并发控制**
   - [x] Redis分布式锁(@RedisLock注解)
   - [x] 数据库乐观锁(version字段)
   - [x] 双重锁机制保证一致性
   - [x] 扣减失败自动重试

3. **消息集成**
   - [x] RocketMQ消费者(监听订单事件)
   - [x] 库存变更事件发送
   - [x] 异步库存同步(StockAsyncServiceImpl)

4. **数据转换**
   - [x] StockConverter

5. **内部服务**
   - [x] Feign接口(供order-service调用)
   - [x] 库存注解服务(StockAnnotationServiceImpl)

### 📋 计划中功能

1. **库存预警增强**
   - [ ] 自动化预警规则配置
   - [ ] 多渠道预警通知(邮件/短信/钉钉)

2. **库存盘点**
   - [ ] 库存盘点功能
   - [ ] 盘盈盘亏处理

3. **库存日志**
   - [ ] 库存操作日志
   - [ ] 库存流水记录

## 本地运行

```bash
cd stock-service
mvn spring-boot:run
```

## 注意事项

### 高并发场景
- 使用Redis分布式锁避免超卖
- 乐观锁失败自动重试(最多3次)
- 库存不足时快速失败

### 库存一致性
- 库存扣减成功后异步通知product-service
- 订单取消自动回滚库存
- 支持幂等性处理

## 相关文档

- [API文档 - Stock Service](../doc/services/stock/API_DOC_STOCK_SERVICE.md)
- [项目整体文档](../doc/README.md)

## 快速链接

- Knife4j API文档: http://localhost:8085/doc.html
- Actuator Health: http://localhost:8085/actuator/health
