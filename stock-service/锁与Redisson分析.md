# stock-service 并发控制与分布式锁（Redisson）分析

> 说明：遵循项目规则“仅更新子服务内的开发文档”。本文聚焦库存服务的并发风险、数据库优先方案与分布式锁（Redisson）补充方案，并提供实施与验收清单。

## 1. 背景与设计原则
- 优先数据库一致性手段：单条 SQL 的原子条件更新、唯一约束、乐观锁（@Version）。
- 幂等与状态机优先：从源头杜绝重复处理和乱序推进。
- 分布式锁为补充：仅在无法用单条 SQL 原子化或需跨资源强一致时使用 Redisson。
- 锁粒度细、临界区短：productId 维度加锁，避免大锁影响吞吐。

## 2. 关键资源与并发风险
- 方法：stockIn（入库）、stockOut（出库）、reserveStock（冻结/预留）、releaseReservedStock（解冻）。
- 风险：
  - 并发出库/预留导致“可用库存”计算竞态（stockQuantity - frozenQuantity），若“检查+更新”不是原子操作，易超卖。
  - 出库与冻结/解冻并发交错，可能出现可用库存负数或不一致。

## 3. 数据库优先方案（强烈推荐，P0）
将“校验 + 更新”合并为单条 SQL 的条件更新，确保行级原子性：

```sql path=null start=null
-- 出库（扣减可用库存）
UPDATE stock
SET stock_quantity = stock_quantity - #{quantity}, updated_at = NOW()
WHERE id = #{stockId}
  AND (stock_quantity - frozen_quantity) >= #{quantity};

-- 预留（冻结库存）
UPDATE stock
SET frozen_quantity = frozen_quantity + #{quantity}, updated_at = NOW()
WHERE id = #{stockId}
  AND (stock_quantity - frozen_quantity) >= #{quantity};

-- 解冻（释放冻结库存）
UPDATE stock
SET frozen_quantity = frozen_quantity - #{quantity}, updated_at = NOW()
WHERE id = #{stockId}
  AND frozen_quantity >= #{quantity};
```

- 若当前使用了 @Version 乐观锁，需避免与上述条件更新“重复约束”冲突。二选一：
  - 继续使用 @Version（由 MyBatis-Plus 维护版本字段），或
  - 仅保留条件更新 SQL（推荐）。

## 4. 分布式锁补充方案（仅当无法 SQL 原子化时）
- 锁键：`stock:product:{productId}`
- 锁类型：RLock；必要时用公平锁（严格排队）。
- 参数建议：`waitTime=100~300ms`，`leaseTime=5~15s`（结合接口 RT 与事务时长）。

```java path=null start=null
RLock lock = redissonClient.getLock("stock:product:" + productId);
boolean locked = lock.tryLock(200, 10000, TimeUnit.MILLISECONDS);
try {
    if (!locked) throw new BusinessException("并发较高，请稍后再试");
    // 临界区仅保留必须的本地计算 + DB 条件更新
} finally {
    if (lock.isHeldByCurrentThread()) lock.unlock();
}
```

## 5. 幂等与重试
- 建议在入库/出库/冻结/解冻的调用侧引入“操作流水号”（业务幂等键），发生重试时进行去重，避免重复扣减。

## 6. 监控与告警
- 指标：
  - 原子更新失败率（受条件不满足导致的更新影响行数=0）
  - 超卖率（应为 0）
  - 锁等待超时次数/平均等待时长 P95/P99（如启用锁）
- 告警：原子更新失败率或超卖率超阈值、锁超时异常突增。

## 7. 实施计划（stock-service）
- P0：将出库/预留/解冻改为单条 SQL 条件更新；压测验证无超卖。
- P1：对复杂扣减（无法 SQL 原子化）路径加 `productId` 维度 Redisson 锁；控制临界区。
- P2：接入监控与告警；补充幂等去重（可选）。

## 8. 验收清单
- 并发压测下超卖率=0。
- 条件更新 SQL 生效（影响行数=0 时按业务返回“库存不足/并发冲突”）。
- 若启用锁：锁键为 `stock:product:{productId}`，等待/持有时间在可控范围内。

