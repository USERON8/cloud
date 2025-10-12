# 异步配置使用指南

## 概述

本文档提供了Spring Cloud微服务系统中异步配置的使用指南，包括配置方法、最佳实践和常见问题解决方案。

## 快速开始

### 1. 基本配置

#### 1.1 启用异步功能
```yaml
# application.yml
app:
  async:
    enabled: true
```

#### 1.2 服务配置类
```java
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "app.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig extends BaseAsyncConfig {

    @Bean("businessExecutor")
    public Executor businessExecutor() {
        ThreadPoolTaskExecutor executor = createWriteExecutor("business-");
        executor.initialize();

        log.info("✅ [BUSINESS-ASYNC] 线程池初始化完成");
        return executor;
    }
}
```

### 2. 使用异步方法

#### 2.1 基本异步方法
```java
@Service
public class BusinessService {

    @Async("businessExecutor")
    public CompletableFuture<Void> processAsyncTask() {
        // 异步业务逻辑
        return CompletableFuture.completedFuture(null);
    }
}
```

#### 2.2 带返回值的异步方法
```java
@Async("businessExecutor")
public CompletableFuture<String> processAsyncTaskWithResult() {
    // 异步处理逻辑
    String result = "处理结果";
    return CompletableFuture.completedFuture(result);
}
```

#### 2.3 异步方法中的异常处理
```java
@Async("businessExecutor")
public CompletableFuture<String> processAsyncTaskWithException() {
    try {
        // 异步处理逻辑
        return CompletableFuture.completedFuture("成功结果");
    } catch (Exception e) {
        log.error("异步任务执行失败", e);
        return CompletableFuture.failedFuture(e);
    }
}
```

## 配置详解

### 1. 线程池类型

#### 1.1 查询型线程池
```java
// 适用于高并发查询场景
@Bean("queryExecutor")
public Executor queryExecutor() {
    return createQueryExecutor("query-");
}

// 配置参数
// 核心线程数: CPU核心数
// 最大线程数: CPU核心数×2
// 队列容量: 500
```

#### 1.2 写操作线程池
```java
// 适用于写操作，控制并发度
@Bean("writeExecutor")
public Executor writeExecutor() {
    return createWriteExecutor("write-");
}

// 配置参数
// 核心线程数: 2
// 最大线程数: 8
// 队列容量: 200
```

#### 1.3 IO密集型线程池
```java
// 适用于文件上传、网络请求等
@Bean("ioExecutor")
public Executor ioExecutor() {
    return createIOExecutor("io-");
}

// 配置参数
// 核心线程数: CPU×2
// 最大线程数: CPU×4
// 队列容量: 300
```

#### 1.4 CPU密集型线程池
```java
// 适用于计算密集型任务
@Bean("cpuExecutor")
public Executor cpuExecutor() {
    return createCPUExecutor("cpu-");
}

// 配置参数
// 核心线程数: CPU核心数
// 最大线程数: CPU核心数+1
// 队列容量: 100
```

### 2. 配置文件详解

#### 2.1 完整配置示例
```yaml
app:
  async:
    # 全局开关
    enabled: true

    # 服务特定配置
    services:
      user:
        async:
          # 查询线程池
          query:
            core-pool-size: 4
            max-pool-size: 16
            queue-capacity: 500
            keep-alive-seconds: 60
            thread-name-prefix: "user-query-"

          # 操作线程池
          operation:
            core-pool-size: 2
            max-pool-size: 8
            queue-capacity: 200

          # 日志线程池
          log:
            core-pool-size: 2
            max-pool-size: 4
            queue-capacity: 800

          # 统计线程池
          statistics:
            core-pool-size: 2
            max-pool-size: 4
            queue-capacity: 500

          # 通知线程池
          notification:
            core-pool-size: 2
            max-pool-size: 6
            queue-capacity: 300

    # 通用配置
    common:
      # 监控配置
      monitoring-enabled: true
      monitoring-interval-seconds: 30

      # 任务装饰器
      task-decorator: true

      # 慢任务记录
      log-slow-tasks: true
      slow-task-threshold-ms: 3000

      # 告警阈值
      alert-threshold-usage-rate: 80.0
      alert-threshold-queue-rate: 85.0
```

## 监控和告警

### 1. 线程池监控

#### 1.1 启用监控
```yaml
app:
  async:
    common:
      monitoring-enabled: true
      monitoring-interval-seconds: 30
```

#### 1.2 监控指标
- **活跃线程数**: 当前正在执行任务的线程数
- **线程池使用率**: 活跃线程数/最大线程数
- **队列使用率**: 队列当前大小/队列容量
- **完成任务数**: 已完成的任务总数
- **任务执行速率**: 每秒完成的任务数

#### 1.3 查看监控数据
```java
@Autowired
private EnhancedThreadPoolMonitor monitor;

// 获取所有线程池状态
Map<String, ThreadPoolInfo> pools = monitor.getAllThreadPoolInfo();

// 获取健康状态
ThreadPoolHealthStatus health = monitor.checkThreadPoolHealth();

// 获取性能统计
Map<String, ThreadPoolPerformanceStats> stats = monitor.getPerformanceStats();
```

### 2. Prometheus集成

