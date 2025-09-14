# 搜索服务 (search-service)

## 服务概述

搜索服务是电商平台的全文搜索服务，基于Elasticsearch实现商品和订单的高效搜索功能。该服务集成了多级缓存和异步处理能力，提供高性能的搜索体验和丰富的搜索功能。

## 核心功能

1. **商品搜索**
    - 商品信息的全文检索
    - 支持关键词、分类、价格区间等筛选条件
    - 支持搜索结果排序和分页
    - 商品搜索建议和自动完成

2. **订单搜索**
    - 订单信息的全文检索
    - 支持订单号、用户信息等关键词搜索
    - 支持时间区间、状态等筛选条件
    - 订单搜索统计和分析

3. **高级搜索**
    - 支持复杂的组合查询条件
    - 多维度聚合分析
    - 搜索结果高亮显示
    - 个性化搜索推荐

4. **搜索分析**
    - 搜索行为分析
    - 热门关键词统计
    - 搜索转化率分析
    - 搜索性能监控

5. **数据同步**
    - 实时数据同步到Elasticsearch
    - 批量数据导入和更新
    - 索引管理和优化

## 技术栈

- Spring Boot 3.5.3
- Spring Cloud 2025.0.0
- Spring Cloud Alibaba 2023.0.3.3
- Elasticsearch 7.17+
- Redis 多级缓存 (Caffeine + Redis)
- 自定义线程池配置

## 多级缓存功能

### 缓存架构

search服务实现了与user服务相同的多级缓存架构：

- **L1缓存**: Caffeine本地缓存，提供极快的访问速度
- **L2缓存**: Redis分布式缓存，支持集群共享
- **数据源**: Elasticsearch搜索引擎

访问顺序：`L1 Cache(Caffeine) -> L2 Cache(Redis) -> Elasticsearch`

### 缓存注解

#### @MultiLevelCacheable

用于缓存搜索结果，支持SpEL表达式和条件缓存：

```java
@MultiLevelCacheable(
    cacheName = "productSearchCache", 
    key = "#searchRequest.keyword + ':' + #searchRequest.category", 
    expire = 1800,
    timeUnit = TimeUnit.SECONDS
)
public SearchResult<ProductVO> searchProducts(SearchRequest searchRequest) {
    // 方法实现
}
```

#### @MultiLevelCachePut

用于更新缓存，总是执行方法并更新缓存：

```java
@MultiLevelCachePut(
    cacheName = "productSearchCache", 
    key = "#result.searchKey",
    expire = 1800,
    timeUnit = TimeUnit.SECONDS
)
public SearchResult<ProductVO> refreshSearchResult(String keyword) {
    // 方法实现
}
```

#### @MultiLevelCacheEvict

用于删除缓存，支持条件删除和批量删除：

```java
@MultiLevelCacheEvict(
    cacheName = "productSearchCache", 
    allEntries = true
)
public void clearProductSearchCache() {
    // 方法实现
}
```

#### @MultiLevelCaching

用于组合多个缓存操作：

```java
@MultiLevelCaching(
    cacheable = @MultiLevelCacheable(cacheName = "searchCache", key = "#keyword"),
    evict = @MultiLevelCacheEvict(cacheName = "suggestionCache", key = "#keyword")
)
public SearchResult complexSearch(String keyword) {
    // 方法实现
}
```

### 缓存配置

- **缓存键前缀**: `search-cache:`
- **默认过期时间**: 30分钟（1800秒）
- **支持SpEL表达式**: 支持复杂的键生成和条件判断
- **序列化方式**: JSON序列化

### 缓存策略

- **搜索结果缓存**: `search-cache:searchCache:{keyword}`，过期时间30分钟
- **商品搜索缓存**: `search-cache:productSearchCache:{conditions}`，过期时间30分钟
- **订单搜索缓存**: `search-cache:orderSearchCache:{conditions}`，过期时间15分钟
- **搜索建议缓存**: `search-cache:suggestionCache:{keyword}`，过期时间1小时
- **热门关键词缓存**: `search-cache:hotKeywordCache:{date}`，过期时间2小时

## Elasticsearch配置

### ES客户端配置

```java
@Configuration
@EnableElasticsearchRepositories
public class ElasticsearchConfig {
    
    @Bean
    public RestHighLevelClient elasticsearchClient() {
        // 自动配置ES客户端连接
        // 支持认证、超时、连接池等配置
    }
}
```

### 配置参数

```yaml
elasticsearch:
  host: localhost
  port: 9200
  scheme: http
  username: 
  password: 
  connection-timeout: 5000
  socket-timeout: 10000
  max-conn-total: 50
  max-conn-per-route: 10
```

### 索引管理

- **商品索引**: `product_index`
- **订单索引**: `order_index`
- **用户行为索引**: `user_behavior_index`
- **搜索日志索引**: `search_log_index`

## 异步线程池配置

### 线程池类型

search服务配置了7个专用线程池：

#### 1. 搜索查询线程池 (searchQueryExecutor)

```java
核心线程数: max(6, CPU核心数)
最大线程数: CPU核心数 × 4
队列容量: 800
线程名前缀: search-query-
```

