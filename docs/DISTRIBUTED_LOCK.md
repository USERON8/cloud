# 分布式锁文档

> 本文档介绍项目基于Redisson的分布式锁实现、使用方法和最佳实践

## 📋 目录

- [1. 架构概览](#1-架构概览)
- [2. 快速开始](#2-快速开始)
- [3. 注解方式使用](#3-注解方式使用)
- [4. 编程方式使用](#4-编程方式使用)
- [5. 锁类型](#5-锁类型)
- [6. 监控管理](#6-监控管理)
- [7. 最佳实践](#7-最佳实践)
- [8. 常见问题](#8-常见问题)

---

## 1. 架构概览

### 1.1 技术选型

项目使用 **Redisson** 实现分布式锁,Redisson是Redis官方推荐的Java客户端,提供了完整的分布式锁实现。

**技术栈:**
- Redisson 3.51.0
- Redis 7.x
- Spring AOP (声明式锁)

### 1.2 核心组件

```
┌─────────────────────────────────────────┐
│          Application Layer              │
│      (@DistributedLock注解)             │
└──────────────┬──────────────────────────┘
               │
┌──────────────┴──────────────────────────┐
│     DistributedLockAspect (AOP)         │
│  (拦截注解,处理锁逻辑)                   │
└──────────────┬──────────────────────────┘
               │
     ┌─────────┴─────────┐
     │                   │
┌────┴────────┐  ┌───────┴────────────┐
│ Redisson    │  │ DistributedLock    │
│ LockManager │  │ Template           │
│ (Redisson)  │  │ (RedisTemplate)    │
└─────────────┘  └────────────────────┘
               │
     ┌─────────┴─────────┐
     │      Redis        │
     └───────────────────┘
```

### 1.3 锁特性

| 特性 | 说明 |
|-----|------|
| **可重入** | 同一线程可多次获取同一锁 |
| **自动续期** | Redisson Watch Dog机制自动延长锁时间 |
| **公平锁** | 支持按请求顺序获取锁 |
| **读写锁** | 支持读写分离,多读单写 |
| **红锁** | 支持多Redis实例的高可用锁 |
| **尝试锁** | 支持非阻塞获取锁 |
| **自动释放** | 方法执行完自动释放锁 |

---

## 2. 快速开始

### 2.1 添加依赖

在 `common-module/pom.xml` 中已包含:

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.51.0</version>
</dependency>
```

### 2.2 配置Redisson

在 `application-common.yml` 中配置:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 5

# Redisson配置(可选)
cloud:
  redisson:
    enabled: true           # 启用Redisson
    threads: 16             # 线程池大小
    netty-threads: 32       # Netty线程池大小
    codec: json             # 编解码器(json/fst)
```

### 2.3 基础使用

```java
@Service
public class ProductService {

    // 使用注解方式
    @DistributedLock(
        key = "'product:' + #productId",
        waitTime = 5,
        leaseTime = 10
    )
    public void updateProduct(Long productId, ProductDTO dto) {
        // 业务逻辑自动在锁保护下执行
        productMapper.updateById(dto);
    }
}
```

---

## 3. 注解方式使用

### 3.1 @DistributedLock 注解

**基本用法:**

```java
@DistributedLock(
    key = "'user:' + #userId",              // 锁键(支持SpEL)
    waitTime = 3,                           // 等待获取锁的时间(秒)
    leaseTime = 10,                         // 锁持有时间(秒)
    lockType = LockType.REENTRANT,         // 锁类型
    failStrategy = LockFailStrategy.THROW_EXCEPTION  // 失败策略
)
public void updateUser(Long userId, UserDTO dto) {
    // 业务逻辑
}
```

**注解参数说明:**

| 参数 | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| `key` | String | 必填 | 锁键,支持SpEL表达式 |
| `prefix` | String | "" | 锁前缀 |
| `waitTime` | long | 3 | 等待获取锁的时间(秒) |
| `leaseTime` | long | 10 | 锁自动释放时间(秒) |
| `timeUnit` | TimeUnit | SECONDS | 时间单位 |
| `lockType` | LockType | REENTRANT | 锁类型 |
| `failStrategy` | LockFailStrategy | THROW_EXCEPTION | 失败策略 |
| `failMessage` | String | "获取分布式锁失败" | 失败消息 |
| `autoRelease` | boolean | true | 是否自动释放 |

### 3.2 SpEL表达式

支持强大的SpEL表达式生成锁键:

```java
// 1. 简单参数
@DistributedLock(key = "'user:' + #userId")
public void updateUser(Long userId) { }

// 2. 对象属性
@DistributedLock(key = "'order:' + #order.id")
public void createOrder(Order order) { }

// 3. 多参数组合
@DistributedLock(key = "'user:' + #userId + ':type:' + #type")
public void updateUserType(Long userId, String type) { }

// 4. 集合参数
@DistributedLock(key = "'batch:' + T(String).join(',', #ids)")
public void batchUpdate(List<Long> ids) { }

// 5. 方法调用
@DistributedLock(key = "'product:' + #product.getCategoryId()")
public void updateProduct(Product product) { }
```

### 3.3 锁类型

```java
// 可重入锁(默认)
@DistributedLock(
    key = "'user:' + #userId",
    lockType = LockType.REENTRANT
)
public void updateUser(Long userId) { }

// 公平锁
@DistributedLock(
    key = "'order:' + #orderId",
    lockType = LockType.FAIR
)
public void processOrder(Long orderId) { }

// 读锁
@DistributedLock(
    key = "'product:' + #productId",
    lockType = LockType.READ
)
public ProductDTO getProduct(Long productId) { }

// 写锁
@DistributedLock(
    key = "'product:' + #productId",
    lockType = LockType.WRITE
)
public void updateProduct(Long productId) { }
```

### 3.4 失败策略

```java
// 抛出异常(默认)
@DistributedLock(
    key = "'stock:' + #productId",
    failStrategy = LockFailStrategy.THROW_EXCEPTION,
    failMessage = "库存操作繁忙,请稍后重试"
)
public void deductStock(Long productId, Integer quantity) { }

// 返回null
@DistributedLock(
    key = "'cache:' + #key",
    failStrategy = LockFailStrategy.RETURN_NULL
)
public String getCachedValue(String key) { }

// 返回默认值
@DistributedLock(
    key = "'counter:' + #id",
    failStrategy = LockFailStrategy.RETURN_DEFAULT
)
public int getCounter(Long id) {
    // 失败返回0
}

// 快速失败
@DistributedLock(
    key = "'task:' + #taskId",
    failStrategy = LockFailStrategy.FAIL_FAST,
    waitTime = 0
)
public void executeTask(Long taskId) { }
```

---

## 4. 编程方式使用

### 4.1 使用RedissonLockManager

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final RedissonLockManager lockManager;

    public void processOrder(Long orderId) {
        // 方式1: 自动管理锁
        String result = lockManager.executeWithLock(
            "order:" + orderId,
            3, 10, TimeUnit.SECONDS,
            () -> {
                // 业务逻辑
                return "success";
            }
        );

        // 方式2: 手动管理锁
        RedissonLockInfo lockInfo = lockManager.tryLock(
            "order:" + orderId,
            3, 10, TimeUnit.SECONDS
        );

        if (lockInfo != null) {
            try {
                // 业务逻辑
            } finally {
                lockManager.unlock(lockInfo);
            }
        }
    }
}
```

### 4.2 使用DistributedLockTemplate

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final DistributedLockTemplate lockTemplate;

    public void updatePrice(Long productId, BigDecimal newPrice) {
        // 方式1: 有返回值
        Product product = lockTemplate.execute(
            "product:" + productId,
            Duration.ofSeconds(10),
            () -> {
                Product p = productMapper.selectById(productId);
                p.setPrice(newPrice);
                productMapper.updateById(p);
                return p;
            }
        );

        // 方式2: 无返回值
        lockTemplate.execute(
            "product:" + productId,
            Duration.ofSeconds(10),
            () -> {
                productMapper.updatePrice(productId, newPrice);
            }
        );

        // 方式3: 尝试执行(不抛异常)
        boolean success = lockTemplate.tryExecute(
            "product:" + productId,
            Duration.ofSeconds(5),
            () -> {
                productMapper.updatePrice(productId, newPrice);
            }
        );
    }
}
```

### 4.3 使用DistributedLockManager

```java
@Service
@RequiredArgsConstructor
public class StockService {

    private final DistributedLockManager lockManager;

    public void deductStock(Long productId, Integer quantity) {
        // 尝试获取锁
        LockInfo lockInfo = lockManager.tryLock(
            "stock:" + productId,
            Duration.ofSeconds(10),
            Duration.ofMillis(200)
        );

        if (lockInfo == null) {
            throw new BusinessException("库存操作繁忙");
        }

        try {
            // 业务逻辑
            stockMapper.deduct(productId, quantity);

            // 如果需要延长锁时间
            lockManager.renewLock(lockInfo, Duration.ofSeconds(20));

        } finally {
            // 释放锁
            lockManager.unlock(lockInfo);
        }
    }

    // 检查锁状态
    public boolean checkLock(String lockKey) {
        return lockManager.isLocked(lockKey);
    }

    // 获取锁剩余时间
    public long getLockTtl(String lockKey) {
        return lockManager.getLockTtl(lockKey);
    }
}
```

---

## 5. 锁类型

### 5.1 可重入锁(Reentrant Lock)

**特点**: 同一线程可多次获取同一锁

```java
public void method1() {
    lockManager.executeWithLock("key", () -> {
        method2();  // ✅ 可以再次获取同一锁
        return null;
    });
}

public void method2() {
    lockManager.executeWithLock("key", () -> {
        // 业务逻辑
        return null;
    });
}
```

### 5.2 公平锁(Fair Lock)

**特点**: 按请求顺序获取锁,避免饥饿

```java
@DistributedLock(
    key = "'order:' + #orderId",
    lockType = LockType.FAIR,
    waitTime = 30  // 公平锁可能需要更长等待时间
)
public void processOrder(Long orderId) {
    // 按请求顺序处理订单
}
```

### 5.3 读写锁(ReadWrite Lock)

**特点**: 多个读锁可以同时持有,写锁独占

```java
// 读操作
@DistributedLock(
    key = "'product:' + #productId",
    lockType = LockType.READ
)
public ProductDTO getProduct(Long productId) {
    // 多个线程可同时读取
    return productMapper.selectById(productId);
}

// 写操作
@DistributedLock(
    key = "'product:' + #productId",
    lockType = LockType.WRITE
)
public void updateProduct(Long productId, ProductDTO dto) {
    // 写入时独占锁
    productMapper.updateById(dto);
}
```

### 5.4 红锁(Red Lock)

**特点**: 在多个Redis实例上获取锁,提高可用性

```java
@DistributedLock(
    key = "'critical:' + #id",
    lockType = LockType.RED_LOCK  // 需要配置多个Redis实例
)
public void criticalOperation(Long id) {
    // 高可用场景使用
}
```

---

## 6. 监控管理

### 6.1 监控API

**基础路径**: `/api/lock/monitor`

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/locks` | GET | 获取所有分布式锁 |
| `/lock/{lockKey}` | GET | 获取指定锁详情 |
| `/stats` | GET | 获取锁统计信息 |
| `/lock/{lockKey}` | DELETE | 强制释放锁(危险操作) |
| `/clear-expired` | DELETE | 清除所有过期锁 |

### 6.2 查看所有锁

```bash
curl http://localhost:8081/api/lock/monitor/locks
```

**返回示例:**

```json
{
  "code": 200,
  "data": [
    {
      "lockKey": "distributed:lock:stock:123",
      "isLocked": true,
      "holdCount": 1,
      "remainTimeToLive": "8500ms"
    }
  ]
}
```

### 6.3 查看锁详情

```bash
curl http://localhost:8081/api/lock/monitor/lock/stock:123
```

### 6.4 锁统计

```bash
curl http://localhost:8081/api/lock/monitor/stats
```

**返回示例:**

```json
{
  "code": 200,
  "data": {
    "totalLocks": 15,
    "activeLocks": 3,
    "inactiveLocks": 12,
    "averageTtl": "7800ms",
    "minTtl": "2000ms",
    "maxTtl": "15000ms"
  }
}
```

---

## 7. 最佳实践

### 7.1 锁键设计

#### ✅ 推荐做法

```java
// 1. 使用有意义的前缀
@DistributedLock(key = "'stock:deduct:' + #productId")

// 2. 包含业务类型
@DistributedLock(key = "'order:create:' + #userId")

// 3. 使用层级结构
@DistributedLock(key = "'user:' + #userId + ':address:' + #addressId")

// 4. 批量操作用统一标识
@DistributedLock(key = "'batch:product:update:' + T(String).join(',', #ids)")
```

#### ❌ 不推荐做法

```java
// 1. 纯数字key(不易理解)
@DistributedLock(key = "#id")  // ❌

// 2. 过于宽泛的key(锁粒度太大)
@DistributedLock(key = "'product'")  // ❌ 所有商品操作都会阻塞

// 3. 包含易变信息(如时间戳)
@DistributedLock(key = "'order:' + #orderId + ':' + T(System).currentTimeMillis()")  // ❌
```

### 7.2 锁时间设置

```java
// 1. 根据业务耗时合理设置leaseTime
@DistributedLock(
    key = "'order:' + #orderId",
    leaseTime = 30  // 订单创建可能需要较长时间
)
public void createOrder(Order order) { }

// 2. 简单操作用短时间
@DistributedLock(
    key = "'counter:' + #id",
    waitTime = 1,
    leaseTime = 5  // 计数器操作很快
)
public void incrementCounter(Long id) { }

// 3. 高并发场景减少waitTime
@DistributedLock(
    key = "'stock:' + #productId",
    waitTime = 0,  // 不等待,快速失败
    failStrategy = LockFailStrategy.FAIL_FAST
)
public void deductStock(Long productId) { }
```

### 7.3 避免死锁

```java
// ✅ 推荐: 自动释放锁
@DistributedLock(
    key = "'user:' + #userId",
    autoRelease = true  // 方法执行完自动释放
)
public void updateUser(Long userId) { }

// ✅ 推荐: 设置合理的leaseTime
@DistributedLock(
    key = "'order:' + #orderId",
    leaseTime = 30  // 超过30秒自动释放
)
public void processOrder(Long orderId) { }

// ❌ 避免: 手动管理锁但忘记释放
public void badExample() {
    RLock lock = redissonClient.getLock("key");
    lock.lock();
    // 如果这里抛异常,锁不会被释放!
    doSomething();
    // 没有 unlock()
}
```

### 7.4 锁粒度控制

```java
// ✅ 细粒度锁(推荐)
@DistributedLock(key = "'stock:' + #productId")
public void deductStock(Long productId, Integer quantity) {
    // 只锁定特定商品的库存
}

// ❌ 粗粒度锁(不推荐)
@DistributedLock(key = "'stock'")
public void deductStock(Long productId, Integer quantity) {
    // 锁定所有商品库存,严重影响并发
}

// ✅ 根据业务选择合适粒度
@DistributedLock(key = "'user:' + #userId")  // 用户级别
@DistributedLock(key = "'order:' + #orderId")  // 订单级别
@DistributedLock(key = "'batch:' + #batchId")  // 批次级别
```

### 7.5 异常处理

```java
@Service
public class OrderService {

    @DistributedLock(
        key = "'order:' + #orderId",
        failStrategy = LockFailStrategy.THROW_EXCEPTION,
        failMessage = "订单处理中,请稍后重试"
    )
    @Transactional(rollbackFor = Exception.class)
    public void processOrder(Long orderId) {
        try {
            // 业务逻辑
            orderMapper.updateStatus(orderId, OrderStatus.PROCESSING);
            // ...
        } catch (Exception e) {
            log.error("订单处理失败: {}", orderId, e);
            throw new BusinessException("订单处理失败", e);
        }
        // 锁会自动释放,即使抛出异常
    }
}
```

### 7.6 性能优化

```java
// 1. 使用tryExecute避免长时间阻塞
public void updateCache(String key, String value) {
    boolean success = lockTemplate.tryExecute(
        "cache:" + key,
        Duration.ofSeconds(5),
        Duration.ZERO,  // 不等待
        () -> {
            redisTemplate.opsForValue().set(key, value);
        }
    );

    if (!success) {
        log.warn("缓存更新跳过,锁已被占用: {}", key);
    }
}

// 2. 读写分离
// 读操作不加锁或使用读锁
public ProductDTO getProduct(Long productId) {
    return productMapper.selectById(productId);  // 无锁
}

// 写操作使用写锁
@DistributedLock(
    key = "'product:' + #productId",
    lockType = LockType.WRITE
)
public void updateProduct(Long productId, ProductDTO dto) {
    productMapper.updateById(dto);
}

// 3. 批量操作考虑分段加锁
public void batchUpdate(List<Long> ids) {
    // 按批次分段处理,避免长时间持有锁
    Lists.partition(ids, 100).forEach(batch -> {
        lockTemplate.execute(
            "batch:" + batch.get(0),
            Duration.ofSeconds(10),
            () -> {
                productMapper.batchUpdate(batch);
            }
        );
    });
}
```

---

## 8. 常见问题

### Q1: 锁未释放?

**检查清单:**

1. ✅ 确认 `autoRelease = true`
2. ✅ 确认方法执行完成(没有被卡住)
3. ✅ 检查 `leaseTime` 是否过长
4. ✅ 查看监控API确认锁状态

**解决方案:**

```bash
# 查看锁详情
curl http://localhost:8081/api/lock/monitor/lock/stock:123

# 如果确认需要,强制释放锁
curl -X DELETE http://localhost:8081/api/lock/monitor/lock/stock:123
```

### Q2: 获取锁失败频繁?

**原因分析:**

- `waitTime` 设置过短
- `leaseTime` 设置过长
- 业务逻辑执行时间过长
- 并发量过大

**优化方案:**

```java
// 1. 增加waitTime
@DistributedLock(
    key = "'order:' + #orderId",
    waitTime = 10,  // 从3秒增加到10秒
    leaseTime = 15
)

// 2. 减少leaseTime
@DistributedLock(
    key = "'stock:' + #productId",
    waitTime = 5,
    leaseTime = 10  // 从30秒减少到10秒
)

// 3. 优化业务逻辑,减少持锁时间
@DistributedLock(key = "'user:' + #userId")
public void updateUser(Long userId, UserDTO dto) {
    // 只在锁内执行必要操作
    User user = userMapper.selectById(userId);
    user.setName(dto.getName());
    userMapper.updateById(user);

    // 耗时操作放在锁外执行
    // (在方法返回后自动释放锁)
}

// 4. 使用快速失败策略
@DistributedLock(
    key = "'stock:' + #productId",
    waitTime = 0,
    failStrategy = LockFailStrategy.FAIL_FAST,
    failMessage = "库存扣减繁忙,请稍后重试"
)
```

### Q3: 锁超时自动释放导致并发问题?

**问题场景:**

```java
@DistributedLock(
    key = "'order:' + #orderId",
    leaseTime = 10  // 10秒后自动释放
)
public void processOrder(Long orderId) {
    // 如果业务逻辑执行超过10秒
    // 锁会自动释放,其他线程可能同时处理
    longTimeOperation();  // 需要15秒
}
```

**解决方案:**

```java
// 方案1: 增加leaseTime
@DistributedLock(
    key = "'order:' + #orderId",
    leaseTime = 30  // 增加到30秒
)

// 方案2: 使用Redisson的Watch Dog机制(默认启用)
// leaseTime = -1 时,Redisson会自动续期
@DistributedLock(
    key = "'order:' + #orderId",
    leaseTime = -1  // 使用Watch Dog自动续期
)

// 方案3: 手动续期
lockManager.renewLock(lockInfo, Duration.ofSeconds(20));
```

### Q4: Redis故障导致锁失效?

**问题**: Redis宕机或网络故障导致锁不可用

**解决方案:**

```java
// 方案1: 使用红锁(需要多个Redis实例)
@DistributedLock(
    key = "'critical:' + #id",
    lockType = LockType.RED_LOCK
)

// 方案2: 增加重试机制
@DistributedLock(
    key = "'order:' + #orderId",
    waitTime = 30,  // 增加等待时间
    failStrategy = LockFailStrategy.THROW_EXCEPTION
)

// 方案3: 降级方案
public void updateProduct(Long productId) {
    try {
        lockTemplate.execute("product:" + productId, Duration.ofSeconds(10), () -> {
            productMapper.updateById(productId);
        });
    } catch (LockException e) {
        log.warn("分布式锁获取失败,使用数据库锁: {}", productId);
        // 降级到数据库悲观锁
        productMapper.updateByIdWithLock(productId);
    }
}
```

### Q5: 如何测试分布式锁?

```java
@SpringBootTest
public class DistributedLockTest {

    @Autowired
    private RedissonLockManager lockManager;

    @Test
    public void testConcurrentAccess() throws Exception {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    lockManager.executeWithLock("test:lock", () -> {
                        successCount.incrementAndGet();
                        Thread.sleep(100);  // 模拟业务逻辑
                        return null;
                    });
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        // 验证所有线程都成功获取了锁
        assertEquals(threadCount, successCount.get());
    }
}
```

---

## 9. 配置参考

### 9.1 单机模式

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your-password
      database: 0
```

### 9.2 集群模式

```yaml
spring:
  redis:
    cluster:
      nodes:
        - 192.168.1.1:6379
        - 192.168.1.2:6379
        - 192.168.1.3:6379
      max-redirects: 3
```

### 9.3 哨兵模式

```yaml
spring:
  redis:
    sentinel:
      master: mymaster
      nodes:
        - 192.168.1.1:26379
        - 192.168.1.2:26379
        - 192.168.1.3:26379
```

---

## 10. 参考资料

- [Redisson官方文档](https://github.com/redisson/redisson/wiki)
- [Redis分布式锁官方指南](https://redis.io/docs/manual/patterns/distributed-locks/)
- [项目CLAUDE.md](../CLAUDE.md)
- [缓存系统文档](CACHE.md)

---

**最后更新**: 2025-10-12
**版本**: v1.0
**维护者**: CloudDevAgent
