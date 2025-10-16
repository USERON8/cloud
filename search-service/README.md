# 🔍 Search Service - 云电商搜索服务

## 📋 服务概述

**search-service** 是云电商平台的核心搜索服务，基于Elasticsearch提供高性能的产品、商家、分类搜索功能。

### 🎯 核心特性

- **无SQL依赖**: 使用Elasticsearch作为主要存储，无需MySQL
- **多级缓存**: 支持Caffeine(L1) + Redis(L2)双层缓存
- **高性能搜索**: 基于Elasticsearch 8.x的全文检索
- **实时同步**: 通过RocketMQ消费数据变更事件
- **智能推荐**: 支持热门搜索、搜索建议等功能

## 🏗️ 技术架构

### 📊 服务信息

| 配置项      | 值                           |
|----------|-----------------------------|
| **服务名**  | search-service              |
| **端口**   | 8087                        |
| **数据存储** | Elasticsearch (无MySQL)      |
| **缓存**   | Redis database:7 + Caffeine |
| **消息队列** | RocketMQ (端口39876)          |
| **认证**   | OAuth2.1 JWT                |

### 🗄️ 数据存储架构

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Product-ES    │    │    Shop-ES       │    │  Category-ES    │
│  (商品索引)      │    │   (商家索引)      │    │   (分类索引)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                        │                        │
         └────────────────────────┼────────────────────────┘
                                  │
                      ┌──────────────────┐
                      │   Elasticsearch  │
                      │    (主存储)      │
                      └──────────────────┘
                                  │
                      ┌──────────────────┐
                      │ Multi-Level Cache│
                      │ L1: Caffeine     │
                      │ L2: Redis db:7   │
                      └──────────────────┘
```

### 🔄 数据流架构

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│Product      │    │User         │    │Order        │
│Service      │    │Service      │    │Service      │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                          │
                ┌──────────────────┐
                │    RocketMQ      │
                │  SEARCH_EVENTS   │
                │     Topic        │
                └──────┬───────────┘
                       │
               ┌──────────────────┐
               │  Search Service  │
               │   EventConsumer  │
               └──────┬───────────┘
                      │
               ┌──────────────────┐
               │  Elasticsearch   │
               │   Index Update   │
               └──────────────────┘
```

## 🚀 服务启动

### ✅ 启动前检查

1. **Elasticsearch** (localhost:9200) 已启动
2. **Redis** (localhost:6379) 已启动
3. **RocketMQ** (localhost:39876) 已启动
4. **Nacos** (localhost:8848) 已启动

### 🔧 启动命令

```bash
# 开发环境启动
mvn spring-boot:run -DskipTests=true

# 或使用jar包启动
java -jar target/search-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 🎯 启动验证

- **服务健康检查**: http://localhost:8087/actuator/health
- **API文档**: http://localhost:8087/swagger-ui.html
- **Nacos控制台**: 检查服务注册状态

## 📚 API接口

### 🔍 商品搜索接口 (/api/search/product)

**ProductSearchController** - 商品搜索功能

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/search/product/query` | POST | 商品关键词搜索(分页、排序、过滤) |
| `/api/search/product/suggest` | GET | 商品搜索建议(自动补全) |
| `/api/search/product/hot` | GET | 热门搜索词 |
| `/api/search/product/{id}` | GET | 根据ID查询商品 |
| `/api/search/product/sync` | POST | 手动同步商品到ES索引 |

### 🏪 商家搜索接口 (/api/search/shop)

**ShopSearchController** - 商家搜索功能

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/search/shop/query` | POST | 商家搜索(按名称、位置等) |
| `/api/search/shop/nearby` | GET | 附近商家搜索(基于地理位置) |
| `/api/search/shop/{id}` | GET | 根据ID查询商家 |
| `/api/search/shop/sync` | POST | 手动同步商家到ES索引 |

### 🗂️ 分类搜索接口 (/api/search/category)

**CategorySearchController** - 分类搜索功能(规划中)

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/search/category/query` | POST | 分类搜索 |
| `/api/search/category/tree` | GET | 分类树查询 |

### 🎛️ 管理接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/search/manage/index/rebuild` | POST | 重建所有索引 |
| `/api/search/manage/index/product` | POST | 重建商品索引 |
| `/api/search/manage/index/shop` | POST | 重建商家索引 |
| `/api/search/manage/cache/clear` | POST | 清除搜索缓存 |
| `/api/search/manage/sync/full` | POST | 全量数据同步 |

## 🔧 配置说明

### 🗄️ 核心配置

```yaml
# Elasticsearch配置
spring:
  elasticsearch:
    uris: http://localhost:9200

# Redis配置 (database: 7)
  data:
    redis:
      host: localhost
      port: 6379
      database: 7

# 多级缓存配置
cache:
  multi-level:
    enabled: true
    l1:
      type: caffeine
      caffeine:
        maximum-size: 1000
        expire-after-write: 5m
    l2:
      type: redis
      redis:
        time-to-live: 30m
```

### 🚫 特殊配置注意

**search-service具有以下特殊配置**:

1. **排除数据源自动配置**:
   ```java
   @SpringBootApplication(exclude = {
       DataSourceAutoConfiguration.class,
       HibernateJpaAutoConfiguration.class,
       DataSourceTransactionManagerAutoConfiguration.class
   })
   ```

2. **排除MyBatis-Plus依赖**:
   ```xml
   <exclusion>
       <groupId>com.baomidou</groupId>
       <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
   </exclusion>
   ```

3. **组件扫描排除**:
   ```java
   @ComponentScan(excludeFilters = {
       @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
           MybatisPlusConfig.class,
           PerformanceMonitor.class,
           BaseOAuth2ResourceServerConfig.class
       })
   })
   ```

