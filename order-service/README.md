# 订单服务 (order-service)

## 服务概述

订单服务是电商平台的核心服务之一，负责处理所有订单相关业务，包括订单创建、状态流转、订单查询、订单统计等功能。该服务集成了Redis缓存和异步处理能力，支持高并发场景下的订单处理。

## 核心功能

1. 订单生命周期管理（创建、支付、发货、完成、取消）
2. 订单详情查询和列表查询
3. 订单状态流转和业务规则验证
4. 订单统计和报表生成
5. 订单项管理
6. 异步订单处理和通知
7. 高并发场景下的订单操作

## 技术栈

- Spring Boot 3.5.3
- Spring Cloud 2025.0.0
- Spring Cloud Alibaba 2023.0.3.3
- MyBatis-Plus 3.5.13
- Redis 单机缓存
- 自定义线程池配置

## Redis缓存功能

### 缓存注解

订单服务实现了与stock服务相同的单Redis缓存注解：

#### @RedisCacheable

用于缓存查询结果，支持SpEL表达式和条件缓存：

```java
@RedisCacheable(
    cacheName = "order", 
    key = "#orderId", 
    expire = 900,
    timeUnit = TimeUnit.SECONDS
)
public OrderVO getOrderById(Long orderId) {
    // 方法实现
}
```

#### @RedisCachePut

用于更新缓存，总是执行方法并更新缓存：

```java
@RedisCachePut(
    cacheName = "order", 
    key = "#result.orderId",
    expire = 900,
    timeUnit = TimeUnit.SECONDS
)
public OrderVO updateOrderStatus(Long orderId, OrderStatus status) {
    // 方法实现
}
```

#### @RedisCacheEvict

用于删除缓存，支持条件删除和批量删除：

```java
@RedisCacheEvict(
    cacheName = "order", 
    key = "#orderId"
)
public void deleteOrder(Long orderId) {
    // 方法实现
}
```

### 缓存配置

- **缓存键前缀**: `order-cache:`
- **默认过期时间**: 15分钟（900秒）
- **支持SpEL表达式**: 支持复杂的键生成和条件判断
- **序列化方式**: JSON序列化

### 缓存策略

- **订单信息缓存**: `order-cache:order:{orderId}`，过期时间15分钟
- **用户订单列表缓存**: `order-cache:userOrders:{userId}`，过期时间10分钟
- **订单统计缓存**: `order-cache:statistics:{date}`，过期时间1小时
- **订单状态缓存**: `order-cache:status:{orderId}`，过期时间5分钟

## 异步线程池配置

### 线程池类型

订单服务配置了多个专用线程池：

#### 1. 订单查询线程池 (orderQueryExecutor)

```java
核心线程数: max(4, CPU核心数)
最大线程数: CPU核心数 × 4
队列容量: 500
线程名前缀: order-query-
```

#### 2. 订单操作线程池 (orderOperationExecutor)

```java
核心线程数: 2
最大线程数: 10
队列容量: 100
线程名前缀: order-operation-
```

#### 3. 通用异步线程池 (orderCommonAsyncExecutor)

```java
继承自BaseAsyncConfig的通用配置
核心线程数: 4
最大线程数: 8
队列容量: 100
线程名前缀: common-async-
```

### 异步方法使用示例

```java
@Async("orderQueryExecutor")
public CompletableFuture<List<OrderVO>> queryUserOrdersAsync(Long userId) {
    List<OrderVO> orders = getUserOrders(userId);
    return CompletableFuture.completedFuture(orders);
}

@Async("orderOperationExecutor")
public void processOrderStatusChange(Long orderId, OrderStatus newStatus) {
    // 处理订单状态变更逻辑
}

@Async("orderCommonAsyncExecutor")  
public void sendOrderNotification(OrderNotificationDTO notification) {
    // 发送订单通知
}
```

## 服务接口

### 查询接口

1. `GET /order/{orderId}` - 根据订单ID查询订单详情
2. `GET /order/user/{userId}` - 查询用户订单列表
3. `POST /order/page` - 分页查询订单
4. `GET /order/status/{status}` - 根据状态查询订单
5. `GET /order/statistics` - 查询订单统计信息

### 操作接口

1. `POST /order/create` - 创建订单
2. `PUT /order/{orderId}/status` - 更新订单状态
3. `POST /order/{orderId}/cancel` - 取消订单
4. `POST /order/{orderId}/pay` - 订单支付
5. `POST /order/{orderId}/deliver` - 订单发货
6. `POST /order/{orderId}/complete` - 订单完成

### 异步接口

1. `GET /order/async/{orderId}` - 异步查询订单详情
2. `POST /order/async/batch` - 异步批量查询订单
3. `POST /order/async/statistics` - 异步查询订单统计

## 数据库设计

### 订单表 (tb_order)

- `id`: 主键
- `order_id`: 订单号
- `user_id`: 用户ID
- `total_amount`: 订单总金额
- `order_status`: 订单状态
- `payment_method`: 支付方式
- `delivery_address`: 收货地址
- `create_time`: 创建时间
- `update_time`: 更新时间
- `version`: 版本号

### 订单项表 (tb_order_item)

- `id`: 主键
- `order_id`: 关联订单ID
- `product_id`: 商品ID
- `product_name`: 商品名称
- `quantity`: 数量
- `price`: 单价
- `total_price`: 总价
- `create_time`: 创建时间

## 分页查询实现

### MyBatis-Plus分页插件配置

