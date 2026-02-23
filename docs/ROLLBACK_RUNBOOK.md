# 上线回滚手册（最小版）

## 触发条件

- 网关核心入口出现持续 5xx（`/api/orders`、`/api/payments`、`/api/stocks`）。
- 交易事件堆积持续增长，且 10 分钟内无法恢复。
- 支付成功后订单状态无法回写，或库存出现持续脏数据。

## 回滚顺序

1. 冻结流量变更：暂停发布流水线，保持当前实例数不变。
2. 回滚网关配置：恢复 `gateway/src/main/resources/application-route.yml` 到上一版本。
3. 回滚交易服务：顺序 `order-service -> payment-service -> stock-service`。
4. 回滚搜索同步：回滚 `product-service` 与 `search-service` 的同步逻辑。
5. 重启并验证健康检查：确认 `/actuator/health` 全部为 UP。

## 数据一致性修复

1. 检查支付成功但订单未更新：重放 `payment-success` 事件（同一 `eventId` 不会重复处理）。
2. 检查库存预占失败异常：重放 `stock-freeze-failed` 事件触发取消。
3. 检查退款回补异常：重放 `stock-restore` 事件并核对库存。

## 回滚后验收

- 通过网关验证：`/api/orders`、`/api/payments`、`/api/stocks`、`/api/search`。
- 抽样 10 笔订单：状态、支付、库存三方一致。
- 观察 RocketMQ 消费重试与积压指标回落。
