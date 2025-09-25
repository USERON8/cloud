# user-service 并发控制与分布式锁（Redisson）分析

> 说明：仅更新子服务内开发文档。本文聚焦注册与资料更新的一致性控制：唯一约束/乐观锁优先，分布式锁为可选。

## 1. 风险与目标
- 风险：并发注册重名（读后写）；并发更新资料导致丢失更新。
- 目标：以唯一索引与乐观锁/条件更新覆盖大部分场景；锁仅在高并发注册下作为可选降本手段。

## 2. 唯一约束（P0）
- username / phone / email 建立唯一索引，以数据库为准：

```sql path=null start=null
ALTER TABLE `user`
ADD UNIQUE KEY uk_user_username (username),
ADD UNIQUE KEY uk_user_phone (phone),
ADD UNIQUE KEY uk_user_email (email);
```

- 并发注册：依赖唯一键，捕获重复异常并返回“已存在”。

## 3. 乐观锁/条件更新（P0）
- 更新资料携带 version/updated_at 条件，避免丢失更新：

```sql path=null start=null
UPDATE `user`
SET nickname = #{nickname}, status = #{status}, version = version + 1, updated_at = NOW()
WHERE id = #{userId} AND version = #{version};
```

## 4. 分布式锁（可选）
- 锁键：`user:register:{username}`；RLock。
- 适用：高并发注册 + 希望减少长事务回滚成本；但通常唯一键足够，无需锁。

```java path=null start=null
RLock lock = redissonClient.getLock("user:register:" + username);
if (lock.tryLock(150, 5000, TimeUnit.MILLISECONDS)) {
  try { /* 再次检查存在性 -> 插入 */ } finally {
    if (lock.isHeldByCurrentThread()) lock.unlock();
  }
}
```

## 5. 监控与告警
- 指标：唯一键冲突率、更新影响行数=0 占比、锁等待超时（如启用）。

## 6. 实施计划与验收
- P0：唯一键+条件更新落地；并发注册/更新压测通过。
- P1：高并发注册场景按需临时启用锁；指标可控。