项目使用MyBatis-Plus 3.5.13版本的分页插件，在`common-module`中统一配置：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // 分页插件（必须指定数据库类型）
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    // 乐观锁插件
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    // 防全表更新插件
    interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
    return interceptor;
}
```

### 订单分页查询实现

#### 服务层实现

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Page<OrderVO> pageQuery(OrderPageQueryDTO queryDTO) {
    try {
        // 1. 构造分页对象
        Page<Order> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        
        // 2. 执行分页查询（使用XML中定义的复杂查询）
        Page<Order> resultPage = this.baseMapper.pageQuery(page, queryDTO);
        
        // 3. 转换为VO对象
        Page<OrderVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(resultPage.getRecords().stream()
                .map(orderConverter::toVO)
                .toList());
                
        return voPage;
    } catch (Exception e) {
        log.error("分页查询订单失败: ", e);
        throw new OrderServiceException("分页查询订单失败: " + e.getMessage());
    }
}
```

#### Mapper层XML实现

```xml
<select id="pageQuery" resultType="com.cloud.order.module.entity.Order">
    SELECT o.*, oi.product_count, oi.total_amount as item_total
    FROM tb_order o
    LEFT JOIN (
        SELECT order_id, 
               COUNT(*) as product_count,
               SUM(quantity * price) as total_amount
        FROM tb_order_item 
        GROUP BY order_id
    ) oi ON o.id = oi.order_id
    <where>
        o.deleted = 0
        <if test="queryDTO.userId != null">
            AND o.user_id = #{queryDTO.userId}
        </if>
        <if test="queryDTO.orderStatus != null">
            AND o.order_status = #{queryDTO.orderStatus}
        </if>
        <if test="queryDTO.startTime != null">
            AND o.create_time >= #{queryDTO.startTime}
        </if>
        <if test="queryDTO.endTime != null">
            AND o.create_time &lt;= #{queryDTO.endTime}
        </if>
    </where>
    ORDER BY o.create_time DESC
</select>
```

### 分页查询最佳实践

1. **使用XML复杂查询**：对于复杂的关联查询，推荐使用XML文件实现
2. **缓存策略**：对于热点查询条件的分页结果进行缓存
3. **参数校验**：限制分页参数的合理范围
4. **索引优化**：为常用查询字段建立索引，如(user_id, order_status, create_time)

## 订单状态流转

```
待支付 -> 已支付 -> 待发货 -> 已发货 -> 已完成
   |                              ↑
   ↓                              |
 已取消 <----------------------- 已取消
```

### 状态说明

- **PENDING**: 待支付
- **PAID**: 已支付
- **PREPARING**: 待发货
- **SHIPPED**: 已发货
- **COMPLETED**: 已完成
- **CANCELLED**: 已取消

## 使用说明

### 1. 启用Redis缓存

在Service类上使用缓存注解：

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    @RedisCacheable(cacheName = "order", key = "#orderId")
    @Override
    public OrderVO getOrderById(Long orderId) {
        // 实现逻辑
    }
    
    @RedisCacheEvict(cacheName = "order", key = "#orderId")
    @Override
    public void deleteOrder(Long orderId) {
        // 实现逻辑
    }
    
    @RedisCachePut(cacheName = "order", key = "#orderId")
    @Override
    public OrderVO updateOrderStatus(Long orderId, OrderStatus status) {
        // 实现逻辑
    }
}
```

### 2. 使用异步处理

在Controller或Service中使用异步方法：

```java
@RestController
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @GetMapping("/async/{orderId}")
    public CompletableFuture<Result<OrderVO>> getOrderAsync(@PathVariable Long orderId) {
        return orderService.queryOrderAsync(orderId)
                .thenApply(order -> Result.success(order));
    }
}
```

### 3. 订单状态流转示例

```java
@Service
@RequiredArgsConstructor
public class OrderStatusService {
    
    @Async("orderOperationExecutor")
    public void processPayment(Long orderId) {
        // 处理支付逻辑
        updateOrderStatus(orderId, OrderStatus.PAID);
    }
    
    @Async("orderOperationExecutor") 
    public void processDelivery(Long orderId) {
        // 处理发货逻辑
        updateOrderStatus(orderId, OrderStatus.SHIPPED);
    }
}
```

### 4. 缓存策略配置

```java
@RedisCacheable(
    cacheName = "order",
    key = "#userId + ':page:' + #pageNum", 
    condition = "#pageNum <= 5",
    expire = 600,
    timeUnit = TimeUnit.SECONDS
)
public PageResult<OrderVO> getUserOrderPage(Long userId, int pageNum, int pageSize) {
    // 只缓存前5页数据，过期时间10分钟
}
```

## 业务规则

### 1. 订单创建规则

- 用户必须已登录
- 商品库存必须充足
- 收货地址必须有效
- 支付金额必须正确

### 2. 订单状态变更规则

- 只有待支付订单可以取消
- 只有已支付订单可以发货
- 只有已发货订单可以确认收货
- 状态变更必须按照流程进行

### 3. 订单异常处理

- 支付超时自动取消订单
- 发货超时提醒商家
- 确认收货超时自动完成订单

## 监控与日志

- 所有缓存操作都有详细的DEBUG级别日志
- 订单状态变更都有完整的审计日志
- 线程池状态可通过Actuator端点监控
- 异步任务执行情况可通过日志跟踪
- 订单关键操作都有性能指标记录

## 注意事项

1. 缓存注解必须在Spring管理的Bean中使用
2. 异步方法不能在同一个类中调用，需要通过依赖注入调用
3. 订单状态变更需要保证数据一致性
4. 缓存更新和删除需要与业务操作保持一致性
5. 订单创建需要考虑库存扣减的原子性
6. 高并发场景下需要注意订单重复创建问题
7. 订单查询缓存需要考虑实时性要求

## 性能优化建议

1. 对于热点订单数据，适当延长缓存时间
2. 订单列表查询使用分页缓存策略
3. 订单统计数据可以使用更长的缓存时间
4. 合理设置线程池大小，避免资源浪费
5. 使用批量查询减少数据库访问次数
