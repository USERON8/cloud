# 缓存系统文档

> 本文档介绍项目多级缓存架构的设计、使用方法和最佳实践

## 📋 目录

- [1. 架构概览](#1-架构概览)
- [2. 快速开始](#2-快速开始)
- [3. 缓存注解使用](#3-缓存注解使用)
- [4. 缓存预热](#4-缓存预热)
- [5. 监控管理](#5-监控管理)
- [6. 最佳实践](#6-最佳实践)
- [7. 性能调优](#7-性能调优)
- [8. 常见问题](#8-常见问题)

---

## 1. 架构概览

### 1.1 双层缓存架构

```
┌─────────────────────────────────────────┐
│          Application Layer              │
│  (@Cacheable/@CacheEvict/@CachePut)    │
└──────────────┬──────────────────────────┘
               │
┌──────────────┴──────────────────────────┐
│      MultiLevelCacheManager             │
│  (缓存管理器 - 多级缓存)                 │
└──────────────┬──────────────────────────┘
               │
     ┌─────────┴─────────┐
     │                   │
┌────┴────┐      ┌───────┴───────┐
│ L1 Cache│      │   L2 Cache    │
│ Caffeine│      │     Redis     │
│ (本地)  │      │   (分布式)    │
│ 1-5ms   │      │   10-20ms     │
└─────────┘      └───────────────┘
```

### 1.2 缓存层级说明

| 层级 | 技术 | 容量 | TTL | 响应时间 | 作用域 |
|-----|------|------|-----|---------|--------|
| **L1** | Caffeine | 1000条 | 10-30分钟 | 1-5ms | 单节点 |
| **L2** | Redis | 无限制 | 5分钟-1小时 | 10-20ms | 集群共享 |

**工作原理:**
1. 查询时先查L1本地缓存,命中直接返回(1-5ms)
2. L1未命中则查L2 Redis缓存(10-20ms)
3. L2命中则回填到L1,并返回结果
4. L2也未命中则查询数据库,并写入L1和L2

### 1.3 缓存配置

#### 核心配置类

- `MultiLevelCacheManager` - 多级缓存管理器(common-module/cache/core/)
- `MultiLevelCache` - 单个缓存实例(common-module/cache/core/)
- `CacheConfigFactory` - 缓存管理器工厂(common-module/cache/config/)
- `RedisConfig` - Redis配置和单级缓存(common-module/config/)

#### TTL配置

在 `application-common.yml` 中配置:

| 缓存名称 | TTL | 使用场景 |
|---------|-----|---------|
| `user` / `userInfo` | 30分钟 | 用户基本信息 |
| `product` / `productInfo` | 45分钟 | 商品信息 |
| `stock` / `stockInfo` | 5分钟 | 库存信息 |
| `order` / `orderInfo` | 15分钟 | 订单信息 |
| `payment` / `paymentInfo` | 10分钟 | 支付信息 |
| `search` / `searchResult` | 20分钟 | 搜索结果 |
| `auth` / `permission` | 1小时 | 权限认证 |

---

## 2. 快速开始

### 2.1 启用多级缓存

在 `application-common.yml` 中配置:

```yaml
cache:
  multi-level: true  # 启用多级缓存
  ttl:
    user: 1800      # 30分钟
    product: 2700   # 45分钟
    stock: 300      # 5分钟
    order: 900      # 15分钟
    payment: 600    # 10分钟
    search: 1200    # 20分钟
    auth: 3600      # 1小时
```

### 2.2 基础使用

```java
@Service
public class UserServiceImpl implements UserService {

    // 查询时缓存
    @Cacheable(cacheNames = "user", key = "#userId")
    public UserDTO getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    // 更新时清除缓存
    @CacheEvict(cacheNames = "user", key = "#userId")
    public void updateUser(Long userId, UserDTO dto) {
        userMapper.updateById(dto);
    }

    // 更新并刷新缓存
    @CachePut(cacheNames = "user", key = "#result.id")
    public UserDTO createUser(UserDTO dto) {
        userMapper.insert(dto);
        return dto;
    }
}
```

---

## 3. 缓存注解使用

### 3.1 @Cacheable - 查询缓存

**作用**: 方法执行前先查缓存,命中则不执行方法

```java
@Cacheable(
    cacheNames = "user",              // 缓存名称
    key = "#id",                      // 缓存key(SpEL表达式)
    unless = "#result == null",       // 条件:结果为null时不缓存
    condition = "#id > 0"             // 条件:id>0时才缓存
)
public UserDTO getUserById(Long id) { }
```

**Key生成策略**:

```java
// 简单参数
key = "#userId"                           // user:123

// 复合参数
key = "#userId + ':' + #type"             // user:123:VIP

// 对象属性
key = "#user.id"                          // user:123

// 多参数
key = "'batch:' + #userIds.toString()"    // user:batch:[1,2,3]
```

### 3.2 @CacheEvict - 清除缓存

```java
// 清除单个缓存
@CacheEvict(cacheNames = "user", key = "#userId")
public void deleteUser(Long userId) { }

// 清除所有缓存
@CacheEvict(cacheNames = "user", allEntries = true)
public void deleteAllUsers() { }

// 方法执行前清除
@CacheEvict(cacheNames = "user", key = "#userId", beforeInvocation = true)
public void updateUserImportant(Long userId) { }
```

### 3.3 @CachePut - 更新缓存

**作用**: 方法执行后更新缓存(不管是否存在)

```java
@CachePut(cacheNames = "user", key = "#result.id")
public UserDTO updateUser(UserDTO dto) {
    userMapper.updateById(dto);
    return dto;
}
```

### 3.4 @Caching - 组合操作

```java
@Caching(
    evict = {
        @CacheEvict(cacheNames = "user", key = "#userId"),
        @CacheEvict(cacheNames = "userInfo", key = "#userId")
    },
    put = {
        @CachePut(cacheNames = "user", key = "#result.id")
    }
)
public UserDTO updateUserProfile(Long userId, ProfileDTO dto) {
    return updatedUser;
}
```

---

## 4. 缓存预热

### 4.1 实现预热策略

创建预热策略类:

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class UserCacheWarmupStrategy
    implements CacheWarmupStrategy {

    private final UserMapper userMapper;

    @Override
    public int warmup(CacheManager cacheManager) {
        Cache cache = cacheManager.getCache("userInfo");
        if (cache == null) {
            return 0;
        }

        // 查询热点数据
        List<User> hotUsers = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByDesc(User::getUpdatedAt)
                .last("LIMIT 100")
        );

        // 预热到缓存
        for (User user : hotUsers) {
            cache.put(user.getId(), user);
        }

        return hotUsers.size();
    }

    @Override
    public String getStrategyName() {
        return "UserCacheWarmupStrategy";
    }
}
```

### 4.2 预热触发时机

- ✅ **应用启动时自动触发** (由 `CacheWarmupManager` 管理)
- ✅ **支持手动触发** (通过API接口)

### 4.3 预热最佳实践

1. **数量控制**: 每个策略预热100-500条热点数据
2. **查询优化**: 使用索引,避免全表扫描
3. **数据筛选**: 只预热活跃/热门数据
4. **脱敏处理**: 移除敏感信息(密码、token等)
5. **异步执行**: 不阻塞应用启动

### 4.4 现有预热策略

| 服务 | 策略类 | 预热内容 | 数量 |
|-----|-------|---------|------|
| user-service | `UserCacheWarmupStrategy` | 最近活跃用户 | 100 |
| product-service | `ProductCacheWarmupStrategy` | 热销商品+推荐商品 | 80 |
| order-service | `OrderCacheWarmupStrategy` | 待支付订单+处理中订单 | ~200 |

---

## 5. 监控管理

### 5.1 监控API

**基础路径**: `/api/cache/monitor`

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/names` | GET | 获取所有缓存名称 |
| `/stats` | GET | 获取所有缓存统计 |
| `/stats/{cacheName}` | GET | 获取指定缓存统计 |
| `/hotspot/{cacheName}` | GET | 获取热点数据Top10 |
| `/metrics/summary` | GET | 获取系统总体指标 |
| `/clear/{cacheName}` | DELETE | 清除指定缓存 |
| `/clear-all` | DELETE | 清除所有缓存 |
| `/manager-info` | GET | 获取缓存管理器信息 |

### 5.2 监控指标

**接口示例**: `GET /api/cache/monitor/stats/user`

**返回数据**:

```json
{
  "code": 200,
  "data": {
    "cacheName": "user",
    "cacheType": "MultiLevelCache",
    "hitCount": 1523,
    "missCount": 127,
    "evictionCount": 45,
    "totalAccess": 1650,
    "hitRatio": "92.30%",
    "averageAccessTime": "3.25ms"
  }
}
```

**指标含义**:

- `hitCount`: 缓存命中次数
- `missCount`: 缓存未命中次数
- `evictionCount`: 缓存驱逐次数
- `totalAccess`: 总访问次数
- `hitRatio`: 命中率(越高越好,建议>90%)
- `averageAccessTime`: 平均访问耗时(毫秒)

### 5.3 Actuator监控

```bash
# 查看所有缓存指标
curl http://localhost:8081/actuator/metrics

# 查看缓存命中次数
curl http://localhost:8081/actuator/metrics/cache.hit

# 查看缓存命中率
curl http://localhost:8081/actuator/metrics/cache.hit_ratio
```

### 5.4 日志监控

缓存操作会输出DEBUG级别日志:

```
2025-10-12 10:15:23.456 DEBUG [user-service] 缓存L1命中: cacheName=user, key=123
2025-10-12 10:15:24.789 DEBUG [user-service] 缓存L2命中并回填: cacheName=user, key=456
2025-10-12 10:15:25.123 DEBUG [user-service] 缓存未命中: cacheName=user, key=789
```

---

## 6. 最佳实践

### 6.1 缓存Key设计

#### ✅ 推荐做法

```java
// 1. 使用业务含义的key
key = "'user:' + #userId"                    // ✅ user:123

// 2. 包含版本信息
key = "'v1:user:' + #userId"                 // ✅ v1:user:123

// 3. 使用分隔符
key = "'user:profile:' + #userId"            // ✅ user:profile:123

// 4. 复合key用冒号分隔
key = "'user:' + #userId + ':address:' + #addressId"  // ✅ user:123:address:456
```

#### ❌ 不推荐做法

```java
// 1. 纯数字key (不易理解)
key = "#userId"                              // ❌ 123

// 2. 过长的key (影响性能)
key = "'very_long_prefix_' + #userId + '_' + #timestamp + '_' + #random"  // ❌

// 3. 特殊字符 (可能导致问题)
key = "#userId + '@' + #email"               // ❌ 123@test.com
```

### 6.2 缓存失效策略

#### 主动失效

```java
// 更新时清除
@CacheEvict(cacheNames = "user", key = "#userId")
public void updateUser(Long userId, UserDTO dto) { }

// 删除时清除
@CacheEvict(cacheNames = "user", key = "#userId")
public void deleteUser(Long userId) { }
```

#### 被动失效

```java
// 设置unless条件
@Cacheable(
    cacheNames = "user",
    key = "#userId",
    unless = "#result == null || #result.status == 0"  // 禁用用户不缓存
)
public UserDTO getUserById(Long userId) { }
```

### 6.3 避免缓存穿透

**问题**: 大量请求不存在的key,导致直接击穿到数据库

**解决方案**:

```java
// 方案1: 缓存null值 (短TTL)
@Cacheable(
    cacheNames = "user",
    key = "#userId",
    unless = "false"  // 即使result为null也缓存
)
public UserDTO getUserById(Long userId) {
    UserDTO user = userMapper.selectById(userId);
    return user;  // 可以返回null,会被缓存5-10秒
}
```

### 6.4 避免缓存雪崩

**问题**: 大量缓存同时过期,导致数据库瞬时压力巨大

**解决方案**:

```java
// 方案1: 添加随机过期时间 (已在RedisConfig中实现)
configMap.put("hotspot", createCacheConfig(
    jsonSerializer,
    Duration.ofHours(2).plusMinutes((long) (Math.random() * 30))  // 2小时±30分钟
));
```

### 6.5 缓存更新策略

#### Cache Aside Pattern (推荐)

```java
// 读: 先查缓存,未命中查DB并写缓存
@Cacheable(cacheNames = "user", key = "#userId")
public UserDTO getUser(Long userId) {
    return userMapper.selectById(userId);
}

// 写: 先更新DB,再删除缓存
@CacheEvict(cacheNames = "user", key = "#userId")
public void updateUser(Long userId, UserDTO dto) {
    userMapper.updateById(dto);
}
```

---

## 7. 性能调优

### 7.1 Caffeine优化

```java
// 通过代码配置(CacheConfigFactory)
CaffeineConfig config = new CaffeineConfig();
config.setMaximumSize(2000L);                    // 容量:1000 -> 2000
config.setExpireAfterWriteMinutes(45);           // 写后过期:30 -> 45分钟
config.setExpireAfterAccessMinutes(15);          // 访问后过期:10 -> 15分钟
config.setRecordStats(true);                     // 启用统计
```

### 7.2 Redis优化

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 100              # 高并发场景:50 -> 100
          max-idle: 50
          min-idle: 10
          time-between-eviction-runs: 30000  # 检测周期:60s -> 30s
```

### 7.3 序列化优化

```java
// 使用Protobuf/Kryo替代Jackson (更快,更小)
@Bean
public RedisSerializer<Object> redisSerializer() {
    return new KryoRedisSerializer<>();  // 性能提升20-30%
}
```

---

## 8. 常见问题

### Q1: 缓存未生效?

**检查清单**:

1. ✅ 确认 `cache.multi-level=true` 已配置
2. ✅ 方法必须是 `public`
3. ✅ 方法不能在同类中调用 (AOP代理失效)
4. ✅ 返回值必须可序列化

**正确示例**:

```java
@Service
public class UserService {
    @Autowired
    private UserService self;  // 注入自己

    public void outerMethod() {
        self.cachedMethod();  // ✅ 通过代理调用
    }

    @Cacheable(cacheNames = "user", key = "#id")
    public UserDTO cachedMethod(Long id) { }
}
```

### Q2: 缓存命中率低?

**分析步骤**:

```bash
# 1. 查看缓存统计
curl http://localhost:8081/api/cache/monitor/stats/user

# 2. 查看热点数据
curl http://localhost:8081/api/cache/monitor/hotspot/user?limit=20

# 3. 分析日志
grep "缓存L1命中" logs/app.log | wc -l
grep "缓存L2命中" logs/app.log | wc -l
grep "缓存未命中" logs/app.log | wc -l
```

**常见原因**:

- TTL设置过短
- 缓存key设计不合理(每次都不同)
- L1缓存容量太小(频繁驱逐)
- 数据变化太频繁

### Q3: 内存占用过高?

**优化方案**:

```java
// 1. 减小L1缓存容量
caffeineConfig.setMaximumSize(500L);  // 从1000降到500

// 2. 缩短TTL
cache.ttl.user: 900  # 从30分钟改为15分钟

// 3. 只缓存必要字段
public UserDTO sanitizeUser(User user) {
    UserDTO dto = new UserDTO();
    dto.setId(user.getId());
    dto.setUsername(user.getUsername());
    // 不复制大字段(如头像base64、详情等)
    return dto;
}
```

### Q4: Redis连接超时?

**检查配置**:

```yaml
spring:
  data:
    redis:
      timeout: 10000ms        # 连接超时
      lettuce:
        pool:
          max-active: 50      # 最大连接数
          max-wait: 3000ms    # 最大等待时间
```

### Q5: 缓存一致性问题?

**解决方案**:

```java
// 方案1: 更新时删除缓存 (推荐)
@CacheEvict(cacheNames = "user", key = "#userId")
public void updateUser(Long userId, UserDTO dto) {
    userMapper.updateById(dto);
}

// 方案2: 短TTL + 定时刷新
@Cacheable(cacheNames = "stock", key = "#productId")  // TTL=5分钟
public Integer getStock(Long productId) {
    return stockMapper.selectStock(productId);
}
```

---

## 9. 迁移指南

### 9.1 从单Redis迁移到多级缓存

**步骤**:

1. 添加配置
```yaml
cache:
  multi-level: true
```

2. 无需修改代码 (注解保持不变)

3. 重启服务,观察日志:
```
🚀 启用多级缓存管理器: nodeId=xxx, metricsEnabled=true
```

4. 验证效果:
```bash
curl http://localhost:8081/api/cache/monitor/stats
```

### 9.2 回滚方案

如遇问题,可快速回滚:

```yaml
cache:
  multi-level: false  # 回滚到单Redis
```

---

## 10. 参考资料

- [Spring Cache官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Caffeine GitHub](https://github.com/ben-manes/caffeine)
- [Redis最佳实践](https://redis.io/docs/manual/patterns/)
- [项目CLAUDE.md](../CLAUDE.md)

---

**最后更新**: 2025-10-12
**版本**: v1.0
**维护者**: CloudDevAgent