#### 2. 搜索索引操作线程池 (searchIndexExecutor)

```java
核心线程数: 4
最大线程数: 16
队列容量: 400
线程名前缀: search-index-
```

#### 3. 数据同步线程池 (dataSyncExecutor)

```java
核心线程数: 3
最大线程数: 12
队列容量: 200
线程名前缀: data-sync-
```

#### 4. 搜索聚合线程池 (searchAggregationExecutor)

```java
核心线程数: 2
最大线程数: 8
队列容量: 100
线程名前缀: search-aggregation-
```

#### 5. 搜索建议线程池 (searchSuggestionExecutor)

```java
核心线程数: 2
最大线程数: 6
队列容量: 150
线程名前缀: search-suggestion-
```

#### 6. 搜索分析线程池 (searchAnalysisExecutor)

```java
核心线程数: 1
最大线程数: 4
队列容量: 80
线程名前缀: search-analysis-
```

#### 7. 通用异步线程池 (searchCommonAsyncExecutor)

```java
继承自BaseAsyncConfig的通用配置
核心线程数: 4
最大线程数: 8
队列容量: 100
线程名前缀: common-async-
```

### 异步方法使用示例

```java
@Async("searchQueryExecutor")
public CompletableFuture<SearchResult> searchAsync(SearchRequest request) {
    SearchResult result = performSearch(request);
    return CompletableFuture.completedFuture(result);
}

@Async("searchIndexExecutor")
public void updateIndex(IndexUpdateRequest request) {
    // 更新ES索引
}

@Async("dataSyncExecutor")  
public void syncDataToES(SyncDataRequest request) {
    // 同步数据到ES
}
```

## 核心接口

### 搜索接口

1. `GET /search/product` - 搜索商品
2. `GET /search/order` - 搜索订单
3. `POST /search/advanced` - 高级搜索
4. `GET /search/suggest/{keyword}` - 搜索建议
5. `GET /search/hot-keywords` - 热门关键词

### 异步接口

1. `GET /search/async/product` - 异步搜索商品
2. `POST /search/async/batch` - 异步批量搜索
3. `GET /search/async/aggregation` - 异步聚合查询

### 索引管理接口

1. `POST /search/index/create` - 创建索引
2. `PUT /search/index/update` - 更新索引
3. `DELETE /search/index/delete` - 删除索引
4. `POST /search/data/sync` - 数据同步

## 使用说明

### 1. 启用多级缓存

在Service类上使用缓存注解：

```java
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    @MultiLevelCacheable(cacheName = "productSearchCache", key = "#request.toKey()")
    @Override
    public SearchResult<ProductVO> searchProducts(SearchRequest request) {
        // 实现逻辑
    }
    
    @MultiLevelCacheEvict(cacheName = "productSearchCache", allEntries = true)
    @Override
    public void clearSearchCache() {
        // 实现逻辑
    }
}
```

### 2. 使用异步处理

在Controller或Service中使用异步方法：

```java
@RestController
@RequiredArgsConstructor
public class SearchController {
    
    private final SearchService searchService;
    
    @GetMapping("/async/product")
    public CompletableFuture<Result<SearchResult>> searchProductsAsync(@RequestParam String keyword) {
        return searchService.searchProductsAsync(keyword)
                .thenApply(result -> Result.success(result));
    }
}
```

### 3. ES索引操作

```java
@Async("searchIndexExecutor")
public void createProductIndex() {
    // 创建商品索引
    elasticsearchTemplate.createIndex(ProductDocument.class);
}

@Async("dataSyncExecutor")
public void syncProducts(List<Product> products) {
    // 批量同步商品数据到ES
    elasticsearchTemplate.save(convertToDocuments(products));
}
```

## 性能优化

### 1. 缓存优化

- 热门搜索关键词使用更长的缓存时间
- 搜索建议数据缓存1小时
- 实时搜索结果缓存30分钟
- 聚合统计数据缓存2小时

### 2. ES优化

- 合理设计索引结构和映射
- 使用合适的分析器和分词器
- 优化查询DSL语句
- 使用ES集群提高性能

### 3. 异步处理优化

- 根据业务特点调整线程池大小
- 使用批量操作提高效率
- 合理设置超时时间

## 监控与日志

- 所有缓存操作都有详细的DEBUG级别日志
- ES操作都有完整的执行日志
- 线程池状态可通过Actuator端点监控
- 异步任务执行情况可通过日志跟踪
- 搜索性能指标实时监控

## 部署说明

```bash
# 编译打包
mvn clean package

# 运行服务
java -jar target/search-service.jar
```

## 注意事项

1. 缓存注解必须在Spring管理的Bean中使用
2. 异步方法不能在同一个类中调用，需要通过依赖注入调用
3. ES连接配置需要在application.yml中正确配置
4. 缓存更新和删除需要与ES数据保持一致性
5. 搜索结果缓存需要考虑数据实时性要求
6. 大量数据同步时需要注意ES集群性能
7. 合理设置缓存过期时间，平衡性能和数据新鲜度
