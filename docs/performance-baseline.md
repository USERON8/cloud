# 性能基线与容量规划（V2）

## 1. 热点数据缓存

### 1.1 当前实现
- `search-service` 已实现 L1（进程内）+ L2（Redis）双层缓存：
  - 热词：`search:hot:list:*`
  - 建议词：`search:suggest:*`
  - 推荐词：`search:recommend:*`
  - 智能检索结果：`search:smart:*`
- `product-service/order-service/stock-service/user-service` 已大量使用 `@Cacheable/@CacheEvict`。

### 1.2 热点缓存准则
- 高频读接口优先缓存，写接口必须同步失效缓存。
- 空结果允许短 TTL 缓存，防止缓存穿透。
- TTL 加随机抖动，避免同一时刻批量过期导致雪崩。

## 2. 超时控制

### 2.1 当前统一配置入口
- `common-module/src/main/resources/application-common.yml`
  - Feign：`FEIGN_CONNECT_TIMEOUT`、`FEIGN_READ_TIMEOUT`
  - Redis：`REDIS_TIMEOUT`
  - Druid：`DB_CONNECTION_TIMEOUT`
  - Tomcat：线程与连接数在 `server.tomcat.*`
- `search-service` 额外配置：`search.optimized.elasticsearch.request-timeout-ms`

### 2.2 建议阈值（默认环境）
- 网关 -> 服务：`1s~2s`
- 服务 -> Redis：`200ms~500ms`
- 服务 -> MySQL 单次查询：`< 300ms`（慢 SQL 单独治理）
- 服务 -> ES：`500ms~800ms`

## 3. 连接、线程、队列大小规划

### 3.1 MySQL 连接池（Druid）
- 统一参数入口：`spring.datasource.druid.*`
- 建议值（单服务单实例）：
  - `initial-size=5`
  - `min-idle=5`
  - `max-active=20`
  - `max-wait=30000`
- 规划公式：
  - `实例总连接数 <= MySQL max_connections * 0.7`
  - 单实例 `max-active = floor(可用连接数 / 服务实例数)`

### 3.2 线程池与队列（@Async）
- 统一入口：`app.async.executors.*`
- 当前已按业务划分线程池：
  - `searchQueryExecutor/searchSuggestionExecutor/searchCacheExecutor`
  - `stockOperationExecutor`
  - 各服务自定义 `order*/payment*/user*` 异步池
- 规划建议：
  - IO 密集：`core = CPU * 2`，`max = CPU * 4`，`queue = 300~1000`
  - CPU 密集：`core = CPU`，`max = CPU + 1`，`queue = 50~200`
  - 拒绝策略默认 `CallerRunsPolicy`，保证可退化不丢任务

## 4. 异步化（MQ / @Async）

### 4.1 MQ 异步
- RocketMQ 已用于订单、库存、支付链路事件。
- 消费端已接入消息幂等（`MessageIdempotencyService`）。

### 4.2 方法异步
- 多服务已启用 `@EnableAsync` 与业务线程池。
- 建议只对“非核心事务路径”做 `@Async`，核心写链路优先本地事务 + MQ 事件。

## 5. MySQL 索引规范

### 5.1 通用规则
- 每张核心表至少包含：
  - 业务唯一键（如 `order_no/payment_no/refund_no`）
  - 软删过滤索引（含 `deleted`）
  - 高频查询复合索引（按 `where` 条件顺序）
- 避免：
  - 在低区分度字段单独建索引（如 `status` 单列）
  - 过多冗余索引导致写放大

### 5.2 本仓库落地状态
- `order_db/payment_db/stock_db/product_db/user_db` 的 `init.sql` 已按主链路补齐：
  - 幂等唯一键（`idempotency_key`）
  - 主从查询索引（`user_id + status + deleted`、`main_order_no + status + deleted` 等）
  - Outbox/Inbox 事件索引（`status + next_retry_at + deleted`）

## 6. 调参优先级

1. 先压测确定瓶颈是 DB / Redis / ES / 网关。
2. 先调超时与线程池，再扩连接池。
3. 连接池最后调大，避免把慢 SQL 放大成雪崩。
4. 索引变更必须先验证执行计划（`EXPLAIN`）再落库。
