# 库存服务 (stock-service)

## 服务概述

库存服务是电商平台的核心服务之一，负责管理商品库存信息，提供库存查询、变更、冻结等操作。该服务支持同步和异步接口调用，以满足不同场景下的性能需求。

## 核心功能

1. 商品库存查询（同步/异步）
2. 库存分页查询
3. 批量库存查询
4. 库存统计信息查询
5. 高并发场景下的并发查询
6. 库存数量变更（增加、扣减、冻结、解冻）

## 技术栈

- Spring Boot 3.5.3
- Spring Cloud 2025.0.0
- Spring Cloud Alibaba 2023.0.3.3
- MyBatis-Plus 3.5.12
- Redis 8.2-rc1
- Nacos 3.0.2

## 服务接口

### 查询接口

#### 同步查询接口

1. `GET /stock/query/product/{productId}`
   - 根据商品ID获取库存信息
   - 返回库存VO对象

2. `POST /stock/query/page`
   - 分页查询库存列表
   - 接收分页参数和查询条件
   - 返回分页结果

3. `GET /stock/query/{id}`
   - 根据库存ID获取库存详情
   - 返回库存VO对象

#### 异步查询接口

1. `GET /stock/async/product/{productId}`
   - 异步根据商品ID查询库存
   - 使用CompletableFuture实现异步调用

2. `POST /stock/async/page`
   - 异步分页查询库存

3. `POST /stock/async/batch`
   - 异步批量查询库存

4. `GET /stock/async/statistics`
   - 异步查询库存统计信息

5. `POST /stock/async/concurrent`
   - 并发查询多个商品库存

### 库存操作接口

1. `POST /stock/count/increase/{productId}?count={count}`
   - 增加指定商品的库存数量

2. `POST /stock/count/reduce/{productId}?count={count}`
   - 扣减指定商品的库存数量

3. `POST /stock/count/freeze/{productId}?count={count}`
   - 冻结指定商品的库存数量

4. `POST /stock/count/unfreeze/{productId}?count={count}`
   - 解冻指定商品的库存数量

5. `POST /stock/count/batch`
   - 批量变更多个商品的库存数量

### Feign接口

库存服务通过Feign提供内部服务调用接口：

1. `GET /stocks/{productId}`
   - 查询指定商品的库存信息

2. `PUT /stocks/{productId}?quantity={quantity}`
   - 更新指定商品的库存数量

## 数据库设计

### 库存表 (tb_stock)

- `id`: 主键
- `product_id`: 商品ID
- `product_name`: 商品名称
- `stock_quantity`: 总库存数量
- `frozen_quantity`: 冻结库存数量
- `available_quantity`: 可用库存数量（虚拟字段，计算得出）
- `version`: 版本号（用于乐观锁）
- `stock_status`: 库存状态
- `create_time`: 创建时间
- `update_time`: 更新时间

## 缓存策略

使用Redis缓存热点库存数据，提高查询性能：
- 缓存键：`stock:product:{productId}`
- 缓存时间：30分钟
- 缓存更新策略：写操作后删除缓存，下次查询时重新加载

## 异步处理

使用CompletableFuture实现异步处理：
- 异步查询接口
- 超时控制（默认5秒）
- 异常处理机制
- 批量任务并发执行

## 监控与日志

- 集成Actuator监控端点
- 提供健康检查接口
- 完整的操作日志记录
- 性能指标收集

## 部署说明

1. 确保Nacos配置中心和注册中心已启动
2. 确保MySQL数据库和Redis服务已启动
3. 配置application.yml相关参数
4. 运行`java -jar stock-service.jar`启动服务

## 注意事项

1. 所有库存变更操作都使用乐观锁防止并发问题
2. 冻结库存用于订单处理过程中的库存预占
3. 可用库存 = 总库存 - 冻结库存
4. 所有接口都提供详细的日志记录便于问题排查