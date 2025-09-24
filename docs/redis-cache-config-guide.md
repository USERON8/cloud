# Redis缓存配置分离指南

## 概述

本项目已完成Redis配置的分离重构，实现了以下目标：

- 各子服务可以更细化的个性化配置Redis
- 公共包不再包含具体的缓存Bean，仅提供配置模板
- 仅在user、product、search服务中实现多级缓存
- 其他服务仅使用Redis分布式缓存

## 架构设计

### 1. 配置层次结构

```
common-module/
├── config/
│   ├── base/
│   │   └── BaseRedisConfig.java           # 基础Redis配置抽象类
│   ├── BaseLocalCacheConfig.java          # 本地缓存基础配置类（仅提供工具方法）
│   ├── RedisConfigFactory.java            # Redis配置工厂类
│   └── MultiLevelCacheConfigFactory.java  # 多级缓存配置工厂类

各服务/
├── config/
│   ├── XxxRedisConfig.java                # 服务专用Redis配置
│   └── XxxLocalCacheConfig.java           # 服务专用本地缓存配置（仅限user、product、search）
```

### 2. 服务配置策略

| 服务              | Redis配置   | 本地缓存   | 配置特点        |
|-----------------|-----------|--------|-------------|
| user-service    | ✅ 高性能+事务  | ✅ 多级缓存 | 会话管理，用户信息缓存 |
| product-service | ✅ 缓存专用    | ✅ 多级缓存 | 商品信息缓存，查询优化 |
| search-service  | ✅ 缓存专用    | ✅ 多级缓存 | 搜索结果缓存，热点数据 |
| order-service   | ✅ 高性能+事务  | ❌      | 订单状态管理，强一致性 |
| stock-service   | ✅ 高性能+事务  | ❌      | 库存扣减，防超卖    |
| payment-service | ✅ 高性能+事务  | ❌      | 支付状态管理，安全性  |
| auth-service    | ✅ 会话专用+事务 | ❌      | 令牌管理，会话存储   |
| log-service     | ✅ 基础配置    | ❌      | 统计缓存，不需要事务  |

## 配置模板说明

### 1. Redis配置模板

#### 基础配置 (createBasicRedisTemplate)

- 适用场景：一般缓存场景
- 特点：基础序列化配置，不支持事务
- 使用服务：log-service

#### 高性能配置 (createHighPerformanceRedisTemplate)

- 适用场景：高并发场景
- 特点：优化序列化性能，支持事务
- 使用服务：user-service、order-service、stock-service、payment-service

#### 缓存专用配置 (createCacheRedisTemplate)

- 适用场景：纯缓存场景
- 特点：缓存优化，不支持事务
- 使用服务：product-service、search-service

#### 会话专用配置 (createSessionRedisTemplate)

- 适用场景：会话存储场景
- 特点：会话优化，支持事务
- 使用服务：auth-service

### 2. 多级缓存配置

#### L1缓存（Caffeine本地缓存）

- **user-service**: 2000条目，30分钟写入过期，15分钟访问过期
- **product-service**: 1500条目，45分钟写入过期，20分钟访问过期
- **search-service**: 3000条目，20分钟写入过期，10分钟访问过期

#### L2缓存（Redis分布式缓存）

- 各服务根据业务特点设置不同的过期时间
- 支持服务前缀区分：user:、product:、search:等

## 使用方式

### 1. 服务中启用Redis配置

```java

@Configuration
public class XxxRedisConfig extends BaseRedisConfig {

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 选择合适的配置模板
        return RedisConfigFactory.createHighPerformanceRedisTemplate(factory);
    }

    @Override
    protected String getServicePrefix() {
        return "xxx"; // 服务前缀
    }

    @Override
    protected long getCacheExpireTime(String type) {
        // 自定义过期时间
        switch (type) {
            case "userInfo":
                return 3600L;
            default:
                return 1800L;
        }
    }
}
```

### 2. 启用多级缓存（仅限user、product、search服务）

```java

@Configuration
@EnableCaching
@EnableAspectJAutoProxy
public class XxxLocalCacheConfig extends BaseLocalCacheConfig {

    @Bean
    @Primary
    public CacheManager localCacheManager() {
        // 使用预定义的多级缓存配置
        return MultiLevelCacheConfigFactory.createUserServiceCacheManager();
    }

    @Override
    protected String getServiceName() {
        return "xxx-service";
    }
}
```

### 3. 自定义配置

```java
// 自定义Redis配置
RedisTemplate<String, Object> customTemplate = RedisConfigFactory
                .createCustomRedisTemplate(
                        new RedisConfigFactory.RedisTemplateBuilder()
                                .connectionFactory(factory)
                                .enableTransactionSupport(true)
                                .build()
                );

// 自定义多级缓存配置
CacheManager customCacheManager = MultiLevelCacheConfigFactory
        .createCustomCacheManager(
                new MultiLevelCacheConfigFactory.CacheManagerBuilder()
                        .maximumSize(5000L)
                        .expireAfterWrite(Duration.ofMinutes(60))
                        .cacheNames("cache1", "cache2")
                        .build()
        );
```

## 最佳实践

### 1. 缓存键命名规范

- 格式：`{service}:{type}:{key}`
- 示例：`user:userInfo:123`、`product:productList:category1`

### 2. 过期时间设置原则

- **用户信息**: 30分钟-2小时（相对稳定）
- **商品信息**: 45分钟-2小时（分类信息更长）
- **搜索结果**: 10-30分钟（变化较快）
- **订单状态**: 5-30分钟（实时性要求高）
- **库存信息**: 2-5分钟（强实时性）
- **支付信息**: 5-30分钟（安全性考虑）
- **认证令牌**: 5分钟-2小时（安全性考虑）

### 3. 事务支持选择

- **需要事务**: 用户会话、订单状态、库存扣减、支付状态、认证令牌
- **不需要事务**: 商品信息、搜索结果、日志统计

### 4. 多级缓存使用场景

- **适合多级缓存**: 查询频繁、数据相对稳定的场景
- **不适合多级缓存**: 强一致性要求、数据变化频繁的场景

## 注意事项

1. **配置优先级**: 服务专用配置使用@Primary注解，优先于公共配置
2. **依赖管理**: 本地缓存需要添加caffeine依赖
3. **AOP支持**: 多级缓存需要启用@EnableAspectJAutoProxy
4. **缓存预热**: 生产环境建议实现缓存预热机制
5. **监控告警**: 建议添加缓存命中率和性能监控

## 扩展功能

### 1. 添加新的配置模板

在RedisConfigFactory中添加新的静态方法，提供特定场景的配置模板。

### 2. 自定义缓存策略

继承BaseRedisConfig，重写相关方法实现自定义缓存策略。

### 3. 集成缓存监控

可以集成Micrometer等监控框架，实现缓存性能监控。
