# 支付服务 (payment-service)

## 服务概述

支付服务是电商平台的核心服务之一，负责处理所有支付相关业务，包括支付订单创建、支付状态查询、支付回调处理、支付通知等功能。该服务集成了Redis缓存和异步处理能力，确保高性能和高可用性。

## 核心功能

1. 支付订单管理（创建、查询、更新、取消）
2. 支付流水记录和查询
3. 第三方支付平台集成
4. 支付回调处理
5. 支付状态同步和通知
6. 支付统计和报表
7. 高并发场景下的异步处理

## 技术栈

- Spring Boot 3.5.3
- Spring Cloud 2025.0.0
- Spring Cloud Alibaba 2023.0.3.3
- MyBatis-Plus 3.5.13
- Redis 单机缓存
- 自定义线程池配置

## Redis缓存功能

### 缓存注解

支付服务实现了与stock服务相同的单Redis缓存注解：

#### @RedisCacheable

用于缓存查询结果，支持SpEL表达式和条件缓存：

```java
@RedisCacheable(
    cacheName = "payment", 
    key = "#paymentId", 
    expire = 1800,
    timeUnit = TimeUnit.SECONDS
)
public PaymentVO getPaymentById(Long paymentId) {
    // 方法实现
}
```

#### @RedisCachePut

用于更新缓存，总是执行方法并更新缓存：

```java
@RedisCachePut(
    cacheName = "payment", 
    key = "#result.paymentId",
    expire = 1800,
    timeUnit = TimeUnit.SECONDS
)
public PaymentVO updatePayment(PaymentUpdateDTO updateDTO) {
    // 方法实现
}
```

#### @RedisCacheEvict

用于删除缓存，支持条件删除和批量删除：

```java
@RedisCacheEvict(
    cacheName = "payment", 
    key = "#paymentId"
)
public void deletePayment(Long paymentId) {
    // 方法实现
}
```

### 缓存配置

- **缓存键前缀**: `payment-cache:`
- **默认过期时间**: 30分钟（1800秒）
- **支持SpEL表达式**: 支持复杂的键生成和条件判断
- **序列化方式**: JSON序列化

### 缓存策略

- **支付信息缓存**: `payment-cache:payment:{paymentId}`，过期时间30分钟
- **支付流水缓存**: `payment-cache:flow:{flowId}`，过期时间15分钟
- **用户支付列表缓存**: `payment-cache:userPayments:{userId}`，过期时间10分钟
- **支付统计缓存**: `payment-cache:statistics:{date}`，过期时间2小时

## 异步线程池配置

### 线程池类型

支付服务配置了多个专用线程池：

#### 1. 支付查询线程池 (paymentQueryExecutor)

```java
核心线程数: max(4, CPU核心数)
最大线程数: CPU核心数 × 3
队列容量: 300
线程名前缀: payment-query-
```

#### 2. 支付操作线程池 (paymentOperationExecutor)

```java
核心线程数: 3
最大线程数: 12
队列容量: 150
线程名前缀: payment-operation-
```

#### 3. 支付回调线程池 (paymentCallbackExecutor)

```java
核心线程数: 2
最大线程数: 8
队列容量: 100
线程名前缀: payment-callback-
```

#### 4. 支付通知线程池 (paymentNotificationExecutor)

```java
核心线程数: 2
最大线程数: 6
队列容量: 80
线程名前缀: payment-notification-
```

#### 5. 通用异步线程池 (paymentCommonAsyncExecutor)

```java
继承自BaseAsyncConfig的通用配置
核心线程数: 4
最大线程数: 8
队列容量: 100
线程名前缀: common-async-
```

### 异步方法使用示例

```java
@Async("paymentQueryExecutor")
public CompletableFuture<PaymentVO> queryPaymentAsync(Long paymentId) {
    PaymentVO payment = getPaymentById(paymentId);
    return CompletableFuture.completedFuture(payment);
}

@Async("paymentCallbackExecutor")
public void processPaymentCallback(PaymentCallbackDTO callback) {
    // 处理支付回调逻辑
}

@Async("paymentNotificationExecutor")  
public void sendPaymentNotification(PaymentNotificationDTO notification) {
    // 发送支付通知
}
```

## 服务接口

### 查询接口

1. `GET /payment/{paymentId}` - 根据支付ID查询支付信息
2. `GET /payment/order/{orderId}` - 根据订单ID查询支付信息
3. `POST /payment/page` - 分页查询支付记录
4. `GET /payment/flow/{flowId}` - 查询支付流水详情

### 操作接口