#### 2.1 配置指标导出
```yaml
management:
  endpoints:
    web:
      exposure:
        include: threadpool
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 2.2 可用指标
- `threadpool.active.threads`: 活跃线程数
- `threadpool.queue.size`: 队列大小
- `threadpool.usage.rate`: 线程池使用率
- `threadpool.queue.usage.rate`: 队列使用率
- `threadpool.completed.tasks`: 完成任务数

## 最佳实践

### 1. 线程池设计原则

#### 1.1 合理的线程池数量
- **避免过多线程池**: 每个服务建议3-5个专用线程池
- **功能分离**: 按业务功能分离线程池（查询、操作、通知等）
- **资源控制**: 控制总线程数，避免资源竞争

#### 1.2 合理的队列大小
- **查询类**: 500-1000，容忍一定延迟
- **操作类**: 200-500，避免内存压力
- **通知类**: 300-500，平衡性能和资源
- **统计类**: 500-800，允许积压

### 2. 异步使用原则

#### 2.1 适合异步的场景
- **通知发送**: 邮件、短信、推送通知
- **日志记录**: 操作日志、审计日志
- **数据统计**: 用户行为统计、业务指标计算
- **文件处理**: 文件上传、图片处理
- **第三方调用**: 外部API调用、webhook通知

#### 2.2 不适合异步的场景
- **需要实时返回结果的操作**
- **事务性操作**: 需要保证事务一致性的写操作
- **顺序敏感操作**: 需要严格按顺序执行的操作

### 3. 异常处理

#### 3.1 异步方法异常处理
```java
@Async("businessExecutor")
public CompletableFuture<Void> asyncMethod() {
    try {
        // 业务逻辑
        businessLogic();
        return CompletableFuture.completedFuture(null);
    } catch (BusinessException e) {
        // 业务异常记录日志
        log.error("异步业务处理失败: {}", e.getMessage());
        return CompletableFuture.failedFuture(e);
    } catch (Exception e) {
        // 系统异常记录详细日志
        log.error("异步处理系统异常", e);
        return CompletableFuture.failedFuture(e);
    }
}
```

#### 3.2 异步调用异常处理
```java
public void callAsyncMethod() {
    asyncMethod()
        .thenAccept(result -> {
            // 成功处理
            log.info("异步处理成功: {}", result);
        })
        .exceptionally(throwable -> {
            // 异常处理
            log.error("异步处理失败", throwable);
            return null;
        });
}
```

### 4. 性能优化

#### 4.1 合理配置线程数
```java
// 获取CPU核心数
int processors = Runtime.getRuntime().availableProcessors();

// 查询型线程池（IO密集型）
int queryCore = Math.max(4, processors);
int queryMax = processors * 2;

// 计算型线程池（CPU密集型）
int cpuCore = processors;
int cpuMax = processors + 1;
```

#### 4.2 避免线程泄漏
```java
@Async("businessExecutor")
public void asyncMethod() {
    try {
        // 业务逻辑
    } finally {
        // 确保资源清理
        clearResources();
    }
}
```

## 常见问题

### 1. 线程池满载

#### 1.1 症状
- 任务执行缓慢
- 线程池使用率持续90%以上
- 队列积压严重

#### 1.2 解决方案
```yaml
# 增加最大线程数
app:
  async:
    services:
      user:
        async:
          query:
            max-pool-size: 20  # 从16增加到20
            queue-capacity: 800  # 从500增加到800
```

#### 1.3 监控和告警
```yaml
app:
  async:
    common:
      alert-threshold-usage-rate: 70.0  # 从80%降低到70%
      alert-threshold-queue-rate: 75.0   # 从85%降低到75%
```

### 2. 内存泄漏

#### 2.1 症状
- 内存使用持续增长
- GC频繁但回收效果差
- 系统响应变慢

#### 2.2 解决方案
- 检查队列大小是否过大
- 确保异步任务正确释放资源
- 监控线程池队列使用情况

### 3. 任务丢失

#### 2.1 症状
- 异步任务没有执行
- 业务数据不一致

#### 2.2 解决方案
```java
// 使用CallerRunsPolicy拒绝策略
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

// 或者增加队列容量
executor.setQueueCapacity(1000);
```

## 性能测试

### 1. 压力测试脚本
```java
@Test
public void testAsyncPerformance() {
    // 模拟高并发异步调用
    ExecutorService executor = Executors.newFixedThreadPool(100);
    CountDownLatch latch = new CountDownLatch(1000);

    long startTime = System.currentTimeMillis();

    for (int i = 0; i < 1000; i++) {
        executor.submit(() -> {
            try {
                asyncService.processAsync();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    long endTime = System.currentTimeMillis();

    log.info("1000个异步任务执行时间: {} ms", endTime - startTime);
}
```

### 2. 监控指标
- **吞吐量**: 每秒处理的任务数
- **响应时间**: P50, P95, P99响应时间
- **资源使用**: CPU、内存使用率
- **错误率**: 任务执行失败率

## 总结

通过合理配置异步线程池，可以显著提���系统性能和吞吐量。关键要点：

1. **合理设计**: 按业务功能分离线程池
2. **合适配置**: 根据业务特点调整线程数和队列大小
3. **完善监控**: 实时监控线程池状态和性能指标
4. **及时告警**: 设置合理的告警阈值
5. **持续优化**: 根据监控数据持续优化配置

遵循本文档的指导原则和最佳实践，可以构建高性能、高可靠的异步处理系统。