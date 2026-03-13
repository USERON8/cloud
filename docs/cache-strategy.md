# 项目缓存策略与实现思路

本文档说明当前项目的缓存总体策略、各服务缓存内容与 TTL、关键实现与注意事项。目标是提升性能与稳定性，同时保证数据正确性与安全性。

## 总体原则

- 业务优先：金额、终态、对账相关数据不缓存。
- 分层策略：可用时采用 L1（本地）+ L2（Redis），降低跨网络访问成本。
- 以一致性为先：写操作优先更新数据库，再失效/回源刷新缓存。
- 控制热点：热点/高频查询采用更短 TTL 或刷新机制；支持轻微抖动避免雪崩。
- 安全场景分离：支付服务使用 Redis 的目的以安全与幂等为主，而非性能。

## 全局缓存配置（默认 TTL）

默认 TTL 位于公共 Redis 配置，按服务维度设置：

- user-service: 10 分钟
- order-service: 1 小时
- search-service: 默认 10 分钟（实际按功能细分 2–10 分钟）

配置位置：
- `common-parent/common-db/src/main/java/com/cloud/common/config/RedisConfig.java`

## 服务级缓存策略

### product-service（商品服务）

目标：详情查询高频，使用 L1+L2 缓存，减少 DB 压力。

- 缓存类型：L1 Caffeine + L2 Redis
- 关键内容：商品详情（SpuDetail）
- TTL：30 分钟（默认 1800s）
- Key：`product:detail:{spuId}`
- 失效策略：更新商品/状态变更时主动失效

实现位置：
- `services/product-service/src/main/java/com/cloud/product/service/support/ProductDetailCacheService.java`
- `services/product-service/src/main/java/com/cloud/product/service/impl/ProductCatalogServiceImpl.java`
- 配置：`services/product-service/src/main/resources/application.yml`

### stock-service（库存服务）

目标：库存需要原子操作，优先保证正确性，使用 Redis Lua 原子更新，避免 TTL。

- 缓存类型：Redis Hash + Lua 脚本原子更新
- Key：`stock:ledger:{skuId}`
- TTL：不设置（永久）
- 写路径：DB 更新成功后尝试 Lua 原子更新缓存，失败回源刷新

实现位置：
- `services/stock-service/src/main/java/com/cloud/stock/service/support/StockRedisCacheService.java`
- `services/stock-service/src/main/java/com/cloud/stock/service/impl/StockLedgerServiceImpl.java`

### user-service（用户服务）

目标：用户与商家信息读取频繁，Redis 缓存 + 双 key 失效。

- 缓存类型：L2 Redis
- TTL：10 分钟
- 失效策略：按 id 与 username 双 key 同步清理

实现位置：
- `services/user-service/src/main/java/com/cloud/user/service/impl/UserServiceImpl.java`
- 全局 TTL：`common-parent/common-db/src/main/java/com/cloud/common/config/RedisConfig.java`

### order-service（订单服务）

目标：仅缓存已完成订单聚合数据，避免中间态一致性问题。

- 缓存类型：L2 Redis
- TTL：1 小时
- 缓存内容：完成订单的聚合视图
- 失效策略：订单状态变更、售后状态变更时清理
- Key：`order:aggregate:{mainOrderId}`

实现位置：
- `services/order-service/src/main/java/com/cloud/order/service/support/OrderAggregateCacheService.java`
- `services/order-service/src/main/java/com/cloud/order/service/impl/OrderServiceImpl.java`

### search-service（搜索服务）

目标：高频检索与推荐，采用 L1+Caffeine 与 L2/Redis，按热点分层 TTL。

- 缓存类型：L1 Caffeine + L2 Redis
- TTL（L2）
- 搜索结果：10 分钟
- 搜索建议：5 分钟
- 热词：2 分钟
- 推荐：5 分钟

实现位置：
- `services/search-service/src/main/java/com/cloud/search/service/ElasticsearchOptimizedService.java`
- 配置：`services/search-service/src/main/resources/application.yml`

### auth-service（认证服务）

目标：JWT 黑名单同步 token 生命周期，保障安全。

- 缓存类型：Redis
- TTL：与 JWT 过期时间同步
- Key 前缀：`oauth2:blacklist:`

实现位置：
- `services/auth-service/src/main/java/com/cloud/auth/service/TokenBlacklistService.java`
- `common-parent/common-security/src/main/java/com/cloud/common/security/JwtBlacklistTokenValidator.java`

### payment-service（支付服务）

目标：安全与幂等优先，不缓存金额与终态数据。

- Redis 幂等 key：`pay:idempotent:{orderId}`，TTL 10 分钟
- 支付结果缓存：`pay:result:{orderId}`，TTL 10 分钟，仅缓存支付单 ID
- 支付状态（非终态）：`pay:status:{orderId}`，TTL 3 秒，挡轮询
- 限流计数器：`pay:rate:{userId}`，1 分钟滑动窗口
- 第三方 token：`pay:alipay:token`，TTL 25 分钟
- 终态与金额不缓存，回调与补偿完成后清理状态缓存

实现位置：
- `services/payment-service/src/main/java/com/cloud/payment/service/support/PaymentSecurityCacheService.java`
- `services/payment-service/src/main/java/com/cloud/payment/service/impl/PaymentOrderServiceImpl.java`
- `services/payment-service/src/main/java/com/cloud/payment/controller/PaymentOrderController.java`
- `services/payment-service/src/main/java/com/cloud/payment/service/impl/PaymentCompensationServiceImpl.java`
- 配置：`services/payment-service/src/main/resources/application.yml`

## 关键设计思路

- 写优先：先落库再改缓存，缓存失败时以 DB 为准并可回源刷新。
- 轻量结果缓存：支付结果缓存仅存 ID，不存金额与终态。
- 热点分层：热词/建议与结果的 TTL 分层，兼顾新鲜度与命中率。
- 安全场景隔离：支付缓存用于幂等与限流，避免业务金额缓存。

## 注意事项

- 若需要调整 TTL，优先通过配置项覆盖，避免硬编码。
- 线上变更 TTL 需评估雪崩风险，必要时加抖动或分级释放。
- 关键链路（支付、库存）可增加日志或监控，追踪缓存命中与回源比例。