1. `POST /payment/create` - 创建支付订单
2. `PUT /payment/{paymentId}` - 更新支付状态
3. `POST /payment/cancel/{paymentId}` - 取消支付
4. `POST /payment/callback` - 处理支付回调

### 异步接口

1. `GET /payment/async/{paymentId}` - 异步查询支付信息
2. `POST /payment/async/batch` - 异步批量查询支付信息
3. `POST /payment/async/statistics` - 异步查询支付统计

## 数据库设计

### 支付表 (tb_payment)

- `id`: 主键
- `payment_id`: 支付单号
- `order_id`: 关联订单ID
- `user_id`: 用户ID
- `amount`: 支付金额
- `payment_method`: 支付方式
- `payment_status`: 支付状态
- `third_party_id`: 第三方支付单号
- `callback_time`: 回调时间
- `create_time`: 创建时间
- `update_time`: 更新时间

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

### 支付分页查询实现

#### 控制器层实现

```java
@PostMapping("/page")
public Result<Page<PaymentDTO>> getPayments(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) Integer channel) {
    try {
        // 1. 构造分页对象
        Page<Payment> paymentPage = new Page<>(page, size);
        
        // 2. 构造查询条件
        LambdaQueryWrapper<Payment> queryWrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq(Payment::getUserId, userId);
        }
        if (status != null) {
            queryWrapper.eq(Payment::getStatus, status);
        }
        if (channel != null) {
            queryWrapper.eq(Payment::getChannel, channel);
        }
        queryWrapper.orderByDesc(Payment::getCreatedAt);
        
        // 3. 执行分页查询
        Page<Payment> resultPage = paymentService.page(paymentPage, queryWrapper);
        
        // 4. 转换为DTO
        Page<PaymentDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<PaymentDTO> dtoList = paymentConverter.toDTOList(resultPage.getRecords());
        dtoPage.setRecords(dtoList);
        
        return Result.success(dtoPage);
    } catch (Exception e) {
        log.error("分页查询支付记录异常", e);
        return Result.error("分页查询支付记录失败: " + e.getMessage());
    }
}
```

### 分页查询性能优化

1. **缓存策略**：对于热点查询条件的分页结果进行缓存
2. **索引优化**：为常用查询字段建立索引，如(user_id, status, created_at)
3. **异步分页**：对于大数据量分页查询，使用专用的查询线程池
4. **参数限制**：限制分页参数的合理范围，防止深度分页

### 支付流水表 (tb_payment_flow)

- `id`: 主键
- `flow_id`: 流水号
- `payment_id`: 关联支付ID
- `flow_type`: 流水类型
- `amount`: 金额
- `status`: 状态
- `remark`: 备注
- `create_time`: 创建时间

## 使用说明

### 1. 启用Redis缓存

在Service类上使用缓存注解：

```java
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @RedisCacheable(cacheName = "payment", key = "#paymentId")
    @Override
    public PaymentVO getPaymentById(Long paymentId) {
        // 实现逻辑
    }
    
    @RedisCacheEvict(cacheName = "payment", key = "#paymentId")
    @Override 
    public void deletePayment(Long paymentId) {
        // 实现逻辑
    }
}
```

### 2. 使用异步处理

在Controller或Service中使用异步方法：

```java
@RestController
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @GetMapping("/async/{paymentId}")
    public CompletableFuture<Result<PaymentVO>> getPaymentAsync(@PathVariable Long paymentId) {
        return paymentService.queryPaymentAsync(paymentId)
                .thenApply(payment -> Result.success(payment));
    }
}
```

### 3. 配置自定义线程池

如需要自定义线程池配置，可以重写配置方法：

```java
@Configuration
public class CustomPaymentAsyncConfig extends PaymentAsyncConfig {
    
    @Bean("customPaymentExecutor")
    @Override
    public Executor paymentOperationExecutor() {
        return createThreadPoolTaskExecutor(5, 20, 200, "custom-payment-");
    }
}
```

## 监控与日志

- 所有缓存操作都有详细的DEBUG级别日志
- 线程池状态可通过Actuator端点监控
- 异步任务执行情况可通过日志跟踪
- 支付相关操作都有完整的审计日志

## 注意事项

1. 缓存注解必须在Spring管理的Bean中使用
2. 异步方法不能在同一个类中调用，需要通过依赖注入调用
3. Redis连接配置需要在application.yml中正确配置
4. 线程池配置需要根据实际业务量调整
5. 支付回调处理需要保证幂等性
6. 缓存更新和删除需要与业务操作保持一致性
