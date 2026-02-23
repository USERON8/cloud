# 核心验收用例（8条）

## 用例列表

1. 网关路由验收  
`/api/query/users`、`/api/product`、`/api/orders`、`/api/payments`、`/api/stocks`、`/api/search` 可达（2xx/4xx，非404）。

2. 下单成功链路  
创建订单后发布 `order-created`，库存预占成功，支付记录为待支付。

3. 支付成功链路  
支付回调后发布 `payment-success`，订单改已支付，库存确认扣减。

4. 库存不足链路  
预占失败发布 `stock-freeze-failed`，订单自动取消，库存无残留预占。

5. 退款通过链路  
退款处理完成后，订单退款状态终态正确，库存正确回补。

6. 事件幂等链路  
重复发送同一 `eventId`，订单/支付/库存不重复变更。

7. 搜索同步链路  
商品新增/更新/删除后，搜索索引最终一致。

8. 回归基线  
`mvn test` 可执行，覆盖关键流程单元/集成冒烟。

## 当前自动化覆盖（本轮）

- 已新增单测：
  - `search-service/src/test/java/com/cloud/search/messaging/SearchSyncEventConsumerTest.java`
  - `product-service/src/test/java/com/cloud/product/messaging/ProductSearchSyncProducerTest.java`
- 当前状态：已覆盖用例 7 的核心消息处理逻辑；其他用例待补集成测试。

## 验收压测接入（Prometheus/Grafana）

- 脚本：`tests/perf/k6/acceptance-cases.js`
- 运行：`tests/perf/k6/run-acceptance.ps1` 或 `tests/perf/k6/run-acceptance.sh`
- 看板：`Cloud Acceptance Load`

说明：

- 脚本包含 8 个 `k6 scenario`，与上方 8 条验收用例一一对应（`case_01` 到 `case_08`）。
- 指标会写入 Prometheus（remote write），并在 Grafana 以 `case_id/case_name/result` 聚合展示。
- 对缺少前置数据（如 `ORDER_ID`、`PAYMENT_ID`、登录 token）的场景，统一计为 `skipped`，避免误判为失败。

## MySQL 全量回灌（搜索场景）

- 接口：`POST /api/product/search-sync/full`
- 文档：`docs/SEARCH_SYNC_MYSQL_TO_ES.md`
- 建议顺序：先 `POST /api/search/rebuild-index`，再触发全量回灌。
