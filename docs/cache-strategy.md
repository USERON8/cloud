# Cache Strategy And Implementation Notes

This document describes the current cache strategy across the project, including service-level cache contents, TTL choices, consistency rules, and operational notes. The goal is to improve performance and stability without sacrificing data correctness or security.

## Global Principles

- Business correctness first: do not cache money, terminal states, or reconciliation-critical data.
- Layered strategy: when practical, use L1 (local) + L2 (Redis) to reduce cross-network reads.
- Unified consistency rule: use Cache-Aside, write the database first, then evict cache after commit.
- Delayed double delete: for Redis-backed business reads, perform one eviction after commit and one delayed follow-up eviction to reduce stale reads under replica lag or racing rebuilds.
- Hotspot control: use shorter TTLs or refresh mechanisms for hot queries, and add light jitter to avoid cache avalanches.
- Security isolation: the payment service uses Redis mainly for security and idempotency instead of raw performance.

## Shared Cache Defaults

Default TTLs are defined in the shared Redis configuration and grouped by service:

- `user-service`: 10 minutes
- `order-service`: 1 hour
- `search-service`: 10 minutes by default, with feature-specific TTLs ranging from 2 to 10 minutes

Configuration location:

- `common-parent/common-db/src/main/java/com/cloud/common/config/RedisConfig.java`

## Service-Level Strategies

### `product-service`

Goal: keep hot product reads fast while standardizing write-path consistency on post-commit eviction.

- Product detail
  - Cache type: L1 Caffeine + L2 Redis
  - Cached data: product details (`SpuDetail`)
  - TTL: 30 minutes (`1800s` by default)
  - Key: `product:detail:{spuId}`
  - Invalidation: `ProductDetailCacheService.evictAfterCommit(...)` plus delayed second eviction
- Category tree
  - Cache type: Redis
  - Invalidation: `CategoryRedisCacheService.clearAllAfterCommit()` plus delayed second eviction
- Shop reads and statistics
  - Cache type: Redis
  - Invalidation: `ShopRedisCacheService.clearAllAfterCommit()` or `evictByIdAfterCommit(...)` plus delayed second eviction

Implementation:

- `services/product-service/src/main/java/com/cloud/product/service/support/ProductDetailCacheService.java`
- `services/product-service/src/main/java/com/cloud/product/service/cache/CategoryRedisCacheService.java`
- `services/product-service/src/main/java/com/cloud/product/service/cache/ShopRedisCacheService.java`
- `services/product-service/src/main/java/com/cloud/product/service/impl/ProductCatalogServiceImpl.java`
- `services/product-service/src/main/java/com/cloud/product/service/impl/CategoryServiceImpl.java`
- `services/product-service/src/main/java/com/cloud/product/service/impl/ShopServiceImpl.java`
- Configuration: `services/product-service/src/main/resources/application.yml`

### `stock-service`

Goal: keep stock writes authoritative in MySQL while using Redis only as summary cache and entry-side pre-check.

- Cache type: Redis summary cache + Lua pre-check
- Key: `stock:ledger:{skuId}`
- Cached data: aggregated `available`, `locked`, and `sold` totals over `stock_segment`
- Write path: update MySQL first, then evict cache after commit and schedule delayed second eviction
- Pre-check path: Redis Lua is used only for fast availability judgment before the command enters the heavy write path
- Search sync: reservation, confirmation, release, and rollback also trigger product-search sync for affected SKU/SPU pairs

Implementation:

- `services/stock-service/src/main/java/com/cloud/stock/service/support/StockRedisCacheService.java`
- `services/stock-service/src/main/java/com/cloud/stock/service/impl/StockLedgerServiceImpl.java`
- `services/stock-service/src/main/java/com/cloud/stock/service/support/StockSearchSyncService.java`

### `user-service`

Goal: user and merchant reads are frequent, so Redis caching with key-aware invalidation is used.

- Cache type: L2 Redis
- TTL: 10 minutes
- Invalidation: clear both `id` and `username` keys together

Implementation:

