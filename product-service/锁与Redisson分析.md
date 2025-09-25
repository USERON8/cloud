# product-service 并发控制与分布式锁（Redisson）分析

> 说明：仅更新子服务内开发文档。本文聚焦商品更新/上下架的并发控制，以乐观锁/条件更新优先，分布式锁为补充。

## 1. 风险与目标
- 风险：并发上下架/多人同时编辑同一商品出现“最后写覆盖（lost update）”。
- 目标：更新携带 version/updated_at 条件，拒绝覆盖旧数据；必要时再对 `productId` 加细粒度锁。

## 2. 乐观锁/条件更新（优先）
- 为 Product 增加 `version` 字段（若未启用），或使用 `updated_at` 条件；更新时带上条件，影响行数=0 则提示“数据已变更，请重试”。

```sql path=null start=null
-- 示例：基于 version 的条件更新
UPDATE product
SET name = #{name}, price = #{price}, status = #{status}, version = version + 1, updated_at = NOW()
WHERE id = #{productId} AND version = #{version};
```

## 3. 分布式锁（仅在极端并发时）
- 锁键：`product:{productId}`；RLock。
- 建议：大多数场景乐观锁足够，不必额外加锁；若多端同时批量更新同一商品且产生竞争，可临时加锁兜底。

```java path=null start=null
RLock lock = redissonClient.getLock("product:" + productId);
if (lock.tryLock(200, 8000, TimeUnit.MILLISECONDS)) {
  try { /* 更新 + 条件校验 */ } finally {
    if (lock.isHeldByCurrentThread()) lock.unlock();
  }
}
```

## 4. 监控与告警
- 指标：更新失败（影响行数=0）占比、锁等待超时（如启用）。

## 5. 实施计划与验收
- 启用 version/updated_at 条件更新；压测在并发下“丢失更新”为 0。
- 如启用锁：锁键 productId，等待/持锁时间可控。

