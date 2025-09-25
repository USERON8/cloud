# payment-service 并发控制与分布式锁（Redisson）分析

> 说明：仅更新子服务内开发文档。本文聚焦“同一订单支付单唯一”“支付完成幂等”，以数据库唯一约束与幂等为主，Redisson 为补充。

## 1. 风险与目标
- 风险：同一订单生成多条支付记录；重复完成支付/重复入账。
- 目标：payment(order_id) 唯一；完成支付幂等去重；必要时对 `orderId` 串行化。

## 2. 数据库唯一约束（P0）
从根源杜绝“同一订单多支付单”：

```sql path=null start=null
ALTER TABLE payment
ADD UNIQUE KEY uk_payment_order_id (order_id);
```

- `createPaymentForOrder`：依赖该唯一键，捕获重复键异常并返回“已存在”。

## 3. 完成支付幂等（P0）
- 以“网关交易号/业务流水号/事件ID”为幂等键落库去重（唯一键或幂等表）。
- 重复消息或重试时，应判定“已处理”并返回成功，而非再次入账。

## 4. 分布式锁（可选兜底）
- 锁键：`payment:order:{orderId}`；RLock。
- 场景：并发完成同一订单支付；或“创建支付单 + 同步第三方”复杂链路需互斥。

```java path=null start=null
RLock lock = redissonClient.getLock("payment:order:" + orderId);
if (lock.tryLock(200, 10000, TimeUnit.MILLISECONDS)) {
  try {
    // 临界区：校验幂等 -> 更新支付状态（条件更新）
  } finally {
    if (lock.isHeldByCurrentThread()) lock.unlock();
  }
} else {
  throw new BusinessException("并发较高，请稍后再试");
}
```

## 5. 监控与告警
- 指标：重复处理命中率、唯一键冲突率、锁等待超时（如启用）。
- 告警：重复/冲突率突增。

## 6. 实施计划
- P0：新增唯一键 uk_payment_order_id；完成支付幂等；
- P1：（可选）在高并发入口对 `orderId` 短锁兜底；
- P2：完善监控与告警。

## 7. 验收
- 压测下不存在同单多支付单；重复回调不重复入账；锁指标可控（如启用）。

