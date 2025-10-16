# Order Service (订单服务)

## 服务概述

Order Service 是电商平台的**核心交易服务**,负责订单全生命周期管理、退款处理和订单状态流转。通过RocketMQ与payment-service、stock-service异步协同完成分布式事务,支持订单创建、支付、发货、确认收货、退款等完整业务流程。

- **服务端口**: 8083
- **服务名称**: order-service
- **数据库**: MySQL (orders数据库)
- **消息队列**: RocketMQ (订单事件驱动)

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.3 | 应用框架 |
| MySQL | 9.3.0 | 数据持久化 |
| MyBatis Plus | 最新 | ORM框架 |
| Redis | - | 缓存、分布式锁 |
| Redisson | - | 分布式锁 |
| RocketMQ | - | 订单事件、分布式事务 |
| Seata | - | 分布式事务(AT模式) |
| Spring Cloud Alibaba | 2025.0.0.0-preview | 微服务组件 |
| MapStruct | 1.5.5.Final | DTO转换 |

## 核心功能

### 1. 订单管理 (/api/orders)

**OrderController** - 订单CRUD与流转

- ✅ POST `/api/orders` - 创建订单
- ✅ GET `/api/orders/{id}` - 查询订单详情
- ✅ GET `/api/orders` - 分页查询订单列表
- ✅ GET `/api/orders/user/{userId}` - 查询用户订单
- ✅ PUT `/api/orders/{id}/cancel` - 取消订单
- ✅ PUT `/api/orders/{id}/pay` - 确认支付
- ✅ PUT `/api/orders/{id}/deliver` - 确认发货
- ✅ PUT `/api/orders/{id}/receive` - 确认收货
- ✅ PUT `/api/orders/{id}/complete` - 完成订单
- ✅ GET `/api/orders/status/{status}` - 按状态查询订单

### 2. 退款管理 (/api/v1/refund)

**RefundController** - 完整的退款申请与处理流程

- ✅ POST `/api/v1/refund/create` - 创建退款申请(用户提交退款/退货申请)
- ✅ POST `/api/v1/refund/audit/{refundId}` - 审核退款申请(商家审批,支持通过/拒绝)
- ✅ POST `/api/v1/refund/cancel/{refundId}` - 取消退款申请(用户主动取消)
- ✅ GET `/api/v1/refund/{refundId}` - 查询退款详情
- ✅ GET `/api/v1/refund/order/{orderId}` - 查询订单所有退款记录
- ✅ GET `/api/v1/refund/user/list` - 查询用户的退款申请列表(分页)
- ✅ GET `/api/v1/refund/merchant/list` - 查询商家的退款申请列表(分页)
- ✅ POST `/api/v1/refund/process/{refundId}` - 处理退款(实际退款操作)

**退款状态流转**: PENDING(待审核) → APPROVED(审核通过) → PROCESSING(处理中) → COMPLETED(已完成) | REJECTED(已拒绝) | CANCELLED(已取消)

## 数据模型

### 核心实体

#### Order (orders表)
```sql
CREATE TABLE orders (
  id BIGINT PRIMARY KEY,
  order_no VARCHAR(50) UNIQUE NOT NULL,    -- 订单编号
  user_id BIGINT NOT NULL,                 -- 用户ID
  product_id BIGINT NOT NULL,              -- 商品ID
  product_name VARCHAR(200),               -- 商品名称
  product_price DECIMAL(10,2),             -- 商品价格
  quantity INT NOT NULL,                   -- 数量
  total_amount DECIMAL(10,2) NOT NULL,     -- 总金额
  status VARCHAR(20) NOT NULL,             -- PENDING/PAID/DELIVERED/COMPLETED/CANCELLED
  refund_status VARCHAR(20),               -- 退款状态
  payment_id BIGINT,                       -- 支付ID
  delivery_address TEXT,                   -- 收货地址
  remark TEXT,                             -- 备注
  created_at DATETIME,
  updated_at DATETIME,
  deleted TINYINT DEFAULT 0,
  version INT DEFAULT 0
);
```