- `services/user-service/src/main/java/com/cloud/user/service/impl/UserServiceImpl.java`
- Shared TTL: `common-parent/common-db/src/main/java/com/cloud/common/config/RedisConfig.java`

### `order-service`

Goal: cache only completed order aggregates and avoid inconsistent intermediate states.

- Cache type: L2 Redis
- TTL: 1 hour
- Cached data: aggregate view of completed orders
- Invalidation: clear on order status or after-sale status changes
- Key: `order:aggregate:{mainOrderId}`

Implementation:

- `services/order-service/src/main/java/com/cloud/order/service/support/OrderAggregateCacheService.java`
- `services/order-service/src/main/java/com/cloud/order/service/impl/OrderServiceImpl.java`

### `search-service`

Goal: search and recommendation traffic is hot, so use Redis caches with TTLs tuned by freshness sensitivity and MQ-driven index sync for freshness.

- Cache type: Redis
- L2 TTLs:
  - Search results: 10 minutes
  - Suggestions: 5 minutes
  - Hot keywords: 2 minutes
  - Recommendations: 5 minutes
- Index freshness: product, category, and stock changes publish MQ sync events that rebuild affected Elasticsearch documents

Implementation:

- `services/search-service/src/main/java/com/cloud/search/service/ElasticsearchOptimizedService.java`
- `services/search-service/src/main/java/com/cloud/search/service/ProductDocumentBuildService.java`
- `services/search-service/src/main/java/com/cloud/search/messaging/ProductSyncConsumer.java`
- Configuration: `services/search-service/src/main/resources/application.yml`

### `auth-service`

Goal: keep blacklist entries aligned with JWT lifetime to preserve security guarantees.

- Cache type: Redis
- TTL: aligned with JWT expiration
- Key prefix: `oauth2:blacklist:`

Implementation:

- `services/auth-service/src/main/java/com/cloud/auth/service/TokenBlacklistService.java`
- `common-parent/common-security/src/main/java/com/cloud/common/security/JwtBlacklistTokenValidator.java`

### `payment-service`

Goal: prioritize security and idempotency instead of caching monetary values or terminal states.

- Idempotency key: `pay:idempotent:{orderId}`, TTL 10 minutes
- Lightweight payment result cache: `pay:result:{orderId}`, TTL 10 minutes, storing only the payment order identifier
- Non-terminal payment status cache: `pay:status:{orderId}`, TTL 3 seconds, used to absorb polling traffic
- Rate-limit counter: `pay:rate:{userId}`, 1-minute sliding window
- Third-party token cache: `pay:alipay:token`, TTL 25 minutes
- Terminal states and money values are never cached; status caches are cleared after callback processing or compensation

Implementation:

- `services/payment-service/src/main/java/com/cloud/payment/service/support/PaymentSecurityCacheService.java`
- `services/payment-service/src/main/java/com/cloud/payment/service/impl/PaymentOrderServiceImpl.java`
- `services/payment-service/src/main/java/com/cloud/payment/controller/PaymentOrderController.java`
- `services/payment-service/src/main/java/com/cloud/payment/service/impl/PaymentCompensationServiceImpl.java`
- Configuration: `services/payment-service/src/main/resources/application.yml`

## Key Design Choices

- Write-first policy: persist to the database before touching caches; if cache writes fail, the database remains the source of truth and can rebuild cache state.
- Post-commit invalidation: business caches should not be updated inside the same transaction that mutates MySQL state.
- Delayed double delete: this is the default Redis invalidation pattern for product, category, shop, and stock summary caches.
- Lightweight result caching: payment result caches store identifiers only, not monetary data or terminal outcomes.
- TTL layering for hot paths: hot keywords, suggestions, and result sets use different TTLs to balance freshness and hit rate.
- Security isolation: payment caches support idempotency and rate limiting rather than business state caching.

## Operational Notes

- Prefer configuration overrides when adjusting TTLs instead of hard-coded changes.
- Evaluate cache avalanche risk before changing production TTLs; add jitter or staged rollouts when needed.
- For critical paths such as payment and inventory, track hit rates, delayed-delete failures, rebuild frequency, and stale-read anomalies.