## 📊 缓存策略

### 🔄 多级缓存架构

```
┌─────────────────────────────────────────────┐
│              搜索请求                         │
└──────────────────┬──────────────────────────┘
                   │
    ┌──────────────▼──────────────┐
    │        L1 Cache             │
    │     (Caffeine 5min)         │
    └──────────┬─────────┬────────┘
               │ HIT     │ MISS
               ▼         ▼
    ┌─────────────┐    ┌──────────────────┐
    │   返回结果   │    │   L2 Cache       │
    │            │    │ (Redis 30min)     │
    └─────────────┘    └─────────┬────────┘
                                 │ MISS
                                 ▼
                      ┌──────────────────┐
                      │   Elasticsearch  │
                      │    (主存储)      │
                      └──────────────────┘
```

### 📈 缓存监控

- **缓存命中率**: `/actuator/metrics/cache.gets`
- **缓存大小**: `/actuator/metrics/cache.size`
- **缓存驱逐**: `/actuator/metrics/cache.evictions`

## 🎭 事件消费

### 📬 消费的事件类型

| 事件类型 | Topic                 | 处理逻辑             |
|------|-----------------------|------------------|
| 商品变更 | PRODUCT_CHANGE_TOPIC  | 更新product_index  |
| 商家变更 | SHOP_CHANGE_TOPIC     | 更新shop_index     |
| 分类变更 | CATEGORY_CHANGE_TOPIC | 更新category_index |

### ⚡ RocketMQ配置

```yaml
spring:
  cloud:
    stream:
      rocketmq:
        binder:
          name-server: 127.0.0.1:39876
      bindings:
        search-consumer-in-0:
          destination: SEARCH_EVENTS_TOPIC
          content-type: application/json
          group: search-consumer-group
```

## 🔐 安全配置

### 🎫 OAuth2.1 JWT验证

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://127.0.0.1:80/.well-known/jwks.json
          cache-duration: PT30M
```

### 🛡️ 权限控制

- **内部API**: `SCOPE_internal_api`
- **管理API**: `ROLE_ADMIN`
- **查询API**: 需要认证用户

## 📈 性能监控

### 🎯 关键指标

| 指标         | 说明         | 监控端点                                             |
|------------|------------|--------------------------------------------------|
| **搜索QPS**  | 每秒查询数      | `/actuator/metrics/http.server.requests`         |
| **ES响应时间** | 平均响应时间     | `/actuator/metrics/elasticsearch.client.request` |
| **缓存命中率**  | L1/L2缓存命中率 | `/actuator/metrics/cache.*`                      |
| **线程池状态**  | 异步处理线程池    | `/actuator/metrics/executor.*`                   |

### 📊 自定义线程池

```java
// 搜索查询线程池 (核心:8, 最大:24, 队列:500)
@Bean("searchQueryExecutor")

// 索引构建线程池 (核心:4, 最大:12, 队列:800) 
@Bean("searchIndexExecutor")

// 搜索建议线程池 (核心:3, 最大:8, 队列:200)
@Bean("searchSuggestionExecutor")
```

## 🔍 常见问题

### ❌ 启动失败

**Q: 提示数据源配置错误**

```
Failed to configure a DataSource: 'url' attribute is not specified
```

**A: 确认已正确排除数据源自动配置**

```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class
})
```

**Q: MyBatis-Plus类找不到**

```
ClassNotFoundException: com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor
```

**A: 确认pom.xml中已排除MyBatis-Plus依赖**

### 🔄 缓存问题

**Q: Redis连接失败**

- 检查Redis服务状态: `redis-cli ping`
- 确认database编号: `database: 7`

**Q: Caffeine缓存不生效**

- 检查缓存配置: `cache.multi-level.enabled: true`
- 查看日志: `logging.level.com.cloud.search: debug`

### 🔍 Elasticsearch问题

**Q: ES连接超时**

- 确认ES服务状态: `curl http://localhost:9200/_cluster/health`
- 检查网络连接和防火墙设置

**Q: 索引不存在**

- 执行索引创建: `POST /search/manage/index/rebuild`
- 检查索引状态: `GET http://localhost:9200/_cat/indices`

## 🚀 部署建议

### 🏭 生产环境

1. **JVM参数调优**:
   ```bash
   -Xms2g -Xmx4g -XX:+UseG1GC
   ```

2. **ES集群配置**:
   ```yaml
   spring:
     elasticsearch:
       uris: 
         - http://es-node1:9200
         - http://es-node2:9200
         - http://es-node3:9200
   ```

3. **缓存集群**:
   ```yaml
   spring:
     data:
       redis:
         cluster:
           nodes:
             - redis-node1:6379
             - redis-node2:6379
             - redis-node3:6379
   ```

### 📊 监控告警

- **服务可用性**: 99.9%
- **平均响应时间**: < 100ms
- **缓存命中率**: > 80%
- **ES集群状态**: Green

---

## 📝 更新日志

### v1.0.0 (2025-10-03)

- ✅ 修复数据源配置问题，成功排除MySQL依赖
- ✅ 实现多级缓存架构 (Caffeine + Redis)
- ✅ 集成Elasticsearch 8.x搜索功能
- ✅ 配置OAuth2.1 JWT安全认证
- ✅ 实现RocketMQ事件消费机制
- ✅ 添加完整的监控和健康检查

---

**📞 技术支持**: Cloud Platform Team  
**📧 联系邮箱**: support@cloud-platform.com  
**🔗 项目地址**: https://github.com/cloud-platform/search-service