#### Refund (refunds表)
```sql
CREATE TABLE refunds (
  id BIGINT PRIMARY KEY,
  refund_no VARCHAR(50) UNIQUE NOT NULL,   -- 退款编号
  order_id BIGINT NOT NULL,                -- 订单ID
  order_no VARCHAR(50),                    -- 订单编号
  user_id BIGINT NOT NULL,                 -- 用户ID
  refund_amount DECIMAL(10,2) NOT NULL,    -- 退款金额
  refund_reason VARCHAR(500),              -- 退款原因
  status VARCHAR(20) NOT NULL,             -- PENDING/APPROVED/REJECTED/COMPLETED
  reject_reason VARCHAR(500),              -- 拒绝原因
  approved_by BIGINT,                      -- 审批人
  approved_at DATETIME,                    -- 审批时间
  created_at DATETIME,
  updated_at DATETIME
);
```

## 依赖服务

| 服务 | 用途 | 通信方式 |
|------|------|----------|
| payment-service | 支付处理 | RocketMQ异步 |
| stock-service | 库存扣减/回滚 | RocketMQ异步 |
| product-service | 商品信息查询 | Feign同步 |
| user-service | 用户地址查询 | Feign同步 |

## 消息队列配置

### RocketMQ Topics

**生产者:**
- `ORDER_TO_PAYMENT_TOPIC` - 订单到支付事件
- `ORDER_TO_STOCK_TOPIC` - 订单到库存事件
- `order-events` - 通用订单事件

**消费者:**
- `ORDER_CREATE_TOPIC` - 订单创建事件
- `ORDER_CANCEL_TOPIC` - 订单取消事件
- `ORDER_PAY_TOPIC` - 订单支付事件
- `STOCK_UPDATE_TOPIC` - 库存更新事件

## 开发状态

### ✅ 已完成功能

1. **订单核心流程**
   - [x] 订单创建与保存
   - [x] 订单状态流转(6种状态)
   - [x] 订单取消逻辑
   - [x] 订单支付确认
   - [x] 订单发货/收货
   - [x] 订单完成
   - [x] 按状态查询订单
   - [x] 用户订单列表查询

2. **退款完整流程** ✨ 新增
   - [x] 用户提交退款申请(支持退款/退货类型)
   - [x] 商家审核退款(通过/拒绝+备注)
   - [x] 用户取消退款申请
   - [x] 退款详情查询
   - [x] 订单退款记录查询
   - [x] 用户退款列表(分页)
   - [x] 商家退款列表(分页)
   - [x] 退款处理与状态流转
   - [x] 退款通知机制

3. **分布式协同**
   - [x] RocketMQ异步消息
   - [x] 订单-支付事件
   - [x] 订单-库存事件
   - [x] 事件消费者
   - [x] 退款事件发送

4. **数据转换**
   - [x] MapStruct自动转换
   - [x] OrderConverter
   - [x] RefundConverter

5. **业务增强**
   - [x] 订单超时处理服务
   - [x] 订单锁定机制
   - [x] 订单导出功能

### 🚧 进行中功能

1. **分布式事务**
   - [ ] Seata AT模式完整集成
   - [ ] 事务补偿机制
   - [ ] 事务日志记录

### 📋 计划中功能

1. **订单超时**
   - [ ] 自动取消未支付订单
   - [ ] 超时关单通知

2. **订单导出**
   - [ ] Excel导出
   - [ ] 批量导出

### ⚠️ 技术债

- 增加事务失败重试机制
- 完善订单状态机
- 增加单元测试覆盖

## 本地运行

### 前置条件
```bash
cd docker
docker-compose up -d mysql redis nacos rocketmq
```

### 启动服务
```bash
cd order-service
mvn spring-boot:run
```

### 验证服务
```bash
curl http://localhost:8083/actuator/health
```

## 注意事项

### 分布式事务
- 订单创建会触发库存扣减和支付事件
- 订单取消会触发库存回滚
- 使用RocketMQ保证最终一致性

### 订单状态
- PENDING(待支付) → PAID(已支付) → DELIVERED(已发货) → COMPLETED(已完成)
- CANCELLED(已取消) 可从PENDING状态转换

## 相关文档

- [API文档 - Order Service](../doc/services/order/API_DOC_ORDER_SERVICE.md)
- [退款流程指南](../doc/services/order/REFUND_FLOW_GUIDE.md)
- [项目整体文档](../doc/README.md)

## 快速链接

- Knife4j API文档: http://localhost:8083/doc.html
- Actuator Health: http://localhost:8083/actuator/health
