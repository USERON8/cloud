# 2026-03-13 代码审查与修复记录（后端）

## 范围
- 服务：`user-service`、`order-service`
- 类型：业务正确性 / 一致性 / 权限控制 / 性能与稳定性
- 说明：仅记录本次审查发现的问题与修复策略；测试未执行。

## 总结
- `user-service`：统计缓存刷新失效、商户认证撤销缓存残留、默认地址重置跨事务、用户名批量校验容错、统计查询性能。
- `order-service`：角色越权动作、幂等键跨用户复用、预占库存部分成功未补偿、超时取消覆盖不足、订单号撞号风险。

---

## user-service

### 1) 统计缓存刷新无效（中）
- 风险：`/api/statistics/refresh-cache` 只命中 `@Cacheable`，不会真正刷新已有缓存。
- 修复策略：显式重算并写入 `user:statistics` 缓存（overview、role/status 分布、active:7/30）。
- 变更：`services/user-service/src/main/java/com/cloud/user/service/impl/UserStatisticsServiceImpl.java`
- 状态：已修复。

### 2) 商户认证撤销后缓存残留（中）
- 风险：`remove` 删除记录但未驱逐缓存，查询仍可能返回旧数据。
- 修复策略：新增 `removeByMerchantId`，显式驱逐 `merchantAuthCache` 的 `id:*` 与 `merchantId:*` 缓存；控制器改用新方法。
- 变更：
  - `services/user-service/src/main/java/com/cloud/user/service/MerchantAuthService.java`
  - `services/user-service/src/main/java/com/cloud/user/service/impl/MerchantAuthServiceImpl.java`
  - `services/user-service/src/main/java/com/cloud/user/controller/merchant/MerchantAuthController.java`
- 状态：已修复。

### 3) 默认地址重置跨事务（中）
- 风险：控制层先 reset 默认地址，再保存/更新失败会导致用户无默认地址。
- 修复策略：把默认地址 reset 放到 `save/updateById` 内，保证与持久化同事务；控制层移除重复 reset。
- 变更：
  - `services/user-service/src/main/java/com/cloud/user/service/impl/UserAddressServiceImpl.java`
  - `services/user-service/src/main/java/com/cloud/user/controller/user/UserAddressController.java`
- 状态：已修复。

### 4) 批量用户名存在校验空值异常（低）
- 风险：空用户名触发 `BusinessException`，整批校验失败。
- 修复策略：忽略空值并加容错，异常时回落为 `false`。
- 变更：`services/user-service/src/main/java/com/cloud/user/service/impl/UserAsyncServiceImpl.java`
- 状态：已修复。

### 5) 统计查询 Redis/DB 性能（低/性能）
- 风险：Redis `SCAN` + N 次 `GET`，注册趋势按天多次 DB 计数。
- 修复策略：
  - Redis 使用 `multiGet` 批量取值。
  - 注册趋势改为单次聚合查询并补齐空日期。
- 变更：
  - `services/user-service/src/main/java/com/cloud/user/service/impl/UserStatisticsServiceImpl.java`
  - `services/user-service/src/main/java/com/cloud/user/service/impl/UserAsyncServiceImpl.java`
- 状态：已修复。

---

## order-service

### 1) 角色越权触发订单/售后动作（高）
- 风险：普通用户可直接触发发货/审核/退款等敏感动作。
- 修复策略：控制层按角色限制动作集合（用户/商家/管理员）。
- 变更：`services/order-service/src/main/java/com/cloud/order/controller/OrderController.java`
- 状态：已修复。

### 2) 幂等键跨用户复用（高）
- 风险：不同用户使用相同 `Idempotency-Key` 可能返回他人订单。
- 修复策略：幂等键按 `userId:` 前缀进行隔离；管理员创建订单必须提供 `userId`。
- 变更：
  - `services/order-service/src/main/java/com/cloud/order/controller/OrderController.java`
  - `services/order-service/src/main/java/com/cloud/order/service/impl/OrderPlacementServiceImpl.java`
  - `services/order-service/src/main/java/com/cloud/order/service/impl/OrderServiceImpl.java`
- 状态：已修复。

### 3) 预占库存部分成功无补偿（高）
- 风险：并行预占某 SKU 失败时，已成功预占的库存不释放。
- 修复策略：记录成功预占任务，失败时批量 release 进行补偿。
- 变更：`services/order-service/src/main/java/com/cloud/order/service/impl/OrderPlacementServiceImpl.java`
- 状态：已修复。

### 4) 超时取消覆盖不足（中）
- 风险：只取消 `CREATED`，`STOCK_RESERVED` 订单不处理，预占库存可能长期占用。
- 修复策略：超时扫描包含 `STOCK_RESERVED`。
- 变更：`services/order-service/src/main/java/com/cloud/order/service/impl/OrderTimeoutServiceImpl.java`
- 状态：已修复。

### 5) 订单号生成可能撞号（中）
- 风险：`System.currentTimeMillis` 高并发存在撞号风险，且与另一链路格式不一致。
- 修复策略：主/子订单号统一改为 UUID。
- 变更：`services/order-service/src/main/java/com/cloud/order/service/impl/OrderServiceImpl.java`
- 状态：已修复。

---

## 测试说明
- 本次未执行测试（按当前约定）。
- 建议回归：
  - 下单链路（含并行预占失败场景）。
  - 超时取消任务与库存释放。
  - 售后流程的角色权限。
  - 统计接口与刷新接口。
